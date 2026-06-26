package ru.github.debitcredit.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TIMEZONE_OFFSET = "timezone_offset"
        private const val DEFAULT_CURRENCY = "RUB"
        private const val DEFAULT_LANGUAGE = "ru"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrency(): String = prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_THEME, false)

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun getTimeZoneOffset(): Int = prefs.getInt(KEY_TIMEZONE_OFFSET, 0)

    fun saveCurrency(currency: String) {
        prefs.edit { putString(KEY_CURRENCY, currency) }
    }

    fun saveTheme(isDark: Boolean) {
        prefs.edit { putBoolean(KEY_THEME, isDark) }
    }

    fun saveLanguage(language: String) {
        prefs.edit { putString(KEY_LANGUAGE, language) }
    }

    fun saveTimeZoneOffset(offset: Int) {
        prefs.edit { putInt(KEY_TIMEZONE_OFFSET, offset) }
    }
}