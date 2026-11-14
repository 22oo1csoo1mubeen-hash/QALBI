import {
  isTooLate,
  LEASE_DURATION_MS,
  MAX_ALARM_LATENESS_MS,
  MAX_EXECUTION_ATTEMPTS,
  retryDelayMs,
  scheduledRequestId,
} from "./policy.js";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIREBASE_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const MAX_BATCH_SIZE = 20;

let cachedAccessToken = null;
let cachedAccessTokenExpiry = 0;

export default {
  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(runDueSchedules(env));
  },

  // A tiny unauthenticated health endpoint for deployment checks only. It never
  // reads schedule data and cannot trigger delivery.
  async fetch(request) {
    const url = new URL(request.url);
    if (url.pathname === "/health" && request.method === "GET") {
      return new Response("QALBI scheduled alarm worker is healthy\n", {
        status: 200,
        headers: { "content-type": "text/plain; charset=utf-8" },
      });
    }
    return new Response("Not found\n", { status: 404 });
  },
};

export async function runDueSchedules(env, nowMillis = Date.now()) {
  const serviceAccount = parseServiceAccount(env.SERVICE_ACCOUNT_JSON);
  const db = new FirestoreRest(serviceAccount);
  const [scheduled, expiredLeases] = await Promise.all([
    db.queryDue("SCHEDULED", "nextAttemptAt", nowMillis),
    db.queryDue("PROCESSING", "leaseUntil", nowMillis),
  ]);
  const docs = [...scheduled, ...expiredLeases].slice(0, MAX_BATCH_SIZE);

  for (const document of docs) {
    try {
      await processSchedule(db, serviceAccount, document, nowMillis);
    } catch (error) {
      console.error("[QALBI Scheduler] Schedule processing failed", {
        scheduleId: documentId(document),
        code: error.code || "worker_error",
        message: String(error.message || error).slice(0, 160),
      });
      await recordFailure(db, document, nowMillis, error);
    }
  }
  await expireDueWakeRequests(db, nowMillis);
  await notifyPendingSentSchedules(db, serviceAccount, nowMillis);
  await finalizeTerminalScheduledRequests(db, nowMillis);
  await notifyAcknowledgedScheduledRequests(db, serviceAccount, nowMillis);
  return { examined: docs.length };
}

