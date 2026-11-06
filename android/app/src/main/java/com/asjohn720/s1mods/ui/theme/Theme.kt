package com.asjohn720.s1mods.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentMint,
    onPrimary = BgDark,
    primaryContainer = AccentMintSubtle,
    onPrimaryContainer = TextPrimary,
    secondary = AccentSky,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = DividerColor,
    error = StatusDanger,
    onError = TextPrimary
)

@Composable
fun S1ModsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

@Composable
fun OlaModsTheme(content: @Composable () -> Unit) = S1ModsTheme(content)
