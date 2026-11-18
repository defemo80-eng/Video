package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

private val DeiDarkColorScheme = darkColorScheme(
    primary = DeiRed,
    onPrimary = DeiWhite,
    primaryContainer = DeiRedDark,
    onPrimaryContainer = DeiWhite,
    secondary = DeiWhite,
    onSecondary = DeiBlack,
    secondaryContainer = DeiBlackCard,
    onSecondaryContainer = DeiWhite,
    tertiary = DeiRedBright,
    onTertiary = DeiWhite,
    background = DeiBlack,
    onBackground = DeiWhite,
    surface = DeiBlackElevated,
    onSurface = DeiWhite,
    surfaceVariant = DeiBlackCard,
    onSurfaceVariant = DeiTextMutedDark,
    outline = DeiBlackOutline,
    error = DeiRedBright,
    onError = DeiWhite
)

private val DeiLightColorScheme = lightColorScheme(
    primary = DeiRed,
    onPrimary = DeiWhite,
    primaryContainer = ColorTokens.LightRedContainer,
    onPrimaryContainer = DeiRedDark,
    secondary = DeiBlack,
    onSecondary = DeiWhite,
    secondaryContainer = DeiLightSurfaceVariant,
    onSecondaryContainer = DeiBlack,
    tertiary = DeiRedBright,
    onTertiary = DeiWhite,
    background = DeiOffWhite,
    onBackground = DeiBlack,
    surface = DeiLightCard,
    onSurface = DeiBlack,
    surfaceVariant = DeiLightSurfaceVariant,
    onSurfaceVariant = DeiTextMutedLight,
    outline = DeiLightOutline,
    error = DeiRed,
    onError = DeiWhite
)

private object ColorTokens {
    val LightRedContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD9)
}

val DeiPlayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeiPlayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDark) DeiDarkColorScheme else DeiLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeiPlayTypography,
        shapes = DeiPlayShapes,
        content = content
    )
}
