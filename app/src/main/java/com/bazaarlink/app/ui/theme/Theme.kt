package com.bazaarlink.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,                   // #07203F
    onPrimary = SoftCream,                // #EBDED4
    primaryContainer = SoftCream,         // #EBDED4
    onPrimaryContainer = DeepNavy,        // #07203F
    secondary = TerracottaRust,           // #A65E46
    onSecondary = LightSurface,
    secondaryContainer = WarmCopper,      // #D9AA90
    onSecondaryContainer = ObsidianMidnight, // #02000D
    background = LightBackground,         // #F9F6F0
    onBackground = LightTextPrimary,      // #02000D
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmCopper,                 // #D9AA90
    onPrimary = ObsidianMidnight,         // #02000D
    primaryContainer = DeepNavy,          // #07203F
    onPrimaryContainer = SoftCream,       // #EBDED4
    secondary = TerracottaRust,           // #A65E46
    onSecondary = LightSurface,
    secondaryContainer = DeepNavy,
    onSecondaryContainer = WarmCopper,
    background = DarkBackground,          // #02000D
    onBackground = DarkTextPrimary,       // #EBDED4
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = StatusError
)

@Composable
fun BazaarLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
