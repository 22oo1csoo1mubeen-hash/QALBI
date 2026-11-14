import test from "node:test";
import assert from "node:assert/strict";
import {
  isTooLate,
  MAX_ALARM_LATENESS_MS,
  MAX_EXECUTION_ATTEMPTS,
  retryDelayMs,
  scheduledRequestId,
  shouldRetry,
} from "../src/policy.js";

test("schedule is expired only after the 15-minute delivery window", () => {
  const due = 1_800_000_000_000;
  assert.equal(isTooLate(due, due + MAX_ALARM_LATENESS_MS - 1), false);
  assert.equal(isTooLate(due, due + MAX_ALARM_LATENESS_MS), true);
});

test("retry delays are bounded and increase gradually", () => {
  assert.deepEqual([1, 2, 3, 4, 5].map(retryDelayMs), [30_000, 60_000, 120_000, 300_000, 300_000]);
  assert.equal(shouldRetry(MAX_EXECUTION_ATTEMPTS - 1), true);
  assert.equal(shouldRetry(MAX_EXECUTION_ATTEMPTS), false);
});

test("request id is stable across scheduler retries", () => {
  assert.equal(scheduledRequestId("a1b2"), "scheduled_a1b2");
  assert.equal(scheduledRequestId("a1b2"), scheduledRequestId("a1b2"));
});
