package com.pedrohmsouza.sliced.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Tema do app: segue o claro/escuro do sistema. */
@Composable
fun SlicedTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.fg, onPrimary = c.bg, background = c.bg, onBackground = c.fg,
            surface = c.surface, onSurface = c.fg, surfaceVariant = c.soft, onSurfaceVariant = c.muted,
            surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.soft, outline = c.line, outlineVariant = c.line,
            error = c.danger, inverseSurface = c.fg, inverseOnSurface = c.bg, secondaryContainer = c.soft,
        )
    } else {
        lightColorScheme(
            primary = c.fg, onPrimary = c.bg, background = c.bg, onBackground = c.fg,
            surface = c.surface, onSurface = c.fg, surfaceVariant = c.soft, onSurfaceVariant = c.muted,
            surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.soft, outline = c.line, outlineVariant = c.line,
            error = c.danger, inverseSurface = c.fg, inverseOnSurface = c.bg, secondaryContainer = c.soft,
        )
    }
    CompositionLocalProvider(LocalSlicedColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTypography, content = content)
    }
}

/** Atalho: `SlicedTheme.colors.muted`, `SlicedTheme.type.display`. */
object SlicedTheme {
    val colors: SlicedColors
        @Composable @ReadOnlyComposable get() = LocalSlicedColors.current
    val type = SlicedType
}
