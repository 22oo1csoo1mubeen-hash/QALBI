package com.wakeus.app.data.fcm

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Deferrable retry for token uploads; this is not part of real-time alarm delivery. */
class FcmTokenRegistrationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID)?.takeIf(String::isNotBlank) ?: return Result.failure()
        if (com.google.firebase.FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        val activeUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (activeUid != uid) return Result.success()

        val result = FcmTokenRepository().registerCurrentDeviceToken(uid)
        return if (result.isSuccess) {
            AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_TOKEN_REGISTERED)
            Result.success()
        } else {
            AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_TOKEN_REGISTRATION_FAILED)
            Result.retry()
        }
    }

    companion object {
        private const val KEY_UID = "authenticated_uid"

        private fun workName(uid: String) = "qalbi-fcm-token-registration-$uid"

        fun enqueue(context: Context, uid: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<FcmTokenRegistrationWorker>()
                .setInputData(androidx.work.workDataOf(KEY_UID to uid))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName(uid),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context, uid: String) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(uid))
        }
    }
}
