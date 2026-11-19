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

private val FictionDarkColorScheme = darkColorScheme(
    primary = IlluminatedBrass,
    onPrimary = ObsidianForest,
    primaryContainer = WarmGoldContainer,
    onPrimaryContainer = Color(0xFFFFE09A),
    secondary = TrailEmerald,
    onSecondary = ObsidianForest,
    secondaryContainer = DeepEmeraldContainer,
    onSecondaryContainer = Color(0xFFB0F4D7),
    tertiary = TorchlightAmber,
    onTertiary = ObsidianForest,
    tertiaryContainer = Color(0xFF47250D),
    onTertiaryContainer = Color(0xFFFFD5B8),
    background = ObsidianForest,
    onBackground = ParchmentWhite,
    surface = DeepPine,
    onSurface = ParchmentWhite,
    surfaceVariant = SlateTimber,
    onSurfaceVariant = MutedSage,
    surfaceContainerLowest = Color(0xFF080E0C),
    surfaceContainerLow = DeepPine,
    surfaceContainer = SlateTimber,
    surfaceContainerHigh = MossSurface,
    outline = OutlineNight,
    error = CrimsonBeacon,
    onError = ObsidianForest
)

private val FictionLightColorScheme = lightColorScheme(
    primary = DeepMoss,
    onPrimary = WarmVellum,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = Color(0xFF0A3323),
    secondary = BurnishedBronze,
    onSecondary = WarmVellum,
    secondaryContainer = LightGoldContainer,
    onSecondaryContainer = Color(0xFF422802),
    tertiary = EmberRust,
    onTertiary = WarmVellum,
    tertiaryContainer = Color(0xFFFAD8CC),
    onTertiaryContainer = Color(0xFF4D1807),
    background = WarmVellum,
    onBackground = ForestInk,
    surface = WarmVellum,
    onSurface = ForestInk,
    surfaceVariant = AgedParchment,
    onSurfaceVariant = MutedSlateInk,
    surfaceContainerLowest = Color(0xFFFFFCF5),
    surfaceContainerLow = WarmVellum,
    surfaceContainer = AgedParchment,
    surfaceContainerHigh = SandCard,
    outline = OutlineParchment,
    error = Color(0xFFB3261E),
    onError = WarmVellum
)

val FictionShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FictionHikeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FictionDarkColorScheme else FictionLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FictionHikeTypography,
        shapes = FictionShapes,
        content = content
    )
}
