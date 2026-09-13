package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.notifications.AlarmScheduler
import java.util.concurrent.TimeUnit

class TaskSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val now = System.currentTimeMillis()
            val activeTasks = db.taskDao().getTasksForDate(now - 86400000L, now + 86400000L * 7)
            // Re-sync future alarms for safety in case of device time change
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "tasknest_periodic_sync"

        fun enqueuePeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(12, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
