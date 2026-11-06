package dev.leosnczki.sonora.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme = darkColorScheme(
    primary = SonoraColors.Accent,
    onPrimary = SonoraColors.OnAccent,
    secondary = SonoraColors.Accent,
    onSecondary = SonoraColors.OnAccent,
    background = Color.Transparent,
    onBackground = SonoraColors.Text,
    surface = SonoraColors.SurfaceModal,
    onSurface = SonoraColors.Text,
    surfaceVariant = SonoraColors.SurfaceModal,
    onSurfaceVariant = SonoraColors.TextMuted,
    outline = SonoraColors.BorderInput,
    outlineVariant = SonoraColors.Divider,
    error = SonoraColors.Danger,
    inverseSurface = SonoraColors.SurfaceModal,
    inverseOnSurface = SonoraColors.Text,
    inversePrimary = SonoraColors.Accent,
)

private val typography = Typography(
    displayLarge = SonoraType.Display,
    titleLarge = SonoraType.Title,
    labelLarge = SonoraType.Button,
    labelMedium = SonoraType.Label,
    bodyMedium = SonoraType.Body,
    bodyLarge = SonoraType.Body,
    bodySmall = SonoraType.Caption,
)

/** Sonora is dark only: its UI always sits over the wallpaper behind a black scrim. */
@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
