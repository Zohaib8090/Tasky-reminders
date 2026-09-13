package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Color(0xFF0F1B63),
    primaryContainer = Color(0xFF283688),
    onPrimaryContainer = Color(0xFFDEE0FD),
    secondary = IndigoGrey80,
    onSecondary = Color(0xFF2B2F3D),
    secondaryContainer = Color(0xFF414555),
    onSecondaryContainer = Color(0xFFE2E1EC),
    tertiary = Coral80,
    onTertiary = Color(0xFF5B1B15),
    tertiaryContainer = Color(0xFF7A2B24),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = Color(0xFFE2E2E9),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FD),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = IndigoGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E1EC),
    onSecondaryContainer = Color(0xFF181B26),
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF3B0907),
    background = LightBackground,
    onBackground = Color(0xFF191C22),
    surface = LightSurface,
    onSurface = Color(0xFF191C22),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF45464F),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh
)

@Composable
fun TaskNestTheme(
    palette: AppThemePalette = AppThemePalette.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = palette.primaryContainer,
            onPrimary = palette.onPrimaryContainer,
            primaryContainer = palette.primaryColor,
            onPrimaryContainer = palette.primaryContainer,
            secondary = palette.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF2E3342),
            onSecondaryContainer = Color(0xFFE2E1EC),
            tertiary = palette.accentColor,
            onTertiary = Color.White,
            background = DarkBackground,
            onBackground = Color(0xFFE2E2E9),
            surface = DarkSurface,
            onSurface = Color(0xFFE2E2E9),
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFC7C5D0),
            surfaceContainer = DarkSurfaceContainer,
            surfaceContainerHigh = DarkSurfaceContainerHigh
        )
        else -> lightColorScheme(
            primary = palette.primaryColor,
            onPrimary = Color.White,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = palette.primaryContainer.copy(alpha = 0.45f),
            onSecondaryContainer = palette.onPrimaryContainer,
            tertiary = palette.accentColor,
            onTertiary = Color.White,
            background = LightBackground,
            onBackground = Color(0xFF191C22),
            surface = LightSurface,
            onSurface = Color(0xFF191C22),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF45464F),
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceContainerHigh
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}

// Keep backward compatibility alias if needed
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    TaskNestTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