async function processSchedule(db, serviceAccount, document, nowMillis) {
  const scheduleId = documentId(document);
  const data = decodeFields(document.fields || {});
  const scheduledAt = timestampMillis(data.scheduledAt);
  if (data.status === "SCHEDULED") {
    if (scheduledAt > nowMillis) return;
    if (isTooLate(scheduledAt, nowMillis)) {
      await db.patch(document.name, {
        status: "EXPIRED",
        updatedAt: timestamp(nowMillis),
        failureCode: "schedule_missed_delivery_window",
      }, ["status", "updatedAt", "failureCode"], document.updateTime);
      return;
    }
  } else if (data.status === "PROCESSING") {
    const leaseUntil = timestampMillis(data.leaseUntil);
    if (leaseUntil > nowMillis) return;
    if (isTooLate(scheduledAt, nowMillis)) {
      await db.patch(document.name, {
        status: "EXPIRED",
        updatedAt: timestamp(nowMillis),
        failureCode: "schedule_missed_delivery_window",
      }, ["status", "updatedAt", "failureCode"], document.updateTime);
      return;
    }
  } else {
    return;
  }

  const attemptCount = Number(data.attemptCount || 0) + 1;
  const claimed = await db.patch(document.name, {
    status: "PROCESSING",
    attemptCount,
    lastAttemptAt: timestamp(nowMillis),
    leaseUntil: timestamp(nowMillis + LEASE_DURATION_MS),
    updatedAt: timestamp(nowMillis),
  }, ["status", "attemptCount", "lastAttemptAt", "leaseUntil", "updatedAt"], document.updateTime);
  if (!claimed) return; // Another execution or a cancellation won the update-time race.

  const schedule = { ...data, attemptCount };
  const authorization = await validateConnection(db, schedule);
  if (!authorization.allowed) {
    await db.patch(document.name, {
      status: "FAILED",
      failureCode: authorization.reason,
      updatedAt: timestamp(nowMillis),
      leaseUntil: nullValue(),
    }, ["status", "failureCode", "updatedAt", "leaseUntil"]);
    return;
  }

  const deliveryId = scheduledRequestId(scheduleId);
  const requestName = db.documentName("wakeUpRequests", deliveryId);
  const existingRequest = await db.get(requestName);
  if (existingRequest?.fields?.fcmDispatched?.booleanValue === true) {
    await markScheduleSent(db, document.name, nowMillis, deliveryId, data.senderNotificationSent === true);
    return;
  }
  const priorRequestStatus = existingRequest?.fields?.status?.stringValue;
  if (priorRequestStatus && priorRequestStatus !== "PENDING") {
    if (["ACKNOWLEDGED", "DECLINED"].includes(priorRequestStatus)) {
      await markScheduleSent(db, document.name, nowMillis, deliveryId, data.senderNotificationSent === true);
    } else {
      await db.patch(document.name, {
        status: "FAILED",
        failureCode: `delivery_request_${priorRequestStatus.toLowerCase()}`,
        updatedAt: timestamp(nowMillis),
        leaseUntil: nullValue(),
      }, ["status", "failureCode", "updatedAt", "leaseUntil"]);
    }
    return;
  }

  const now = nowMillis;
  const expiry = now + MAX_ALARM_LATENESS_MS;

  const [senderProfile, receiverProfile, tokenDocuments] = await Promise.all([
    db.get(db.documentName("publicProfiles", schedule.senderUid)),
    db.get(db.documentName("publicProfiles", schedule.receiverUid)),
    db.list(db.documentName("users", schedule.receiverUid, "fcmTokens")),
  ]);
  const sender = decodeFields(senderProfile?.fields || {});
  const receiver = decodeFields(receiverProfile?.fields || {});
  const tokens = tokenDocuments
    .map((tokenDocument) => decodeFields(tokenDocument.fields || {}).token)
    .filter((token) => typeof token === "string" && token.length > 20);

  if (tokens.length === 0) {
    throw Object.assign(new Error("recipient_has_no_registered_fcm_tokens"), { code: "no_fcm_tokens" });
  }

  const requestData = {
    requestId: deliveryId,
    scheduleId,
    deliverySource: "CLOUDFLARE_SCHEDULED_ALARM",
    senderUid: schedule.senderUid,
    senderDisplayName: sender.displayName || schedule.senderDisplayName || "Your friend",
    senderUsername: sender.username || schedule.senderUsername || "friend",
    receiverUid: schedule.receiverUid,
    receiverDisplayName: receiver.displayName || schedule.receiverDisplayName || "Friend",
    receiverUsername: receiver.username || schedule.receiverUsername || "friend",
    connectionId: schedule.connectionId,
    customMessage: (schedule.message || "").slice(0, 160),
    status: "PENDING",
    senderAcknowledgementSent: false,
    scheduleFinalized: false,
    createdAt: timestamp(now),
    expiresAt: timestamp(expiry),
    updatedAt: timestamp(now),
    schemaVersion: 1,
  };

  await db.createIfMissing(requestName, requestData);
  const results = await sendFcm(serviceAccount, tokens, {
    requestId: deliveryId,
    scheduleId,
    senderUid: schedule.senderUid,
    senderDisplayName: requestData.senderDisplayName,
    senderUsername: requestData.senderUsername,
    receiverUid: schedule.receiverUid,
    type: "WAKE_UP_REQUEST",
    createdAt: String(now),
    expiresAt: String(expiry),
    customMessage: requestData.customMessage,
  }, expiry - now);
  const successCount = results.filter((result) => result.ok).length;
  if (successCount === 0) {
    const error = Object.assign(new Error("fcm_rejected_all_devices"), { code: "fcm_send_failed" });
    throw error;
  }

  // Persist the stable request's accepted state before the schedule state. If a
  // later cron recovers a lease after a worker crash, it will not resend it.
  await db.patch(requestName, {
    fcmDispatched: true,
    fcmDispatchState: "ACCEPTED_BY_FCM",
    fcmAcceptedCount: successCount,
    fcmFailureCount: results.length - successCount,
    fcmAttemptedAt: timestamp(nowMillis),
  }, ["fcmDispatched", "fcmDispatchState", "fcmAcceptedCount", "fcmFailureCount", "fcmAttemptedAt"]);
  await markScheduleSent(db, document.name, nowMillis, deliveryId, data.senderNotificationSent === true);
}

