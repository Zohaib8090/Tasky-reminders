package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Priority(val label: String) {
    LOW("Low"),
    MEDIUM("Med"),
    HIGH("High")
}

enum class Category(val label: String) {
    STUDY("Study"),
    PERSONAL("Personal"),
    WORK("Work")
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
) {
    companion object {
        fun encodeList(items: List<ChecklistItem>): String {
            if (items.isEmpty()) return ""
            return items.joinToString("\n") { "${it.id}|||${it.text.replace("\n", " ")}|||${it.isDone}" }
        }

        fun decodeList(encoded: String): List<ChecklistItem> {
            if (encoded.isBlank()) return emptyList()
            return encoded.lines().mapNotNull { line ->
                val parts = line.split("|||")
                if (parts.size >= 3) {
                    ChecklistItem(
                        id = parts[0],
                        text = parts[1],
                        isDone = parts[2].toBooleanStrictOrNull() ?: false
                    )
                } else null
            }
        }
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long, // timestamp for day
    val dueTime: String, // "HH:mm" (e.g., "14:30")
    val priority: Priority = Priority.MEDIUM,
    val category: Category = Category.PERSONAL,
    val isCompleted: Boolean = false,
    val checklistJson: String = "",
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 0, // 0 = at due time, 5, 10, 15, 30, 60, 1440
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getReminderLabel(): String {
        if (!reminderEnabled) return "Reminder Off"
        return when (reminderMinutesBefore) {
            0 -> "At due time"
            5 -> "5 min before"
            10 -> "10 min before"
            15 -> "15 min before"
            30 -> "30 min before"
            60 -> "1 hour before"
            1440 -> "1 day before"
            else -> "$reminderMinutesBefore min before"
        }
    }
}
