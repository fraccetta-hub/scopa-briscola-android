package com.fraccetta.scopabriscola.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TableGreen = Color(0xFF2D6A2D)
val CardCream = Color(0xFFFFF8DC)
val AccentGold = Color(0xFFDAA520)

private val DarkColorScheme = darkColorScheme(
    primary = TableGreen,
    secondary = AccentGold,
    tertiary = CardCream,
    background = Color(0xFF1B1B1B),
    surface = Color(0xFF2C2C2C)
)

private val LightColorScheme = lightColorScheme(
    primary = TableGreen,
    secondary = AccentGold,
    tertiary = CardCream,
    background = CardCream,
    surface = Color(0xFFF5F0E0)
)

@Composable
fun ScopaBriscolaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