async function validateConnection(db, schedule) {
  if (!schedule.senderUid || !schedule.receiverUid || !schedule.connectionId ||
      schedule.senderUid === schedule.receiverUid) {
    return { allowed: false, reason: "invalid_participants" };
  }
  const [connection, senderBlock, receiverBlock] = await Promise.all([
    db.get(db.documentName("connections", schedule.connectionId)),
    db.get(db.documentName("userBlocks", schedule.senderUid, "blockedUsers", schedule.receiverUid)),
    db.get(db.documentName("userBlocks", schedule.receiverUid, "blockedUsers", schedule.senderUid)),
  ]);
  const connectionData = decodeFields(connection?.fields || {});
  const members = connectionData.memberUids || [];
  if (!connection || connectionData.status !== "CONNECTED" || members.length !== 2 ||
      !members.includes(schedule.senderUid) || !members.includes(schedule.receiverUid)) {
    return { allowed: false, reason: "connection_not_active" };
  }
  if (senderBlock || receiverBlock) return { allowed: false, reason: "participant_blocked" };
  return { allowed: true };
}

async function sendFcm(serviceAccount, tokens, data, ttlMillis) {
  const accessToken = await getAccessToken(serviceAccount);
  const url = `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(serviceAccount.project_id)}/messages:send`;
  const sends = await Promise.allSettled(tokens.map(async (token) => {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          data: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, String(value)])),
          android: { priority: "HIGH", ttl: `${Math.max(1, Math.floor(ttlMillis / 1000))}s` },
        },
      }),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      console.warn("[QALBI Scheduler] FCM rejected a device token", {
        status: response.status,
        code: body.error?.status || "fcm_error",
      });
    }
    return { ok: response.ok, body };
  }));
  return sends.map((send) => send.status === "fulfilled"
    ? send.value
    : { ok: false, body: { error: { status: "network_error" } } });
}

async function recordFailure(db, document, nowMillis, error) {
  const data = decodeFields(document.fields || {});
  const attempts = Math.max(1, Number(data.attemptCount || 0) + 1);
  const scheduleAt = timestampMillis(data.scheduledAt);
  if (isTooLate(scheduleAt, nowMillis)) {
    await db.patch(document.name, {
      status: "EXPIRED",
      failureCode: String(error.code || "schedule_execution_failed").slice(0, 120),
      updatedAt: timestamp(Date.now()),
      leaseUntil: nullValue(),
    }, ["status", "failureCode", "updatedAt", "leaseUntil"]).catch(() => {});
    return;
  }
  if (attempts >= MAX_EXECUTION_ATTEMPTS) {
    await db.patch(document.name, {
      status: "FAILED",
      failureCode: String(error.code || "retry_exhausted").slice(0, 120),
      updatedAt: timestamp(Date.now()),
      leaseUntil: nullValue(),
    }, ["status", "failureCode", "updatedAt", "leaseUntil"]).catch(() => {});
    return;
  }
  const nextAttempt = Date.now() + retryDelayMs(attempts);
  await db.patch(document.name, {
    status: "SCHEDULED",
    nextAttemptAt: timestamp(nextAttempt),
    failureCode: String(error.code || "temporary_execution_failure").slice(0, 120),
    updatedAt: timestamp(Date.now()),
    leaseUntil: nullValue(),
  }, ["status", "nextAttemptAt", "failureCode", "updatedAt", "leaseUntil"]).catch(() => {});
}

async function markScheduleSent(db, scheduleName, nowMillis, deliveryId, senderNotificationSent = false) {
  await db.patch(scheduleName, {
    status: "SENT",
    sentAt: timestamp(nowMillis),
    deliveryRequestId: deliveryId,
    updatedAt: timestamp(nowMillis),
    leaseUntil: nullValue(),
    failureCode: nullValue(),
    senderNotificationSent,
  }, ["status", "sentAt", "deliveryRequestId", "updatedAt", "leaseUntil", "failureCode", "senderNotificationSent"]);
}

