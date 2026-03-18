package com.aktivitasku.presentation.theme

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

// ── Light Color Scheme ────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = Blue700,
    onPrimary          = White,
    primaryContainer   = Blue50,
    onPrimaryContainer = Blue900,

    secondary          = Teal400,
    onSecondary        = White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal600,

    tertiary           = Warning,
    onTertiary         = White,

    background         = SurfaceWhite,
    onBackground       = Gray900,

    surface            = White,
    onSurface          = Gray900,
    surfaceVariant     = Surface2,
    onSurfaceVariant   = Gray700,

    outline            = Gray300,
    outlineVariant     = Gray100,

    error              = Error,
    onError            = White,
    errorContainer     = ErrorLight,
    onErrorContainer   = Error,
)

// ── Dark Color Scheme ─────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Blue400,
    onPrimary          = Blue900,
    primaryContainer   = Blue800,
    onPrimaryContainer = Blue100,

    secondary          = Teal400,
    onSecondary        = Teal600,
    secondaryContainer = Teal600,
    onSecondaryContainer = Teal50,

    background         = DarkBackground,
    onBackground       = White,

    surface            = DarkSurface,
    onSurface          = White,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = Gray300,

    outline            = DarkBorder,
    outlineVariant     = DarkSurface3,

    error              = Error,
    onError            = White,
)

@Composable
fun AktivitasKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
