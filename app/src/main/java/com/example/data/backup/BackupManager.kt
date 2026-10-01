package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.AttachmentItem
import com.example.data.model.AttachmentType
import com.example.data.model.Category
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import com.example.notifications.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSettings(
    val themePalette: String = "BLUE",
    val isDarkMode: Boolean? = null
)

sealed class BackupResult {
    data class Success(
        val fileName: String,
        val taskCount: Int,
        val noteCount: Int,
        val mediaCount: Int,
        val lockedCount: Int = 0
    ) : BackupResult()

    data class Error(val message: String, val cause: Throwable? = null) : BackupResult()
}

sealed class RestoreResult {
    data class Success(
        val taskCount: Int,
        val noteCount: Int,
        val mediaCount: Int,
        val settings: BackupSettings?
    ) : RestoreResult()

    data class Error(val message: String, val cause: Throwable? = null) : RestoreResult()

    /** The backup holds locked items; ask for the export password and try again. */
    object NeedsPassword : RestoreResult()

    object WrongPassword : RestoreResult()
}

object BackupManager {

    private const val TAG = "BackupManager"
    private const val BACKUP_VERSION = 1
    private const val APP_NAME = "TaskNest"

    fun generateBackupFileName(date: Date = Date()): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "TaskNest_Backup_${dateFormat.format(date)}.zip"
    }

    /**
     * Creates a 100% offline local backup zip containing:
     * - data.json (all tasks, notes, checklists, settings)
     * - images/ folder
     * - videos/ folder
     * - audio/ folder
     * - files/ folder
     */
    suspend fun createBackup(
        context: Context,
        destinationUri: Uri,
        repository: TaskRepository,
        settings: BackupSettings,
        lockedPassword: CharArray? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val tasks = repository.getAllTasksSync()
            val notes = repository.getAllNotesSync()
            val lockedCount = tasks.count { it.isLocked } + notes.count { it.isLocked }
            if (lockedCount > 0 && (lockedPassword == null || lockedPassword.isEmpty())) {
                return@withContext BackupResult.Error("A password is required to export locked items.")
            }
            // Locked items (and their media) are sealed with a password-derived key
            val sealer = if (lockedCount > 0) BackupSealer.create(lockedPassword!!) else null

            var mediaCount = 0
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext BackupResult.Error("Could not open output destination for writing backup file.")

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // Ensure required folders exist in the zip
                val folderNames = listOf("images/", "videos/", "audio/", "files/")
                for (folder in folderNames) {
                    try {
                        zipOut.putNextEntry(ZipEntry(folder))
                        zipOut.closeEntry()
                    } catch (e: Exception) {
                        // ignore if folder entry already exists
                    }
                }

                // Process task attachments
                val updatedTasks = tasks.map { task ->
                    val rawAttachments = AttachmentItem.decodeList(task.attachmentsJson)
                    if (rawAttachments.isEmpty()) {
                        task
                    } else {
                        val exportedAttachments = rawAttachments.map { item ->
                            val zipPath = copyMediaToZip(context, item, zipOut, if (task.isLocked) sealer else null)
                            if (zipPath != null) {
                                mediaCount++
                                item.copy(uriOrUrl = "zip://$zipPath")
                            } else {
                                item
                            }
                        }
                        task.copy(attachmentsJson = AttachmentItem.encodeList(exportedAttachments))
                    }
                }

                // Process note attachments
                val updatedNotes = notes.map { note ->
                    val rawAttachments = AttachmentItem.decodeList(note.attachmentsJson)
                    if (rawAttachments.isEmpty()) {
                        note
                    } else {
                        val exportedAttachments = rawAttachments.map { item ->
                            val zipPath = copyMediaToZip(context, item, zipOut, if (note.isLocked) sealer else null)
                            if (zipPath != null) {
                                mediaCount++
                                item.copy(uriOrUrl = "zip://$zipPath")
                            } else {
                                item
                            }
                        }
                        note.copy(attachmentsJson = AttachmentItem.encodeList(exportedAttachments))
                    }
                }

                // Write data.json
                val rootJson = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("appName", APP_NAME)
                    put("exportedAt", System.currentTimeMillis())

                    if (sealer != null) {
                        put("locked", JSONObject().apply {
                            put("salt", android.util.Base64.encodeToString(sealer.salt, android.util.Base64.NO_WRAP))
                            put("iterations", sealer.iterations)
                            put("kdf", sealer.kdf)
                        })
                    }

                    // Settings
                    put("settings", JSONObject().apply {
                        put("themePalette", settings.themePalette)
                        if (settings.isDarkMode != null) {
                            put("isDarkMode", settings.isDarkMode)
                        } else {
                            put("isDarkMode", JSONObject.NULL)
                        }
                    })

                    // Tasks
                    val tasksArray = JSONArray()
                    for (task in updatedTasks) {
                        tasksArray.put(taskToJson(task, sealer))
                    }
                    put("tasks", tasksArray)

                    // Notes
                    val notesArray = JSONArray()
                    for (note in updatedNotes) {
                        notesArray.put(noteToJson(note, sealer))
                    }
                    put("notes", notesArray)
                }

                zipOut.putNextEntry(ZipEntry("data.json"))
                val jsonBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)
                zipOut.write(jsonBytes)
                zipOut.closeEntry()
            }

            BackupResult.Success(
                fileName = destinationUri.lastPathSegment ?: generateBackupFileName(),
                taskCount = tasks.size,
                noteCount = notes.size,
                mediaCount = mediaCount,
                lockedCount = lockedCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            BackupResult.Error("Backup failed: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Restores backup from zip: extracts media to app filesDir and inserts tasks, notes, and settings back into Room.
     */
    suspend fun restoreBackup(
        context: Context,
        sourceUri: Uri,
        repository: TaskRepository,
        replaceExisting: Boolean = true,
        lockedPassword: CharArray? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext RestoreResult.Error("Could not open selected backup file.")

            var dataJsonString: String? = null
            var mediaCount = 0
            val mediaBaseDir = File(context.filesDir, "attachments").apply { mkdirs() }
            val lockedTmpDir = File(context.cacheDir, "locked_restore").apply {
                deleteRecursively()
                mkdirs()
            }
            try {

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name

                    if (!entry.isDirectory && entryName.startsWith("locked/")) {
                        // Encrypted media of locked items: stage it until the password is verified
                        val staged = File(lockedTmpDir, entryName)
                        if (staged.canonicalPath.startsWith(lockedTmpDir.canonicalPath + File.separator)) {
                            staged.parentFile?.mkdirs()
                            FileOutputStream(staged).use { out -> zipIn.copyTo(out) }
                        }
                    } else if (entryName == "data.json") {
                        dataJsonString = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (!entry.isDirectory && (
                            entryName.startsWith("images/") ||
                            entryName.startsWith("videos/") ||
                            entryName.startsWith("audio/") ||
                            entryName.startsWith("files/")
                        )) {
                        // Extract media file to attachments directory
                        val targetFile = File(mediaBaseDir, entryName)
                        targetFile.parentFile?.mkdirs()

                        FileOutputStream(targetFile).use { out ->
                            zipIn.copyTo(out)
                        }
                        mediaCount++
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (dataJsonString.isNullOrBlank()) {
                return@withContext RestoreResult.Error("Invalid backup: data.json not found in the zip file.")
            }

            val rootJson = JSONObject(dataJsonString)

            // Locked items arrive sealed with the export password
            var opener: BackupSealer? = null
            val lockedMeta = rootJson.optJSONObject("locked")
            if (lockedMeta != null) {
                if (lockedPassword == null || lockedPassword.isEmpty()) {
                    return@withContext RestoreResult.NeedsPassword
                }
                opener = BackupSealer.derive(
                    lockedPassword,
                    android.util.Base64.decode(lockedMeta.getString("salt"), android.util.Base64.NO_WRAP),
                    lockedMeta.getInt("iterations"),
                    lockedMeta.optString("kdf", "PBKDF2WithHmacSHA256")
                )
            }
            // Decrypt staged media of locked items into the attachments folder
            if (opener != null) {
                val stagedRoot = File(lockedTmpDir, "locked")
                val stagedFiles = stagedRoot.walkTopDown().filter { it.isFile && it.name.endsWith(".enc") }.toList()
                for (staged in stagedFiles) {
                    val rel = staged.relativeTo(stagedRoot).path.removeSuffix(".enc")
                    val target = File(mediaBaseDir, rel)
                    if (!target.canonicalPath.startsWith(mediaBaseDir.canonicalPath + File.separator)) continue
                    target.parentFile?.mkdirs()
                    try {
                        BufferedInputStream(FileInputStream(staged)).use { input ->
                            BufferedOutputStream(FileOutputStream(target)).use { out ->
                                opener.decryptStream(input, out)
                            }
                        }
                        mediaCount++
                    } catch (e: Exception) {
                        target.delete()
                        return@withContext RestoreResult.WrongPassword
                    }
                }
            }
            fun unseal(obj: JSONObject): Boolean {
                val blob = obj.optString("sealed", "")
                if (blob.isEmpty()) return true
                val plain = opener?.open(blob) ?: return false
                val payload = JSONObject(plain)
                for (key in payload.keys()) obj.put(key, payload.getString(key))
                return true
            }

            // Settings
            var restoredSettings: BackupSettings? = null
            if (rootJson.has("settings")) {
                val settingsObj = rootJson.getJSONObject("settings")
                val palette = settingsObj.optString("themePalette", "BLUE")
                val isDark = if (settingsObj.isNull("isDarkMode")) null else settingsObj.optBoolean("isDarkMode")
                restoredSettings = BackupSettings(themePalette = palette, isDarkMode = isDark)
            }

            // Tasks
            val tasksArray = rootJson.optJSONArray("tasks") ?: JSONArray()
            val restoredTasks = mutableListOf<Task>()
            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.getJSONObject(i)
                if (!unseal(taskObj)) return@withContext RestoreResult.WrongPassword
                val task = jsonToTask(taskObj)

                // Relink media attachments
                val rawAttachments = AttachmentItem.decodeList(task.attachmentsJson)
                val relinkedAttachments = rawAttachments.map { item ->
                    if (item.uriOrUrl.startsWith("zip://")) {
                        val relPath = item.uriOrUrl.removePrefix("zip://")
                        val file = File(mediaBaseDir, relPath)
                        if (file.exists()) {
                            item.copy(uriOrUrl = Uri.fromFile(file).toString())
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }

                restoredTasks.add(task.copy(attachmentsJson = AttachmentItem.encodeList(relinkedAttachments)))
            }

            // Notes
            val notesArray = rootJson.optJSONArray("notes") ?: JSONArray()
            val restoredNotes = mutableListOf<Note>()
            for (i in 0 until notesArray.length()) {
                val noteObj = notesArray.getJSONObject(i)
                if (!unseal(noteObj)) return@withContext RestoreResult.WrongPassword
                val note = jsonToNote(noteObj)

                // Relink media attachments
                val rawAttachments = AttachmentItem.decodeList(note.attachmentsJson)
                val relinkedAttachments = rawAttachments.map { item ->
                    if (item.uriOrUrl.startsWith("zip://")) {
                        val relPath = item.uriOrUrl.removePrefix("zip://")
                        val file = File(mediaBaseDir, relPath)
                        if (file.exists()) {
                            item.copy(uriOrUrl = Uri.fromFile(file).toString())
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }

                restoredNotes.add(note.copy(attachmentsJson = AttachmentItem.encodeList(relinkedAttachments)))
            }

            // Insert into Room
            if (replaceExisting) {
                repository.clearAllData()
            }

            repository.insertTasks(restoredTasks)
            repository.insertNotes(restoredNotes)

            // Reschedule reminders for any uncompleted tasks
            AlarmScheduler.rescheduleAllActiveAlarms(context)

            RestoreResult.Success(
                taskCount = restoredTasks.size,
                noteCount = restoredNotes.size,
                mediaCount = mediaCount,
                settings = restoredSettings
            )
            } finally {
                lockedTmpDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            RestoreResult.Error("Restore failed: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun copyMediaToZip(
        context: Context,
        item: AttachmentItem,
        zipOut: ZipOutputStream,
        sealer: BackupSealer? = null
    ): String? {
        val uriStr = item.uriOrUrl
        if (uriStr.isBlank() || uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("sample://")) {
            return null
        }

        val folder = when (item.type) {
            AttachmentType.IMAGE -> "images"
            AttachmentType.VIDEO -> "videos"
            AttachmentType.AUDIO -> "audio"
            AttachmentType.FILE -> "files"
            else -> "files"
        }

        val safeTitle = item.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "attachment_${item.id.take(8)}" }
        val entryPath = "$folder/${item.id.take(8)}_$safeTitle"

        return try {
            val mediaIn: InputStream? = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(uriStr))
            } else if (uriStr.startsWith("/")) {
                val file = File(uriStr)
                if (file.exists() && file.isFile) FileInputStream(file) else null
            } else {
                null
            }

            if (mediaIn != null) {
                mediaIn.use { input ->
                    if (sealer != null) {
                        // Stored encrypted under locked/; the logical path is kept for relinking
                        zipOut.putNextEntry(ZipEntry("locked/$entryPath.enc"))
                        sealer.encryptStream(input, zipOut)
                    } else {
                        zipOut.putNextEntry(ZipEntry(entryPath))
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
                entryPath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not stream media attachment $uriStr: ${e.message}")
            null
        }
    }

    private fun taskToJson(task: Task, sealer: BackupSealer?): JSONObject = JSONObject().apply {
        put("id", task.id)
        put("title", task.title)
        put("description", if (task.isLocked) "" else task.description)
        put("dueDate", task.dueDate)
        put("dueTime", task.dueTime)
        put("priority", task.priority.name)
        put("category", task.category.name)
        put("isCompleted", task.isCompleted)
        put("checklistJson", if (task.isLocked) "" else task.checklistJson)
        put("attachmentsJson", if (task.isLocked) "" else task.attachmentsJson)
        put("reminderEnabled", task.reminderEnabled)
        put("reminderMinutesBefore", task.reminderMinutesBefore)
        put("createdAt", task.createdAt)
        put("isLocked", task.isLocked)
        if (task.isLocked && sealer != null) {
            put("sealed", sealer.seal(JSONObject().apply {
                put("description", task.description)
                put("checklistJson", task.checklistJson)
                put("attachmentsJson", task.attachmentsJson)
            }.toString()))
        }
    }

    private fun jsonToTask(json: JSONObject): Task {
        val priorityName = json.optString("priority", Priority.MEDIUM.name)
        val priority = try {
            Priority.valueOf(priorityName)
        } catch (e: Exception) {
            Priority.MEDIUM
        }

        val categoryName = json.optString("category", Category.PERSONAL.name)
        val category = try {
            Category.valueOf(categoryName)
        } catch (e: Exception) {
            Category.PERSONAL
        }

        return Task(
            id = json.optLong("id", 0L),
            title = json.optString("title", ""),
            description = json.optString("description", ""),
            dueDate = json.optLong("dueDate", System.currentTimeMillis()),
            dueTime = json.optString("dueTime", "09:00"),
            priority = priority,
            category = category,
            isCompleted = json.optBoolean("isCompleted", false),
            checklistJson = json.optString("checklistJson", ""),
            attachmentsJson = json.optString("attachmentsJson", ""),
            reminderEnabled = json.optBoolean("reminderEnabled", true),
            reminderMinutesBefore = json.optInt("reminderMinutesBefore", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            isLocked = json.optBoolean("isLocked", false)
        )
    }

    private fun noteToJson(note: Note, sealer: BackupSealer?): JSONObject = JSONObject().apply {
        put("id", note.id)
        if (note.taskId != null) {
            put("taskId", note.taskId)
        } else {
            put("taskId", JSONObject.NULL)
        }
        put("title", note.title)
        put("content", if (note.isLocked) "" else note.content)
        put("colorIndex", note.colorIndex)
        put("checklistJson", if (note.isLocked) "" else note.checklistJson)
        put("attachmentsJson", if (note.isLocked) "" else note.attachmentsJson)
        put("isBold", note.isBold)
        put("isItalic", note.isItalic)
        put("createdAt", note.createdAt)
        put("isLocked", note.isLocked)
        if (note.isLocked && sealer != null) {
            put("sealed", sealer.seal(JSONObject().apply {
                put("content", note.content)
                put("checklistJson", note.checklistJson)
                put("attachmentsJson", note.attachmentsJson)
            }.toString()))
        }
    }

    private fun jsonToNote(json: JSONObject): Note {
        val taskId = if (json.isNull("taskId")) null else json.optLong("taskId")
        return Note(
            id = json.optLong("id", 0L),
            taskId = taskId,
            title = json.optString("title", ""),
            content = json.optString("content", ""),
            colorIndex = json.optInt("colorIndex", 0),
            checklistJson = json.optString("checklistJson", ""),
            attachmentsJson = json.optString("attachmentsJson", ""),
            isBold = json.optBoolean("isBold", false),
            isItalic = json.optBoolean("isItalic", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            isLocked = json.optBoolean("isLocked", false)
        )
    }
}
