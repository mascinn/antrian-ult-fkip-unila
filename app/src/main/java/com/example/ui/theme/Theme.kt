package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UltLightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = PureWhite,
    primaryContainer = SoftLavender,
    onPrimaryContainer = DeepRoyalPurple,
    secondary = DeepRoyalPurple,
    onSecondary = PureWhite,
    secondaryContainer = LavenderTint,
    onSecondaryContainer = DeepRoyalPurple,
    tertiary = VibrantPurple,
    onTertiary = PureWhite,
    background = LavenderMist,
    onBackground = DeepInk,
    surface = PureWhite,
    onSurface = DeepInk,
    surfaceVariant = SoftLavender,
    onSurfaceVariant = MutedInk,
    outline = LavenderOutline,
    outlineVariant = LavenderBorder,
    error = StatusSkippedFg,
    onError = PureWhite,
    errorContainer = StatusSkippedBg,
    onErrorContainer = StatusSkippedFg
)

private val UltDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = SoftLavenderDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = VibrantPurple,
    onSecondary = PureWhite,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = VibrantPurple,
    outlineVariant = DarkSurfaceVariant
)

// Modern rounded Material 3 Android geometry
val UltShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun UltQueueTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) UltDarkColorScheme else UltLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UltTypography,
        shapes = UltShapes,
        content = content
    )
}
