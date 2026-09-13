package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val CHANNEL_NAME = "Task Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val isEarly = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_EARLY, false)

        when (intent.action) {
            AlarmScheduler.ACTION_MARK_DONE -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (taskId != -1L) {
                    notificationManager.cancel(taskId.toInt())
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(context)
                        db.taskDao().setTaskCompletion(taskId, true)
                        AlarmScheduler.cancelTaskAlarms(context, taskId)
                    }
                }
            }

            AlarmScheduler.ACTION_SNOOZE -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (taskId != -1L) {
                    notificationManager.cancel(taskId.toInt())
                    AlarmScheduler.snoozeTask(context, taskId, taskTitle)
                }
            }

            AlarmScheduler.ACTION_TASK_REMINDER -> {
                showNotification(context, taskId, taskTitle, isEarly)
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, taskTitle: String, isEarly: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for due tasks and 15-minute reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification to open MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TASK_ID", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Done
        val doneIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_MARK_DONE
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 3).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TASK_TITLE, taskTitle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 4).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (isEarly) {
            "Starts in 15 minutes! Get ready."
        } else {
            "Due right now! Stay on track."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(taskTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze (10m)", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }
}
