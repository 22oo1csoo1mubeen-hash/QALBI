'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const {
    MAX_REQUEST_EXPIRY_AHEAD_MS,
    MAX_REQUEST_TTL_MS,
    isExpiryWithinHorizon,
    timestampToMillis,
    remainingTtlMillis,
} = require('./dispatch-policy');

test('converts Firestore timestamps and epoch milliseconds', () => {
    assert.equal(timestampToMillis({ toMillis: () => 1234 }), 1234);
    assert.equal(timestampToMillis(5678), 5678);
    assert.equal(timestampToMillis('9012'), 9012);
});

test('rejects missing or malformed expiry values', () => {
    assert.equal(timestampToMillis(undefined), null);
    assert.equal(timestampToMillis('not-a-time'), null);
});

test('TTL never extends beyond the request expiration', () => {
    assert.equal(remainingTtlMillis(20_000, 15_000), 5_000);
    assert.equal(remainingTtlMillis(15_000, 15_000), 0);
    assert.equal(remainingTtlMillis(10_000, 15_000), 0);
});

test('accepts Firestore-allowed expiry clock skew but caps FCM delivery at 15 minutes', () => {
    const now = 1_800_000_000_000;
    const expiry = now + 20 * 60 * 1000;

    assert.equal(isExpiryWithinHorizon(expiry, now, MAX_REQUEST_EXPIRY_AHEAD_MS), true);
    assert.equal(
        isExpiryWithinHorizon(now + MAX_REQUEST_EXPIRY_AHEAD_MS + 1, now, MAX_REQUEST_EXPIRY_AHEAD_MS),
        false,
    );
    assert.equal(remainingTtlMillis(expiry, now, MAX_REQUEST_TTL_MS), MAX_REQUEST_TTL_MS);
});
