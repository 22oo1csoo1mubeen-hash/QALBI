'use strict';

const MAX_REQUEST_TTL_MS = 15 * 60 * 1000;
const MAX_REQUEST_EXPIRY_AHEAD_MS = 30 * 60 * 1000;

function timestampToMillis(value) {
    if (value && typeof value.toMillis === 'function') return value.toMillis();
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Number(value))) return Number(value);
    return null;
}

function isExpiryWithinHorizon(expiresAtMillis, nowMillis, maxAheadMillis) {
    return Number.isFinite(expiresAtMillis) &&
        Number.isFinite(nowMillis) &&
        expiresAtMillis > nowMillis &&
        expiresAtMillis - nowMillis <= maxAheadMillis;
}

function remainingTtlMillis(expiresAtMillis, nowMillis, maxTtlMillis = Number.POSITIVE_INFINITY) {
    if (!Number.isFinite(expiresAtMillis) || !Number.isFinite(nowMillis)) return 0;
    const remaining = Math.max(0, expiresAtMillis - nowMillis);
    return Number.isFinite(maxTtlMillis) ? Math.min(remaining, Math.max(0, maxTtlMillis)) : remaining;
}

module.exports = {
    MAX_REQUEST_EXPIRY_AHEAD_MS,
    MAX_REQUEST_TTL_MS,
    isExpiryWithinHorizon,
    timestampToMillis,
    remainingTtlMillis,
};
