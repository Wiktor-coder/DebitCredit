package ru.github.debitcredit.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.viewmodel.MainViewModel
import kotlin.math.min

class StatisticsFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var statsView: StatsView
    private lateinit var lineChart: LineChart
    private lateinit var percentageTextView: TextView
    private var currentPeriod = "месяц"

    // Данные для расходов (проценты)
    private val expenseData = mapOf(
        "день" to listOf(5f, 12f, 18f, 25f, 30f, 28f, 35f),
        "неделя" to listOf(15f, 22f, 18f, 25f, 30f, 28f, 35f),
        "месяц" to listOf(10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f, 60f, 65f, 70f, 75f, 80f, 82f, 85f, 88f, 90f, 92f, 94f, 95f, 96f, 97f, 98f, 99f, 99.5f, 100f, 100f, 100f),
        "квартал" to listOf(15f, 20f, 25f, 30f, 40f, 50f, 60f, 70f, 80f, 85f, 90f, 95f, 100f),
        "год" to listOf(10f, 12f, 15f, 18f, 22f, 28f, 35f, 45f, 55f, 65f, 75f, 85f, 100f)
    )

    // Данные для доходов (проценты от целевого значения)
    private val incomeData = mapOf(
        "день" to listOf(8f, 15f, 22f, 28f, 32f, 30f, 38f),
        "неделя" to listOf(18f, 25f, 22f, 28f, 32f, 30f, 38f),
        "месяц" to listOf(12f, 18f, 25f, 30f, 35f, 40f, 45f, 50f, 55f, 60f, 65f, 70f, 75f, 78f, 82f, 85f, 88f, 90f, 92f, 94f, 96f, 97f, 98f, 99f, 100f, 100f, 100f, 100f, 100f, 100f),
        "квартал" to listOf(18f, 25f, 30f, 35f, 45f, 55f, 65f, 75f, 85f, 90f, 95f, 98f, 100f),
        "год" to listOf(15f, 18f, 22f, 28f, 35f, 45f, 55f, 65f, 75f, 80f, 85f, 90f, 100f)
    )

    // Подписи для оси X
    private val xAxisLabels = mapOf(
        "день" to listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00"),
        "неделя" to listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
        "месяц" to (1..31).map { "$it" },
        "квартал" to listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"),
        "год" to listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statsView = view.findViewById(R.id.statsView)
        lineChart = view.findViewById(R.id.lineChart)
        percentageTextView = view.findViewById(R.id.percentageTextView)

        setupPeriodButtons(view)
        setupBackButton(view)
        setupStatsViewClick()
        setupLineChart()
        observeData()

        updateChart(currentPeriod)
        updateLineChart(currentPeriod)
    }

    private fun setupLineChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true

            // Используем цвета из темы
            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            val textColor = if (isNightMode) "#EEEEEE" else "#333333"
            val gridColor = if (isNightMode) "#555555" else "#CCCCCC"

            // Фон графика - полупрозрачный белый
            setBackgroundColor(Color.parseColor("#D0FFFFFF"))

            // Настройка оси X
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.parseColor(textColor)
            xAxis.textSize = 10f

            // Настройка левой оси Y
            axisLeft.setDrawGridLines(true)
            axisLeft.setDrawZeroLine(true)
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.textColor = Color.parseColor(textColor)
            axisLeft.textSize = 10f
            axisLeft.gridColor = Color.parseColor(gridColor)

            // Отключаем правую ось
            axisRight.isEnabled = false

            setNoDataText("Нет данных")
            setNoDataTextColor(Color.parseColor(textColor))
        }
    }

    private fun setupPeriodButtons(view: View) {
        val buttons = mapOf(
            R.id.dayButton to getString(R.string.day),
            R.id.weekButton to getString(R.string.week),
            R.id.monthButton to getString(R.string.month),
            R.id.quarterButton to getString(R.string.quarter),
            R.id.yearButton to getString(R.string.year)
        )

        buttons.forEach { (id, period) ->
            view.findViewById<Button>(id).setOnClickListener {
                currentPeriod = period
                updateChart(period)
                updateLineChart(period)
                updateButtonStates(view, id)
            }
        }

        // Устанавливаем начальное состояние кнопки "Месяц"
        updateButtonStates(view, R.id.monthButton)
    }

    private fun updateButtonStates(view: View, selectedId: Int) {
        val buttonIds = listOf(R.id.dayButton, R.id.weekButton, R.id.monthButton, R.id.quarterButton, R.id.yearButton)
        buttonIds.forEach { id ->
            val button = view.findViewById<Button>(id)
            if (id == selectedId) {
                button.setBackgroundColor(Color.parseColor("#DAA520"))
            } else {
                button.setBackgroundColor(Color.parseColor("#80DAA520"))
            }
        }
    }

    private fun setupBackButton(view: View) {
        view.findViewById<Button>(R.id.backButton).setOnClickListener {
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
            viewModel.balance.collect { updateChart(currentPeriod) }
        }
        lifecycleScope.launch {
            viewModel.expenses.collect { updateChart(currentPeriod) }
        }
        lifecycleScope.launch {
            viewModel.totalIncome.collect { updateChart(currentPeriod) }
        }
    }

    private fun updateChart(period: String) {
        lifecycleScope.launch {
            val income = viewModel.totalIncome.value
            val expenses = viewModel.expenses.value

            val percentage = if (income > 0) {
                min((expenses / income) * 100, 100f)
            } else {
                0f
            }

            val statsData = listOf(
                StatsView.CategoryData(
                    name = "Потрачено",
                    amount = percentage,
                    color = Color.parseColor("#FF6B6B")
                ),
                StatsView.CategoryData(
                    name = "Остаток",
                    amount = 100f - percentage,
                    color = Color.parseColor("#4ECDC4")
                )
            )

            statsView.isSmallMode = false
            statsView.data = statsData

            val spentAmount = income * percentage / 100
            val remainingAmount = income - spentAmount

            percentageTextView.text = String.format(
                "📊 ${getString(R.string.spent_percent)}: %.1f%%\n" +
                        "${getString(R.string.income)}: %.2f ₽\n" +
                        "${getString(R.string.expenses)}: %.2f ₽\n" +
                        "${getString(R.string.remaining)}: %.2f ₽",
                percentage, income, spentAmount, remainingAmount
            )
        }
    }

    private fun updateLineChart(period: String) {
        val expenseEntries = mutableListOf<Entry>()
        val incomeEntries = mutableListOf<Entry>()

        val expenseValues = expenseData[period] ?: return
        val incomeValues = incomeData[period] ?: return

        expenseValues.forEachIndexed { index, value ->
            expenseEntries.add(Entry(index.toFloat(), value))
        }

        incomeValues.forEachIndexed { index, value ->
            incomeEntries.add(Entry(index.toFloat(), value))
        }

        // Набор данных для расходов (красная линия)
        val expenseDataSet = LineDataSet(expenseEntries, getString(R.string.expenses))
        //        val expenseDataSet = LineDataSet(expenseEntries, "Расходы").apply {
//            color = Color.parseColor("#FF6B6B")
//            setCircleColor(Color.parseColor("#FF6B6B"))
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextColor = Color.parseColor("#333333")
//            valueTextSize = 9f
//            setDrawValues(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            setDrawFilled(true)
//            fillColor = Color.parseColor("#FF6B6B")
//            fillAlpha = 50
//        }

        // Набор данных для доходов (зеленая линия)
        val incomeDataSet = LineDataSet(incomeEntries, getString(R.string.incomes))
//        val incomeDataSet = LineDataSet(incomeEntries, "Доходы").apply {
//            color = Color.parseColor("#4ECDC4")
//            setCircleColor(Color.parseColor("#4ECDC4"))
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextColor = Color.parseColor("#333333")
//            valueTextSize = 9f
//            setDrawValues(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            setDrawFilled(true)
//            fillColor = Color.parseColor("#4ECDC4")
//            fillAlpha = 50
//        }

        lineChart.data = LineData(expenseDataSet, incomeDataSet)

        // Настройка оси X с подписями
        val labels = xAxisLabels[period] ?: return
        val xAxis = lineChart.xAxis

        // Для месяца показываем каждые 3 дня, чтобы не было плотно
        val labelCount = when (period) {
            "месяц" -> min(labels.size / 3, 10)
            else -> min(labels.size, 8)
        }

        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.setLabelCount(labelCount, true)
        xAxis.granularity = 1f

        // Активируем возможность перемещения
        lineChart.setVisibleXRangeMaximum(if (period == "месяц") 10f else 7f)

        lineChart.invalidate()
    }
}