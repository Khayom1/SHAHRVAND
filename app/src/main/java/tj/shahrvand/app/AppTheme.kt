package tj.shahrvand.app

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class ShahrvandPalette(
    val background: Color, val surface: Color, val surfaceVariant: Color,
    val ink: Color, val muted: Color, val border: Color,
    val navy: Color, val navyText: Color, val navyLight: Color, val gold: Color, val green: Color, val isDark: Boolean,
    val danger: Color, val selected: Color, val goldSurface: Color,
    val successSurface: Color, val successBorder: Color, val dangerSurface: Color
)

private val LightPalette = ShahrvandPalette(
    background = Color(0xFFF4F7FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF2FB),
    ink = Color(0xFF1D2941),
    muted = Color(0xFF6D7C95),
    border = Color(0xFFE5EBF5),
    navy = Color(0xFF3559E8),
    navyText = Color(0xFF192746),
    navyLight = Color(0xFF5678F5),
    gold = Color(0xFF8F78EC),
    green = Color(0xFF50D6BA),
    danger = Color(0xFFC65E58),
    selected = Color(0xFFE9EEFF),
    goldSurface = Color(0xFFF3F0FF),
    successSurface = Color(0xFFEAF7EF),
    successBorder = Color(0xFFCBE8D5),
    dangerSurface = Color(0xFFFFF0EE),
    isDark = false
)

private val DarkPalette = ShahrvandPalette(
    background = Color(0xFF101A27),
    surface = Color(0xFF1B2939),
    surfaceVariant = Color(0xFF29394C),
    ink = Color(0xFFF0F5FB),
    muted = Color(0xFFAAB9CA),
    border = Color(0xFF34475D),
    navy = Color(0xFF86B7F2),
    navyText = Color(0xFFE8F1FC),
    navyLight = Color(0xFF9BC6F7),
    gold = Color(0xFFC1A0F5),
    green = Color(0xFF7AD5A3),
    danger = Color(0xFFF08C82),
    selected = Color(0xFF39344F),
    goldSurface = Color(0xFF342C49),
    successSurface = Color(0xFF203B32),
    successBorder = Color(0xFF315A49),
    dangerSurface = Color(0xFF3A2928),
    isDark = true
)

internal val LocalShahrvandPalette = staticCompositionLocalOf { LightPalette }

@Composable
internal fun ShahrvandTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val palette = if (dark) DarkPalette else LightPalette
    val colors = if (dark) darkColorScheme(
        primary = palette.navyLight, onPrimary = Color(0xFF10212B),
        secondary = palette.gold, onSecondary = Color(0xFF211A10), tertiary = palette.green,
        background = palette.background, onBackground = palette.ink,
        surface = palette.surface, onSurface = palette.ink,
        surfaceVariant = palette.surfaceVariant, onSurfaceVariant = palette.muted,
        outline = palette.border, error = palette.danger
    ) else lightColorScheme(
        primary = palette.navy, onPrimary = Color.White,
        secondary = palette.gold, onSecondary = Color(0xFF211A10), tertiary = palette.green,
        background = palette.background, onBackground = palette.ink,
        surface = palette.surface, onSurface = palette.ink,
        surfaceVariant = palette.surfaceVariant, onSurfaceVariant = palette.muted,
        outline = palette.border, error = palette.danger
    )
    CompositionLocalProvider(LocalShahrvandPalette provides palette) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable
internal fun Modifier.shahrvandCanvas(): Modifier {
    val palette = LocalShahrvandPalette.current
    return if (palette.isDark) {
        this.background(palette.background)
    } else {
        this.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF4F7FC),
                    Color(0xFFEEF3FB),
                    Color(0xFFF8FAFF)
                )
            )
        )
    }
}
