package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AttachmentType {
    CHECKLIST,
    IMAGE,
    VIDEO,
    LINK,
    AUDIO
}

data class AttachmentItem(
    val id: String = UUID.randomUUID().toString(),
    val type: AttachmentType,
    val title: String = "",
    val subtitle: String = "",
    val uriOrUrl: String = "",
    val extraData: String = "",
    val isDone: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun encodeList(items: List<AttachmentItem>): String {
            if (items.isEmpty()) return ""
            return items.joinToString("\n---ATTACHMENT_ITEM---\n") { item ->
                listOf(
                    item.id,
                    item.type.name,
                    item.title.replace("\n", " "),
                    item.subtitle.replace("\n", " "),
                    item.uriOrUrl.replace("\n", " "),
                    item.extraData.replace("\n", " "),
                    item.isDone.toString(),
                    item.timestamp.toString()
                ).joinToString("|||")
            }
        }

        fun decodeList(encoded: String): List<AttachmentItem> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split("\n---ATTACHMENT_ITEM---\n").mapNotNull { chunk ->
                val parts = chunk.split("|||")
                if (parts.size >= 8) {
                    val type = try {
                        AttachmentType.valueOf(parts[1])
                    } catch (e: Exception) {
                        AttachmentType.LINK
                    }
                    AttachmentItem(
                        id = parts[0],
                        type = type,
                        title = parts[2],
                        subtitle = parts[3],
                        uriOrUrl = parts[4],
                        extraData = parts[5],
                        isDone = parts[6].toBooleanStrictOrNull() ?: false,
                        timestamp = parts[7].toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else null
            }
        }
    }
}

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val title: String = "",
    val content: String = "",
    val colorIndex: Int = 0,
    val checklistJson: String = "",
    val attachmentsJson: String = "",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
