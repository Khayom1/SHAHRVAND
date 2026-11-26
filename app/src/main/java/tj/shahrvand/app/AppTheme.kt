package tj.shahrvand.app

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
    background=Color(0xFFF3F7FA), surface=Color(0xFFFFFFFF), surfaceVariant=Color(0xFFEAF0F4),
    ink=Color(0xFF1D2C38), muted=Color(0xFF687987), border=Color(0xFFDCE5EB),
    navy=Color(0xFF183247), navyText=Color(0xFF183247), navyLight=Color(0xFF315A72), gold=Color(0xFFC8954A),
    green=Color(0xFF2F7969), danger=Color(0xFFB45D50), selected=Color(0xFFEDE5F8),
    goldSurface=Color(0xFFFFF5E3), successSurface=Color(0xFFEAF5EF),
    successBorder=Color(0xFFC9DED1), dangerSurface=Color(0xFFFFF1EE), isDark=false
)

private val DarkPalette = ShahrvandPalette(
    background=Color(0xFF101820), surface=Color(0xFF1B2732), surfaceVariant=Color(0xFF293743),
    ink=Color(0xFFEDF3F7), muted=Color(0xFFA7B5C0), border=Color(0xFF344653),
    navy=Color(0xFF203D52), navyText=Color(0xFFD8E6F0), navyLight=Color(0xFF91B6CD), gold=Color(0xFFE3BB79),
    green=Color(0xFF80C8AD), danger=Color(0xFFF18B80), selected=Color(0xFF37344A),
    goldSurface=Color(0xFF3B3327), successSurface=Color(0xFF203B32),
    successBorder=Color(0xFF315A49), dangerSurface=Color(0xFF3A2928), isDark=true
)

internal val LocalShahrvandPalette = staticCompositionLocalOf { LightPalette }

@Composable
internal fun ShahrvandTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val palette = if (dark) DarkPalette else LightPalette
    val colors = if (dark) darkColorScheme(
        primary=palette.navyLight, onPrimary=Color(0xFF10212B),
        secondary=palette.gold, onSecondary=Color(0xFF211A10), tertiary=palette.green,
        background=palette.background, onBackground=palette.ink,
        surface=palette.surface, onSurface=palette.ink,
        surfaceVariant=palette.surfaceVariant, onSurfaceVariant=palette.muted,
        outline=palette.border, error=palette.danger
    ) else lightColorScheme(
        primary=palette.navy, onPrimary=Color.White,
        secondary=palette.gold, onSecondary=Color(0xFF211A10), tertiary=palette.green,
        background=palette.background, onBackground=palette.ink,
        surface=palette.surface, onSurface=palette.ink,
        surfaceVariant=palette.surfaceVariant, onSurfaceVariant=palette.muted,
        outline=palette.border, error=palette.danger
    )
    CompositionLocalProvider(LocalShahrvandPalette provides palette) {
        MaterialTheme(colorScheme=colors, content=content)
    }
}
