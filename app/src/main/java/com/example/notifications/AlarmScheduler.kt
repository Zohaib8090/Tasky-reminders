package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val ACTION_TASK_REMINDER = "com.example.tasknest.ACTION_TASK_REMINDER"
    const val ACTION_MARK_DONE = "com.example.tasknest.ACTION_MARK_DONE"
    const val ACTION_SNOOZE = "com.example.tasknest.ACTION_SNOOZE"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    const val EXTRA_IS_EARLY = "extra_is_early"
    const val EXTRA_IS_TEST = "extra_is_test"
    const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"

    fun calculateTaskTimeMillis(dueDateMillis: Long, dueTime: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
        }
        val parts = dueTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun scheduleTaskAlarms(context: Context, task: Task) {
        if (task.isCompleted || !task.reminderEnabled) {
            cancelTaskAlarms(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val taskTime = calculateTaskTimeMillis(task.dueDate, task.dueTime)
        val now = System.currentTimeMillis()

        // 1. Exact Alarm at Task Due Time
        if (taskTime > now) {
            val exactIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_DESCRIPTION, task.description)
                putExtra(EXTRA_IS_EARLY, false)
                putExtra(EXTRA_IS_TEST, false)
                putExtra(EXTRA_OFFSET_MINUTES, 0)
            }
            val exactPendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 10).toInt(),
                exactIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, taskTime, exactPendingIntent)
            Log.d(TAG, "Scheduled due alarm for task ${task.id} at $taskTime")
        }

        // 2. Early Advance Alarm (if configured, e.g. 5, 10, 15, 30, 60, 1440 min prior)
        if (task.reminderMinutesBefore > 0) {
            val earlyTime = taskTime - (task.reminderMinutesBefore * 60 * 1000L)
            if (earlyTime > now) {
                val earlyIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    action = ACTION_TASK_REMINDER
                    putExtra(EXTRA_TASK_ID, task.id)
                    putExtra(EXTRA_TASK_TITLE, task.title)
                    putExtra(EXTRA_TASK_DESCRIPTION, task.description)
                    putExtra(EXTRA_IS_EARLY, true)
                    putExtra(EXTRA_IS_TEST, false)
                    putExtra(EXTRA_OFFSET_MINUTES, task.reminderMinutesBefore)
                }
                val earlyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (task.id * 10 + 1).toInt(),
                    earlyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, earlyTime, earlyPendingIntent)
                Log.d(TAG, "Scheduled early alarm for task ${task.id} at $earlyTime (${task.reminderMinutesBefore}m prior)")
            }
        }
    }

    fun snoozeTask(context: Context, taskId: Long, taskTitle: String, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_DESCRIPTION, "Snoozed task reminder")
            putExtra(EXTRA_IS_EARLY, false)
            putExtra(EXTRA_IS_TEST, false)
            putExtra(EXTRA_OFFSET_MINUTES, 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(alarmManager, snoozeTime, pendingIntent)
    }

    fun scheduleTestAlarm(context: Context, taskId: Long, taskTitle: String, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_DESCRIPTION, "Local notification test via AlarmManager ($delaySeconds sec countdown)")
            putExtra(EXTRA_IS_EARLY, false)
            putExtra(EXTRA_IS_TEST, true)
            putExtra(EXTRA_OFFSET_MINUTES, 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 5).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun cancelTaskAlarms(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java)

        // Cancel exact alarm
        val exactPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        exactPendingIntent?.let { alarmManager.cancel(it) }

        // Cancel early alarm
        val earlyPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        earlyPendingIntent?.let { alarmManager.cancel(it) }

        // Cancel snooze alarm
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        snoozePendingIntent?.let { alarmManager.cancel(it) }

        // Cancel test alarm
        val testPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 5).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        testPendingIntent?.let { alarmManager.cancel(it) }
    }

    fun rescheduleAllActiveAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val activeTasks = db.taskDao().getActiveReminderTasks()
                for (task in activeTasks) {
                    scheduleTaskAlarms(context, task)
                }
                Log.d(TAG, "Rescheduled alarms for ${activeTasks.size} active tasks")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule active alarms", e)
            }
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
