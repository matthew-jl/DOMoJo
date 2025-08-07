package edu.bluejack24_2.domojo.utils

import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    internal const val THEME_PREFERENCE = "app_theme_preference"
    internal const val THEME_LIGHT = "light"
    internal const val THEME_DARK = "dark"
    internal const val THEME_SYSTEM = "system"

    fun applyTheme(themePreference: String) {
        when (themePreference) {
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getSavedTheme(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(THEME_PREFERENCE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveThemePreference(context: Context, theme: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(THEME_PREFERENCE, theme)
            .apply()
    }
}