package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttachmentItem
import com.example.data.model.AttachmentType
import com.example.data.model.Category
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import com.example.notifications.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavTab(val title: String) {
    TODAY("Today"),
    CALENDAR("Calendar"),
    NOTES("Notes"),
    SETTINGS("Settings")
}

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Navigation
    private val _currentTab = MutableStateFlow(NavTab.TODAY)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Filters & Search
    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(getTodayStartMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sheet / Dialog states
    private val _isAddTaskSheetVisible = MutableStateFlow(false)
    val isAddTaskSheetVisible: StateFlow<Boolean> = _isAddTaskSheetVisible.asStateFlow()

    private val _isTaskDetailSheetVisible = MutableStateFlow(false)
    val isTaskDetailSheetVisible: StateFlow<Boolean> = _isTaskDetailSheetVisible.asStateFlow()

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _isAddNoteDialogVisible = MutableStateFlow(false)
    val isAddNoteDialogVisible: StateFlow<Boolean> = _isAddNoteDialogVisible.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    // Note Editor Screen State
    private val _isNoteEditorOpen = MutableStateFlow(false)
    val isNoteEditorOpen: StateFlow<Boolean> = _isNoteEditorOpen.asStateFlow()

    private val _editorNote = MutableStateFlow<Note?>(null)
    val editorNote: StateFlow<Note?> = _editorNote.asStateFlow()

    // New Note Heading Dialog State
    private val _isNewNoteHeadingDialogVisible = MutableStateFlow(false)
    val isNewNoteHeadingDialogVisible: StateFlow<Boolean> = _isNewNoteHeadingDialogVisible.asStateFlow()

    // Selected Material 3 Color Theme Palette
    private val _selectedThemePalette = MutableStateFlow(com.example.ui.theme.AppThemePalette.BLUE)
    val selectedThemePalette: StateFlow<com.example.ui.theme.AppThemePalette> = _selectedThemePalette.asStateFlow()

    fun setAppThemePalette(palette: com.example.ui.theme.AppThemePalette) {
        _selectedThemePalette.value = palette
    }

    fun openNewNoteHeadingDialog() {
        _isNewNoteHeadingDialogVisible.value = true
    }

    fun closeNewNoteHeadingDialog() {
        _isNewNoteHeadingDialogVisible.value = false
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Notes for the selected task
    val selectedTaskNotes: StateFlow<List<Note>> = _selectedTask
        .flatMapLatest { task ->
            if (task != null) repository.getNotesForTask(task.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // First note preview cache for all tasks: map taskId -> note preview text
    val taskNotePreviews: StateFlow<Map<Long, String>> = allNotes.combine(allTasks) { notes, _ ->
        notes.filter { it.taskId != null }
            .groupBy { it.taskId!! }
            .mapValues { entry -> entry.value.firstOrNull()?.content ?: "" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered tasks for today (with category and search query support)
    val todayTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _categoryFilter,
        _searchQuery,
        allNotes
    ) { tasks, category, query, notes ->
        val todayStart = getTodayStartMillis()
        val todayEnd = todayStart + (24 * 60 * 60 * 1000L) - 1
        val trimmed = query.trim()

        val notesByTaskId = notes.filter { it.taskId != null }
            .groupBy { it.taskId!! }
            .mapValues { entry ->
                entry.value.joinToString(" ") { "${it.title} ${it.content}" }
            }

        tasks.filter { task ->
            val isCatMatch = category == null || task.category == category

            if (trimmed.isEmpty()) {
                val isDateMatch = task.dueDate in todayStart..todayEnd
                isCatMatch && isDateMatch
            } else {
                val matchesTitle = task.title.contains(trimmed, ignoreCase = true)
                val matchesDesc = task.description.contains(trimmed, ignoreCase = true)
                val matchesChecklist = task.checklistJson.contains(trimmed, ignoreCase = true)
                val matchesNote = notesByTaskId[task.id]?.contains(trimmed, ignoreCase = true) == true
                val matchesContent = matchesTitle || matchesDesc || matchesChecklist || matchesNote

                isCatMatch && matchesContent
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks for selected calendar date
    val calendarTasks: StateFlow<List<Task>> = combine(allTasks, _selectedCalendarDate) { tasks, selectedDate ->
        val dateStart = getDayStartMillis(selectedDate)
        val dateEnd = dateStart + (24 * 60 * 60 * 1000L) - 1
        tasks.filter { it.dueDate in dateStart..dateEnd }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and Sheet controls
    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setCategoryFilter(category: Category?) {
        _categoryFilter.value = category
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDate.value = getDayStartMillis(dateMillis)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddTaskSheet(taskToEdit: Task? = null, presetDate: Long? = null) {
        _editingTask.value = taskToEdit
        if (taskToEdit == null && presetDate != null) {
            _selectedCalendarDate.value = getDayStartMillis(presetDate)
        }
        _isAddTaskSheetVisible.value = true
    }

    fun closeAddTaskSheet() {
        _isAddTaskSheetVisible.value = false
        _editingTask.value = null
    }

    fun openTaskDetail(task: Task) {
        _selectedTask.value = task
        _isTaskDetailSheetVisible.value = true
    }

    fun closeTaskDetail() {
        _isTaskDetailSheetVisible.value = false
        _selectedTask.value = null
    }

    fun openAddNoteDialog(noteToEdit: Note? = null) {
        _editingNote.value = noteToEdit
        _isAddNoteDialogVisible.value = true
    }

    fun closeAddNoteDialog() {
        _isAddNoteDialogVisible.value = false
        _editingNote.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // CRUD Operations for Task
    fun saveTask(
        title: String,
        description: String,
        dueDate: Long,
        dueTime: String,
        priority: Priority,
        category: Category,
        attachedNoteContent: String,
        checklistItems: List<ChecklistItem>,
        context: Context
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val checklistEncoded = ChecklistItem.encodeList(checklistItems)
            val editing = _editingTask.value

            if (editing != null) {
                val updated = editing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    dueDate = getDayStartMillis(dueDate),
                    dueTime = dueTime,
                    priority = priority,
                    category = category,
                    checklistJson = checklistEncoded
                )
                repository.updateTask(updated)
                if (attachedNoteContent.isNotBlank()) {
                    repository.insertNote(
                        Note(
                            taskId = updated.id,
                            title = "Note on ${updated.title}",
                            content = attachedNoteContent.trim()
                        )
                    )
                }
                AlarmScheduler.scheduleTaskAlarms(context, updated)
                _selectedTask.value = updated
                _userMessage.value = "Task updated"
            } else {
                val newTask = Task(
                    title = title.trim(),
                    description = description.trim(),
                    dueDate = getDayStartMillis(dueDate),
                    dueTime = dueTime,
                    priority = priority,
                    category = category,
                    checklistJson = checklistEncoded
                )
                val taskId = repository.insertTask(newTask)
                val createdTask = newTask.copy(id = taskId)
                if (attachedNoteContent.isNotBlank()) {
                    repository.insertNote(
                        Note(
                            taskId = taskId,
                            title = "Note on ${createdTask.title}",
                            content = attachedNoteContent.trim()
                        )
                    )
                }
                AlarmScheduler.scheduleTaskAlarms(context, createdTask)
                _userMessage.value = "Task saved with reminder"
            }
            closeAddTaskSheet()
        }
    }

    fun toggleTaskCompletion(task: Task, context: Context) {
        viewModelScope.launch {
            val newStatus = !task.isCompleted
            val updated = task.copy(isCompleted = newStatus)
            repository.updateTask(updated)
            if (_selectedTask.value?.id == task.id) {
                _selectedTask.value = updated
            }
            if (newStatus) {
                AlarmScheduler.cancelTaskAlarms(context, task.id)
                _userMessage.value = "Completed: ${task.title}"
            } else {
                AlarmScheduler.scheduleTaskAlarms(context, updated)
                _userMessage.value = "Task marked active"
            }
        }
    }

    fun deleteTask(task: Task, context: Context) {
        viewModelScope.launch {
            AlarmScheduler.cancelTaskAlarms(context, task.id)
            repository.deleteTaskById(task.id)
            if (_selectedTask.value?.id == task.id) {
                closeTaskDetail()
            }
            _userMessage.value = "Task deleted"
        }
    }

    // Checklist Operations
    fun toggleChecklistItem(task: Task, itemId: String) {
        viewModelScope.launch {
            val currentItems = ChecklistItem.decodeList(task.checklistJson)
            val updatedItems = currentItems.map {
                if (it.id == itemId) it.copy(isDone = !it.isDone) else it
            }
            val updatedTask = task.copy(checklistJson = ChecklistItem.encodeList(updatedItems))
            repository.updateTask(updatedTask)
            _selectedTask.value = updatedTask
        }
    }

    fun addChecklistItem(task: Task, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val currentItems = ChecklistItem.decodeList(task.checklistJson)
            val updatedItems = currentItems + ChecklistItem(text = text.trim())
            val updatedTask = task.copy(checklistJson = ChecklistItem.encodeList(updatedItems))
            repository.updateTask(updatedTask)
            _selectedTask.value = updatedTask
        }
    }

    fun removeChecklistItem(task: Task, itemId: String) {
        viewModelScope.launch {
            val currentItems = ChecklistItem.decodeList(task.checklistJson)
            val updatedItems = currentItems.filterNot { it.id == itemId }
            val updatedTask = task.copy(checklistJson = ChecklistItem.encodeList(updatedItems))
            repository.updateTask(updatedTask)
            _selectedTask.value = updatedTask
        }
    }

    // Note Operations
    fun saveNote(title: String, content: String, colorIndex: Int, taskId: Long? = null) {
        if (content.isBlank() && title.isBlank()) return
        viewModelScope.launch {
            val editing = _editingNote.value
            if (editing != null) {
                repository.updateNote(
                    editing.copy(
                        title = title.trim(),
                        content = content.trim(),
                        colorIndex = colorIndex,
                        taskId = taskId ?: editing.taskId
                    )
                )
                _userMessage.value = "Note updated"
            } else {
                repository.insertNote(
                    Note(
                        title = title.trim(),
                        content = content.trim(),
                        colorIndex = colorIndex,
                        taskId = taskId
                    )
                )
                _userMessage.value = "Note created"
            }
            closeAddNoteDialog()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _userMessage.value = "Note deleted"
        }
    }

    fun createNoteAndOpenEditor(heading: String, colorIndex: Int = 0) {
        viewModelScope.launch {
            val cleanTitle = if (heading.isNotBlank()) heading.trim() else "Untitled Note"
            val newNote = Note(
                title = cleanTitle,
                content = "",
                colorIndex = colorIndex,
                checklistJson = "[]",
                attachmentsJson = "[]"
            )
            val newId = repository.insertNote(newNote)
            _editorNote.value = newNote.copy(id = newId)
            _isNoteEditorOpen.value = true
            _isNewNoteHeadingDialogVisible.value = false
            _isAddNoteDialogVisible.value = false
        }
    }

    fun openNoteEditor(note: Note? = null) {
        if (note != null) {
            _editorNote.value = note
            _isNoteEditorOpen.value = true
        } else {
            _isNewNoteHeadingDialogVisible.value = true
        }
    }

    fun closeNoteEditor() {
        _isNoteEditorOpen.value = false
    }

    fun saveEditorNote(note: Note) {
        _editorNote.value = note
        viewModelScope.launch {
            if (note.id != 0L) {
                repository.updateNote(note)
            } else {
                val newId = repository.insertNote(note)
                _editorNote.value = note.copy(id = newId)
            }
        }
    }

    fun addNoteToSelectedTask(content: String) {
        val currentTask = _selectedTask.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.insertNote(
                Note(
                    taskId = currentTask.id,
                    title = "Note for ${currentTask.title}",
                    content = content.trim()
                )
            )
        }
    }

    companion object {
        fun getTodayStartMillis(): Long {
            return getDayStartMillis(System.currentTimeMillis())
        }

        fun getDayStartMillis(timestamp: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repository = TaskRepository(db.taskDao(), db.noteDao())
                    return TaskViewModel(repository) as T
                }
            }
    }
}
