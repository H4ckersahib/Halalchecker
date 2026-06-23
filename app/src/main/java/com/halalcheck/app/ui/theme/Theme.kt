package com.halalcheck.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Slate900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,

    secondary = Gold400,
    onSecondary = Slate900,
    secondaryContainer = Gold700,
    onSecondaryContainer = Gold100,

    tertiary = Amber400,
    onTertiary = Slate900,
    tertiaryContainer = Amber700,
    onTertiaryContainer = Amber100,

    error = Coral400,
    onError = Slate900,
    errorContainer = Coral700,
    onErrorContainer = Coral100,

    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,

    outline = Slate500,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = PureWhite,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,

    secondary = Gold600,
    onSecondary = PureWhite,
    secondaryContainer = Gold100,
    onSecondaryContainer = Gold700,

    tertiary = Amber600,
    onTertiary = PureWhite,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,

    error = Coral600,
    onError = PureWhite,
    errorContainer = Coral100,
    onErrorContainer = Coral700,

    background = WarmWhite,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,

    outline = Slate400,
    outlineVariant = Slate200,
    inverseSurface = Slate800,
    inverseOnSurface = Slate100
)

@Composable
fun HalalCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
