# Moto G34 5G Alarm Reliability Test Guide

This guide does not assume an Android release for the test phone. Record the exact device build first because Motorola menu labels differ by Android and My UX version.

## 1. Capture the device and app state

Connect the Moto G34 5G with USB debugging enabled and run these commands in PowerShell:

```powershell
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.display.id
adb shell dumpsys package com.wakeus.app | Select-String 'versionName|targetSdk|userId|stopped=' 
adb shell pm check-permission android.permission.POST_NOTIFICATIONS com.wakeus.app
adb shell appops get com.wakeus.app USE_FULL_SCREEN_INTENT
adb shell appops get com.wakeus.app RUN_ANY_IN_BACKGROUND
adb shell cmd deviceidle whitelist
adb shell dumpsys deviceidle
adb shell dumpsys netpolicy
```

Run `adb shell dumpsys power` for power-saver/idle clues and `adb shell dumpsys activity services com.wakeus.app` to see whether an alarm service is active during an actual ringing alarm. Do not paste unredacted notification or logcat dumps publicly; they may contain personal data. Capture focused logs with:

```powershell
adb logcat -c
adb logcat -v time QalbiMessagingService:D WakeUpAlarmService:D WakeUpAlarmManager:D FirebaseMessaging:D AndroidRuntime:E '*:S'
```

Record the Settings screen's **Background Alarm Reliability** results and export its redacted report. A token upload marked successful does not prove the daemon still has a valid token or that FCM delivered a later message.

## 2. Motorola settings to inspect

On the phone, open **Settings â†’ Apps â†’ QALBI â†’ App battery usage** (or the equivalent Battery section shown on that build). Record the exact selected mode and whether background use is allowed. If the device offers an unrestricted mode, test that as a separate condition. Check QALBI's app-specific background data and notification settings. Check Battery Saver separately and record whether it was on. Avoid changing global battery settings when testing a single app condition.

The Android-version-dependent labels may be **Unrestricted**, **Allow background usage**, **Optimized**, or **Restricted**. Do not treat the Battery optimization exemption check as a complete view of Motorola's per-app battery policy.

## 3. Two-device baseline

Use a second QALBI account as the sender. Ensure both devices are online, paired, and signed in. On the Moto G34, open Settings â†’ Background Alarm Reliability and run the check. Send a wake-up while QALBI is visible, then repeat with the Moto screen locked. Confirm that the request arrives, audio/vibration start, the full-screen UI appears if access is allowed, and **I'm Awake!** stops the alarm and updates the sender.

## 4. Background and extended-idle matrix

Repeat the same real request and record send time, receiver event timestamps, notification/audio result, acknowledgement time, network, and battery mode for each condition:

| Condition | Procedure |
|---|---|
| Background | Go Home without force-stopping; lock the phone; send a request. |
| Removed from Recents | Swipe QALBI away from Recents (do not use Android Settings â†’ Force stop); lock; send. |
| Doze/idle | Leave screen off and unplugged; test after 1 hour, 8 hours, about 24 hours, then several days. |
| Network transition | Test Wi-Fi, mobile data, Wi-Fi-to-mobile handoff, temporary offline period, and restored network. |
| Battery controls | Compare default app battery mode, unrestricted (if offered), and Battery Saver on/off. Change one setting per run. |
| Reboot | Reboot, wait for network, do not launch QALBI, then send. Record whether FCM and the app recover. |
| Token refresh/update | Reinstall, update the APK, and sign out/in in separate runs. Compare the app's last-registration time and the daemon's send results. |

Do not use Force stop for the removed-from-Recents test. A force-stopped app is expected to remain unable to process FCM until manually launched. A completely powered-off phone cannot receive an alarm.

## 5. Reproducing the reported failure

1. Record all device/build/settings data above and export a baseline diagnostic report.
2. Send and receive a baseline alarm while locked.
3. Leave QALBI unopened for a day, then several days, with the phone online and record Battery Saver/app battery mode.
4. Send one alarm while QALBI remains closed. Record the sender request ID, daemon `ACCEPTED_BY_FCM` or error state, and whether the local report gained `FCM_MESSAGE_RECEIVED` / `ALARM_SERVICE_STARTED`.
5. Without changing settings, open QALBI and record whether token registration is refreshed. Send another alarm.
6. Repeat under unrestricted app battery use if available. Compare results without changing global settings.

The dispatcher logs `ACCEPTED_BY_FCM` only as an FCM submission result. It is not evidence of device delivery. A missing local receipt with accepted send points toward delivery/device state; a missing/failed send points toward request, authorization, token, or daemon state. Share only the redacted diagnostic report and relevant error codes.

## 6. Record results

| Run | Date/time | Android/build | Battery mode | Network | App state | FCM backend state | Device received | Service/audio | Ack received |
|---|---|---|---|---|---|---|---|---|---|
| Baseline | | | | | | | | | |
| 1 hour | | | | | | | | | |
| 8 hours | | | | | | | | | |
| 24 hours | | | | | | | | | |
| Multi-day | | | | | | | | | |
| After opening app | | | | | | | | | |


