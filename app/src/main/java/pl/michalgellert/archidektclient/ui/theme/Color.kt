package pl.michalgellert.archidektclient.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

// MTG-inspired color palette

// Gold/Amber - like card borders
val MtgGold = Color(0xFFD4A84B)
val MtgGoldLight = Color(0xFFE8C878)
val MtgGoldDark = Color(0xFFB8860B)

// Deep colors for accents
val MtgBronze = Color(0xFF8B6914)
val MtgBurgundy = Color(0xFF722F37)

// Light theme backgrounds - parchment/cream tones
val ParchmentLight = Color(0xFFFAF6F0)
val ParchmentMedium = Color(0xFFF5EFE6)
val ParchmentDark = Color(0xFFE8E0D5)

// Dark theme backgrounds - rich dark tones
val DarkBackground = Color(0xFF1A1816)
val DarkSurface = Color(0xFF252220)
val DarkSurfaceVariant = Color(0xFF322E2A)

// Text colors
val TextOnLight = Color(0xFF1C1917)
val TextOnLightVariant = Color(0xFF57534E)
val TextOnDark = Color(0xFFFAFAF9)
val TextOnDarkVariant = Color(0xFFA8A29E)

// MTG Mana colors (for reference)
val MtgWhite = Color(0xFFF9FAF4)
val MtgBlue = Color(0xFF0E68AB)
val MtgBlack = Color(0xFF150B00)
val MtgRed = Color(0xFFD3202A)
val MtgGreen = Color(0xFF00733E)
val MtgColorless = Color(0xFF9E9E9E)

/**
 * Calculate the best contrasting text color (black or white) for a given background color.
 * Uses WCAG luminance calculation.
 */
fun Color.contrastingTextColor(): Color {
    val luminance = ColorUtils.calculateLuminance(
        android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    )
    // Use white text on dark backgrounds, black text on light backgrounds
    // 0.5 threshold gives good results for most colors
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Parse a hex color string to Compose Color, with fallback.
 */
fun parseColorSafe(colorString: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        fallback
    }
}
