package com.daime.grow.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val GrowLightColorScheme = lightColorScheme(
    primary = GrowPrimary,
    onPrimary = GrowSurface,
    primaryContainer = GrowPrimaryDark,
    onPrimaryContainer = GrowSurface,
    background = GrowBackground,
    onBackground = GrowText,
    surface = GrowSurface,
    onSurface = GrowText,
    surfaceVariant = GrowSurfaceSoft,
    outline = GrowOutline,
    secondary = GrowPrimaryDark,
    onSecondary = GrowSurface,
    tertiary = GrowFabAccent,
    onTertiary = GrowText
)

val GrowDarkColorScheme = darkColorScheme(
    primary = GrowPrimaryDarkTheme,
    onPrimary = GrowBackgroundDark,
    primaryContainer = GrowPrimaryDarkContainer,
    onPrimaryContainer = GrowTextDark,
    background = GrowBackgroundDark,
    onBackground = GrowTextDark,
    surface = GrowSurfaceDark,
    onSurface = GrowTextDark,
    surfaceVariant = GrowSurfaceSoftDark,
    outline = GrowOutlineDark,
    secondary = GrowFabAccentDark,
    onSecondary = GrowBackgroundDark,
    tertiary = GrowFabAccentDark,
    onTertiary = GrowBackgroundDark
)
