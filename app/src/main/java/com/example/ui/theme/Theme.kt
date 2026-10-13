package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StageColorScheme = darkColorScheme(
    primary = StageCyan,
    onPrimary = StageBlack,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = StageAmber,
    onSecondary = StageBlack,
    secondaryContainer = Color(0xFF5D3E00),
    onSecondaryContainer = Color(0xFFFFF8E1),
    tertiary = StageGreen,
    onTertiary = StageBlack,
    error = StageRed,
    onError = Color.White,
    background = StageBlack,
    onBackground = StageTextPrimary,
    surface = StageSurface,
    onSurface = StageTextPrimary,
    surfaceVariant = StageSurfaceElevated,
    onSurfaceVariant = StageTextSecondary,
    outline = StageBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StageColorScheme,
        typography = Typography,
        content = content
    )
}
