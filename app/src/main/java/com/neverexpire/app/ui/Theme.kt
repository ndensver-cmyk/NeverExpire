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
    primary = Color(0xFF1B6B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9EED3),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF4F6354),
    secondaryContainer = Color(0xFFD2E8D5),
    tertiary = Color(0xFFE0701B),
    tertiaryContainer = Color(0xFFFFDCC4),
    background = Color(0xFFF6FBF4),
    surface = Color(0xFFF6FBF4),
    surfaceVariant = Color(0xFFDCE5DB),
    surfaceContainer = Color(0xFFEAF0E8),
    surfaceContainerHigh = Color(0xFFE4EAE2),
    surfaceContainerLow = Color(0xFFF0F5EE),
    error = Color(0xFFBA1A1A)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8DD8A3),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00522A),
    onPrimaryContainer = Color(0xFFA8F5BE),
    secondary = Color(0xFFB6CCB9),
    secondaryContainer = Color(0xFF384B3D),
    tertiary = Color(0xFFFFB783),
    tertiaryContainer = Color(0xFF6E3900),
    background = Color(0xFF101510),
    surface = Color(0xFF101510),
    surfaceVariant = Color(0xFF404941),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerLow = Color(0xFF181D18)
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
