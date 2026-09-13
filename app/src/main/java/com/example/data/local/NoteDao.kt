package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getNotesForTask(taskId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getNotesForTaskSync(taskId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE taskId = :taskId LIMIT 1")
    fun getFirstNoteForTask(taskId: Long): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes WHERE taskId = :taskId")
    suspend fun deleteNotesByTaskId(taskId: Long)
}
