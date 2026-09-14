package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.SquircleFab
import com.example.ui.screens.AddTaskBottomSheet
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.TaskDetailBottomSheet
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.TaskNestTheme
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.TaskViewModel
import com.example.workers.TaskSyncWorker

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModel.provideFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enqueue periodic maintenance
        TaskSyncWorker.enqueuePeriodicSync(this)

        // Handle intent if opened from reminder notification
        val navigateTaskId = intent.getLongExtra("EXTRA_NAVIGATE_TASK_ID", -1L)

        setContent {
            val selectedPalette by viewModel.selectedThemePalette.collectAsState()
            TaskNestTheme(palette = selectedPalette) {
                MainAppScreen(
                    viewModel = viewModel,
                    initialTaskId = if (navigateTaskId != -1L) navigateTaskId else null
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val taskId = intent.getLongExtra("EXTRA_NAVIGATE_TASK_ID", -1L)
        if (taskId != -1L) {
            viewModel.loadAndOpenTaskById(taskId)
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: TaskViewModel,
    initialTaskId: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // State collections
    val currentTab by viewModel.currentTab.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val calendarTasks by viewModel.calendarTasks.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    val selectedDateMillis by viewModel.selectedCalendarDate.collectAsState()
    val notePreviews by viewModel.taskNotePreviews.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Sheet / Dialog states
    val isAddTaskSheetVisible by viewModel.isAddTaskSheetVisible.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val isTaskDetailVisible by viewModel.isTaskDetailSheetVisible.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val selectedTaskNotes by viewModel.selectedTaskNotes.collectAsState()
    val isAddNoteDialogVisible by viewModel.isAddNoteDialogVisible.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val editorNote by viewModel.editorNote.collectAsState()

    if (isNoteEditorOpen) {
        NoteEditorScreen(
            viewModel = viewModel,
            note = editorNote,
            onBack = { viewModel.closeNoteEditor() }
        )
        return
    }

    // Handle initial navigation from notification tap
    LaunchedEffect(initialTaskId, allTasks) {
        if (initialTaskId != null && allTasks.isNotEmpty()) {
            val task = allTasks.find { it.id == initialTaskId }
            if (task != null) {
                viewModel.openTaskDetail(task)
            }
        }
    }

    // Handle user snackbar messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasknest_main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated content for tabs: Today, Calendar, Notes
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 120,
                            easing = FastOutSlowInEasing
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = 80,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    NavTab.TODAY -> {
                        TodayScreen(
                            viewModel = viewModel,
                            todayTasks = todayTasks,
                            allTasks = allTasks,
                            notePreviews = notePreviews,
                            selectedCategory = selectedCategory
                        )
                    }

                    NavTab.CALENDAR -> {
                        CalendarScreen(
                            viewModel = viewModel,
                            calendarTasks = calendarTasks,
                            allTasks = allTasks,
                            selectedDateMillis = selectedDateMillis,
                            notePreviews = notePreviews
                        )
                    }

                    NavTab.NOTES -> {
                        NotesScreen(
                            viewModel = viewModel,
                            allNotes = allNotes,
                            allTasks = allTasks,
                            isAddNoteDialogVisible = isAddNoteDialogVisible,
                            editingNote = editingNote
                        )
                    }

                    NavTab.SETTINGS -> {
                        com.example.ui.screens.SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Squircle FAB
            if (currentTab != NavTab.SETTINGS) {
                SquircleFab(
                    onClick = {
                        if (currentTab == NavTab.NOTES) {
                            viewModel.openNewNoteHeadingDialog()
                        } else {
                            viewModel.openAddTaskSheet()
                        }
                    },
                    contentDescription = if (currentTab == NavTab.NOTES) "Add Note" else "Add Task",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 86.dp)
                )
            }

            // Floating Pill Bottom Navigation Bar (Requirement 2: "Bottom navigation is a floating pill")
            FloatingPillBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Add Task Bottom Sheet (Modal Bottom Sheet)
        if (isAddTaskSheetVisible) {
            AddTaskBottomSheet(
                viewModel = viewModel,
                editingTask = editingTask,
                onDismiss = { viewModel.closeAddTaskSheet() }
            )
        }

        // Task Detail Bottom Sheet
        if (isTaskDetailVisible && selectedTask != null) {
            TaskDetailBottomSheet(
                viewModel = viewModel,
                task = selectedTask!!,
                notes = selectedTaskNotes,
                onDismiss = { viewModel.closeTaskDetail() }
            )
        }
    }
}
