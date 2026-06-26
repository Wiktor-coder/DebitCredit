package ru.github.debitcredit.presentation.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.adapter.StatisticsItemsAdapter
import ru.github.debitcredit.customview.CustomBarChart
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.presentation.state.UiState
import ru.github.debitcredit.presentation.viewmodel.MainViewModel
import ru.github.debitcredit.utils.TransactionFilter

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var chart: CustomBarChart
    private lateinit var statsView: StatsView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemsAdapter: StatisticsItemsAdapter
    private val transactionFilter = TransactionFilter()
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
        observeTransactions() // Наблюдаем за транзакциями
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
                updateChart() // Обновляем только график при смене периода
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
                button.setBackgroundColor("#DAA520".toColorInt())
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
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    isDataLoaded = true
                    val data = state.data
                    updateStatsView(data.totalIncome, data.totalExpenses)
                    updateInfoItems(data.totalIncome, data.totalExpenses, data.balance)
                }
                is UiState.Loading -> {
                    // Показать прогресс
                }
                is UiState.Error -> {
                    // Показать ошибку
                }
            }
        }
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                updateChart()
            } else {
                chart.visibility = View.GONE
            }
        }
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
                color = "#FF6B6B".toColorInt()
            ),
            StatsView.CategoryData(
                name = getString(R.string.remaining),
                amount = 100f - percentage,
                color = "#4ECDC4".toColorInt()
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
                 getString(R.string.percent_format, percentage) //String.format("%.1f%%", percentage)
            ),
            InfoItem(
                getString(R.string.incomes),
                 getString(R.string.amount_format, income) //String.format("%.2f ₽", income)
            ),
            InfoItem(
                getString(R.string.expenses),
                 getString(R.string.amount_format, expenses) //String.format("%.2f ₽", expenses)
            ),
            InfoItem(
                getString(R.string.remaining),
                getString(R.string.amount_format, balance) //String.format("%.2f ₽", balance)
            )
        )

        itemsAdapter.updateData(infoItems)
    }

    private fun updateChart() {
        val transactions = viewModel.transactions.value

        if (transactions.isNullOrEmpty()) {
            chart.visibility = View.GONE
            return
        }

        // Фильтруем транзакции по выбранному периоду
        val periodData = transactionFilter.filterByPeriod(
            transactions,
            currentPeriod,
            requireContext()
        )

        if (periodData.isEmpty()) {
            chart.visibility = View.GONE
            return
        }

        chart.visibility = View.VISIBLE

        val chartData = periodData.map {
            CustomBarChart.BarData(
                label = it.label,
                expense = it.expense,
                income = it.income
            )
        }

        chart.data = chartData
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded) {
            updateChart()
        }
    }
}

data class InfoItem(
    val title: String,
    val value: String
)