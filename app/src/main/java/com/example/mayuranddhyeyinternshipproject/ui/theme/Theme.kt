package com.example.mayuranddhyeyinternshipproject.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftSurfaceVariant,
    onPrimaryContainer = TealPrimaryDark,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = MintAccent.copy(alpha = 0.25f),
    onSecondaryContainer = TealPrimaryDark,
    tertiary = MintAccent,
    onTertiary = TextPrimary,
    background = SoftBackground,
    onBackground = TextPrimary,
    surface = SoftSurface,
    onSurface = TextPrimary,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorBg,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryLight,
    onPrimary = Color(0xFF003831),
    primaryContainer = Color(0xFF004C40),
    onPrimaryContainer = MintAccent,
    secondary = MintAccent,
    onSecondary = Color(0xFF003831),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF334644),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF5D0005)
)

val MedMateShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MedMateTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When dark theme is active, status bar icons must be WHITE (light icons -> isAppearanceLightStatusBars = false)
                // When light theme is active, status bar icons must be DARK (dark icons -> isAppearanceLightStatusBars = true)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MedMateShapes,
        content = content
    )
}
