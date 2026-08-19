package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Indigo Palettes
private val IndigoDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoOnContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanOnContainerDark,
    tertiary = AmberTertiaryDark,
    error = CrimsonErrorDark,
    onError = CrimsonOnErrorDark,
    errorContainer = CrimsonErrorContainerDark,
    onErrorContainer = CrimsonOnErrorContainerDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    outline = SlateOutlineDark
)

private val IndigoLightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = CyanSecondaryLight,
    onSecondary = CyanOnSecondaryLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = CyanOnContainerLight,
    tertiary = AmberTertiaryLight,
    error = CrimsonErrorLight,
    onError = CrimsonOnErrorLight,
    errorContainer = CrimsonErrorContainerLight,
    onErrorContainer = CrimsonOnErrorContainerLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight
)

// Emerald Palettes
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = EmeraldOnPrimaryDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    outline = SlateOutlineDark,
    error = CrimsonErrorDark
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = EmeraldOnPrimaryLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight,
    error = CrimsonErrorLight
)

// Sunset Palettes
private val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetPrimaryDark,
    onPrimary = SunsetOnPrimaryDark,
    primaryContainer = SunsetContainerDark,
    onPrimaryContainer = SunsetOnContainerDark,
    secondary = RoseSecondaryDark,
    onSecondary = SunsetOnPrimaryDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    outline = SlateOutlineDark,
    error = CrimsonErrorDark
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetPrimaryLight,
    onPrimary = SunsetOnPrimaryLight,
    primaryContainer = SunsetContainerLight,
    onPrimaryContainer = SunsetOnContainerLight,
    secondary = RoseSecondaryLight,
    onSecondary = SunsetOnPrimaryLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight,
    error = CrimsonErrorLight
)

// Midnight Palettes
private val MidnightDarkColorScheme = darkColorScheme(
    primary = MidnightPrimaryDark,
    onPrimary = MidnightOnPrimaryDark,
    primaryContainer = MidnightContainerDark,
    onPrimaryContainer = MidnightOnContainerDark,
    secondary = NeonCyanDark,
    onSecondary = MidnightOnPrimaryDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    outline = SlateOutlineDark,
    error = CrimsonErrorDark
)

private val MidnightLightColorScheme = lightColorScheme(
    primary = MidnightPrimaryLight,
    onPrimary = MidnightOnPrimaryLight,
    primaryContainer = MidnightContainerLight,
    onPrimaryContainer = MidnightOnContainerLight,
    secondary = NeonCyanLight,
    onSecondary = MidnightOnPrimaryLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight,
    error = CrimsonErrorLight
)

@Composable
fun ScreenTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeKey: String = "indigo",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeKey.lowercase()) {
        "emerald" -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        "sunset" -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
        "midnight" -> if (darkTheme) MidnightDarkColorScheme else MidnightLightColorScheme
        else -> if (darkTheme) IndigoDarkColorScheme else IndigoLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
