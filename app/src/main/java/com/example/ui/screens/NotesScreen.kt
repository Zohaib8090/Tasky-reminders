package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.data.model.Task
import com.example.ui.components.EmptyStateIllustration
import com.example.ui.theme.NoteDarkColors
import com.example.ui.theme.NotePastelColors
import com.example.ui.theme.PillShape
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: TaskViewModel,
    allNotes: List<Note>,
    allTasks: List<Task>,
    isAddNoteDialogVisible: Boolean,
    editingNote: Note?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    var searchQuery by remember { mutableStateOf("") }

    // Map task id to task title for linked note badge
    val taskMap = remember(allTasks) { allTasks.associateBy { it.id } }

    val filteredNotes = remember(allNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val noteColors = if (isDark) NoteDarkColors else NotePastelColors

    val isNewNoteHeadingDialogVisible by viewModel.isNewNoteHeadingDialogVisible.collectAsState()

    // Dialog: Prompt Heading before opening Editor
    if (isNewNoteHeadingDialogVisible) {
        var noteHeading by remember { mutableStateOf("") }
        var selectedColorIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.closeNewNoteHeadingDialog() },
            title = {
                Text(
                    text = "New Note",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a title or heading to get started. You can write your note text, checklist, voice memos, and attachments inside.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = noteHeading,
                        onValueChange = { noteHeading = it },
                        label = { Text("Heading / Title *") },
                        placeholder = { Text("e.g. Design Ideas, Weekly Grocery, Trip Plan...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_note_heading_input")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Note Card Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        noteColors.forEachIndexed { index, color ->
                            val isSelected = (selectedColorIndex == index)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedColorIndex = index
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createNoteAndOpenEditor(
                            heading = noteHeading,
                            colorIndex = selectedColorIndex
                        )
                    },
                    shape = PillShape,
                    modifier = Modifier.testTag("continue_to_note_editor_button")
                ) {
                    Text("Open Editor", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeNewNoteHeadingDialog() }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("new_note_heading_dialog")
        )
    }

    // Add / Edit Note Dialog (simple fallback)
    if (isAddNoteDialogVisible) {
        var noteTitle by remember { mutableStateOf(editingNote?.title ?: "") }
        var noteContent by remember { mutableStateOf(editingNote?.content ?: "") }
        var selectedColorIndex by remember { mutableIntStateOf(editingNote?.colorIndex ?: 0) }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddNoteDialog() },
            title = {
                Text(
                    text = if (editingNote != null) "Edit Note" else "New Note",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("Note title...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Content *") },
                        placeholder = { Text("Write your thoughts, checklist, or ideas...") },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_content_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Card Color Tint",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        noteColors.forEachIndexed { index, color ->
                            val isSelected = selectedColorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedColorIndex = index
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank() || noteTitle.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveNote(
                                title = noteTitle,
                                content = noteContent,
                                colorIndex = selectedColorIndex,
                                taskId = editingNote?.taskId
                            )
                        }
                    },
                    shape = PillShape,
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text(if (editingNote != null) "Update" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddNoteDialog() }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("note_dialog")
        )
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_staggered_grid"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 120.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        // Expressive Header (Span full width)
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK THOUGHTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openNewNoteHeadingDialog()
                                }
                                .testTag("create_new_note_top_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${allNotes.size} notes",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Notes Nest",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = PillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_notes_input")
                )
            }
        }

        // Empty state or Notes Grid
        if (filteredNotes.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyStateIllustration(
                    title = if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"" else "No notes in your nest",
                    subtitle = "Capture quick ideas, task checklists, or links anytime.",
                    actionLabel = "Create a Note",
                    onAction = { viewModel.openNewNoteHeadingDialog() },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else {
            items(
                items = filteredNotes,
                key = { it.id }
            ) { note ->
                val bg = noteColors.getOrElse(note.colorIndex % noteColors.size) { noteColors[0] }
                val linkedTask = note.taskId?.let { taskMap[it] }
                val timeFormat = SimpleDateFormat("MMM d", Locale.getDefault())

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.openNoteEditor(note = note)
                        }
                        .testTag("note_card_${note.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top row: Title + Delete icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            if (note.title.isNotBlank()) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isDark) Color(0xFFF1F1F1) else Color(0xFF202124),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deleteNote(note)
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("delete_note_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete note",
                                    tint = if (isDark) Color(0xFFAAAAAA) else Color(0xFF757575),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (note.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = if (isDark) Color(0xFFD0D0D0) else Color(0xFF3C4043),
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer row: Linked Task Chip or Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (linkedTask != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = "Linked task",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = linkedTask.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 80.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Text(
                                text = timeFormat.format(Date(note.createdAt)),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF70757A)
                            )
                        }
                    }
                }
            }
        }
    }
}
