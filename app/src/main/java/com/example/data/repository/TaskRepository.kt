package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.TaskDao
import com.example.data.model.Note
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>> {
        return taskDao.getTasksForDate(startOfDay, endOfDay)
    }

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun getTaskByIdSync(id: Long): Task? = taskDao.getTaskByIdSync(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

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

    fun getNotesForTask(taskId: Long): Flow<List<Note>> = noteDao.getNotesForTask(taskId)

    fun getFirstNoteForTask(taskId: Long): Flow<Note?> = noteDao.getFirstNoteForTask(taskId)

    suspend fun getNotesForTaskSync(taskId: Long): List<Note> = noteDao.getNotesForTaskSync(taskId)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
}
