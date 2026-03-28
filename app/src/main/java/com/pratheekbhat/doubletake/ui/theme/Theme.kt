package com.pratheekbhat.doubletake.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = NavyDark,
    background = BackgroundGrey,
    onBackground = NavyDark,
    surface = SurfaceWhite,
    onSurfaceVariant = SlateMid,
    outline = BorderGrey,
    surfaceVariant = BorderGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = Color.White,
    background = NavyDark,
    onBackground = SurfaceWhite,
    surface = Color(0xFF1E293B),
    onSurface = SurfaceWhite,
    onSurfaceVariant = SlateLight,
    outline = SlateGrey,
    surfaceVariant = Color(0xFF334155)
)

@Composable
fun DoubleTakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}