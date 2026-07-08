package ru.github.debitcredit.presentation.ui.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.presentation.adapter.TransactionAdapter
import ru.github.debitcredit.presentation.viewmodel.MainViewModel
import ru.github.debitcredit.utils.CategoryMapper
import java.util.Calendar

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var dateFilterSpinner: Spinner
    private lateinit var emptyStateLayout: View
    private lateinit var backButton: ImageButton
    private lateinit var transactionCountText: TextView

    private var allTransactions = listOf<TransactionEntity>()
    private var filteredTransactions = listOf<TransactionEntity>()
    private var selectedCategoryKey = "all"
    private var selectedDateFilterKey = "all"  // Изменено: храним ключ фильтра даты
    private var searchQuery = ""

    private val categoryKeys = listOf("all") + CategoryMapper.getAllCategoryKeys()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeTransactions()
        setupBackButton()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.transactionsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        categoryFilterSpinner = view.findViewById(R.id.categoryFilterSpinner)
        dateFilterSpinner = view.findViewById(R.id.dateFilterSpinner)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        backButton = view.findViewById(R.id.backButton)
        transactionCountText = view.findViewById(R.id.transactionCountText)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            context = requireContext(),
            onItemClick = { transaction ->
                val displayName = CategoryMapper.getLocalizedName(requireContext(), transaction.categoryName)
                val message = getString(R.string.transaction_details, displayName, transaction.amount)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    private fun setupFilters() {
        // Фильтр по категориям
        val categoryDisplayNames = listOf(getString(R.string.all_categories)) +
                CategoryMapper.getAllCategoryKeys().map { key ->
                    if (key == "income") {
                        getString(R.string.income_category)
                    } else {
                        CategoryMapper.getLocalizedName(requireContext(), key)
                    }
                }

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryDisplayNames
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = categoryAdapter

        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryKey = if (position == 0) "all" else categoryKeys[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Фильтр по дате
        val dateDisplayNames = listOf(
            getString(R.string.all_dates),
            getString(R.string.today),
            getString(R.string.this_week),
            getString(R.string.this_month),
            getString(R.string.this_year)
        )

        val dateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dateDisplayNames
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateFilterSpinner.adapter = dateAdapter

        dateFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Сохраняем ключ фильтра, а не отображаемое имя
                selectedDateFilterKey = when (position) {
                    0 -> "all"
                    1 -> "today"
                    2 -> "week"
                    3 -> "month"
                    4 -> "year"
                    else -> "all"
                }
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            allTransactions = transactions ?: emptyList()
            applyFilters()
        }
    }

    private fun applyFilters() {
        filteredTransactions = allTransactions.filter { transaction ->
            // Поиск
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                val searchNumber = searchQuery.toFloatOrNull()
                if (searchNumber != null) {
                    transaction.amount == searchNumber
                } else {
                    val categoryDisplayName = CategoryMapper.getLocalizedName(requireContext(), transaction.categoryName)
                    categoryDisplayName.contains(searchQuery, ignoreCase = true) ||
                            transaction.categoryName.contains(searchQuery, ignoreCase = true)
                }
            }

            // Фильтр по категории
            val matchesCategory = if (selectedCategoryKey == "all") {
                true
            } else {
                transaction.categoryName == selectedCategoryKey
            }

            // Фильтр по дате - используем ключ
            val matchesDate = when (selectedDateFilterKey) {
                "today" -> isToday(transaction.date)
                "week" -> isThisWeek(transaction.date)
                "month" -> isThisMonth(transaction.date)
                "year" -> isThisYear(transaction.date)
                else -> true  // "all" или любое другое значение
            }

            matchesSearch && matchesCategory && matchesDate
        }

        adapter.submitList(filteredTransactions)
        updateEmptyState()
        updateTransactionCount()
    }

    private fun isToday(date: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = date
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val year2 = calendar.get(Calendar.YEAR)

        return today == day && year == year2
    }

    private fun isThisWeek(date: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = date
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year2 = calendar.get(Calendar.YEAR)

        return currentWeek == week && currentYear == year2
    }

    private fun isThisMonth(date: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = date
        val month = calendar.get(Calendar.MONTH)
        val year2 = calendar.get(Calendar.YEAR)

        return currentMonth == month && currentYear == year2
    }

    private fun isThisYear(date: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = date
        val year2 = calendar.get(Calendar.YEAR)

        return currentYear == year2
    }

    private fun updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateTransactionCount() {
        val count = filteredTransactions.size
        val countText = resources.getQuantityString(
            R.plurals.transactions_count,
            count,
            count
        )
        transactionCountText.text = countText
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}