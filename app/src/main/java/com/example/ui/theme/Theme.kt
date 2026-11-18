package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ArenaLightColorScheme = lightColorScheme(
    primary = ArenaBluePrimary,
    onPrimary = ArenaWhite,
    primaryContainer = ArenaSurfaceBlueTint,
    onPrimaryContainer = ArenaBlueDark,
    secondary = ArenaCyanAccent,
    onSecondary = ArenaWhite,
    secondaryContainer = ArenaSurfaceSubtle,
    onSecondaryContainer = ArenaInkPrimary,
    tertiary = ArenaBlueBright,
    onTertiary = ArenaWhite,
    background = ArenaCanvas,
    onBackground = ArenaInkPrimary,
    surface = ArenaWhite,
    onSurface = ArenaInkPrimary,
    surfaceVariant = ArenaSurfaceSubtle,
    onSurfaceVariant = ArenaInkSecondary,
    outline = ArenaBorder,
    outlineVariant = ArenaBorderBlue,
    error = ArenaCrimson,
    onError = ArenaWhite,
    errorContainer = ArenaCrimsonBg,
    onErrorContainer = ArenaCrimson
)

val ArenaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BattleArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArenaLightColorScheme,
        typography = ArenaTypography,
        shapes = ArenaShapes,
        content = content
    )
}
