package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.ui.components.EmptyStateIllustration
import com.example.ui.components.TaskCard
import com.example.ui.theme.PillShape
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    calendarTasks: List<Task>,
    allTasks: List<Task>,
    selectedDateMillis: Long,
    notePreviews: Map<Long, String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()

    // Current displayed calendar month state
    var displayCal by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDateMillis })
    }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val selectedDayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    // Group all tasks by day timestamp for day dots
    val tasksByDay = remember(allTasks) {
        allTasks.groupBy { TaskViewModel.getDayStartMillis(it.dueDate) }
    }

    // Days grid calculation
    val daysInMonthList = remember(displayCal) {
        val cal = displayCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, etc.
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Convert Sunday=1 to Monday-first: Monday=0, Tuesday=1 ... Sunday=6
        val offset = (firstDayOfWeek + 5) % 7

        val list = mutableListOf<CalendarDay?>()
        // Leading empty slots
        for (i in 0 until offset) {
            list.add(null)
        }
        // Actual days
        for (day in 1..maxDays) {
            val dayCal = cal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = TaskViewModel.getDayStartMillis(dayCal.timeInMillis)
            list.add(CalendarDay(dayNumber = day, dayMillis = dayStart))
        }
        list
    }

    val todayStart = TaskViewModel.getTodayStartMillis()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen_list"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expressive Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "SCHEDULE & TIMELINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Calendar Card
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_month_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newCal = displayCal.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                displayCal = newCal
                            },
                            modifier = Modifier.testTag("cal_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = monthYearFormat.format(displayCal.time),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Jump to Today
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    displayCal = Calendar.getInstance()
                                    viewModel.setSelectedCalendarDate(todayStart)
                                },
                                modifier = Modifier.testTag("cal_today_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Today,
                                    contentDescription = "Today",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newCal = displayCal.clone() as Calendar
                                    newCal.add(Calendar.MONTH, 1)
                                    displayCal = newCal
                                },
                                modifier = Modifier.testTag("cal_next_month")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header row (Mon Tue Wed Thu Fri Sat Sun)
                    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekDays.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calendar Grid (rows of 7 days)
                    val rows = daysInMonthList.chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { day ->
                                if (day == null) {
                                    Spacer(modifier = Modifier.size(38.dp))
                                } else {
                                    val isSelected = day.dayMillis == selectedDateMillis
                                    val isToday = day.dayMillis == todayStart
                                    val tasksOnThisDay = tasksByDay[day.dayMillis] ?: emptyList()

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setSelectedCalendarDate(day.dayMillis)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = day.dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                ),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            // Task dot indicators
                                            if (tasksOnThisDay.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val dotsToShow = tasksOnThisDay.take(3)
                                                    dotsToShow.forEach { t ->
                                                        val dotColor = if (isSelected) {
                                                            Color.White
                                                        } else {
                                                            getCategoryColor(t.category, isDark)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Tasks for Selected Date
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDayFormat.format(Date(selectedDateMillis)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${calendarTasks.size} tasks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // List of tasks for selected date
        if (calendarTasks.isEmpty()) {
            item {
                EmptyStateIllustration(
                    title = "No tasks on this day",
                    subtitle = "Schedule a task with specific due time and alarm notifications.",
                    actionLabel = "Add Task for this Day",
                    onAction = { viewModel.openAddTaskSheet(presetDate = selectedDateMillis) },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        } else {
            items(
                items = calendarTasks,
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

private data class CalendarDay(
    val dayNumber: Int,
    val dayMillis: Long
)
