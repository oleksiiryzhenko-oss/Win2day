package com.taichiflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val Scheme = darkColorScheme(
    primary = TC.Crimson,
    onPrimary = TC.White,
    primaryContainer = TC.DeepRed,
    onPrimaryContainer = TC.White,
    secondary = TC.Ember,
    onSecondary = TC.Black,
    secondaryContainer = TC.Blood,
    onSecondaryContainer = TC.White,
    tertiary = TC.Soft,
    background = TC.Black,
    onBackground = TC.White,
    surface = TC.Charcoal,
    onSurface = TC.White,
    surfaceVariant = TC.DarkGray,
    onSurfaceVariant = TC.Muted,
    surfaceContainer = TC.Charcoal,
    surfaceContainerHigh = TC.DarkGray,
    surfaceContainerHighest = TC.Gray,
    surfaceContainerLow = TC.Charcoal,
    surfaceContainerLowest = TC.Black,
    outline = TC.Line,
    outlineVariant = TC.Gray,
    error = TC.Ember,
    onError = TC.Black
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TaiChiFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
