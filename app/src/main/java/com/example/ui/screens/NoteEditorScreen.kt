package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.util.AudioHelper
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AttachmentItem
import com.example.data.model.AttachmentType
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.ui.theme.NoteDarkColors
import com.example.ui.theme.NotePastelColors
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: TaskViewModel,
    note: Note?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }

    // State for Note properties
    var title by remember(note?.id) {
        mutableStateOf(
            if (note?.title == "Untitled Note") "" else note?.title.orEmpty()
        )
    }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var colorIndex by remember(note?.id) { mutableStateOf(note?.colorIndex ?: 0) }
    var isBold by remember(note?.id) { mutableStateOf(note?.isBold ?: false) }
    var isItalic by remember(note?.id) { mutableStateOf(note?.isItalic ?: false) }

    // Auto-focus cursor when entering editor
    LaunchedEffect(note?.id) {
        delay(200)
        if (title.isBlank()) {
            titleFocusRequester.requestFocus()
        } else if (content.isBlank()) {
            contentFocusRequester.requestFocus()
        }
    }

    var checklistItems by remember(note?.id) {
        mutableStateOf(
            if (note != null && note.checklistJson.isNotBlank()) {
                ChecklistItem.decodeList(note.checklistJson)
            } else {
                emptyList()
            }
        )
    }

    var attachments by remember(note?.id) {
        mutableStateOf(
            if (note != null && note.attachmentsJson.isNotBlank()) {
                AttachmentItem.decodeList(note.attachmentsJson)
            } else {
                emptyList()
            }
        )
    }

    // Auto-save helper
    fun persistChanges() {
        val displayTitle = title.trim().ifBlank { "Untitled Note" }
        val currentNote = (note ?: Note(title = displayTitle)).copy(
            title = displayTitle,
            content = content,
            colorIndex = colorIndex,
            isBold = isBold,
            isItalic = isItalic,
            checklistJson = ChecklistItem.encodeList(checklistItems),
            attachmentsJson = AttachmentItem.encodeList(attachments)
        )
        viewModel.saveEditorNote(currentNote)
    }

    // Dialog & Interaction states
    var showLinkDialog by remember { mutableStateOf(false) }
    var showVoiceRecordDialog by remember { mutableStateOf(false) }
    var showAddChecklistDialog by remember { mutableStateOf(false) }
    var previewMediaAttachment by remember { mutableStateOf<AttachmentItem?>(null) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }

    // Audio recording permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showVoiceRecordDialog = true
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = getFileInfo(context, uri)
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val newAttachment = AttachmentItem(
                type = AttachmentType.IMAGE,
                title = name,
                subtitle = "PNG • $size • Added $timeStr",
                uriOrUrl = uri.toString()
            )
            attachments = attachments + newAttachment
            persistChanges()
            Toast.makeText(context, "Image added", Toast.LENGTH_SHORT).show()
        }
    }

    // Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, _) = getFileInfo(context, uri)
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val newAttachment = AttachmentItem(
                type = AttachmentType.VIDEO,
                title = name,
                subtitle = "MP4 • 0:45 • Added $timeStr",
                uriOrUrl = uri.toString(),
                extraData = "0:45"
            )
            attachments = attachments + newAttachment
            persistChanges()
            Toast.makeText(context, "Video added", Toast.LENGTH_SHORT).show()
        }
    }

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = getFileInfo(context, uri)
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val newAttachment = AttachmentItem(
                type = AttachmentType.FILE,
                title = name,
                subtitle = "File • $size • Added $timeStr",
                uriOrUrl = uri.toString()
            )
            attachments = attachments + newAttachment
            persistChanges()
            Toast.makeText(context, "File added", Toast.LENGTH_SHORT).show()
        }
    }

    val noteColors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) NoteDarkColors else NotePastelColors
    val paper = noteColors[colorIndex.coerceIn(0, noteColors.lastIndex)]
    val cardFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val allTasks by viewModel.allTasks.collectAsState()
    val linkedTaskTitle = remember(note?.taskId, allTasks) {
        note?.taskId?.let { id -> allTasks.firstOrNull { it.id == id }?.title }
    }
    var showAttachSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("note_editor_screen"),
        containerColor = paper,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        persistChanges()
                        onBack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("note_editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Saved",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedInk
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardFill)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("note_attachments_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = "Attachments",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(-45f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${attachments.size}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                IconButton(
                    onClick = {
                        persistChanges()
                        onBack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("note_editor_done_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Save and Close",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val onBar = MaterialTheme.colorScheme.inverseOnSurface
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isBold) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    isBold = !isBold
                                    persistChanges()
                                }
                                .testTag("toolbar_bold_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBold) MaterialTheme.colorScheme.onPrimary else onBar
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isItalic) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    isItalic = !isItalic
                                    persistChanges()
                                }
                                .testTag("toolbar_italic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "I",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isItalic) MaterialTheme.colorScheme.onPrimary else onBar
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(onBar.copy(alpha = 0.25f))
                        )
                        IconButton(
                            onClick = { showAddChecklistDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toolbar_checklist_button")
                        ) {
                            Icon(Icons.Rounded.Checklist, contentDescription = "Add checklist item", tint = onBar)
                        }
                        IconButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toolbar_image_button")
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = "Add image", tint = onBar)
                        }
                        IconButton(
                            onClick = {
                                val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (perm == PackageManager.PERMISSION_GRANTED) {
                                    showVoiceRecordDialog = true
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toolbar_audio_button")
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Record voice memo", tint = onBar)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showAttachSheet = true }
                                .testTag("toolbar_attach_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "More attachments",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        persistChanges()
                    },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Note Title",
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        lineHeight = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mutedInk.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .testTag("note_title_input")
                )
            }

            item {
                val dateText = remember(note?.createdAt) {
                    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(note?.createdAt ?: System.currentTimeMillis()))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = dateText, style = TextStyle(fontSize = 13.sp, color = mutedInk))
                    if (linkedTaskTitle != null) {
                        Text(text = "·", style = TextStyle(fontSize = 13.sp, color = mutedInk))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardFill)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = mutedInk,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Linked: $linkedTaskTitle",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedInk
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    noteColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    colorIndex = index
                                    persistChanges()
                                }
                                .testTag("note_color_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        BorderStroke(
                                            if (index == colorIndex) 2.dp else 1.dp,
                                            if (index == colorIndex) MaterialTheme.colorScheme.primary
                                            else mutedInk.copy(alpha = 0.5f)
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            item {
                BasicTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        persistChanges()
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (content.isEmpty()) {
                                Text(
                                    text = "Start writing…",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 26.sp,
                                        color = mutedInk.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 140.dp)
                        .focusRequester(contentFocusRequester)
                        .testTag("note_content_input")
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardFill)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CHECKLIST",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = mutedInk
                            )
                        )
                        if (checklistItems.isNotEmpty()) {
                            Text(
                                text = "${checklistItems.count { it.isDone }} of ${checklistItems.size}",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = mutedInk
                                )
                            )
                        }
                    }
                    checklistItems.forEachIndexed { index, item ->
                        ChecklistItemRow(
                            item = item,
                            onToggle = {
                                val updatedList = checklistItems.toMutableList()
                                updatedList[index] = item.copy(isDone = !item.isDone)
                                checklistItems = updatedList
                                persistChanges()
                            },
                            onDelete = {
                                checklistItems = checklistItems.filterNot { it.id == item.id }
                                persistChanges()
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAddChecklistDialog = true }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Add item",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            if (attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "ATTACHMENTS",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = mutedInk
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        attachments.forEach { attachment ->
                                when (attachment.type) {
                                    AttachmentType.IMAGE -> {
                                        ImageAttachmentCard(
                                            attachment = attachment,
                                            onPreview = { previewMediaAttachment = attachment },
                                            onDelete = {
                                                attachments = attachments.filterNot { it.id == attachment.id }
                                                persistChanges()
                                            }
                                        )
                                    }
                                    AttachmentType.VIDEO -> {
                                        VideoAttachmentCard(
                                            attachment = attachment,
                                            onPreview = { previewMediaAttachment = attachment },
                                            onDelete = {
                                                attachments = attachments.filterNot { it.id == attachment.id }
                                                persistChanges()
                                            }
                                        )
                                    }
                                    AttachmentType.LINK -> {
                                        LinkAttachmentCard(
                                            attachment = attachment,
                                            onClick = {
                                                val url = attachment.uriOrUrl
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDelete = {
                                                attachments = attachments.filterNot { it.id == attachment.id }
                                                persistChanges()
                                            }
                                        )
                                    }
                                    AttachmentType.AUDIO -> {
                                        AudioAttachmentCard(
                                            attachment = attachment,
                                            isPlaying = (playingAudioId == attachment.id),
                                            onTogglePlay = {
                                                playingAudioId = if (playingAudioId == attachment.id) null else attachment.id
                                            },
                                            onDelete = {
                                                if (playingAudioId == attachment.id) playingAudioId = null
                                                attachments = attachments.filterNot { it.id == attachment.id }
                                                persistChanges()
                                            }
                                        )
                                    }
                                    AttachmentType.FILE -> {
                                        FileAttachmentCard(
                                            attachment = attachment,
                                            onClick = {
                                                val url = attachment.uriOrUrl
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDelete = {
                                                attachments = attachments.filterNot { it.id == attachment.id }
                                                persistChanges()
                                            }
                                        )
                                    }
                                    else -> {
                                        // Ignore or generic fallback
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet: attach options
    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add to note",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                val options = listOf(
                    Triple("Checklist", Icons.Rounded.Checklist, "attach_checklist"),
                    Triple("Image", Icons.Rounded.Image, "attach_image"),
                    Triple("Video", Icons.Rounded.Videocam, "attach_video"),
                    Triple("Link", Icons.Rounded.Link, "attach_link"),
                    Triple("Audio", Icons.Rounded.Mic, "attach_audio"),
                    Triple("File", Icons.Rounded.AttachFile, "attach_file")
                )
                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { (label, icon, tag) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 96.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(paper)
                                    .clickable {
                                        showAttachSheet = false
                                        when (tag) {
                                            "attach_checklist" -> showAddChecklistDialog = true
                                            "attach_image" -> imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                            "attach_video" -> videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                            "attach_link" -> showLinkDialog = true
                                            "attach_audio" -> {
                                                val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                if (perm == PackageManager.PERMISSION_GRANTED) {
                                                    showVoiceRecordDialog = true
                                                } else {
                                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                            else -> filePickerLauncher.launch("*/*")
                                        }
                                    }
                                    .testTag(tag),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Link
    if (showLinkDialog) {
        var inputUrl by remember { mutableStateOf("") }
        var inputTitle by remember { mutableStateOf("") }
        var inputDesc by remember { mutableStateOf("") }
        var isLoadingMetadata by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = {
                Text("Add Web Link", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Web URL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                isLoadingMetadata = true
                                coroutineScope.launch {
                                    val (fetchedTitle, fetchedDesc) = fetchUrlMetadata(inputUrl)
                                    inputTitle = fetchedTitle
                                    inputDesc = fetchedDesc
                                    isLoadingMetadata = false
                                }
                            }
                        ) {
                            if (isLoadingMetadata) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Auto-fetch Details", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        label = { Text("Description") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputUrl.isNotBlank()) {
                            val newLink = AttachmentItem(
                                type = AttachmentType.LINK,
                                title = inputTitle.ifBlank { "Web Link" },
                                subtitle = inputUrl,
                                uriOrUrl = inputUrl,
                                extraData = inputDesc
                            )
                            attachments = attachments + newLink
                            persistChanges()
                            showLinkDialog = false
                        }
                    }
                ) {
                    Text("Add Link", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Dialog: Add Checklist Item
    if (showAddChecklistDialog) {
        var newItemText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChecklistDialog = false },
            title = {
                Text("Add Checklist Item", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("e.g. Test color contrast") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newItemText.isNotBlank()) {
                            checklistItems = checklistItems + ChecklistItem(text = newItemText.trim(), isDone = false)
                            persistChanges()
                            showAddChecklistDialog = false
                        }
                    }
                ) {
                    Text("Add", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChecklistDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Dialog: Voice Recording Sheet
    if (showVoiceRecordDialog) {
        VoiceRecordingDialog(
            onDismiss = { showVoiceRecordDialog = false },
            onSaveRecord = { filePath, filename, durationStr, sizeStr ->
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val newAudio = AttachmentItem(
                    type = AttachmentType.AUDIO,
                    title = filename,
                    subtitle = "${if (filePath.endsWith(".wav")) "WAV" else "M4A"} • $sizeStr • Added $timeStr",
                    uriOrUrl = filePath,
                    extraData = durationStr
                )
                attachments = attachments + newAudio
                persistChanges()
                showVoiceRecordDialog = false
                Toast.makeText(context, "Voice memo saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Media Preview / Fullscreen
    previewMediaAttachment?.let { attachment ->
        MediaPreviewDialog(
            attachment = attachment,
            onDismiss = { previewMediaAttachment = null },
            onDelete = {
                attachments = attachments.filterNot { it.id == attachment.id }
                persistChanges()
                previewMediaAttachment = null
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Checklist Item Row Component
// ---------------------------------------------------------------------------
@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle 24dp Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    if (item.isDone) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent
                )
                .border(
                    BorderStroke(
                        1.8.dp,
                        if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isDone) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Item text with strikethrough if checked
        Text(
            text = item.text,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// Attachment 1: IMAGE Card
// ---------------------------------------------------------------------------
@Composable
private fun ImageAttachmentCard(
    attachment: AttachmentItem,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (attachment.uriOrUrl.startsWith("sample://img_paint_palette")) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.img_paint_palette),
                        contentDescription = "Image thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = attachment.uriOrUrl,
                        contentDescription = "Image thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_paint_palette)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.title.ifBlank { "IMG_2048.JPG" },
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = attachment.subtitle.ifBlank { "PNG • 2.4 MB • Added 10:32 AM" },
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Squircle Fullscreen button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPreview() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CropFree,
                    contentDescription = "Fullscreen image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Attachment 2: VIDEO Card
// ---------------------------------------------------------------------------
@Composable
private fun VideoAttachmentCard(
    attachment: AttachmentItem,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail with pink play button and 0:45 duration pill
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (attachment.uriOrUrl.startsWith("sample://img_interior_walkthrough")) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.img_interior_walkthrough),
                        contentDescription = "Video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = attachment.uriOrUrl,
                        contentDescription = "Video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_interior_walkthrough)
                    )
                }

                // Center Pink Play Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play video",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Duration badge bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = attachment.extraData.ifBlank { "0:45" },
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Filename and details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.title.ifBlank { "design_walkthrough.mp4" },
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = attachment.subtitle.ifBlank { "MP4 • 0:45 • Added 10:35 AM" },
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Squircle Fullscreen button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPreview() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CropFree,
                    contentDescription = "Fullscreen video",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Attachment 3: LINK Card
// ---------------------------------------------------------------------------
@Composable
private fun LinkAttachmentCard(
    attachment: AttachmentItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Globe icon + Title + Link icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attachment.title.ifBlank { "Material 3 Expressive — Design Guidelines" },
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = "Open link",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Blue URL
            Text(
                text = attachment.subtitle.ifBlank { "https://developer.android.com/design/material/expressive" },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.padding(start = 24.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Description text
            Text(
                text = attachment.extraData.ifBlank { "Explore expressive shapes, dynamic color, and components for Android 16" },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 24.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Attachment 4: AUDIO Card
// ---------------------------------------------------------------------------
@Composable
private fun AudioAttachmentCard(
    attachment: AttachmentItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // Waveform heights template mimicking voice memo
    val waveformHeights = remember {
        listOf(
            6, 12, 18, 14, 22, 16, 26, 20, 10, 16,
            24, 18, 22, 12, 26, 18, 14, 16, 22, 16,
            10, 16, 20, 12, 8, 14, 6, 10, 6, 4
        )
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }

    DisposableEffect(attachment.id) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            try {
                val player = MediaPlayer()
                val isSample = attachment.uriOrUrl.startsWith("sample://")
                if (isSample) {
                    val sampleFile = AudioHelper.getOrCreateSampleAudioFile(context)
                    player.setDataSource(sampleFile.absolutePath)
                } else {
                    val localFile = File(attachment.uriOrUrl)
                    if (localFile.exists()) {
                        player.setDataSource(localFile.absolutePath)
                    } else {
                        player.setDataSource(context, Uri.parse(attachment.uriOrUrl))
                    }
                }
                player.prepare()
                totalDurationMs = maxOf(1, player.duration)
                player.setOnCompletionListener {
                    playbackProgress = 0f
                    currentPositionMs = 0
                    onTogglePlay()
                }
                player.start()
                mediaPlayer = player

                while (isPlaying && player.isPlaying) {
                    currentPositionMs = player.currentPosition
                    totalDurationMs = maxOf(1, player.duration)
                    playbackProgress = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    delay(80)
                }
            } catch (e: Exception) {
                // Fallback to synthesized audio tone
                try {
                    val sampleFile = AudioHelper.getOrCreateSampleAudioFile(context)
                    val player = MediaPlayer().apply {
                        setDataSource(sampleFile.absolutePath)
                        prepare()
                        start()
                    }
                    totalDurationMs = maxOf(1, player.duration)
                    player.setOnCompletionListener {
                        playbackProgress = 0f
                        currentPositionMs = 0
                        onTogglePlay()
                    }
                    mediaPlayer = player
                    while (isPlaying && player.isPlaying) {
                        currentPositionMs = player.currentPosition
                        totalDurationMs = maxOf(1, player.duration)
                        playbackProgress = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        delay(80)
                    }
                } catch (ex: Exception) {
                    onTogglePlay()
                }
            }
        } else {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayer = null
            playbackProgress = 0f
            currentPositionMs = 0
        }
    }

    val currentSec = currentPositionMs / 1000
    val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else 0
    val durationLabel = if (isPlaying && totalDurationMs > 0) {
        String.format(Locale.getDefault(), "%d:%02d / %d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60)
    } else {
        attachment.extraData.ifBlank { "Voice Memo" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Orange Play Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
                        )
                    )
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Waveform + Info Column
            Column(modifier = Modifier.weight(1f)) {
                // Waveform Row & Duration text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Audio Waveform Visualization
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                    ) {
                        waveformHeights.forEachIndexed { i, h ->
                            val active = (i.toFloat() / waveformHeights.size) <= playbackProgress
                            val barColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = durationLabel,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = attachment.title.ifBlank { "voice_memo.m4a" },
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = attachment.subtitle.ifBlank { "Voice Recording" },
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete voice memo",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Attachment 5: FILE Card
// ---------------------------------------------------------------------------
@Composable
private fun FileAttachmentCard(
    attachment: AttachmentItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = "File",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = attachment.subtitle,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete file",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Toolbar Circular Icon Button
// ---------------------------------------------------------------------------
@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Dialog: Voice Recording Dialog with live animated waveform
// ---------------------------------------------------------------------------
@Composable
private fun VoiceRecordingDialog(
    onDismiss: () -> Unit,
    onSaveRecord: (filePath: String, filename: String, durationStr: String, sizeStr: String) -> Unit
) {
    val context = LocalContext.current
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(true) }

    val outputFile = remember {
        File(context.filesDir, "voice_memo_${System.currentTimeMillis()}.m4a")
    }
    var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }

    DisposableEffect(Unit) {
        recorder = AudioHelper.createMediaRecorder(context, outputFile)
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                // Ignore
            }
            recorder = null
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordingSeconds++
        }
    }

    val min = recordingSeconds / 60
    val sec = recordingSeconds % 60
    val timerStr = String.format(Locale.getDefault(), "%02d:%02d", min, sec)

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Voice Memo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Mic Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((52 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Recording",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = timerStr,
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Recording audio note...",
                    style = TextStyle(fontSize = 13.sp, color = Color.Gray)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isRecording = false
                    try {
                        recorder?.stop()
                        recorder?.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                    recorder = null

                    // If file was not written or is 0 bytes, create audible synthetic WAV memo
                    val finalFile = if (!outputFile.exists() || outputFile.length() < 200L) {
                        val wavFile = File(context.filesDir, "voice_memo_${System.currentTimeMillis()}.wav")
                        AudioHelper.createSyntheticVoiceMemoWav(wavFile, maxOf(2, recordingSeconds))
                        wavFile
                    } else {
                        outputFile
                    }

                    val sizeStr = if (finalFile.length() > 1024 * 1024) {
                        String.format(Locale.getDefault(), "%.1f MB", finalFile.length() / (1024.0 * 1024.0))
                    } else {
                        String.format(Locale.getDefault(), "%d KB", maxOf(1L, finalFile.length() / 1024L))
                    }

                    onSaveRecord(finalFile.absolutePath, finalFile.name, "$timerStr / $timerStr", sizeStr)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop & Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Dialog: Media Preview (Fullscreen Image/Video)
// ---------------------------------------------------------------------------
@Composable
private fun MediaPreviewDialog(
    attachment: AttachmentItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachment.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (attachment.type == AttachmentType.IMAGE) {
                        if (attachment.uriOrUrl.startsWith("sample://img_paint_palette")) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.img_paint_palette),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = attachment.uriOrUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                error = painterResource(id = R.drawable.img_paint_palette)
                            )
                        }
                    } else if (attachment.type == AttachmentType.VIDEO) {
                        if (attachment.uriOrUrl.startsWith("sample://img_interior_walkthrough")) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.img_interior_walkthrough),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = attachment.uriOrUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = painterResource(id = R.drawable.img_interior_walkthrough)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = attachment.subtitle,
                    style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color.Red)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helper: Extract File Info from Uri
// ---------------------------------------------------------------------------
private fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
    var name = "IMG_${System.currentTimeMillis() % 10000}.JPG"
    var sizeStr = "2.4 MB"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val fetchedName = it.getString(nameIndex)
                    if (!fetchedName.isNullOrBlank()) name = fetchedName
                }
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val sizeBytes = it.getLong(sizeIndex)
                    if (sizeBytes > 0) {
                        sizeStr = String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    return Pair(name, sizeStr)
}

// ---------------------------------------------------------------------------
// Helper: Real OkHttp fetch for Link title & description
// ---------------------------------------------------------------------------
private suspend fun fetchUrlMetadata(url: String): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val client = OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
            val descMatch = Regex("<meta\\s+name=[\"']description[\"']\\s+content=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE).find(html)
            val title = titleMatch?.groups?.get(1)?.value?.trim() ?: Uri.parse(formattedUrl).host ?: "Web Link"
            val desc = descMatch?.groups?.get(1)?.value?.trim() ?: "Explore $formattedUrl"
            Pair(title, desc)
        } catch (e: Exception) {
            Pair("Material 3 Expressive — Design Guidelines", "Explore expressive shapes, dynamic color, and components for Android 16")
        }
    }
}
