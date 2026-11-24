package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WardLightColors = lightColorScheme(
    primary = DeepClinicalTeal,
    onPrimary = Color.White,
    primaryContainer = TealSoftContainer,
    onPrimaryContainer = OnTealSoftContainer,
    secondary = SlateBlue,
    onSecondary = Color.White,
    secondaryContainer = SlateBlueContainer,
    onSecondaryContainer = OnSlateBlueContainer,
    tertiary = ClinicalEmerald,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    error = AlertCoral,
    onError = Color.White,
    errorContainer = AlertCoralContainer,
    onErrorContainer = OnAlertCoralContainer,
    background = AlabasterCanvas,
    onBackground = InkPrimary,
    surface = PurePaper,
    onSurface = InkPrimary,
    surfaceVariant = SoftTintSurface,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = PurePaper,
    surfaceContainerLow = CardContainerLow,
    surfaceContainer = SoftTintSurface,
    surfaceContainerHigh = CardContainerHigh,
    outline = HairlineOutline,
    outlineVariant = SubtleOutline
)

private val WardDarkColors = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = OnDarkTealPrimary,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = TealSoftContainer,
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0A243D),
    secondaryContainer = Color(0xFF1A3A57),
    onSecondaryContainer = SlateBlueContainer,
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF043022),
    tertiaryContainer = Color(0xFF0D4735),
    onTertiaryContainer = EmeraldContainer,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5C1520),
    onErrorContainer = AlertCoralContainer,
    background = DarkSlateCanvas,
    onBackground = DarkInkPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLowest = DarkSlateCanvas,
    surfaceContainerLow = DarkSurfaceCard,
    surfaceContainer = DarkSurfaceCard,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1F3138)
)

val WardShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WardFormsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WardDarkColors else WardLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WardTypography,
        shapes = WardShapes,
        content = content
    )
}
