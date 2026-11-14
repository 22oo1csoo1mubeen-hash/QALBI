import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { runDueSchedules } from "../src/worker.js";

const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const serviceAccount = {
  project_id: "qalbi-test-project",
  client_email: "scheduler@qalbi-test-project.iam.gserviceaccount.com",
  private_key: privateKey.export({ type: "pkcs8", format: "pem" }).toString(),
};

function fsString(value) { return { stringValue: value }; }
function fsTime(value) { return { timestampValue: new Date(value).toISOString() }; }
function fsInteger(value) { return { integerValue: String(value) }; }
function fsArray(values) { return { arrayValue: { values: values.map(fsString) } }; }
function response(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

function makeFirestoreDocument(name, fields, updateTime = "2026-11-14T00:00:00.000Z") {
  return { name, fields, updateTime };
}

function createMockFetch({ connectionStatus = "CONNECTED" } = {}) {
  const dueMillis = Date.parse("2026-11-14T01:00:00.000Z");
  const scheduleName = "projects/qalbi-test-project/databases/(default)/documents/scheduledAlarms/schedule-123";
  const requestName = "projects/qalbi-test-project/databases/(default)/documents/wakeUpRequests/scheduled_schedule-123";
  const schedule = makeFirestoreDocument(scheduleName, {
    scheduleId: fsString("schedule-123"),
    senderUid: fsString("sender-1"),
    senderDisplayName: fsString("Sender"),
    senderUsername: fsString("sender"),
    receiverUid: fsString("receiver-1"),
    receiverDisplayName: fsString("Receiver"),
    receiverUsername: fsString("receiver"),
    connectionId: fsString("connection-1"),
    message: fsString("Wake up! ❤️"),
    scheduledAt: fsTime(dueMillis),
    timeZoneId: fsString("Asia/Kolkata"),
    status: fsString("SCHEDULED"),
    attemptCount: fsInteger(0),
    nextAttemptAt: fsTime(dueMillis),
  });
  const state = {
    schedule,
    request: null,
    connectionStatus,
    fcmMessages: [],
    createdRequests: 0,
  };

  const mockFetch = async (input, options = {}) => {
    const url = new URL(input);
    if (url.hostname === "oauth2.googleapis.com") return response({ access_token: "test-token", expires_in: 3600 });
    if (url.hostname === "fcm.googleapis.com") {
      state.fcmMessages.push(JSON.parse(options.body));
      return response({ name: "projects/qalbi-test-project/messages/accepted" });
    }

    if (url.pathname.endsWith(":runQuery")) {
      const body = JSON.parse(options.body);
      const filters = body.structuredQuery.where.compositeFilter.filters;
      const status = filters.find((filter) => filter.fieldFilter.field.fieldPath === "status").fieldFilter.value.stringValue;
      return response(status === "SCHEDULED" && state.schedule.fields.status.stringValue === "SCHEDULED"
        ? [{ document: state.schedule }]
        : []);
    }

    if (url.pathname.endsWith("/connections/connection-1")) {
      return response(makeFirestoreDocument(url.pathname, {
        status: fsString(state.connectionStatus),
        memberUids: fsArray(["sender-1", "receiver-1"]),
      }));
    }
    if (url.pathname.includes("/userBlocks/")) return response({ error: { status: "NOT_FOUND" } }, 404);
    if (url.pathname.endsWith("/publicProfiles/sender-1")) {
      return response(makeFirestoreDocument(url.pathname, { displayName: fsString("Sender"), username: fsString("sender") }));
    }
    if (url.pathname.endsWith("/publicProfiles/receiver-1")) {
      return response(makeFirestoreDocument(url.pathname, { displayName: fsString("Receiver"), username: fsString("receiver") }));
    }
    if (url.pathname.endsWith("/users/receiver-1/fcmTokens")) {
      return response({ documents: [makeFirestoreDocument(`${url.pathname}/token-1`, { token: fsString("registered-fcm-token-1234567890") })] });
    }
    if (url.pathname.endsWith("/wakeUpRequests/scheduled_schedule-123") && (!options.method || options.method === "GET")) {
      return state.request ? response(state.request) : response({ error: { status: "NOT_FOUND" } }, 404);
    }
    if (url.pathname.endsWith("/wakeUpRequests") && options.method === "POST") {
      const requestBody = JSON.parse(options.body);
      assert.equal("name" in requestBody, false);
      state.createdRequests += 1;
      state.request = makeFirestoreDocument(requestName, requestBody.fields);
      return response(state.request);
    }
    if (options.method === "PATCH") {
      const documentPath = url.pathname.split("/documents/")[1];
      const fields = JSON.parse(options.body).fields;
      const target = documentPath.startsWith("scheduledAlarms/") ? state.schedule : state.request;
      for (const [key, value] of Object.entries(fields)) target.fields[key] = value;
      target.updateTime = "2026-11-14T01:00:02.000Z";
      return response(target);
    }
    throw new Error(`Unexpected mocked Google API request: ${options.method || "GET"} ${url.pathname}`);
  };
  return { mockFetch, state, dueMillis };
}

test("due schedule uses the existing request format and sends once with a stable ID", async () => {
  const originalFetch = globalThis.fetch;
  const { mockFetch, state, dueMillis } = createMockFetch();
  globalThis.fetch = mockFetch;
  try {
    const result = await runDueSchedules({ SERVICE_ACCOUNT_JSON: JSON.stringify(serviceAccount) }, dueMillis + 1_000);
    assert.equal(result.examined, 1);
    assert.equal(state.fcmMessages.length, 1);
    assert.equal(state.createdRequests, 1);
    assert.equal(state.fcmMessages[0].message.data.requestId, "scheduled_schedule-123");
    assert.equal(state.fcmMessages[0].message.data.customMessage, "Wake up! ❤️");
    assert.equal(state.request.fields.deliverySource.stringValue, "CLOUDFLARE_SCHEDULED_ALARM");
    assert.equal(state.request.fields.fcmDispatched.booleanValue, true);
    assert.equal(state.schedule.fields.status.stringValue, "SENT");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("schedule is not sent after the connection is disconnected", async () => {
  const originalFetch = globalThis.fetch;
  const { mockFetch, state, dueMillis } = createMockFetch({ connectionStatus: "DISCONNECTED" });
  globalThis.fetch = mockFetch;
  try {
    await runDueSchedules({ SERVICE_ACCOUNT_JSON: JSON.stringify(serviceAccount) }, dueMillis + 1_000);
    assert.equal(state.fcmMessages.length, 0);
    assert.equal(state.createdRequests, 0);
    assert.equal(state.schedule.fields.status.stringValue, "FAILED");
    assert.equal(state.schedule.fields.failureCode.stringValue, "connection_not_active");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
