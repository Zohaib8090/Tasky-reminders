package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Task
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_TASK_REMINDER = "com.example.tasknest.ACTION_TASK_REMINDER"
    const val ACTION_MARK_DONE = "com.example.tasknest.ACTION_MARK_DONE"
    const val ACTION_SNOOZE = "com.example.tasknest.ACTION_SNOOZE"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_IS_EARLY = "extra_is_early"

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
        if (task.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val taskTime = calculateTaskTimeMillis(task.dueDate, task.dueTime)
        val now = System.currentTimeMillis()

        // 1. Exact Alarm at Task Due Time
        if (taskTime > now) {
            val exactIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_IS_EARLY, false)
            }
            val exactPendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 10).toInt(),
                exactIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, taskTime, exactPendingIntent)
        }

        // 2. 15 Minutes Before Alarm
        val earlyTime = taskTime - (15 * 60 * 1000L)
        if (earlyTime > now) {
            val earlyIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_IS_EARLY, true)
            }
            val earlyPendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 10 + 1).toInt(),
                earlyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, earlyTime, earlyPendingIntent)
        }
    }

    fun snoozeTask(context: Context, taskId: Long, taskTitle: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_IS_EARLY, false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(alarmManager, snoozeTime, pendingIntent)
    }

    fun cancelTaskAlarms(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java)

        val exactPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        exactPendingIntent?.let { alarmManager.cancel(it) }

        val earlyPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        earlyPendingIntent?.let { alarmManager.cancel(it) }
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
