package de.sudoq.view.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages theme preferences
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_DARK_MODE = "dark_mode"
    
    fun saveThemeColor(context: Context, themeColor: ThemeColor) {
        getPrefs(context).edit()
            .putInt(KEY_THEME_COLOR, themeColor.ordinal)
            .apply()
    }
    
    fun loadThemeColor(context: Context): ThemeColor {
        val ordinal = getPrefs(context).getInt(KEY_THEME_COLOR, ThemeColor.GREEN.ordinal)
        return ThemeColor.fromOrdinal(ordinal)
    }
    
    fun saveDarkMode(context: Context, isDark: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }
    
    fun loadDarkMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false)
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
