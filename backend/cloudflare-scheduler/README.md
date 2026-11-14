# QALBI scheduled alarm worker

This Cloudflare Worker checks Firestore once per minute for due friend-alarm schedules. It uses a Firebase service-account secret to atomically claim due jobs, revalidate the connection and block lists, create the existing `wakeUpRequests` record, and send the existing data-only FCM alarm payload. The Render dispatcher ignores requests tagged `CLOUDFLARE_SCHEDULED_ALARM`, so it does not send a second push.

The sender's phone does not run the timer. A schedule remains in Firestore if the sender closes QALBI, loses connectivity, or switches the phone off.

## Free-tier limits

This uses Cloudflare Workers Free Cron and QALBI's Firestore free quota. Cloudflare lists 100,000 Worker requests per day and 10 ms CPU per invocation on Free. A one-minute trigger uses about 1,440 scheduled invocations per day before any manual health checks. Firestore's Standard free quota lists 50,000 reads and 20,000 writes per day. Stay within both providers' current plan limits; exceeded free limits may stop work or require a plan change.

This is best-effort scheduling, not a delivery SLA. Cron runs at minute granularity and can be delayed or missed, providers can have outages, FCM acceptance is not proof that the recipient device rang, and a powered-off/force-stopped recipient cannot be guaranteed to receive an alarm. The Worker only sends schedules up to 15 minutes late; older jobs become `EXPIRED`. Sender timezone is stored as an IANA ID and the schedule instant as a Firestore UTC timestamp.

## One-time Firebase setup

1. Use the Firebase service-account JSON already used by the working Render dispatcher, from the same Firebase project as the Android app.
2. Confirm that the account can access Firestore and send Firebase Cloud Messaging. Do not put the JSON in Git, the Android app, or an ordinary environment variable file.
3. Deploy the rules and composite indexes from this folder:

   ```powershell
   npm install -g firebase-tools
   firebase login
   Set-Location C:\QALBI\firebase
   firebase deploy --only firestore:rules,firestore:indexes --project YOUR_FIREBASE_PROJECT_ID
   ```

   Replace `YOUR_FIREBASE_PROJECT_ID` with the `project_id` value from the service-account JSON. If Firebase CLI is already installed and logged in, only the last two commands are needed.

## Deploy the worker to Cloudflare Free

1. Create/sign in to a Cloudflare account and keep the Workers plan on **Free**. Do not select an upgrade.
2. Open PowerShell in `C:\QALBI\backend\cloudflare-scheduler` and run:

   ```powershell
   npm install
   npx wrangler login
   npx wrangler secret put SERVICE_ACCOUNT_JSON
   ```

3. When Wrangler asks for the secret value, paste the **entire contents** of the Firebase service-account JSON file, including the outer `{}`. Do not paste a file path and do not commit the value.
4. Deploy the worker and its every-minute Cron Trigger:

   ```powershell
   npx wrangler deploy
   ```

5. In the Cloudflare dashboard, open **Workers & Pages → qalbi-scheduled-alarm-worker → Settings → Triggers** and confirm the `* * * * *` Cron Trigger is listed. Trigger changes can take up to 15 minutes to propagate.
6. Open **Logs / Cron Events** for the Worker. After a tick, a normal run may log no schedules processed; errors should not include credentials or FCM tokens. The URL ending in `/health` only checks that the Worker deployment responds; it does not test Firestore credentials or FCM delivery.

## Deploy the Android and Render changes

1. Publish the new Firestore rules and indexes as above.
2. Commit and push this repository update when you are ready. Render should redeploy `backend/fcm-dispatcher`; the only Render change is a duplicate-prevention guard. Keep the existing Render service and UptimeRobot monitor for **immediate** alarms.
3. Build and install a new Android APK on both phones:

   ```powershell
   Set-Location C:\QALBI\android
   .\gradlew.bat assembleDebug
   ```

   APK path: `C:\QALBI\android\app\build\outputs\apk\debug\app-debug.apk`.

4. On two connected accounts, schedule an alarm two minutes ahead. Close QALBI and turn off or disconnect the sender phone; keep the recipient phone on, online, and permitted to show notifications. Verify the recipient alarm, the schedule status, then repeat with a schedule cancelled before its due time.

Changing a schedule is handled by cancelling it and creating another. A schedule in `PROCESSING` can no longer be cancelled because execution has already claimed it. `SENT` means FCM accepted at least one device send; it does not confirm the recipient heard the alarm.

## Local checks

```powershell
npm test
```

No live Firebase project, deployed Cloudflare Worker, or physical-device test is included in local tests. Those require the manual account setup above.
