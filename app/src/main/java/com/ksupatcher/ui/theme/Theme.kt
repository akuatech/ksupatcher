package com.ksupatcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B5AA6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DBFF),
    onPrimaryContainer = Color(0xFF1C103B),
    secondary = Color(0xFF5E5C71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E0F4),
    onSecondaryContainer = Color(0xFF1B1927),
    surface = Color(0xFFF7F4FA),
    onSurface = Color(0xFF1A1A1E),
    surfaceVariant = Color(0xFFE6E0EE),
    onSurfaceVariant = Color(0xFF4A4458)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD2C5FF),
    onPrimary = Color(0xFF2B1B55),
    primaryContainer = Color(0xFF3E2F6A),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFC9C3DC),
    onSecondary = Color(0xFF302D40),
    secondaryContainer = Color(0xFF433F55),
    onSecondaryContainer = Color(0xFFE5DFF4),
    surface = Color(0xFF121216),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF443D51),
    onSurfaceVariant = Color(0xFFC9C3D2)
)

@Composable
fun KsuPatcherTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
