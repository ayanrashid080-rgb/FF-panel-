package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val NexusDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = ElectricCyanDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCrimson,
    onSecondary = TextPrimary,
    secondaryContainer = NeonCrimsonDark,
    onSecondaryContainer = TextPrimary,
    tertiary = MatrixGreen,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = MatteCarbon,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedPanel,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderMuted,
    error = NeonCrimson,
    onError = TextPrimary
)

private val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun NexusOverlayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        typography = NexusTypography,
        shapes = NexusShapes,
        content = content
    )
}
