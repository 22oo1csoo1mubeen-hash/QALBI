# QALBI V2 Friend Alarm Scheduling Report

## A. Implementation status

The source implementation is present, but the feature is **not live yet**. It requires Firebase rules/index deployment, a Cloudflare Worker secret and Cron deployment, a Render redeploy for duplicate prevention, a new APK, and two-device validation. No cloud account was accessed or deployed from this workspace.

## B. Architecture found

The Android client uses Kotlin, Jetpack Compose, ViewModels, repositories, Firebase Authentication, and Cloud Firestore. Immediate wake-up requests are written to `wakeUpRequests`; the trusted Render Node dispatcher validates connection/block status and sends high-priority data-only FCM. `QalbiMessagingService` validates the FCM payload and invokes the existing alarm foreground service. The receiver acknowledges/declines by updating the same request record.

The immediate pipeline remains available. The Render dispatcher now ignores schedule-origin requests, because those are sent by the scheduled worker to prevent duplicate FCM sends.

## C. Scheduled execution

The sender creates a `scheduledAlarms/{scheduleId}` document with an absolute Firestore timestamp in UTC, the sender's IANA timezone ID, friend/connection IDs, optional message, status, attempt count, and next execution time. The Cloudflare Worker Cron Trigger runs every minute, queries due jobs, and uses Firestore `updateTime` preconditions to claim jobs with a lease. It rechecks the live connection and both block lists, reads current public profile data and recipient FCM tokens, creates a deterministic `wakeUpRequests/scheduled_{scheduleId}` record, and sends the existing data-only QALBI FCM payload.

The lease lets a later Cron run recover a worker that stopped while processing. Transient failures use bounded retry delays and fail after five attempts. A schedule more than 15 minutes late becomes `EXPIRED`. Statuses shown in the app are Scheduled, Sending, Sent, Failed, Cancelled, and Expired. Editing is implemented as cancel-and-recreate in the user flow; an already claimed Sending schedule cannot be cancelled.

## D. Sender offline behavior

The sender's device does not run the timer. Once Firestore confirms a saved schedule, the sender can close QALBI, disconnect, or switch off the sender phone. The Cloudflare worker checks and dispatches independently. A schedule cannot run if the sender fails to save it before losing connectivity.

## E. Recipient behavior and limits

The scheduled FCM payload uses QALBI's existing request ID, data keys, and receiver alarm service. The optional message is stored on the request and shown on the existing incoming alarm screen. The recipient phone still needs to be powered on, able to connect to FCM, and permitted to show notifications/run background services. `SENT` means FCM accepted at least one send; it does not prove that the device received or played it.

## F. UI, database, and security changes

- Friend details now offer both the existing immediate wake-up action and **Schedule Alarm**.
- The schedule form selects local date/time, displays the IANA timezone, accepts a message up to 160 characters, and blocks past times and duplicate button submissions.
- The Wake Up area links to **Scheduled Alarms**, where the sender can review status and cancel a future pending schedule.
- Firestore rules allow only an authenticated connected sender to create a schedule for a current, unblocked friend, allow only the sender to read their schedules, and allow only pending-to-cancelled client transitions. The worker's service-account key never enters the APK.
- Composite indexes support due-job and expired-lease queries. The worker service-account secret belongs in Cloudflare Worker Secrets.

## G. Duplicate prevention and retries

Schedule claiming uses Firestore update-time compare-and-set. Delivery request IDs are deterministic per schedule. The Render listener skips Worker-owned schedule requests. Retries are bounded, and a previously recorded FCM acceptance is used during lease recovery to avoid resending.

There is an unavoidable at-least-once edge case: FCM may accept a push while the Worker loses the response before persisting that acceptance. Retrying in that narrow case can send the same stable request ID again. The Android alarm manager suppresses duplicate starts for the currently ringing request, but this is not a mathematical exactly-once guarantee across a receiver restart.

## H. Free-tier and reliability verification

This implementation avoids Firebase Cloud Functions/Blaze. It uses a one-minute Cloudflare Workers Cron Trigger and Firestore's existing database. Cloudflare currently documents Workers Free limits of 100,000 requests/day and 10 ms CPU per invocation; Firestore Standard documents daily free quotas. At one Cron run per minute, the base trigger is 1,440 runs/day, before other Worker requests and per-schedule Firestore/FCM operations. Monitor both accounts and do not upgrade plans. Free services do not provide an unconditional alarm delivery SLA; Cron is minute-granular and can be delayed or missed, and provider outages remain possible.

The existing Render dispatcher continues to serve immediate alarms. UptimeRobot can reduce idle spin-down but does not make Render a durable scheduler.

## I. Tests actually run

- Cloudflare scheduler policy and mocked delivery tests: **5 passed** (`npm test` in `backend/cloudflare-scheduler`). Tests include delivery through the existing request format and rejection after a connection disconnects.
- Existing Render dispatcher policy tests: **3 passed** (`node --test dispatch-policy.test.js` in `backend/fcm-dispatcher`).
- Android unit tests and debug APK build: **passed** (`testDebugUnitTest assembleDebug` from `android/`).
- APK produced at `android/app/build/outputs/apk/debug/app-debug.apk`.
- Firebase rules/index deployment validation: **not run**; Firebase CLI is not installed and no Firebase project was accessed.
- Cloudflare deployment, live FCM delivery, emulator/physical-device testing, and the requested sender-off two-device scenario: **not run**.

## J. Manual steps still required

1. Deploy `firebase/firestore.rules` and `firebase/firestore.indexes.json` using the Firebase CLI from the `firebase/` directory. The supplied `firebase/firebase.json` points to both files.
2. In Cloudflare, keep the account on Workers Free. From `backend/cloudflare-scheduler`, install Wrangler, log in, add `SERVICE_ACCOUNT_JSON` as a Worker secret using the full private service-account JSON from the same Firebase project, and run `npx wrangler deploy`.
3. Wait for the Cron Trigger to propagate and verify it appears in Worker Settings → Triggers.
4. Commit/push when ready so Render redeploys the small duplicate-prevention change. Keep Render for immediate alarms.
5. Build/install a new APK on both phones and run the two-device schedule, sender-offline, and cancellation scenarios documented in [`backend/cloudflare-scheduler/README.md`](../../backend/cloudflare-scheduler/README.md).

Do not put the service-account JSON in Git, the Android app, `.env`, or a public environment variable. Only paste it into Cloudflare's secret prompt.

## K. Build and Git checkpoint

Build the APK from PowerShell:

```powershell
Set-Location C:\QALBI\android
.\gradlew.bat assembleDebug
```

Expected output: `android/app/build/outputs/apk/debug/app-debug.apk`.

- Branch: `main`.
- No commit or push was made.
- The working tree includes the scheduling implementation and earlier uncommitted settings cleanup/report changes, plus pre-existing untracked `.gradle/` and `.idea/` folders. Review the full diff before committing; do not include local IDE/cache folders unless intentionally needed.

## Sources

- [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/)
- [Cloudflare Cron Triggers](https://developers.cloudflare.com/workers/configuration/cron-triggers/)
- [Cloudflare Workers limits](https://developers.cloudflare.com/workers/platform/limits/)
- [Firestore Standard quotas](https://firebase.google.com/docs/firestore/quotas)
- [Firebase Cloud Functions billing requirement](https://firebase.google.com/docs/functions/faq-and-troubleshooting)
- [Render free service behavior](https://render.com/docs/free)