async function expireDueWakeRequests(db, nowMillis) {
  const dueRequests = await db.queryDue("PENDING", "expiresAt", nowMillis, "wakeUpRequests");
  for (const document of dueRequests) {
    const data = decodeFields(document.fields || {});
    if (data.status !== "PENDING" || timestampMillis(data.expiresAt) > nowMillis) continue;
    await db.patch(document.name, {
      status: "EXPIRED",
      expiredAt: timestamp(nowMillis),
      updatedAt: timestamp(nowMillis),
    }, ["status", "expiredAt", "updatedAt"], document.updateTime);
  }
}

async function finalizeTerminalScheduledRequests(db, nowMillis) {
  for (const status of ["ACKNOWLEDGED", "DECLINED", "CANCELLED", "EXPIRED"]) {
    const requests = await db.queryFlagged("wakeUpRequests", status, "scheduleFinalized", false);
    for (const document of requests) {
      const data = decodeFields(document.fields || {});
      if (data.deliverySource !== "CLOUDFLARE_SCHEDULED_ALARM" || !data.scheduleId) continue;

      const scheduleStatus = status === "CANCELLED" ? "CANCELLED" : status;
      const scheduleName = db.documentName("scheduledAlarms", data.scheduleId);
      const scheduleDocument = await db.get(scheduleName);
      if (!scheduleDocument) continue;
      const scheduleData = decodeFields(scheduleDocument.fields || {});
      if (scheduleData.status !== scheduleStatus) {
        if (scheduleData.status !== "SENT") continue;
        const finalized = await db.patch(scheduleName, {
          status: scheduleStatus,
          completedAt: timestamp(nowMillis),
          updatedAt: timestamp(nowMillis),
        }, ["status", "completedAt", "updatedAt"], scheduleDocument.updateTime);
        if (!finalized) continue;
      }

      const currentRequest = await db.get(document.name);
      if (!currentRequest || decodeFields(currentRequest.fields || {}).scheduleFinalized === true) continue;
      await db.patch(document.name, {
        scheduleFinalized: true,
        scheduleFinalizedAt: timestamp(nowMillis),
      }, ["scheduleFinalized", "scheduleFinalizedAt"], currentRequest.updateTime);
    }
  }
}

async function notifyPendingSentSchedules(db, serviceAccount, nowMillis) {
  const schedules = await db.queryFlagged("scheduledAlarms", "SENT", "senderNotificationSent", false);
  for (const document of schedules) {
    const schedule = decodeFields(document.fields || {});
    if (!schedule.senderUid || !schedule.receiverUid || !schedule.deliveryRequestId) continue;
    const tokenDocuments = await db.list(db.documentName("users", schedule.senderUid, "fcmTokens"));
    const tokens = tokenDocuments
      .map((tokenDocument) => decodeFields(tokenDocument.fields || {}).token)
      .filter((token) => typeof token === "string" && token.length > 20);
    if (tokens.length === 0) continue;

    const results = await sendFcm(serviceAccount, tokens, {
      type: "SCHEDULED_ALARM_SENT",
      requestId: schedule.deliveryRequestId,
      scheduleId: schedule.scheduleId || documentId(document),
      senderUid: schedule.senderUid,
      receiverUid: schedule.receiverUid,
      receiverDisplayName: schedule.receiverDisplayName || "your friend",
      createdAt: String(nowMillis),
    }, 24 * 60 * 60 * 1000);
    if (!results.some((result) => result.ok)) continue;

    await db.patch(document.name, {
      senderNotificationSent: true,
      senderNotificationSentAt: timestamp(nowMillis),
      updatedAt: timestamp(nowMillis),
    }, ["senderNotificationSent", "senderNotificationSentAt", "updatedAt"], document.updateTime);
  }
}

