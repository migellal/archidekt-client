package pl.michalgellert.archidektclient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Primary - Gold/Amber like card borders
    primary = MtgGoldDark,
    onPrimary = Color.White,
    primaryContainer = MtgGoldLight,
    onPrimaryContainer = MtgBronze,

    // Secondary - Bronze accent
    secondary = MtgBronze,
    onSecondary = Color.White,
    secondaryContainer = ParchmentDark,
    onSecondaryContainer = MtgBronze,

    // Tertiary - Burgundy for special accents
    tertiary = MtgBurgundy,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD9),
    onTertiaryContainer = MtgBurgundy,

    // Background - Parchment/cream tones
    background = ParchmentLight,
    onBackground = TextOnLight,
    surface = Color.White,
    onSurface = TextOnLight,
    surfaceVariant = ParchmentMedium,
    onSurfaceVariant = TextOnLightVariant,

    // Outline
    outline = ParchmentDark,
    outlineVariant = Color(0xFFD5CBC0),

    // Error
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    // Primary - Brighter gold for dark theme
    primary = MtgGold,
    onPrimary = MtgBronze,
    primaryContainer = MtgGoldDark,
    onPrimaryContainer = MtgGoldLight,

    // Secondary
    secondary = MtgGoldLight,
    onSecondary = MtgBronze,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = MtgGoldLight,

    // Tertiary
    tertiary = Color(0xFFFFB3B0),
    onTertiary = Color(0xFF561D1F),
    tertiaryContainer = MtgBurgundy,
    onTertiaryContainer = Color(0xFFFFDAD9),

    // Background - Rich dark tones
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkVariant,

    // Outline
    outline = Color(0xFF5C5650),
    outlineVariant = Color(0xFF4A4540),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun ArchidektClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled - we use our custom MTG theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
