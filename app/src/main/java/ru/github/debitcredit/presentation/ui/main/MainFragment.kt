package ru.github.debitcredit.presentation.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.model.CategoryEntity
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
    private lateinit var addCategoryButton: ImageButton
    private lateinit var incomeButton: ImageButton
    private lateinit var balanceTextView: TextView

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//    }
//
//    @Suppress("DEPRECATION")
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.main_menu, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    @Suppress("DEPRECATION")
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_settings -> {
//                findNavController().navigate(R.id.settingsFragment)
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
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
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        incomeButton = view.findViewById(R.id.incomeButton)
        balanceTextView = view.findViewById(R.id.balanceTextView)

        categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryRecyclerView.setHasFixedSize(false)

        categoryAdapter = CategoryAdapter(
            context = requireContext(),
            onItemClick = { category ->
                navigateToEditCategory(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            },
            onAddClick = { category ->
                navigateToEditCategory(category)
            }
        )
        categoryAdapter.setSelectMode(false)
        categoryRecyclerView.adapter = categoryAdapter
    }

    private fun navigateToEditCategory(category: CategoryEntity) {
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
                    categoryAdapter.submitList(state.data.categories)
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
            "${getString(R.string.balance)}: +${getString(R.string.amount_format, balance)} ₽"
        } else {
            "${getString(R.string.balance)}: ${getString(R.string.amount_format, balance)} ₽"
        }
        balanceTextView.text = balanceText
    }

    private fun updateStatsView(categories: List<CategoryEntity>) {
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
        statsView.data = statsData
    }

    private fun showDeleteConfirmationDialog(category: CategoryEntity) {
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
        addCategoryButton.setOnClickListener {
            findNavController().navigate(R.id.selectCategoryFragment)
        }

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
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}