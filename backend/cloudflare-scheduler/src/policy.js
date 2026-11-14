export const MAX_ALARM_LATENESS_MS = 15 * 60 * 1000;
export const MAX_EXECUTION_ATTEMPTS = 5;
export const LEASE_DURATION_MS = 2 * 60 * 1000;

export function retryDelayMs(attemptCount) {
  const delays = [30_000, 60_000, 2 * 60_000, 5 * 60_000];
  return delays[Math.min(Math.max(attemptCount - 1, 0), delays.length - 1)];
}

export function isTooLate(scheduledAtMillis, nowMillis) {
  return !Number.isFinite(scheduledAtMillis) || nowMillis >= scheduledAtMillis + MAX_ALARM_LATENESS_MS;
}

export function scheduledRequestId(scheduleId) {
  return `scheduled_${scheduleId}`;
}

export function shouldRetry(attemptCount) {
  return attemptCount < MAX_EXECUTION_ATTEMPTS;
}
