package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PillShape
import com.example.ui.viewmodel.NavTab

@Composable
fun FloatingPillBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            shadowElevation = 10.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .shadow(16.dp, shape = PillShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .testTag("floating_bottom_bar")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillNavItem(
                    tab = NavTab.TODAY,
                    icon = Icons.Rounded.Today,
                    isSelected = currentTab == NavTab.TODAY,
                    onClick = {
                        if (currentTab != NavTab.TODAY) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavTab.TODAY)
                        }
                    }
                )

                PillNavItem(
                    tab = NavTab.CALENDAR,
                    icon = Icons.Rounded.CalendarMonth,
                    isSelected = currentTab == NavTab.CALENDAR,
                    onClick = {
                        if (currentTab != NavTab.CALENDAR) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavTab.CALENDAR)
                        }
                    }
                )

                PillNavItem(
                    tab = NavTab.NOTES,
                    icon = Icons.Rounded.Description,
                    isSelected = currentTab == NavTab.NOTES,
                    onClick = {
                        if (currentTab != NavTab.NOTES) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavTab.NOTES)
                        }
                    }
                )

                PillNavItem(
                    tab = NavTab.SETTINGS,
                    icon = Icons.Rounded.Settings,
                    isSelected = currentTab == NavTab.SETTINGS,
                    onClick = {
                        if (currentTab != NavTab.SETTINGS) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavTab.SETTINGS)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    tab: NavTab,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "pill_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "pill_content_color"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 18.dp else 12.dp,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "pill_padding"
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(PillShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tab.title,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
