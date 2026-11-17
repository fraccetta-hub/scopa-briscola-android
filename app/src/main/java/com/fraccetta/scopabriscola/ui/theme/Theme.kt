package com.fraccetta.scopabriscola.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TableGreen = Color(0xFF1A3A5C)   // ocean blue
val CardCream = Color(0xFFFFF8DC)
val AccentGold = Color(0xFFDAA520)

private val DarkColorScheme = darkColorScheme(
    primary = TableGreen,
    secondary = AccentGold,
    tertiary = CardCream,
    background = Color(0xFF0D1F33),
    surface = Color(0xFF1A3A5C)
)

private val LightColorScheme = lightColorScheme(
    primary = TableGreen,
    secondary = AccentGold,
    tertiary = CardCream,
    background = CardCream,
    surface = Color(0xFFE8F0F7)
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
