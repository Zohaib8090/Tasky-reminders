package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Task
import com.example.ui.components.EmptyStateIllustration
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskSearchBar
import com.example.ui.theme.PillShape
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TaskViewModel,
    todayTasks: List<Task>,
    allTasks: List<Task>,
    notePreviews: Map<Long, String>,
    selectedCategory: Category?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching = searchQuery.isNotBlank()

    // Date formatting
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayDateString = dateFormat.format(Date())

    // Progress statistics for today
    val totalToday = todayTasks.size
    val completedToday = todayTasks.count { it.isCompleted }
    val remainingToday = totalToday - completedToday
    val progressFraction = if (totalToday > 0) completedToday.toFloat() / totalToday.toFloat() else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "today_progress_animation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_root")
    ) {
        // Pinned Search Bar at the top of the main task list screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
        ) {
            TaskSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onClear = { viewModel.setSearchQuery("") },
                placeholder = "Search tasks by title or content..."
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("today_screen_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = 120.dp // extra clearance for floating pill nav & squircle FAB
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Date + Headline + Task/Search count badge
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    if (!isSearching) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = todayDateString.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Task status count badge
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("today_count_badge")
                            ) {
                                Text(
                                    text = if (totalToday == 0) "All Clear" else "$remainingToday remaining",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Huge Expressive Headline
                        Text(
                            text = "Today's Focus",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SEARCH RESULTS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "“$searchQuery”",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Match count pill
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("search_count_badge")
                            ) {
                                Text(
                                    text = "${todayTasks.size} ${if (todayTasks.size == 1) "task" else "tasks"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Summary Progress Card (Hidden during search to focus on results)
            if (!isSearching) {
                item {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_progress_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        totalToday == 0 -> "Ready for action"
                                        completedToday == totalToday -> "All done for today! 🎉"
                                        completedToday > 0 -> "Making solid progress!"
                                        else -> "Let's make it happen"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (totalToday > 0) {
                                        "$completedToday of $totalToday completed • $remainingToday to go"
                                    } else {
                                        "No tasks due today. Tap + to schedule."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick stats pills
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$completedToday done",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = "Pending",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$remainingToday pending",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Circular Progress Indicator with Percentage
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(80.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.size(80.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    strokeWidth = 8.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(80.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 8.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Chip
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setCategoryFilter(null)
                    },
                    label = { Text("All") },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )

                // Category Chips
                Category.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setCategoryFilter(
                                if (selectedCategory == category) null else category
                            )
                        },
                        label = { Text(category.label) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                    )
                }
            }
        }

        // List of TaskCards or Empty State
        if (todayTasks.isEmpty()) {
            item {
                if (isSearching) {
                    EmptyStateIllustration(
                        title = "No tasks match “$searchQuery”",
                        subtitle = "We searched your task titles, descriptions, checklists, and attached notes. Try a different keyword or clear your search.",
                        actionLabel = "Clear Search",
                        onAction = { viewModel.setSearchQuery("") },
                        modifier = Modifier.padding(top = 20.dp)
                    )
                } else {
                    EmptyStateIllustration(
                        title = if (selectedCategory != null) "No ${selectedCategory.label} tasks today" else "Nest is clear for today!",
                        subtitle = "Swipe or tap the + button to add a task with alarm reminders, priority, and attached notes.",
                        actionLabel = "Add a Task",
                        onAction = { viewModel.openAddTaskSheet() },
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            }
        } else {
            items(
                items = todayTasks,
                key = { it.id }
            ) { task ->
                val preview = notePreviews[task.id]
                TaskCard(
                    task = task,
                    notePreview = preview,
                    onCardClick = { viewModel.openTaskDetail(task) },
                    onToggleCompletion = { viewModel.toggleTaskCompletion(task, context) },
                    onDelete = { viewModel.deleteTask(task, context) }
                )
            }
        }
    }
}
}
