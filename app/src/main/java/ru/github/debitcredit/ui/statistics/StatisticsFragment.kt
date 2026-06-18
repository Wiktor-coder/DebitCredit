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
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemsAdapter: StatisticsItemsAdapter
    private var currentPeriod = "month"
    private var selectedButtonId = R.id.monthButton
    private var isDataLoaded = false

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
        itemsRecyclerView = view.findViewById(R.id.itemsRecyclerView)

        itemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        itemsAdapter = StatisticsItemsAdapter()
        itemsRecyclerView.adapter = itemsAdapter

        setupPeriodButtons(view)
        setupBackButton(view)
        setupStatsViewClick()
        setupLineChart()

        observeData()
    }

    private fun setupLineChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = false

            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            val textColor = if (isNightMode) "#EEEEEE" else "#333333"
            val gridColor = if (isNightMode) "#555555" else "#CCCCCC"

            setBackgroundColor(Color.parseColor("#D0FFFFFF"))

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.parseColor(textColor)
            xAxis.textSize = 10f

            axisLeft.setDrawGridLines(true)
            axisLeft.setDrawZeroLine(true)
            axisLeft.axisMinimum = -100f
            axisLeft.axisMaximum = 100f
            axisLeft.textColor = Color.parseColor(textColor)
            axisLeft.textSize = 10f
            axisLeft.gridColor = Color.parseColor(gridColor)

            axisRight.isEnabled = false

            setNoDataText(getString(R.string.no_data))
            setNoDataTextColor(Color.parseColor(textColor))
        }
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
            viewModel.categories.collect { categories ->
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
            viewModel.expenses.collect {
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
        val expenses = viewModel.expenses.value
        val balance = viewModel.balance.value

        updateStatsView(income, expenses)
        updateLineChart()
        updateInfoItems(income, expenses, balance)
    }

    private fun updateStatsView(income: Float, expenses: Float) {
        val percentage = if (income > 0) {
            min((expenses / income) * 100, 100f)
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
            min((expenses / income) * 100, 100f)
        } else {
            0f
        }

        val formatWithSign = { value: Float ->
            if (value < 0) {
                String.format("%.2f ₽", value)
            } else {
                String.format("%.2f ₽", value)
            }
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
                formatWithSign(balance)
            )
        )

        itemsAdapter.updateData(infoItems)
    }

    private fun updateLineChart() {
        val expenseValues = when (currentPeriod) {
            "day" -> listOf(5f, 12f, 18f, 25f, 30f, 28f, 35f)
            "week" -> listOf(15f, 22f, 18f, 25f, 30f, 28f, 35f)
            "month" -> listOf(10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f, 60f, 65f, 70f, 75f, 80f, 82f, 85f, 88f, 90f, 92f, 94f, 95f, 96f, 97f, 98f, 99f, 99.5f, 100f, 100f, 100f)
            "year" -> listOf(10f, 12f, 15f, 18f, 22f, 28f, 35f, 45f, 55f, 65f, 75f, 85f)
            else -> listOf(10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f, 60f, 65f, 70f, 75f, 80f, 82f, 85f, 88f, 90f, 92f, 94f, 95f, 96f, 97f, 98f, 99f, 99.5f, 100f, 100f, 100f)
        }

        val labels = when (currentPeriod) {
            "day" -> listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00")
            "week" -> listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            "month" -> (1..31).map { "$it" }
            "year" -> listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
            else -> (1..31).map { "$it" }
        }

        val entries = expenseValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, getString(R.string.expenses)).apply {
            color = Color.parseColor("#FF6B6B")
            setCircleColor(Color.parseColor("#FF6B6B"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextColor = Color.parseColor("#333333")
            valueTextSize = 9f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#FF6B6B")
            fillAlpha = 50
        }

        lineChart.data = LineData(dataSet)

        val xAxis = lineChart.xAxis

        val labelCount = when (currentPeriod) {
            "month" -> min(labels.size / 3, 10)
            else -> min(labels.size, 8)
        }

        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.setLabelCount(labelCount, true)
        xAxis.granularity = 1f

        lineChart.setVisibleXRangeMaximum(if (currentPeriod == "month") 10f else 7f)
        lineChart.invalidate()
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

// Дата класс для информации
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

        // Получаем цвет текста в зависимости от темы
        val isNightMode = (holder.itemView.context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isNightMode) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.black)
        }

        // Если это строка с остатком и значение отрицательное - красим в красный
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