async function notifyAcknowledgedScheduledRequests(db, serviceAccount, nowMillis) {
  const requests = await db.queryFlagged("wakeUpRequests", "ACKNOWLEDGED", "senderAcknowledgementSent", false);
  for (const document of requests) {
    const request = decodeFields(document.fields || {});
    if (request.deliverySource !== "CLOUDFLARE_SCHEDULED_ALARM" || !request.scheduleId || !request.senderUid) continue;
    const tokenDocuments = await db.list(db.documentName("users", request.senderUid, "fcmTokens"));
    const tokens = tokenDocuments
      .map((tokenDocument) => decodeFields(tokenDocument.fields || {}).token)
      .filter((token) => typeof token === "string" && token.length > 20);
    if (tokens.length === 0) {
      await db.patch(document.name, {
        senderAcknowledgementSent: true,
        senderAcknowledgementSkippedAt: timestamp(nowMillis),
      }, ["senderAcknowledgementSent", "senderAcknowledgementSkippedAt"], document.updateTime);
      continue;
    }

    const results = await sendFcm(serviceAccount, tokens, {
      type: "SCHEDULED_ALARM_ACKNOWLEDGED",
      requestId: request.requestId || documentId(document),
      scheduleId: request.scheduleId,
      senderUid: request.senderUid,
      receiverUid: request.receiverUid,
      receiverDisplayName: request.receiverDisplayName || "Your friend",
      acknowledgedAt: String(nowMillis),
    }, 24 * 60 * 60 * 1000);
    if (!results.some((result) => result.ok)) continue;

    await db.patch(document.name, {
      senderAcknowledgementSent: true,
      senderAcknowledgementSentAt: timestamp(nowMillis),
    }, ["senderAcknowledgementSent", "senderAcknowledgementSentAt"], document.updateTime);
  }
}

