package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.Category
import com.example.data.model.Priority

// Material 3 Expressive Theme Palettes
enum class AppThemePalette(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val previewGradient: List<Color>
) {
    BLUE(
        id = "blue",
        title = "Expressive Blue",
        subtitle = "Indigo & Azure dynamic tones",
        primaryColor = Color(0xFF3F51B5),
        primaryContainer = Color(0xFFDEE0FD),
        onPrimaryContainer = Color(0xFF00105C),
        secondaryColor = Color(0xFF535F70),
        accentColor = Color(0xFF2196F3),
        previewGradient = listOf(Color(0xFF3F51B5), Color(0xFF64B5F6))
    ),
    PURPLE(
        id = "purple",
        title = "Royal Lavender",
        subtitle = "Soft lilac & deep amethyst",
        primaryColor = Color(0xFF6750A4),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondaryColor = Color(0xFF625B71),
        accentColor = Color(0xFF9C27B0),
        previewGradient = listOf(Color(0xFF6750A4), Color(0xFFBA68C8))
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald Mint",
        subtitle = "Fresh sage & botanical green",
        primaryColor = Color(0xFF1B6E42),
        primaryContainer = Color(0xFFA8F5BF),
        onPrimaryContainer = Color(0xFF00210E),
        secondaryColor = Color(0xFF506353),
        accentColor = Color(0xFF4CAF50),
        previewGradient = listOf(Color(0xFF1B6E42), Color(0xFF81C784))
    ),
    CORAL(
        id = "coral",
        title = "Sunset Peach",
        subtitle = "Warm terracotta & apricot",
        primaryColor = Color(0xFFBF4A30),
        primaryContainer = Color(0xFFFFDAD3),
        onPrimaryContainer = Color(0xFF3B0903),
        secondaryColor = Color(0xFF77574E),
        accentColor = Color(0xFFFF7043),
        previewGradient = listOf(Color(0xFFBF4A30), Color(0xFFFF8A65))
    ),
    AMBER(
        id = "amber",
        title = "Golden Honey",
        subtitle = "Warm ochre & amber sunlight",
        primaryColor = Color(0xFF8C5000),
        primaryContainer = Color(0xFFFFDDB7),
        onPrimaryContainer = Color(0xFF2C1600),
        secondaryColor = Color(0xFF6E5D47),
        accentColor = Color(0xFFFFB300),
        previewGradient = listOf(Color(0xFF8C5000), Color(0xFFFFD54F))
    ),
    BERRY(
        id = "berry",
        title = "Berry Crimson",
        subtitle = "Rich ruby & soft rosewood",
        primaryColor = Color(0xFFB3261E),
        primaryContainer = Color(0xFFF9DEDC),
        onPrimaryContainer = Color(0xFF410E0B),
        secondaryColor = Color(0xFF75565B),
        accentColor = Color(0xFFE91E63),
        previewGradient = listOf(Color(0xFFB3261E), Color(0xFFF06292))
    ),
    TEAL(
        id = "teal",
        title = "Ocean Cyan",
        subtitle = "Deep marine & aquamarine",
        primaryColor = Color(0xFF006874),
        primaryContainer = Color(0xFF97F0FF),
        onPrimaryContainer = Color(0xFF001F24),
        secondaryColor = Color(0xFF4A6267),
        accentColor = Color(0xFF00ACC1),
        previewGradient = listOf(Color(0xFF006874), Color(0xFF4DD0E1))
    ),
    SLATE(
        id = "slate",
        title = "Modern Graphite",
        subtitle = "Minimalist neutral & charcoal",
        primaryColor = Color(0xFF4A5568),
        primaryContainer = Color(0xFFE2E8F0),
        onPrimaryContainer = Color(0xFF1A202C),
        secondaryColor = Color(0xFF718096),
        accentColor = Color(0xFF607D8B),
        previewGradient = listOf(Color(0xFF4A5568), Color(0xFF90A4AE))
    );

    companion object {
        fun fromId(id: String?): AppThemePalette {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BLUE
        }
    }
}

// Primary & Secondary Brand Colors (Material 3 Expressive)
val Indigo80 = Color(0xFFBAC3FF)
val IndigoGrey80 = Color(0xFFC7C5D0)
val Coral80 = Color(0xFFFFB3AC)

val Indigo40 = Color(0xFF4355B9)
val IndigoGrey40 = Color(0xFF5B5D6B)
val Coral40 = Color(0xFF9E4238)

// Tonal and Background Colors
val DarkBackground = Color(0xFF10131A)
val DarkSurface = Color(0xFF141822)
val DarkSurfaceVariant = Color(0xFF202636)
val DarkSurfaceContainer = Color(0xFF1A1F2C)
val DarkSurfaceContainerHigh = Color(0xFF242A3B)

val LightBackground = Color(0xFFF7F8FD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEFF7)
val LightSurfaceContainer = Color(0xFFF1F3FA)
val LightSurfaceContainerHigh = Color(0xFFE6E9F4)

// Vibrant Category Colors
val StudyColor = Color(0xFF3F51B5)
val StudyColorContainer = Color(0xFFDEE0FD)
val StudyDarkContainer = Color(0xFF273272)

val PersonalColor = Color(0xFFE91E63)
val PersonalColorContainer = Color(0xFFFFD9E2)
val PersonalDarkContainer = Color(0xFF701332)

val WorkColor = Color(0xFF00897B)
val WorkColorContainer = Color(0xFFC7F3EA)
val WorkDarkContainer = Color(0xFF004D44)

// Priority Colors
val PriorityHigh = Color(0xFFE53935)
val PriorityMed = Color(0xFFFB8C00)
val PriorityLow = Color(0xFF43A047)

// Note Palette Colors
val NotePastelColors = listOf(
    Color(0xFFE8EAED),
    Color(0xFFFFD8D8),
    Color(0xFFFFF0B3),
    Color(0xFFD4EDDA),
    Color(0xFFD1ECF1),
    Color(0xFFE2D9F3)
)

val NoteDarkColors = listOf(
    Color(0xFF2A2E39),
    Color(0xFF4C2A2A),
    Color(0xFF4A401A),
    Color(0xFF1F4028),
    Color(0xFF1B3D48),
    Color(0xFF34234E)
)

fun getCategoryColor(category: Category, isDark: Boolean): Color {
    return when (category) {
        Category.STUDY -> if (isDark) StudyDarkContainer else StudyColorContainer
        Category.PERSONAL -> if (isDark) PersonalDarkContainer else PersonalColorContainer
        Category.WORK -> if (isDark) WorkDarkContainer else WorkColorContainer
    }
}

fun getCategoryOnColor(category: Category, isDark: Boolean): Color {
    return when (category) {
        Category.STUDY -> if (isDark) Color(0xFFD8DEFF) else Color(0xFF1C2665)
        Category.PERSONAL -> if (isDark) Color(0xFFFFD9E2) else Color(0xFF6B1130)
        Category.WORK -> if (isDark) Color(0xFFC2F2E9) else Color(0xFF003831)
    }
}

fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.HIGH -> PriorityHigh
        Priority.MEDIUM -> PriorityMed
        Priority.LOW -> PriorityLow
    }
}
