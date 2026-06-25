package ru.github.debitcredit.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

object TimeZoneHelper {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_TIMEZONE_OFFSET = "timezone_offset"

    fun getTimeZoneOffset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // По умолчанию используем системный часовой пояс
        val defaultOffset = TimeZone.getDefault().rawOffset / (60 * 60 * 1000)
        return prefs.getInt(KEY_TIMEZONE_OFFSET, defaultOffset)
    }

    fun saveTimeZoneOffset(context: Context, offset: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TIMEZONE_OFFSET, offset).apply()
    }

    fun getCurrentHourWithOffset(context: Context): Int {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // Применяем смещение
        var adjustedHour = hour + offset
        while (adjustedHour < 0) adjustedHour += 24
        while (adjustedHour >= 24) adjustedHour -= 24
        return adjustedHour
    }

    fun getCurrentDayWithOffset(context: Context): Int {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance()
        // Применяем смещение к дате
        calendar.add(Calendar.HOUR_OF_DAY, offset)
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getCurrentMonthWithOffset(context: Context): Int {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, offset)
        return calendar.get(Calendar.MONTH)
    }

    fun getCurrentYearWithOffset(context: Context): Int {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, offset)
        return calendar.get(Calendar.YEAR)
    }

    fun getCurrentDayOfWeekWithOffset(context: Context): Int {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, offset)
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun adjustTransactionTime(date: Long, context: Context): Long {
        val offset = getTimeZoneOffset(context)
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendar.add(Calendar.HOUR_OF_DAY, offset)
        return calendar.timeInMillis
    }
}