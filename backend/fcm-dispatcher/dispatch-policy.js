'use strict';

function timestampToMillis(value) {
    if (value && typeof value.toMillis === 'function') return value.toMillis();
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Number(value))) return Number(value);
    return null;
}

function remainingTtlMillis(expiresAtMillis, nowMillis) {
    if (!Number.isFinite(expiresAtMillis) || !Number.isFinite(nowMillis)) return 0;
    return Math.max(0, expiresAtMillis - nowMillis);
}

module.exports = { timestampToMillis, remainingTtlMillis };
