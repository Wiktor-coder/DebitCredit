package ru.github.debitcredit.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.customview.CustomBarChart
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.utils.TimeZoneHelper
import ru.github.debitcredit.viewmodel.MainViewModel
import java.util.*

class StatisticsFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var chart: CustomBarChart
    private lateinit var statsView: StatsView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemsAdapter: StatisticsItemsAdapter
    private var currentPeriod = "month"
    private var selectedButtonId = R.id.monthButton
    private var isDataLoaded = false

    private var transactions: List<TransactionEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = view.findViewById(R.id.chart)
        statsView = view.findViewById(R.id.statsView)
        itemsRecyclerView = view.findViewById(R.id.itemsRecyclerView)

        itemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        itemsAdapter = StatisticsItemsAdapter()
        itemsRecyclerView.adapter = itemsAdapter

        setupPeriodButtons(view)
        setupBackButton(view)
        setupStatsViewClick()

        observeData()
    }

    private fun setupPeriodButtons(view: View) {
        val buttons = mapOf(
            R.id.dayButton to "day",
            R.id.weekButton to "week",
            R.id.monthButton to "month",
            R.id.yearButton to "year"
        )

        buttons.forEach { (id, period) ->
            view.findViewById<Button>(id).setOnClickListener {
                currentPeriod = period
                selectedButtonId = id
                updateAllData()
                updateButtonStates(view, id)
            }
        }

        updateButtonStates(view, R.id.monthButton)
    }

    private fun updateButtonStates(view: View, selectedId: Int) {
        val buttonIds = listOf(R.id.dayButton, R.id.weekButton, R.id.monthButton, R.id.yearButton)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.colorAccent)

        buttonIds.forEach { id ->
            val button = view.findViewById<Button>(id)
            if (id == selectedId) {
                button.setBackgroundColor(Color.parseColor("#DAA520"))
            } else {
                button.setBackgroundColor(defaultColor)
            }
        }
    }

    private fun setupBackButton(view: View) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupStatsViewClick() {
        statsView.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.transactions.collect { list ->
                transactions = list
                isDataLoaded = true
                updateAllData()
            }
        }

        lifecycleScope.launch {
            viewModel.totalIncome.collect {
                if (isDataLoaded) {
                    updateAllData()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.totalExpenses.collect {
                if (isDataLoaded) {
                    updateAllData()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.balance.collect {
                if (isDataLoaded) {
                    updateAllData()
                }
            }
        }
    }

    private fun updateAllData() {
        val income = viewModel.totalIncome.value
        val expenses = viewModel.totalExpenses.value
        val balance = viewModel.balance.value

        updateStatsView(income, expenses)
        updateChart()
        updateInfoItems(income, expenses, balance)
    }

    private fun updateStatsView(income: Float, expenses: Float) {
        val percentage = if (income > 0) {
            kotlin.math.min((expenses / income) * 100, 100f)
        } else {
            0f
        }

        val statsData = listOf(
            StatsView.CategoryData(
                name = getString(R.string.spent),
                amount = percentage,
                color = Color.parseColor("#FF6B6B")
            ),
            StatsView.CategoryData(
                name = getString(R.string.remaining),
                amount = 100f - percentage,
                color = Color.parseColor("#4ECDC4")
            )
        )

        statsView.isSmallMode = false
        statsView.data = statsData
    }

    private fun updateInfoItems(income: Float, expenses: Float, balance: Float) {
        val percentage = if (income > 0) {
            kotlin.math.min((expenses / income) * 100, 100f)
        } else {
            0f
        }

        val infoItems = listOf(
            InfoItem(
                getString(R.string.spent_percent),
                String.format("%.1f%%", percentage)
            ),
            InfoItem(
                getString(R.string.incomes),
                String.format("%.2f ₽", income)
            ),
            InfoItem(
                getString(R.string.expenses),
                String.format("%.2f ₽", expenses)
            ),
            InfoItem(
                getString(R.string.remaining),
                String.format("%.2f ₽", balance)
            )
        )

        itemsAdapter.updateData(infoItems)
    }

    private fun updateChart() {
        val chartData = getChartData()

        Log.d("StatisticsFragment", "Метки графика: ${chartData.map { "${it.label} (расход: ${it.expense}, доход: ${it.income})" }}")

        if (chartData.isEmpty()) {
            chart.visibility = View.GONE
            return
        }
        chart.visibility = View.VISIBLE

        chart.data = chartData.map {
            CustomBarChart.BarData(it.label, it.expense, it.income)
        }
    }

    private fun getChartData(): List<ChartDataItem> {
        val result = mutableListOf<ChartDataItem>()

        val currentHour = TimeZoneHelper.getCurrentHourWithOffset(requireContext())
        val currentDay = TimeZoneHelper.getCurrentDayWithOffset(requireContext())
        val currentMonth = TimeZoneHelper.getCurrentMonthWithOffset(requireContext())
        val currentYear = TimeZoneHelper.getCurrentYearWithOffset(requireContext())
        val currentDayOfWeek = TimeZoneHelper.getCurrentDayOfWeekWithOffset(requireContext())

        Log.d("StatisticsFragment", "=== ВРЕМЯ С УЧЕТОМ ЧАСОВОГО ПОЯСА ===")
        Log.d("StatisticsFragment", "Час: $currentHour, День: $currentDay, Месяц: ${currentMonth + 1}, Год: $currentYear")

        when (currentPeriod) {
            "day" -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(currentYear, currentMonth, currentDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = startOfDay + 24 * 60 * 60 * 1000

                val hourMap = mutableMapOf<Int, Pair<Float, Float>>()
                for (hour in 0..currentHour) {
                    hourMap[hour] = 0f to 0f
                }

                for (transaction in transactions) {
                    // ✅ Используем метод для корректировки времени транзакции
                    val adjustedDate = TimeZoneHelper.adjustTransactionTime(transaction.date, requireContext())

                    if (adjustedDate in startOfDay..endOfDay) {
                        val transCal = Calendar.getInstance().apply { timeInMillis = adjustedDate }
                        val hour = transCal.get(Calendar.HOUR_OF_DAY)

                        if (hour <= currentHour) {
                            val current = hourMap[hour] ?: (0f to 0f)
                            if (transaction.type == "expense") {
                                hourMap[hour] = (current.first + transaction.amount) to current.second
                            } else {
                                hourMap[hour] = current.first to (current.second + transaction.amount)
                            }
                            Log.d("StatisticsFragment", "Транзакция в $hour:00 (скорректировано) - ${transaction.amount} ${transaction.type}")
                        }
                    }
                }

                for (hour in 0..currentHour) {
                    val (expense, income) = hourMap[hour] ?: (0f to 0f)
                    result.add(ChartDataItem(hour.toString(), expense, income))
                }
            }

            "week" -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, currentDay)
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfWeek = startOfWeek + 7 * 24 * 60 * 60 * 1000
                val weekTransactions = transactions.filter { it.date in startOfWeek..endOfWeek }

                val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
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

                for (transaction in weekTransactions) {
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
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
                        if (transaction.type == "expense") {
                            dayMap[dayName] = (current.first + transaction.amount) to current.second
                        } else {
                            dayMap[dayName] = current.first to (current.second + transaction.amount)
                        }
                    }
                }

                for (day in 0..daysPassed) {
                    val (expense, income) = dayMap[dayNames[day]] ?: (0f to 0f)
                    result.add(ChartDataItem(dayNames[day], expense, income))
                }
            }

            "month" -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfMonth = startOfMonth + 31 * 24 * 60 * 60 * 1000L
                val monthTransactions = transactions.filter { it.date in startOfMonth..endOfMonth }

                val dayMap = mutableMapOf<Int, Pair<Float, Float>>()
                for (day in 1..currentDay) {
                    dayMap[day] = 0f to 0f
                }

                for (transaction in monthTransactions) {
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    if (day <= currentDay) {
                        val current = dayMap[day] ?: (0f to 0f)
                        if (transaction.type == "expense") {
                            dayMap[day] = (current.first + transaction.amount) to current.second
                        } else {
                            dayMap[day] = current.first to (current.second + transaction.amount)
                        }
                    }
                }

                for (day in 1..currentDay) {
                    val (expense, income) = dayMap[day] ?: (0f to 0f)
                    result.add(ChartDataItem(day.toString(), expense, income))
                }
            }

            "year" -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, 0)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfYear = startOfYear + 366 * 24 * 60 * 60 * 1000L
                val yearTransactions = transactions.filter { it.date in startOfYear..endOfYear }

                val monthNames = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн",
                    "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                val monthMap = mutableMapOf<Int, Pair<Float, Float>>()

                for (month in 0..currentMonth) {
                    monthMap[month] = 0f to 0f
                }

                for (transaction in yearTransactions) {
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    val month = cal.get(Calendar.MONTH)
                    if (month <= currentMonth) {
                        val current = monthMap[month] ?: (0f to 0f)
                        if (transaction.type == "expense") {
                            monthMap[month] = (current.first + transaction.amount) to current.second
                        } else {
                            monthMap[month] = current.first to (current.second + transaction.amount)
                        }
                    }
                }

                for (month in 0..currentMonth) {
                    val (expense, income) = monthMap[month] ?: (0f to 0f)
                    result.add(ChartDataItem(monthNames[month], expense, income))
                }
            }
        }

        return result
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded) {
            updateAllData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

data class ChartDataItem(
    val label: String,
    val expense: Float,
    val income: Float
)

data class InfoItem(
    val title: String,
    val value: String
)

class StatisticsItemsAdapter() : RecyclerView.Adapter<StatisticsItemsAdapter.ViewHolder>() {

    private var items: List<InfoItem> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_statistics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameText.text = item.title
        holder.amountText.text = item.value

        val isNightMode = (holder.itemView.context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isNightMode) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.black)
        }

        if (item.title == holder.itemView.context.getString(R.string.remaining) &&
            item.value.startsWith("-")) {
            holder.amountText.setTextColor(Color.parseColor("#FF6B6B"))
        } else {
            holder.amountText.setTextColor(textColor)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<InfoItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}