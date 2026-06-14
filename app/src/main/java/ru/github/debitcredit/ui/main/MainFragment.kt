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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
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

    private val predefinedCategories = listOf(
        Triple(R.string.products, "#FF5252", android.R.drawable.ic_menu_agenda),
        Triple(R.string.utilities, "#FF4081", android.R.drawable.ic_menu_manage),
        Triple(R.string.transport, "#FFB74D", android.R.drawable.ic_menu_directions),
        Triple(R.string.health, "#4CAF50", android.R.drawable.ic_menu_edit),
        Triple(R.string.clothing, "#9C27B0", android.R.drawable.ic_menu_edit),
        Triple(R.string.entertainment, "#2196F3", android.R.drawable.ic_menu_gallery),
        Triple(R.string.other, "#78909C", android.R.drawable.ic_menu_edit)
    )

    private var categories = mutableListOf<CategoryEntity>()

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
        observeCategories()
        setupClickListeners(view)
        setupStatsViewClick()
        setupPredefinedCategories(view)

        parentFragmentManager.setFragmentResultListener(
            "category_update",
            viewLifecycleOwner
        ) { _, bundle ->
            val categoryName = bundle.getString("category_name") ?: return@setFragmentResultListener
            val newAmount = bundle.getFloat("new_amount")

            val category = categories.find { it.name == categoryName }
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
                categories.clear()
                categories.addAll(categoryList)
                categoryAdapter.updateCategories(categories)
                updateStatsView()

                predefinedCategories.forEach { (nameRes, _, _) ->
                    val catName = getString(nameRes)
                    val category = categories.find { it.name == catName }
                    category?.let {
                        updatePredefinedCategoryAmount(catName, it.amount)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.balance.collect { balance ->
                val balanceText = if (balance >= 0) {
                    "💰 ${getString(R.string.balance)}: +${String.format("%.2f", balance)} ₽"
                } else {
                    "📉 ${getString(R.string.balance)}: ${String.format("%.2f", balance)} ₽"
                }
                view?.findViewById<TextView>(R.id.balanceTextView)?.text = balanceText
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatsView()
    }

    private fun initializeViews(view: View) {
        statsView = view.findViewById(R.id.statsView)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        incomeButton = view.findViewById(R.id.incomeButton)

        categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        categoryAdapter = CategoryAdapter(
            categories,
            onItemClick = { category ->
                val bundle = Bundle().apply {
                    putString("category_name", category.name)
                    putInt("category_id", category.id)
                    putInt("category_color", category.color)
                    putFloat("category_amount", category.amount)
                }
                findNavController().navigate(R.id.categoryEditFragment, bundle)
            }
        )
        categoryRecyclerView.adapter = categoryAdapter
    }

    private fun setupPredefinedCategories(view: View) {
        val categoriesContainer = view.findViewById<LinearLayout>(R.id.predefinedCategoriesContainer)

        predefinedCategories.forEach { (nameRes, colorRes, iconRes) ->
            val categoryName = getString(nameRes)
            val categoryColor = Color.parseColor(colorRes)
            val categoryView = createCategoryView(categoryName, categoryColor, iconRes)
            categoriesContainer.addView(categoryView)

            categoryView.setOnClickListener {
                val currentAmount = categories.find { it.name == categoryName }?.amount ?: 0f

                val bundle = Bundle().apply {
                    putString("category_name", categoryName)
                    putInt("category_color", categoryColor)
                    putFloat("category_amount", currentAmount)
                }
                view.findNavController().navigate(R.id.categoryEditFragment, bundle)
            }
        }
    }

    private fun createCategoryView(categoryName: String, color: Int, iconRes: Int): View {
        val view = layoutInflater.inflate(R.layout.item_category_predefined, null)
        val nameText = view.findViewById<TextView>(R.id.categoryNameText)
        val amountText = view.findViewById<TextView>(R.id.categoryAmountText)
        val cardView = view.findViewById<CardView>(R.id.categoryCard)

        nameText.text = categoryName
        cardView.setCardBackgroundColor(color)

        val category = categories.find { it.name == categoryName }
        amountText.text = String.format("%.2f ₽", category?.amount ?: 0f)

        return view
    }

    private fun updatePredefinedCategoryAmount(categoryName: String, newAmount: Float) {
        val view = requireView()
        val categoriesContainer = view.findViewById<LinearLayout>(R.id.predefinedCategoriesContainer)
        for (i in 0 until categoriesContainer.childCount) {
            val child = categoriesContainer.getChildAt(i)
            val nameText = child.findViewById<TextView>(R.id.categoryNameText)
            if (nameText.text == categoryName) {
                val amountText = child.findViewById<TextView>(R.id.categoryAmountText)
                amountText.text = String.format("%.2f ₽", newAmount)
                break
            }
        }
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
            val newId = (categories.maxOfOrNull { it.id } ?: 0) + 1
            val colors = listOf(
                Color.parseColor("#FF5252"),
                Color.parseColor("#FF4081"),
                Color.parseColor("#FFB74D"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#78909C")
            )
            val newCategoryEntity = CategoryEntity(
                0,
                "${getString(R.string.category)} ${newId}",
                0f,
                colors[newId % colors.size]
            )
            viewModel.addCategory(newCategoryEntity)
            Toast.makeText(
                requireContext(),
                "${getString(R.string.category_added)}: ${newCategoryEntity.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        incomeButton.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("is_income_mode", true)
                putString("category_name", getString(R.string.income))
                putInt("category_color", Color.parseColor("#4ECDC4"))
                putFloat("category_amount", 0f)
            }
            findNavController().navigate(R.id.categoryEditFragment, bundle)
        }
    }
}