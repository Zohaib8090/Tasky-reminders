package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PillShape

@Composable
fun EmptyStateIllustration(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Expressive Canvas illustration of a stylized nest with task cards
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val w = size.width
                val h = size.height

                // Nest base bowl (rounded arch)
                val nestPath = Path().apply {
                    moveTo(w * 0.15f, h * 0.55f)
                    cubicTo(
                        w * 0.2f, h * 0.95f,
                        w * 0.8f, h * 0.95f,
                        w * 0.85f, h * 0.55f
                    )
                    cubicTo(
                        w * 0.65f, h * 0.62f,
                        w * 0.35f, h * 0.62f,
                        w * 0.15f, h * 0.55f
                    )
                    close()
                }
                drawPath(
                    path = nestPath,
                    color = primaryColor.copy(alpha = 0.85f)
                )

                // Twigs / texture lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(w * 0.28f, h * 0.66f),
                    end = Offset(w * 0.72f, h * 0.72f),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(w * 0.32f, h * 0.76f),
                    end = Offset(w * 0.68f, h * 0.78f),
                    strokeWidth = 3.5f
                )

                // Task Card 1 (Tilted Left)
                drawRoundRect(
                    color = tertiaryColor,
                    topLeft = Offset(w * 0.24f, h * 0.26f),
                    size = Size(w * 0.28f, h * 0.36f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // Checkmark inside Card 1
                val checkPath1 = Path().apply {
                    moveTo(w * 0.31f, h * 0.44f)
                    lineTo(w * 0.36f, h * 0.49f)
                    lineTo(w * 0.45f, h * 0.38f)
                }
                drawPath(
                    path = checkPath1,
                    color = Color.White,
                    style = Stroke(width = 5f)
                )

                // Task Card 2 (Tilted Right)
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(w * 0.46f, h * 0.20f),
                    size = Size(w * 0.32f, h * 0.40f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                // Checkmark inside Card 2
                val checkPath2 = Path().apply {
                    moveTo(w * 0.54f, h * 0.40f)
                    lineTo(w * 0.60f, h * 0.46f)
                    lineTo(w * 0.70f, h * 0.34f)
                }
                drawPath(
                    path = checkPath2,
                    color = primaryColor,
                    style = Stroke(width = 5f)
                )

                // Little golden sparkle at top right
                drawCircle(
                    color = tertiaryColor,
                    radius = 8f,
                    center = Offset(w * 0.86f, h * 0.25f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f),
                    radius = 5f,
                    center = Offset(w * 0.18f, h * 0.35f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
