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
    primary = Color(0xFF173B63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8FF),
    onPrimaryContainer = Color(0xFF071D35),
    secondary = Color(0xFF52677F),
    secondaryContainer = Color(0xFFDCEBFA),
    tertiary = Color(0xFF00A6A6),
    tertiaryContainer = Color(0xFFBDEFEA),
    background = Color(0xFFF4F7FC),
    surface = Color(0xFFF4F7FC),
    surfaceVariant = Color(0xFFDCE5F0),
    surfaceContainer = Color(0xFFE9EFF7),
    surfaceContainerHigh = Color(0xFFE1EAF5),
    surfaceContainerLow = Color(0xFFF0F4FA),
    error = Color(0xFFBA1A1A)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8FC7FF),
    onPrimary = Color(0xFF082847),
    primaryContainer = Color(0xFF174A78),
    onPrimaryContainer = Color(0xFFBDE0FF),
    secondary = Color(0xFFB7C9DF),
    secondaryContainer = Color(0xFF293C54),
    tertiary = Color(0xFF70D9D0),
    tertiaryContainer = Color(0xFF075B59),
    background = Color(0xFF0B111B),
    surface = Color(0xFF0B111B),
    surfaceVariant = Color(0xFF3B4B60),
    surfaceContainer = Color(0xFF151F2D),
    surfaceContainerHigh = Color(0xFF1E2B3C),
    surfaceContainerLow = Color(0xFF111A27)
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
