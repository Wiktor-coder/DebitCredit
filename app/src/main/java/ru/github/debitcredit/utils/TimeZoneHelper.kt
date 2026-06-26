package ru.github.debitcredit.utils

import android.content.Context
import java.util.TimeZone

object TimeZoneHelper {

    fun getTimeZoneOffset(context: Context): Int {
        val settingsManager = SettingsManager(context)
        // По умолчанию используем системный часовой пояс
        val defaultOffset = TimeZone.getDefault().rawOffset / (60 * 60 * 1000)
        val offset = settingsManager.getTimeZoneOffset().takeIf { it != 0 } ?: defaultOffset
        return offset
    }
}