package com.example.grama_angana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF00897B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    secondary = Color(0xFF4DB6AC)
)

@Composable
fun GramaAnganaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
