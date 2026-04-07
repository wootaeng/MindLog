package com.ws.skelton.remind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF1B1A17), // DarkBackground와 맞춤
    surface = DarkSurface,
    onSurface = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    secondary = DarkSecondaryYellow, // 톤다운된 노란색 사용
    onSecondary = Color(0xFF1B1A17),
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkSubText
)

private val LightColorScheme = lightColorScheme(
    primary = TextBrown,
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = TextBrown,
    background = BackgroundCream,
    onBackground = TextBrown,
    secondary = CardYellow,
    onSecondary = TextBrown
)

@Composable
fun ReMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontIndex: Int = 0,
    fontSizeIndex: Int = 1, // 추가
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = getTypography(fontIndex, fontSizeIndex) // 수정

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
