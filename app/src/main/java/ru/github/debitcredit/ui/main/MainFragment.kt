package ru.github.debitcredit.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.adapter.CategoryAdapter
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.viewmodel.MainViewModel

class MainFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var statsView: StatsView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var addCategoryButton: ImageButton
    private lateinit var incomeButton: ImageButton

    private var categories = mutableListOf<CategoryEntity>()
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
        setupClickListeners(view)
        setupStatsViewClick()

        observeCategories()
        observeBalance()

        parentFragmentManager.setFragmentResultListener(
            "category_update",
            viewLifecycleOwner
        ) { _, bundle ->
            val categoryKey = bundle.getString("category_key") ?: return@setFragmentResultListener
            val newAmount = bundle.getFloat("new_amount")

            val category = categories.find { it.name == categoryKey }
            category?.let {
                val updatedCategory = it.copy(amount = newAmount)
                viewModel.updateCategory(updatedCategory)
            }
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

    private fun observeCategories() {
        lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->
                // ✅ СОЗДАЕМ НОВЫЙ СПИСОК, а не используем тот же объект
                categories = categoryList.toMutableList()

                // ✅ Обновляем адаптер
                categoryAdapter.updateCategories(categories)
                updateStatsView()

                isDataLoaded = true

                Log.d("MainFragment", "Categories updated: ${categories.size}")
            }
        }
    }

    private fun observeBalance() {
        lifecycleScope.launch {
            viewModel.balance.collect { balance ->
                if (isAdded && view != null) {
                    val balanceText = if (balance >= 0) {
                        "${getString(R.string.balance)}: +${String.format("%.2f", balance)} ₽"
                    } else {
                        "${getString(R.string.balance)}: ${String.format("%.2f", balance)} ₽"
                    }
                    view?.findViewById<TextView>(R.id.balanceTextView)?.text = balanceText
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded) {
            updateStatsView()
        }
    }

    private fun initializeViews(view: View) {
        statsView = view.findViewById(R.id.statsView)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        incomeButton = view.findViewById(R.id.incomeButton)

        categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryRecyclerView.setHasFixedSize(false)

        categoryAdapter = CategoryAdapter(
            categories,
            onItemClick = { category ->
                val bundle = Bundle().apply {
                    putString("category_name", category.name)
                    putInt("category_id", category.id)
                    putInt("category_color", category.color)
                    putFloat("category_amount", category.amount)
                    putInt("category_icon", category.iconRes)
                }
                findNavController().navigate(R.id.categoryEditFragment, bundle)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            },
            onAddClick = { category ->
                val bundle = Bundle().apply {
                    putString("category_name", category.name)
                    putInt("category_id", category.id)
                    putInt("category_color", category.color)
                    putFloat("category_amount", category.amount)
                    putInt("category_icon", category.iconRes)
                }
                findNavController().navigate(R.id.categoryEditFragment, bundle)
            },
            requireContext()
        )
        categoryAdapter.setSelectMode(false)
        categoryRecyclerView.adapter = categoryAdapter
    }

    // В MainFragment.kt, в showDeleteConfirmationDialog:
    private fun showDeleteConfirmationDialog(category: CategoryEntity) {
        val displayName = when (category.name) {
            "products" -> getString(R.string.products)
            "utilities" -> getString(R.string.utilities)
            "transport" -> getString(R.string.transport)
            "health" -> getString(R.string.health)
            "clothing" -> getString(R.string.clothing)
            "entertainment" -> getString(R.string.entertainment)
            "other" -> getString(R.string.other)
            else -> category.name
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_category)
            .setMessage(String.format(getString(R.string.delete_category_confirmation), displayName))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategoryById(category.id)
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

    private fun updateStatsView() {
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
                    color = Color.parseColor("#78909C")
                )
            )
        }

        statsView.isSmallMode = false
        statsView.data = statsData
    }

    private fun setupClickListeners(view: View) {
        addCategoryButton.setOnClickListener {
            findNavController().navigate(R.id.selectCategoryFragment)
        }

        incomeButton.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("is_income_mode", true)
                putString("category_name", "income")
                putInt("category_color", Color.parseColor("#4ECDC4"))
                putFloat("category_amount", 0f)
                putInt("category_icon", R.drawable.ic_ruble)
                putInt("category_id", 0)
            }
            findNavController().navigate(R.id.categoryEditFragment, bundle)
        }
    }
}