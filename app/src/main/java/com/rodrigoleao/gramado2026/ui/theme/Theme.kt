package com.rodrigoleao.gramado2026.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GramadoColorScheme = lightColorScheme(
    primary          = GreenMoss,
    onPrimary        = Color.White,
    primaryContainer = GreenWarm,
    onPrimaryContainer = GreenMoss,

    secondary        = AmberPrimary,
    onSecondary      = Color.White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = AmberPrimary,

    tertiary         = GreenLime,
    onTertiary       = GreenMoss,
    tertiaryContainer = GreenWarm,
    onTertiaryContainer = GreenMoss,

    background       = GreenLight,
    onBackground     = TextPrimary,

    surface          = SurfaceWhite,
    onSurface        = TextPrimary,
    surfaceVariant   = GreenForest,
    onSurfaceVariant = TextSecondary,

    outline          = CardBorder,
    outlineVariant   = DividerColor,
)

@Composable
fun GramadoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor  = GreenLight.toArgb()
            window.navigationBarColor = GreenForest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = GramadoColorScheme,
        typography  = GramadoTypography
    ) {
        // Garante que TODO texto herde Plus Jakarta Sans — inclusive Text() com
        // fontSize/fontWeight inline, que de outro modo cairia na fonte do sistema.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = PlusJakartaSans),
            content = content
        )
    }
}
