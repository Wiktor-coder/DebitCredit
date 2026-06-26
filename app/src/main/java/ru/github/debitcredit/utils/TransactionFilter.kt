package ru.github.debitcredit.utils

import android.content.Context
import ru.github.debitcredit.data.model.TransactionEntity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class PeriodData(
    val label: String,
    val expense: Float,
    val income: Float
)

class TransactionFilter {

    fun filterByPeriod(
        transactions: List<TransactionEntity>,
        period: String,
        context: Context
    ): List<PeriodData> {
        if (transactions.isEmpty()) {
            return emptyList()
        }

        // Получаем смещение часового пояса
        val offset = TimeZoneHelper.getTimeZoneOffset(context)

        // Получаем текущее время в UTC
        val now = System.currentTimeMillis()

        // Создаем календарь в UTC для определения периода
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = now
        }

        // Определяем начальную и конечную дату периода в UTC
        val (start, end) = getPeriodRange(period, calendar)

        // Фильтруем транзакции по периоду (все в UTC)
        val filtered = transactions.filter { transaction ->
            transaction.date in start..end
        }

        // Группируем с учетом часового пояса
        return when (period) {
            "day" -> groupByHour(filtered, offset)
            "week" -> groupByDay(filtered, offset)
            "month" -> groupByDayOfMonth(filtered, offset)
            "year" -> groupByMonth(filtered, offset)
            else -> emptyList()
        }
    }

    private fun getPeriodRange(period: String, calendar: Calendar): Pair<Long, Long> {
        return when (period) {
            "day" -> {
                val start = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to (start + TimeUnit.DAYS.toMillis(1))
            }
            "week" -> {
                val start = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to (start + TimeUnit.DAYS.toMillis(7))
            }
            "month" -> {
                val start = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to (start + TimeUnit.DAYS.toMillis(31))
            }
            "year" -> {
                val start = calendar.apply {
                    set(Calendar.MONTH, 0)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to (start + TimeUnit.DAYS.toMillis(366))
            }
            else -> {
                val start = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to (start + TimeUnit.DAYS.toMillis(31))
            }
        }
    }

    private fun groupByHour(transactions: List<TransactionEntity>, offset: Int): List<PeriodData> {
        // Получаем текущий час с учетом пояса
        val currentHour = (Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY) + offset).let {
            var h = it
            while (h < 0) h += 24
            while (h >= 24) h -= 24
            h
        }

        val hourMap = mutableMapOf<Int, Pair<Float, Float>>()

        for (hour in 0..currentHour) {
            hourMap[hour] = 0f to 0f
        }

        for (transaction in transactions) {
            // Корректируем время транзакции с учетом пояса
            val correctedTime = transaction.date + (offset * 60 * 60 * 1000L)
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = correctedTime
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            if (hour <= currentHour) {
                val current = hourMap[hour] ?: (0f to 0f)
                val (expense, income) = current
                if (transaction.type == "expense") {
                    hourMap[hour] = (expense + transaction.amount) to income
                } else {
                    hourMap[hour] = expense to (income + transaction.amount)
                }
            }
        }

        return (0..currentHour).map { hour ->
            val (expense, income) = hourMap[hour] ?: (0f to 0f)
            PeriodData("$hour:00", expense, income)
        }
    }

    private fun groupByDay(transactions: List<TransactionEntity>, offset: Int): List<PeriodData> {
        val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        // Получаем текущий день недели с учетом пояса
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = System.currentTimeMillis() + (offset * 60 * 60 * 1000L)
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val dayMap = mutableMapOf<String, Pair<Float, Float>>()
        val daysPassed = when (currentDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        for (i in 0..daysPassed) {
            dayMap[dayNames[i]] = 0f to 0f
        }

        for (transaction in transactions) {
            val correctedTime = transaction.date + (offset * 60 * 60 * 1000L)
            val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal2.timeInMillis = correctedTime
            val dayOfWeek = cal2.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Пн"
                Calendar.TUESDAY -> "Вт"
                Calendar.WEDNESDAY -> "Ср"
                Calendar.THURSDAY -> "Чт"
                Calendar.FRIDAY -> "Пт"
                Calendar.SATURDAY -> "Сб"
                Calendar.SUNDAY -> "Вс"
                else -> ""
            }

            if (dayMap.containsKey(dayName)) {
                val current = dayMap[dayName] ?: (0f to 0f)
                val (expense, income) = current
                if (transaction.type == "expense") {
                    dayMap[dayName] = (expense + transaction.amount) to income
                } else {
                    dayMap[dayName] = expense to (income + transaction.amount)
                }
            }
        }

        return (0..daysPassed).map { day ->
            val (expense, income) = dayMap[dayNames[day]] ?: (0f to 0f)
            PeriodData(dayNames[day], expense, income)
        }
    }

    private fun groupByDayOfMonth(transactions: List<TransactionEntity>, offset: Int): List<PeriodData> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = System.currentTimeMillis() + (offset * 60 * 60 * 1000L)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val dayMap = mutableMapOf<Int, Pair<Float, Float>>()

        for (day in 1..currentDay) {
            dayMap[day] = 0f to 0f
        }

        for (transaction in transactions) {
            val correctedTime = transaction.date + (offset * 60 * 60 * 1000L)
            val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal2.timeInMillis = correctedTime
            val day = cal2.get(Calendar.DAY_OF_MONTH)
            if (day <= currentDay) {
                val current = dayMap[day] ?: (0f to 0f)
                val (expense, income) = current
                if (transaction.type == "expense") {
                    dayMap[day] = (expense + transaction.amount) to income
                } else {
                    dayMap[day] = expense to (income + transaction.amount)
                }
            }
        }

        return (1..currentDay).map { day ->
            val (expense, income) = dayMap[day] ?: (0f to 0f)
            PeriodData(day.toString(), expense, income)
        }
    }

    private fun groupByMonth(transactions: List<TransactionEntity>, offset: Int): List<PeriodData> {
        val monthNames = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн",
            "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = System.currentTimeMillis() + (offset * 60 * 60 * 1000L)
        val currentMonth = cal.get(Calendar.MONTH)

        val monthMap = mutableMapOf<Int, Pair<Float, Float>>()
        for (month in 0..currentMonth) {
            monthMap[month] = 0f to 0f
        }

        for (transaction in transactions) {
            val correctedTime = transaction.date + (offset * 60 * 60 * 1000L)
            val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal2.timeInMillis = correctedTime
            val month = cal2.get(Calendar.MONTH)
            if (month <= currentMonth) {
                val current = monthMap[month] ?: (0f to 0f)
                val (expense, income) = current
                if (transaction.type == "expense") {
                    monthMap[month] = (expense + transaction.amount) to income
                } else {
                    monthMap[month] = expense to (income + transaction.amount)
                }
            }
        }

        return (0..currentMonth).map { month ->
            val (expense, income) = monthMap[month] ?: (0f to 0f)
            PeriodData(monthNames[month], expense, income)
        }
    }
}