class FirestoreRest {
  constructor(serviceAccount) {
    this.serviceAccount = serviceAccount;
    this.base = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(serviceAccount.project_id)}/databases/(default)/documents`;
  }

  documentName(...parts) {
    return `projects/${this.serviceAccount.project_id}/databases/(default)/documents/${parts.join("/")}`;
  }

  async request(url, options = {}, allowNotFound = false) {
    const accessToken = await getAccessToken(this.serviceAccount);
    const response = await fetch(url, {
      ...options,
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json",
        ...(options.headers || {}),
      },
    });
    if (allowNotFound && response.status === 404) return null;
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw Object.assign(new Error(body.error?.message || `Firestore HTTP ${response.status}`), {
        code: body.error?.status || `firestore_${response.status}`,
      });
    }
    return body;
  }

  async get(name) {
    return this.request(`${this.base}/${name.slice(name.indexOf("documents/") + 10)}`, {}, true);
  }

  async list(collectionName) {
    const body = await this.request(`${this.base}/${collectionName.slice(collectionName.indexOf("documents/") + 10)}?pageSize=100`);
    return body.documents || [];
  }

  async queryDue(status, dueField, nowMillis, collectionId = "scheduledAlarms") {
    const query = {
      structuredQuery: {
        from: [{ collectionId }],
        where: { compositeFilter: { op: "AND", filters: [
          { fieldFilter: { field: { fieldPath: "status" }, op: "EQUAL", value: { stringValue: status } } },
          { fieldFilter: { field: { fieldPath: dueField }, op: "LESS_THAN_OR_EQUAL", value: { timestampValue: timestamp(nowMillis).timestampValue } } },
        ] } },
        orderBy: [{ field: { fieldPath: dueField }, direction: "ASCENDING" }],
        limit: MAX_BATCH_SIZE,
      },
    };
    const rows = await this.request(`${this.base}:runQuery`, { method: "POST", body: JSON.stringify(query) });
    return rows.map((row) => row.document).filter(Boolean);
  }

  async queryFlagged(collectionId, status, flagField, flagValue) {
    const query = {
      structuredQuery: {
        from: [{ collectionId }],
        where: { compositeFilter: { op: "AND", filters: [
          { fieldFilter: { field: { fieldPath: "status" }, op: "EQUAL", value: { stringValue: status } } },
          { fieldFilter: { field: { fieldPath: flagField }, op: "EQUAL", value: { booleanValue: flagValue } } },
        ] } },
        limit: MAX_BATCH_SIZE,
      },
    };
    const rows = await this.request(`${this.base}:runQuery`, { method: "POST", body: JSON.stringify(query) });
    return rows.map((row) => row.document).filter(Boolean);
  }

  async patch(name, data, fields, updateTime) {
    const url = new URL(`${this.base}/${name.slice(name.indexOf("documents/") + 10)}`);
    for (const field of fields) url.searchParams.append("updateMask.fieldPaths", field);
    if (updateTime) url.searchParams.set("currentDocument.updateTime", updateTime);
    try {
      return await this.request(url.toString(), {
        method: "PATCH",
        body: JSON.stringify({ name, fields: encodeFields(data) }),
      });
    } catch (error) {
      if (error.code === "FAILED_PRECONDITION" || error.code === "ABORTED") return null;
      throw error;
    }
  }

  async createIfMissing(name, data) {
    const path = name.slice(name.indexOf("documents/") + 10);
    const [collectionId, documentId] = [path.slice(0, path.lastIndexOf("/")), path.slice(path.lastIndexOf("/") + 1)];
    const url = `${this.base}/${collectionId}?documentId=${encodeURIComponent(documentId)}`;
    try {
      return await this.request(url, { method: "POST", body: JSON.stringify({ fields: encodeFields(data) }) });
    } catch (error) {
      if (error.code === "ALREADY_EXISTS") {
        const existing = await this.get(name);
        if (existing?.fields?.scheduleId?.stringValue === data.scheduleId) return existing;
      }
      throw error;
    }
  }
}

async function getAccessToken(serviceAccount) {
  const nowSeconds = Math.floor(Date.now() / 1000);
  if (cachedAccessToken && cachedAccessTokenExpiry > nowSeconds + 60) return cachedAccessToken;
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: `${FIRESTORE_SCOPE} ${FIREBASE_MESSAGING_SCOPE}`,
    aud: GOOGLE_TOKEN_URL,
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  }));
  const unsigned = `${header}.${claims}`;
  const key = await importPrivateKey(serviceAccount.private_key);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const assertion = `${unsigned}.${base64UrlBytes(new Uint8Array(signature))}`;
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || !body.access_token) {
    throw Object.assign(new Error(body.error_description || body.error || "Google OAuth token exchange failed"), {
      code: "service_account_auth_failed",
    });
  }
  cachedAccessToken = body.access_token;
  cachedAccessTokenExpiry = nowSeconds + Number(body.expires_in || 3600);
  return cachedAccessToken;
}

async function importPrivateKey(pem) {
  const binary = atob(pem.replace(/-----[^-]+-----/g, "").replace(/\s/g, ""));
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  return crypto.subtle.importKey("pkcs8", bytes, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
}

function parseServiceAccount(value) {
  if (!value) throw Object.assign(new Error("SERVICE_ACCOUNT_JSON secret is missing"), { code: "missing_service_account" });
  const account = typeof value === "string" ? JSON.parse(value) : value;
  if (!account.project_id || !account.client_email || !account.private_key) {
    throw Object.assign(new Error("Service-account JSON is missing project_id, client_email, or private_key"), { code: "invalid_service_account" });
  }
  return account;
}

function encodeFields(data) {
  const fields = {};
  for (const [key, value] of Object.entries(data)) fields[key] = encodeValue(value);
  return fields;
}

function encodeValue(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeValue) } };
  if (typeof value === "object" && ("timestampValue" in value || "nullValue" in value)) return value;
  if (typeof value === "object") return { mapValue: { fields: encodeFields(value) } };
  return { nullValue: null };
}

function decodeFields(fields) {
  return Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, decodeValue(value)]));
}

function decodeValue(value) {
  if ("stringValue" in value) return value.stringValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("timestampValue" in value) return value.timestampValue;
  if ("nullValue" in value) return null;
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(decodeValue);
  if ("mapValue" in value) return decodeFields(value.mapValue.fields || {});
  return undefined;
}

function timestamp(value) {
  return { timestampValue: new Date(value).toISOString() };
}

function nullValue() {
  return { nullValue: null };
}

function timestampMillis(value) {
  if (typeof value === "number") return value;
  const parsed = Date.parse(value || "");
  return Number.isFinite(parsed) ? parsed : NaN;
}

function documentId(document) {
  return String(document?.name || "").split("/").pop() || "";
}

function base64Url(value) {
  return base64UrlBytes(new TextEncoder().encode(value));
}

function base64UrlBytes(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
