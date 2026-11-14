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

require('dotenv').config();

const fs = require('fs');
const path = require('path');
const http = require('http');
const admin = require('firebase-admin');
const { timestampToMillis, remainingTtlMillis } = require('./dispatch-policy');

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
const retryAttempts = new Map();
const MAX_RETRIES = 5;
const MAX_REQUEST_TTL_MS = 15 * 60 * 1000;

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

    if (dispatchedRequests.has(requestId) || data.fcmDispatched === true || data.fcmRetryExhausted === true) {
        return;
    }

    if (data.status !== 'PENDING') {
        return;
    }

    const expiresAt = timestampToMillis(data.expiresAt);
    const now = Date.now();
    if (!Number.isFinite(expiresAt) || expiresAt <= now) {
        await doc.ref.update({
            fcmDispatchState: 'SKIPPED_EXPIRED',
            fcmRetryExhausted: true,
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        }).catch(() => {});
        dispatchedRequests.delete(requestId);
        return;
    }
    if (expiresAt - now > MAX_REQUEST_TTL_MS) {
        await doc.ref.update({
            fcmDispatchState: 'REJECTED_INVALID_EXPIRY',
            fcmRetryExhausted: true,
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        }).catch(() => {});
        dispatchedRequests.add(requestId);
        console.warn(`[QALBI] Request ${requestId} expiry exceeds the allowed wake-up lifetime.`);
        return;
    }

    const receiverUid = data.receiverUid;
    const senderUid = data.senderUid;
    const senderDisplayName = data.senderDisplayName || 'Your Friend';
    const senderUsername = data.senderUsername || 'friend';

    if (!receiverUid) {
        console.warn(`[QALBI] Request ${requestId} missing receiverUid. Skipping.`);
        dispatchedRequests.delete(requestId);
        return;
    }

    let authorization;
    try {
        authorization = await authorizeRequest(data);
    } catch (err) {
        dispatchedRequests.add(requestId);
        console.error(`[QALBI] Authorization check failed for request ${requestId}:`, err.message);
        await doc.ref.update({
            fcmDispatchState: 'FAILED',
            fcmErrorCode: String(err.code || 'authorization_check_failed').slice(0, 120),
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        }).catch(() => {});
        scheduleRetry(doc);
        return;
    }
    if (!authorization.allowed) {
        console.warn(`[QALBI] Request ${requestId} rejected before FCM dispatch: ${authorization.reason}`);
        await doc.ref.update({
            fcmDispatchState: 'REJECTED_UNAUTHORIZED',
            fcmAuthorizationResult: authorization.reason,
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        }).catch(() => {});
        dispatchedRequests.add(requestId);
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
            console.warn(`[QALBI] No FCM device tokens for request ${requestId}; a bounded retry is scheduled.`);
            await doc.ref.update({
                fcmDeliveryAttempted: true,
                fcmTokenFound: false,
                fcmDispatchState: 'NO_TOKENS',
                fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            scheduleRetry(doc);
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
            await doc.ref.update({
                fcmDeliveryAttempted: true,
                fcmTokenFound: false,
                fcmDispatchState: 'NO_TOKENS',
                fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            scheduleRetry(doc);
            return;
        }

        console.log(`[QALBI] Dispatching to ${deviceTokens.length} device(s)...`);

        // Data-only is intentional: Android must invoke QalbiMessagingService so it can
        // validate expiry and start the alarm foreground service. Notification payloads
        // may be displayed by the OS without invoking onMessageReceived in background.
        const multicastMessage = {
            tokens: deviceTokens,
            data: {
                requestId: requestId,
                senderUid: senderUid,
                senderDisplayName: senderDisplayName,
                senderUsername: senderUsername,
                receiverUid: receiverUid,
                type: 'WAKE_UP_REQUEST',
                createdAt: String(timestampToMillis(data.createdAt) || now),
                expiresAt: String(expiresAt)
            },
            android: {
                priority: 'high',
                ttl: remainingTtlMillis(expiresAt, now)
            }
        };

        const response = await messaging.sendEachForMulticast(multicastMessage);
        console.log(`[QALBI] FCM accepted ${response.successCount} device send(s) for request ${requestId}; ${response.failureCount} failed. Acceptance is not device delivery.`);

        // Clean up stale or unregistered tokens
        await Promise.all(response.responses.map(async (resp, idx) => {
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
        }));

        const accepted = response.successCount > 0;
        const retryableFailure = !accepted;
        await doc.ref.update({
            fcmDispatched: accepted,
            fcmDispatchState: accepted ? 'ACCEPTED_BY_FCM' : 'FAILED',
            fcmAcceptedCount: response.successCount,
            fcmFailureCount: response.failureCount,
            fcmAcceptedMessageIds: response.responses.filter(resp => resp.success).map(resp => resp.messageId).filter(Boolean),
            fcmErrorCodes: response.responses.filter(resp => !resp.success && resp.error).map(resp => resp.error.code),
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        if (retryableFailure) scheduleRetry(doc);
        else {
            retryAttempts.delete(requestId);
            dispatchedRequests.delete(requestId);
        }

    } catch (err) {
        console.error(`[QALBI] Error dispatching push for ${requestId}:`, err.message);
        await doc.ref.update({
            fcmDispatchState: 'FAILED',
            fcmErrorCode: String(err.code || 'unknown').slice(0, 120),
            fcmAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
        }).catch(() => {});
        scheduleRetry(doc);
    }
}

async function authorizeRequest(data) {
    if (!data.senderUid || !data.receiverUid || !data.connectionId || data.senderUid === data.receiverUid) {
        return { allowed: false, reason: 'invalid_participants' };
    }
    const connectionRef = db.collection('connections').doc(data.connectionId);
    const connection = await connectionRef.get();
    const members = connection.exists ? connection.get('memberUids') : null;
    if (!connection.exists || connection.get('status') !== 'CONNECTED' ||
        !Array.isArray(members) || members.length !== 2 ||
        !members.includes(data.senderUid) || !members.includes(data.receiverUid)) {
        return { allowed: false, reason: 'connection_not_active' };
    }
    const [senderBlock, receiverBlock] = await Promise.all([
        db.collection('userBlocks').doc(data.senderUid).collection('blockedUsers').doc(data.receiverUid).get(),
        db.collection('userBlocks').doc(data.receiverUid).collection('blockedUsers').doc(data.senderUid).get()
    ]);
    if (senderBlock.exists || receiverBlock.exists) return { allowed: false, reason: 'participant_blocked' };
    return { allowed: true, reason: 'authorized_connection' };
}

async function scheduleRetry(doc) {
    const requestId = doc.id;
    const attempt = (retryAttempts.get(requestId) || 0) + 1;
    if (attempt > MAX_RETRIES) {
        retryAttempts.delete(requestId);
        dispatchedRequests.delete(requestId);
        await doc.ref.update({ fcmRetryExhausted: true, fcmDispatchState: 'RETRY_EXHAUSTED' }).catch(() => {});
        console.warn(`[QALBI] Retry limit reached for request ${requestId}.`);
        return;
    }
    retryAttempts.set(requestId, attempt);
    const delayMs = Math.min(15_000 * (2 ** (attempt - 1)), 4 * 60_000);
    setTimeout(async () => {
        dispatchedRequests.delete(requestId);
        try {
            const fresh = await doc.ref.get();
            if (fresh.exists) await processWakeUpRequest(fresh);
        } catch (err) {
            console.error(`[QALBI] Retry check failed for request ${requestId}:`, err.message);
            scheduleRetry(doc);
        }
    }, delayMs).unref?.();
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

// Render's free tier only offers web services. This lightweight health endpoint
// lets the dispatcher run as a web service while the Firestore listener stays
// active in the same process. It does not participate in alarm dispatching.
const healthServer = http.createServer((req, res) => {
    if (req.method === 'GET' && req.url === '/health') {
        res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('QALBI dispatcher is running\n');
        return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not found\n');
});

const port = Number(process.env.PORT || 3000);
healthServer.listen(port, '0.0.0.0', () => {
    console.log(`[QALBI FCM Dispatcher] Health endpoint listening on port ${port}.`);
});
