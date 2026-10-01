package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.TaskDao
import com.example.data.model.Note
import com.example.data.model.Task
import com.example.util.NoteCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun Task.sealed() = if (!isLocked) this else copy(
    description = NoteCrypto.encrypt(description),
    checklistJson = NoteCrypto.encrypt(checklistJson),
    attachmentsJson = NoteCrypto.encrypt(attachmentsJson)
)

private fun Task.opened() = if (!isLocked) this else copy(
    description = NoteCrypto.decrypt(description),
    checklistJson = NoteCrypto.decrypt(checklistJson),
    attachmentsJson = NoteCrypto.decrypt(attachmentsJson)
)

private fun Note.sealed() = if (!isLocked) this else copy(
    content = NoteCrypto.encrypt(content),
    checklistJson = NoteCrypto.encrypt(checklistJson),
    attachmentsJson = NoteCrypto.encrypt(attachmentsJson)
)

private fun Note.opened() = if (!isLocked) this else copy(
    content = NoteCrypto.decrypt(content),
    checklistJson = NoteCrypto.decrypt(checklistJson),
    attachmentsJson = NoteCrypto.decrypt(attachmentsJson)
)

/** Locked tasks/notes are encrypted at rest; callers always see plain text. */
class TaskRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks().map { list -> list.map { it.opened() } }
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes().map { list -> list.map { it.opened() } }

    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>> {
        return taskDao.getTasksForDate(startOfDay, endOfDay).map { list -> list.map { it.opened() } }
    }

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id).map { it?.opened() }

    suspend fun getTaskByIdSync(id: Long): Task? = taskDao.getTaskByIdSync(id)?.opened()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task.sealed())

    suspend fun updateTask(task: Task) = taskDao.updateTask(task.sealed())

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        noteDao.deleteNotesByTaskId(id)
        taskDao.deleteTaskById(id)
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        taskDao.setTaskCompletion(id, completed)
    }

    suspend fun getActiveReminderTasks(): List<Task> = taskDao.getActiveReminderTasks().map { it.opened() }

    suspend fun updateTaskReminder(id: Long, reminderEnabled: Boolean, reminderMinutesBefore: Int) {
        taskDao.updateTaskReminder(id, reminderEnabled, reminderMinutesBefore)
    }

    fun getNotesForTask(taskId: Long): Flow<List<Note>> = noteDao.getNotesForTask(taskId).map { list -> list.map { it.opened() } }

    fun getFirstNoteForTask(taskId: Long): Flow<Note?> = noteDao.getFirstNoteForTask(taskId).map { it?.opened() }

    suspend fun getNotesForTaskSync(taskId: Long): List<Note> = noteDao.getNotesForTaskSync(taskId).map { it.opened() }

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note.sealed())

    suspend fun updateNote(note: Note) = noteDao.updateNote(note.sealed())

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
    suspend fun getAllTasksSync(): List<Task> = taskDao.getAllTasksSync().map { it.opened() }

    suspend fun getAllNotesSync(): List<Note> = noteDao.getAllNotesSync().map { it.opened() }

    suspend fun insertTasks(tasks: List<Task>) = taskDao.insertTasks(tasks.map { it.sealed() })

    suspend fun insertNotes(notes: List<Note>) = noteDao.insertNotes(notes.map { it.sealed() })

    suspend fun clearAllData() {
        noteDao.deleteAllNotes()
        taskDao.deleteAllTasks()
    }
}
