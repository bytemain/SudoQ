package de.sudoq.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Theme color options for the app
 */
enum class ThemeColor {
    GREEN,
    BLUE,
    PURPLE,
    RED,
    ORANGE;
    
    companion object {
        fun fromOrdinal(ordinal: Int): ThemeColor {
            return values().getOrNull(ordinal) ?: GREEN
        }
    }
}

// Green Theme Colors
private val GreenLight = Color(0xFF2E7D32) // Material Green 800
private val GreenDark = Color(0xFF66BB6A) // Material Green 400
private val GreenContainer = Color(0xFFC8E6C9) // Material Green 100
private val OnGreenContainer = Color(0xFF1B5E20) // Material Green 900

// Blue Theme Colors
private val BlueLight = Color(0xFF1565C0) // Material Blue 800
private val BlueDark = Color(0xFF42A5F5) // Material Blue 400
private val BlueContainer = Color(0xFFBBDEFB) // Material Blue 100
private val OnBlueContainer = Color(0xFF0D47A1) // Material Blue 900

// Purple Theme Colors (Material3 default-like)
private val PurpleLight = Color(0xFF6750A4)
private val PurpleDark = Color(0xFFD0BCFF)
private val PurpleContainer = Color(0xFFEADDFF)
private val OnPurpleContainer = Color(0xFF21005D)

// Red Theme Colors
private val RedLight = Color(0xFFC62828) // Material Red 800
private val RedDark = Color(0xFFEF5350) // Material Red 400
private val RedContainer = Color(0xFFFFCDD2) // Material Red 100
private val OnRedContainer = Color(0xFFB71C1C) // Material Red 900

// Orange Theme Colors
private val OrangeLight = Color(0xFFE65100) // Material Orange 900
private val OrangeDark = Color(0xFFFF9800) // Material Orange 500
private val OrangeContainer = Color(0xFFFFE0B2) // Material Orange 100
private val OnOrangeContainer = Color(0xFFE65100) // Material Orange 900

/**
 * Creates a light color scheme for the given theme color
 */
fun getLightColorScheme(themeColor: ThemeColor): ColorScheme {
    return when (themeColor) {
        ThemeColor.GREEN -> lightColorScheme(
            primary = GreenLight,
            onPrimary = Color.White,
            primaryContainer = GreenContainer,
            onPrimaryContainer = OnGreenContainer,
            secondary = GreenLight,
            onSecondary = Color.White,
            secondaryContainer = GreenContainer,
            onSecondaryContainer = OnGreenContainer
        )
        ThemeColor.BLUE -> lightColorScheme(
            primary = BlueLight,
            onPrimary = Color.White,
            primaryContainer = BlueContainer,
            onPrimaryContainer = OnBlueContainer,
            secondary = BlueLight,
            onSecondary = Color.White,
            secondaryContainer = BlueContainer,
            onSecondaryContainer = OnBlueContainer
        )
        ThemeColor.PURPLE -> lightColorScheme(
            primary = PurpleLight,
            onPrimary = Color.White,
            primaryContainer = PurpleContainer,
            onPrimaryContainer = OnPurpleContainer,
            secondary = PurpleLight,
            onSecondary = Color.White,
            secondaryContainer = PurpleContainer,
            onSecondaryContainer = OnPurpleContainer
        )
        ThemeColor.RED -> lightColorScheme(
            primary = RedLight,
            onPrimary = Color.White,
            primaryContainer = RedContainer,
            onPrimaryContainer = OnRedContainer,
            secondary = RedLight,
            onSecondary = Color.White,
            secondaryContainer = RedContainer,
            onSecondaryContainer = OnRedContainer
        )
        ThemeColor.ORANGE -> lightColorScheme(
            primary = OrangeLight,
            onPrimary = Color.White,
            primaryContainer = OrangeContainer,
            onPrimaryContainer = OnOrangeContainer,
            secondary = OrangeLight,
            onSecondary = Color.White,
            secondaryContainer = OrangeContainer,
            onSecondaryContainer = OnOrangeContainer
        )
    }
}

/**
 * Creates a dark color scheme for the given theme color
 */
fun getDarkColorScheme(themeColor: ThemeColor): ColorScheme {
    return when (themeColor) {
        ThemeColor.GREEN -> darkColorScheme(
            primary = GreenDark,
            onPrimary = Color(0xFF003300),
            primaryContainer = OnGreenContainer,
            onPrimaryContainer = GreenContainer,
            secondary = GreenDark,
            onSecondary = Color(0xFF003300),
            secondaryContainer = OnGreenContainer,
            onSecondaryContainer = GreenContainer
        )
        ThemeColor.BLUE -> darkColorScheme(
            primary = BlueDark,
            onPrimary = Color(0xFF001A33),
            primaryContainer = OnBlueContainer,
            onPrimaryContainer = BlueContainer,
            secondary = BlueDark,
            onSecondary = Color(0xFF001A33),
            secondaryContainer = OnBlueContainer,
            onSecondaryContainer = BlueContainer
        )
        ThemeColor.PURPLE -> darkColorScheme(
            primary = PurpleDark,
            onPrimary = Color(0xFF381E72),
            primaryContainer = OnPurpleContainer,
            onPrimaryContainer = PurpleContainer,
            secondary = PurpleDark,
            onSecondary = Color(0xFF381E72),
            secondaryContainer = OnPurpleContainer,
            onSecondaryContainer = PurpleContainer
        )
        ThemeColor.RED -> darkColorScheme(
            primary = RedDark,
            onPrimary = Color(0xFF330000),
            primaryContainer = OnRedContainer,
            onPrimaryContainer = RedContainer,
            secondary = RedDark,
            onSecondary = Color(0xFF330000),
            secondaryContainer = OnRedContainer,
            onSecondaryContainer = RedContainer
        )
        ThemeColor.ORANGE -> darkColorScheme(
            primary = OrangeDark,
            onPrimary = Color(0xFF4D2600),
            primaryContainer = OnOrangeContainer,
            onPrimaryContainer = OrangeContainer,
            secondary = OrangeDark,
            onSecondary = Color(0xFF4D2600),
            secondaryContainer = OnOrangeContainer,
            onSecondaryContainer = OrangeContainer
        )
    }
}
