package ru.github.debitcredit.presentation.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.databinding.FragmentMainBinding
import ru.github.debitcredit.domain.model.Category
import ru.github.debitcredit.presentation.adapter.CategoryAdapter
import ru.github.debitcredit.presentation.state.UiState
import ru.github.debitcredit.presentation.viewmodel.MainViewModel
import ru.github.debitcredit.utils.CategoryMapper

@AndroidEntryPoint
class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var statsView: StatsView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var transactionsButton: ImageButton
    private lateinit var addCategoryButton: ImageButton
    private lateinit var incomeButton: ImageButton
    private lateinit var balanceTextView: TextView
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupStatsViewClick()
        observeData()

        parentFragmentManager.setFragmentResultListener(
            "category_update",
            viewLifecycleOwner
        ) { _, bundle ->
            val categoryKey = bundle.getString("category_key") ?: return@setFragmentResultListener
            val newAmount = bundle.getFloat("new_amount")
            viewModel.updateCategory(categoryKey, newAmount)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    private fun initializeViews(view: View) {
        statsView = view.findViewById(R.id.statsView)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)

        // Инициализируем все кнопки
        transactionsButton = view.findViewById(R.id.transactionsButton)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        incomeButton = view.findViewById(R.id.incomeButton)
        balanceTextView = view.findViewById(R.id.balanceTextView)

        categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryRecyclerView.setHasFixedSize(false)

        categoryAdapter = CategoryAdapter(
            context = requireContext(),
            onItemClick = { categoryEntity: CategoryEntity ->
                val category = Category(
                    id = categoryEntity.id,
                    name = categoryEntity.name,
                    amount = categoryEntity.amount,
                    color = categoryEntity.color,
                    iconRes = categoryEntity.iconRes,
                    date = categoryEntity.date
                )
                navigateToEditCategory(category)
            },
            onDeleteClick = { categoryEntity: CategoryEntity ->
                val category = Category(
                    id = categoryEntity.id,
                    name = categoryEntity.name,
                    amount = categoryEntity.amount,
                    color = categoryEntity.color,
                    iconRes = categoryEntity.iconRes,
                    date = categoryEntity.date
                )
                showDeleteConfirmationDialog(category)
            },
            onAddClick = { categoryEntity: CategoryEntity ->
                val category = Category(
                    id = categoryEntity.id,
                    name = categoryEntity.name,
                    amount = categoryEntity.amount,
                    color = categoryEntity.color,
                    iconRes = categoryEntity.iconRes,
                    date = categoryEntity.date
                )
                navigateToEditCategory(category)
            }
        )
        categoryAdapter.setSelectMode(false)
        categoryRecyclerView.adapter = categoryAdapter
    }

    private fun navigateToEditCategory(category: Category) {
        val bundle = Bundle().apply {
            putString("category_name", category.name)
            putInt("category_id", category.id)
            putInt("category_color", category.color)
            putFloat("category_amount", category.amount)
            putInt("category_icon", category.iconRes)
        }
        findNavController().navigate(R.id.categoryEditFragment, bundle)
    }

    private fun observeData() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Показать прогресс
                }
                is UiState.Success -> {
                    val categories = state.data.categories.map { category ->
                        CategoryEntity(
                            id = category.id,
                            name = category.name,
                            amount = category.amount,
                            color = category.color,
                            iconRes = category.iconRes,
                            date = category.date
                        )
                    }
                    categoryAdapter.submitList(categories)
                    updateStatsView(state.data.categories)
                    updateBalance(state.data.balance)
                }
                is UiState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBalance(balance: Float) {
        val balanceText = if (balance >= 0) {
            "${getString(R.string.balance)}: +${getString(R.string.amount_format, balance)}"
        } else {
            "${getString(R.string.balance)}: ${getString(R.string.amount_format, balance)}"
        }
        balanceTextView.text = balanceText
    }

    private fun updateStatsView(categories: List<Category>) {
        val positiveCategories = categories.filter { it.amount > 0 }

        val statsData = if (positiveCategories.isNotEmpty()) {
            positiveCategories.map { category ->
                StatsView.CategoryData(
                    name = category.name,
                    amount = category.amount,
                    color = category.color
                )
            }
        } else {
            listOf(
                StatsView.CategoryData(
                    name = getString(R.string.no_data),
                    amount = 1f,
                    color = "#78909C".toColorInt()
                )
            )
        }

        statsView.isSmallMode = false
        statsView.showPercentage = false
        statsView.data = statsData
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        val displayName = CategoryMapper.getLocalizedName(requireContext(), category.name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_category)
            .setMessage(
                String.format(
                    getString(R.string.delete_category_confirmation),
                    displayName
                )
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategory(category.id, category.name)
                Toast.makeText(
                    requireContext(),
                    "$displayName ${getString(R.string.category_deleted)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupStatsViewClick() {
        statsView.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }
    }

    private fun setupClickListeners() {
        // Кнопка "Все транзакции"
        transactionsButton.setOnClickListener {
            findNavController().navigate(R.id.transactionsFragment)
        }

        // Кнопка "Доход"
        incomeButton.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("is_income_mode", true)
                putString("category_name", "income")
                putInt("category_color", "#4ECDC4".toColorInt())
                putFloat("category_amount", 0f)
                putInt("category_icon", R.drawable.ic_ruble)
                putInt("category_id", 0)
            }
            findNavController().navigate(R.id.categoryEditFragment, bundle)
        }

        // Кнопка "Добавить категорию"
        addCategoryButton.setOnClickListener {
            findNavController().navigate(R.id.selectCategoryFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}