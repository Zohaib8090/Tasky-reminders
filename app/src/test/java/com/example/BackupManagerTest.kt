package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupResult
import com.example.data.backup.BackupSettings
import com.example.data.backup.RestoreResult
import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var tempBackupFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepository(database.taskDao(), database.noteDao())
        tempBackupFile = File(context.cacheDir, "test_backup.zip")
        if (tempBackupFile.exists()) tempBackupFile.delete()
    }

    @After
    fun tearDown() {
        database.close()
        if (tempBackupFile.exists()) tempBackupFile.delete()
    }

    @Test
    fun testGenerateBackupFileNameFormat() {
        val fileName = BackupManager.generateBackupFileName()
        assertTrue(fileName.startsWith("TaskNest_Backup_"))
        assertTrue(fileName.endsWith(".zip"))
        assertTrue(Regex("TaskNest_Backup_\\d{4}-\\d{2}-\\d{2}\\.zip").matches(fileName))
    }

    @Test
    fun testBackupAndRestoreCycle() = runBlocking {
        // Insert sample task and note
        val checklists = listOf(
            ChecklistItem(text = "Buy ingredients", isDone = true),
            ChecklistItem(text = "Cook dinner", isDone = false)
        )
        val taskId = repository.insertTask(
            Task(
                title = "Dinner Prep",
                description = "Making spaghetti",
                dueDate = System.currentTimeMillis(),
                dueTime = "19:00",
                priority = Priority.HIGH,
                category = Category.PERSONAL,
                checklistJson = ChecklistItem.encodeList(checklists)
            )
        )

        val noteId = repository.insertNote(
            Note(
                taskId = taskId,
                title = "Recipe Notes",
                content = "Add basil at the end",
                colorIndex = 2
            )
        )

        // Run Backup
        val backupUri = Uri.fromFile(tempBackupFile)
        val settings = BackupSettings(themePalette = "PURPLE", isDarkMode = true)
        val backupResult = BackupManager.createBackup(
            context = context,
            destinationUri = backupUri,
            repository = repository,
            settings = settings
        )

        assertTrue(backupResult is BackupResult.Success)
        val success = backupResult as BackupResult.Success
        assertEquals(1, success.taskCount)
        assertEquals(1, success.noteCount)
        assertTrue(tempBackupFile.exists())

        // Verify zip contents: data.json, images/, videos/, audio/
        ZipFile(tempBackupFile).use { zip ->
            assertNotNull(zip.getEntry("data.json"))
            assertNotNull(zip.getEntry("images/"))
            assertNotNull(zip.getEntry("videos/"))
            assertNotNull(zip.getEntry("audio/"))
        }

        // Clear existing database
        repository.clearAllData()
        assertEquals(0, repository.getAllTasksSync().size)
        assertEquals(0, repository.getAllNotesSync().size)

        // Run Restore
        val restoreResult = BackupManager.restoreBackup(
            context = context,
            sourceUri = backupUri,
            repository = repository,
            replaceExisting = true
        )

        assertTrue(restoreResult is RestoreResult.Success)
        val restoreSuccess = restoreResult as RestoreResult.Success
        assertEquals(1, restoreSuccess.taskCount)
        assertEquals(1, restoreSuccess.noteCount)
        assertNotNull(restoreSuccess.settings)
        assertEquals("PURPLE", restoreSuccess.settings?.themePalette)
        assertEquals(true, restoreSuccess.settings?.isDarkMode)

        // Verify restored entities in Room DB
        val restoredTasks = repository.getAllTasksSync()
        assertEquals(1, restoredTasks.size)
        val restoredTask = restoredTasks.first()
        assertEquals("Dinner Prep", restoredTask.title)
        assertEquals(Priority.HIGH, restoredTask.priority)

        val restoredChecklist = ChecklistItem.decodeList(restoredTask.checklistJson)
        assertEquals(2, restoredChecklist.size)
        assertEquals("Buy ingredients", restoredChecklist[0].text)
        assertTrue(restoredChecklist[0].isDone)
        assertEquals("Cook dinner", restoredChecklist[1].text)
        assertEquals(false, restoredChecklist[1].isDone)

        val restoredNotes = repository.getAllNotesSync()
        assertEquals(1, restoredNotes.size)
        val restoredNote = restoredNotes.first()
        assertEquals("Recipe Notes", restoredNote.title)
        assertEquals("Add basil at the end", restoredNote.content)
        assertEquals(2, restoredNote.colorIndex)
    }
}
