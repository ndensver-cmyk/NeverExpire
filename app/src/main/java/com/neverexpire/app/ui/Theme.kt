package com.neverexpire.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF102B12),
    secondary = Color(0xFF526B55),
    secondaryContainer = Color(0xFFDCEBDD),
    tertiary = Color(0xFF43A047),
    tertiaryContainer = Color(0xFFB9E4BC),
    background = Color(0xFFF5F8F3),
    surface = Color(0xFFF5F8F3),
    surfaceVariant = Color(0xFFDDE8DD),
    surfaceContainer = Color(0xFFEAF1E8),
    surfaceContainerHigh = Color(0xFFE2EDE0),
    surfaceContainerLow = Color(0xFFF0F5EE),
    error = Color(0xFFBA1A1A)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF102B12),
    primaryContainer = Color(0xFF245B2A),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFB8CCB8),
    secondaryContainer = Color(0xFF293F2B),
    tertiary = Color(0xFF81C784),
    tertiaryContainer = Color(0xFF285A2D),
    background = Color(0xFF0D140E),
    surface = Color(0xFF0D140E),
    surfaceVariant = Color(0xFF3E5140),
    surfaceContainer = Color(0xFF172219),
    surfaceContainerHigh = Color(0xFF202E22),
    surfaceContainerLow = Color(0xFF121B14)
)

private val base = Typography()
private val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun NeverExpireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
