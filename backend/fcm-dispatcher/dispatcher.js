/**
 * QALBI — Zero-Cost Trusted FCM Push Notification Dispatcher
 *
 * This daemon runs in a trusted server/local environment using Firebase Admin SDK.
 * It listens in real-time to Firestore wakeUpRequests and dispatches high-priority
 * FCM push notifications to the receiver's registered devices.
 *
 * Cost: ₹0 (Firebase Spark Plan / local execution / self-hosted free runner)
 * Security: Private service-account credentials NEVER enter the Android client.
 */

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

// 1. Initialize Firebase Admin SDK
const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    path.join(__dirname, 'serviceAccountKey.json');

if (fs.existsSync(serviceAccountPath)) {
    try {
        const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });
        console.log('[QALBI FCM Dispatcher] Authenticated via Service Account JSON.');
    } catch (err) {
        console.error('[QALBI FCM Dispatcher] Failed to parse service account file:', err.message);
        process.exit(1);
    }
} else {
    // Attempt default application credentials
    try {
        admin.initializeApp();
        console.log('[QALBI FCM Dispatcher] Initialized with default application credentials.');
    } catch (err) {
        console.warn('------------------------------------------------------------------------');
        console.warn('[QALBI FCM Dispatcher] NOTICE: serviceAccountKey.json not detected.');
        console.warn('To enable real remote FCM push notifications:');
        console.warn('1. Open Firebase Console -> Project Settings -> Service accounts');
        console.warn('2. Click "Generate new private key" and download the JSON.');
        console.warn('3. Save it as backend/fcm-dispatcher/serviceAccountKey.json');
        console.warn('4. Run "npm install && npm start" in backend/fcm-dispatcher/');
        console.warn('------------------------------------------------------------------------');
        process.exit(1);
    }
}

const db = admin.firestore();
const messaging = admin.messaging();
const dispatchedRequests = new Set();

/**
 * Masks an FCM registration token for safe logging without exposing full routing secrets.
 */
function maskToken(token) {
    if (!token || token.length < 10) return '***';
    return `${token.substring(0, 6)}...${token.substring(token.length - 4)}`;
}

/**
 * Dispatches an FCM push notification for a pending wake-up request.
 */
async function processWakeUpRequest(doc) {
    const data = doc.data();
    const requestId = doc.id;

    if (dispatchedRequests.has(requestId) || data.fcmDispatched === true) {
        return;
    }

    if (data.status !== 'PENDING') {
        return;
    }

    const receiverUid = data.receiverUid;
    const senderUid = data.senderUid;
    const senderDisplayName = data.senderDisplayName || 'Your Friend';
    const senderUsername = data.senderUsername || 'friend';

    if (!receiverUid) {
        console.warn(`[QALBI] Request ${requestId} missing receiverUid. Skipping.`);
        return;
    }

    console.log(`[QALBI] Processing wake-up request ${requestId} for receiver ${receiverUid.substring(0, 8)}...`);
    dispatchedRequests.add(requestId);

    try {
        // Query receiver's registered FCM tokens
        const tokensSnapshot = await db.collection('users')
            .doc(receiverUid)
            .collection('fcmTokens')
            .get();

        if (tokensSnapshot.empty) {
            console.log(`[QALBI] No FCM device tokens found for receiver ${receiverUid.substring(0, 8)}.`);
            await doc.ref.update({
                fcmDeliveryAttempted: true,
                fcmTokenFound: false,
                fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            return;
        }

        const deviceTokens = [];
        const tokenDocRefs = [];
        tokensSnapshot.forEach(tokenDoc => {
            const token = tokenDoc.data().token;
            if (token && typeof token === 'string') {
                deviceTokens.push(token);
                tokenDocRefs.push(tokenDoc.ref);
            }
        });

        if (deviceTokens.length === 0) {
            console.log(`[QALBI] No valid tokens extracted for receiver ${receiverUid.substring(0, 8)}.`);
            return;
        }

        console.log(`[QALBI] Dispatching to ${deviceTokens.length} device(s)...`);

        // Construct high-priority FCM message payload
        const multicastMessage = {
            tokens: deviceTokens,
            data: {
                requestId: requestId,
                senderUid: senderUid,
                senderDisplayName: senderDisplayName,
                senderUsername: senderUsername,
                receiverUid: receiverUid,
                type: 'WAKE_UP_REQUEST',
                createdAt: data.createdAt ? data.createdAt.toString() : Date.now().toString()
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: 'qalbi_wake_up_channel',
                    title: `☀️ Wake Up from ${senderDisplayName}`,
                    body: `@${senderUsername} wants to wake you up! Tap to respond.`,
                    priority: 'max',
                    defaultSound: true,
                    defaultVibrateTimings: true,
                    visibility: 'public'
                }
            }
        };

        const response = await messaging.sendEachForMulticast(multicastMessage);
        console.log(`[QALBI] FCM result for ${requestId}: ${response.successCount} sent, ${response.failureCount} failed.`);

        // Clean up stale or unregistered tokens
        response.responses.forEach(async (resp, idx) => {
            if (!resp.success && resp.error) {
                const errorCode = resp.error.code;
                console.warn(`[QALBI] Token ${maskToken(deviceTokens[idx])} error: ${errorCode}`);
                if (
                    errorCode === 'messaging/registration-token-not-registered' ||
                    errorCode === 'messaging/invalid-registration-token'
                ) {
                    try {
                        await tokenDocRefs[idx].delete();
                        console.log(`[QALBI] Pruned invalid token ${maskToken(deviceTokens[idx])}`);
                    } catch (e) {
                        // ignore delete error
                    }
                }
            }
        });

        // Mark request as dispatched in Firestore
        await doc.ref.update({
            fcmDispatched: true,
            fcmSuccessCount: response.successCount,
            fcmFailureCount: response.failureCount,
            fcmDispatchedAt: admin.firestore.FieldValue.serverTimestamp()
        });

    } catch (err) {
        console.error(`[QALBI] Error dispatching push for ${requestId}:`, err.message);
        dispatchedRequests.delete(requestId);
    }
}

/**
 * Start Real-Time Firestore listener on wakeUpRequests collection.
 */
function startListener() {
    console.log('[QALBI FCM Dispatcher] Listening for incoming wake-up requests...');

    db.collection('wakeUpRequests')
        .where('status', '==', 'PENDING')
        .onSnapshot(
            snapshot => {
                snapshot.docChanges().forEach(change => {
                    if (change.type === 'added' || change.type === 'modified') {
                        processWakeUpRequest(change.doc);
                    }
                });
            },
            err => {
                console.error('[QALBI FCM Dispatcher] Firestore listener error:', err.message);
            }
        );
}

startListener();
