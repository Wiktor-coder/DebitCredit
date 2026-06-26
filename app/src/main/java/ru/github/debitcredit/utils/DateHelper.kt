package ru.github.debitcredit.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {
    fun getCurrentDateTimeWithOffset(context: Context): Long {
        val offset = TimeZoneHelper.getTimeZoneOffset(context)
        return System.currentTimeMillis() + offset * 60 * 60 * 1000L
    }

    fun formatDate(date: Long, context: Context): String {
        val offset = TimeZoneHelper.getTimeZoneOffset(context)
        val adjusted = date + offset * 60 * 60 * 1000L
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(adjusted))
    }

    fun getStartOfDay(context: Context): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfWeek(context: Context): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfMonth(context: Context): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfYear(context: Context): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, 0)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getPeriodDates(period: String, context: Context): Pair<Long, Long> {
        return when (period) {
            "day" -> {
                val start = getStartOfDay(context)
                start to (start + 24 * 60 * 60 * 1000)
            }
            "week" -> {
                val start = getStartOfWeek(context)
                start to (start + 7 * 24 * 60 * 60 * 1000)
            }
            "month" -> {
                val start = getStartOfMonth(context)
                start to (start + 31 * 24 * 60 * 60 * 1000L)
            }
            "year" -> {
                val start = getStartOfYear(context)
                start to (start + 366 * 24 * 60 * 60 * 1000L)
            }
            else -> {
                val start = getStartOfMonth(context)
                start to (start + 31 * 24 * 60 * 60 * 1000L)
            }
        }
    }
}