package ru.github.debitcredit.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
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
    private lateinit var statsContainer: ViewGroup
    private lateinit var detailContainer: ViewGroup
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private val predefinedCategories = listOf("Продукты", "Развлечения", "Иное")
    private var categories = mutableListOf<CategoryEntity>()
    private var isDetailMode = false

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
        observeCategories()  // наблюдаем за изменениями в БД
        setupClickListeners(view)
        setupStatsViewClick()
        setupPredefinedCategories(view)

        // Слушаем результат из фрагмента редактирования
        parentFragmentManager.setFragmentResultListener(
            "category_update",
            viewLifecycleOwner
        ) { _, bundle ->
            val categoryName = bundle.getString("category_name") ?: return@setFragmentResultListener
            val newAmount = bundle.getFloat("new_amount")

            // Обновляем через ViewModel
            val category = categories.find { it.name == categoryName }
            category?.let {
                val updatedCategory = it.copy(amount = newAmount)
                viewModel.updateCategory(updatedCategory)
            }
        }

        // Обработка кнопки Back
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isDetailMode) {
                        collapseToSingleMode()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    // Наблюдаем за категориями из базы данных
    private fun observeCategories() {
        lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->
                categories.clear()
                categories.addAll(categoryList)
                categoryAdapter.updateCategories(categories)
                updateStatsView()

                // Обновляем предустановленные категории в UI
                predefinedCategories.forEach { catName ->
                    val category = categories.find { it.name == catName }
                    category?.let {
                        updatePredefinedCategoryAmount(catName, it.amount)
                    }
                }
            }
        }
        // Наблюдаем за балансом
        lifecycleScope.launch {
            viewModel.balance.collect { balance ->
                val balanceText = if (balance >= 0) {
                    "💰 Баланс: +${String.format("%.2f", balance)} ₽"
                } else {
                    "📉 Баланс: ${String.format("%.2f", balance)} ₽"
                }
                view?.findViewById<TextView>(R.id.balanceTextView)?.text = balanceText
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainFragment", "=== onResume ===")

        // Просто обновляем UI без лишних операций
        updateStatsView()
    }

    private fun initializeViews(view: View) {
        statsView = view.findViewById(R.id.statsView)
        statsContainer = view.findViewById(R.id.statsContainer)
        detailContainer = view.findViewById(R.id.detailContainer)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)

        categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(
                requireContext(),
                "Выбрана категория: ${category.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        categoryRecyclerView.adapter = categoryAdapter

        detailContainer.visibility = View.GONE
        detailContainer.alpha = 0f
        detailContainer.scaleX = 0f
        detailContainer.scaleY = 0f
    }

    private fun setupPredefinedCategories(view: View) {
        val categoriesContainer = view.findViewById<LinearLayout>(R.id.predefinedCategoriesContainer)

        for (categoryName in predefinedCategories) {
            val categoryView = createCategoryView(categoryName)
            categoriesContainer.addView(categoryView)

            categoryView.setOnClickListener {
                val currentAmount = categories.find { it.name == categoryName }?.amount ?: 0f
                val categoryColor = getColorForCategory(categoryName)

                val bundle = Bundle().apply {
                    putString ("category_name", categoryName)
                    putInt ("category_color", categoryColor)
                    putFloat ("category_amount", currentAmount)
                }
                view.findNavController().navigate(R.id.categoryEditFragment, bundle)
            }
        }
    }

    private fun createCategoryView(categoryName: String): View {
        val view = layoutInflater.inflate(R.layout.item_category_predefined, null)
        val nameText = view.findViewById<TextView>(R.id.categoryNameText)
        val amountText = view.findViewById<TextView>(R.id.categoryAmountText)
        val cardView = view.findViewById<CardView>(R.id.categoryCard)

        nameText.text = categoryName
        cardView.setCardBackgroundColor(getColorForCategory(categoryName))

        val category = categories.find { it.name == categoryName }
        amountText.text = String.format("%.2f ₽", category?.amount ?: 0f)

        return view
    }

    private fun getColorForCategory(name: String): Int {
        return when (name) {
            "Продукты" -> Color.parseColor("#FF6B6B")
            "Развлечения" -> Color.parseColor("#4ECDC4")
            else -> Color.parseColor("#96CEB4")
        }
    }

    private fun updatePredefinedCategoryAmount(categoryName: String, newAmount: Float) {
        Log.d("MainFragment", "=== updatePredefinedCategoryAmount ===")
        Log.d("MainFragment", "Updating $categoryName to $newAmount")

        val view = requireView()
        val categoriesContainer = view.findViewById<LinearLayout>(R.id.predefinedCategoriesContainer)
        for (i in 0 until categoriesContainer.childCount) {
            val child = categoriesContainer.getChildAt(i)
            val nameText = child.findViewById<TextView>(R.id.categoryNameText)
            if (nameText.text == categoryName) {
                val amountText = child.findViewById<TextView>(R.id.categoryAmountText)
                amountText.text = String.format("%.2f ₽", newAmount)
                Log.d("MainFragment", "  Updated UI for $categoryName")
                break
            }
        }
    }

    private fun setupStatsViewClick() {
        statsView.setOnClickListener {
            if (!isDetailMode) {
                expandToDetailMode()
            }
        }
    }

    private fun expandToDetailMode() {
        if (isDetailMode) return
        isDetailMode = true

        statsView.stopAnimation()
        setupDetailStatsData()

        val startWidth = statsView.width
        val targetWidth = 0

        val widthAnimator = ValueAnimator.ofInt(startWidth, targetWidth).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val params = statsView.layoutParams
                params.width = animation.animatedValue as Int
                params.height = animation.animatedValue as Int
                statsView.layoutParams = params
            }
        }

        val fadeOut = ObjectAnimator.ofFloat(statsView, "alpha", 1f, 0f).apply {
            duration = 300
        }

        widthAnimator.start()
        fadeOut.start()

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                statsView.visibility = View.GONE

                val params = statsView.layoutParams
                params.width = 0
                params.height = 0
                statsView.layoutParams = params

                detailContainer.visibility = View.VISIBLE
                detailContainer.alpha = 0f
                detailContainer.scaleX = 0f
                detailContainer.scaleY = 0f

                detailContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        })
    }

    fun collapseToSingleMode() {
        if (!isDetailMode) return
        isDetailMode = false

        stopAllDetailAnimations()

        detailContainer.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction {
                detailContainer.visibility = View.GONE

                statsView.stopAnimation()

                val targetSize = dpToPx(300)
                val params = statsView.layoutParams
                params.width = targetSize
                params.height = targetSize
                statsView.layoutParams = params

                statsView.scaleX = 1f
                statsView.scaleY = 1f
                statsView.alpha = 1f
                statsView.visibility = View.VISIBLE

                updateStatsView()

                statsView.requestLayout()
                statsView.invalidate()
            }
            .start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun stopAllDetailAnimations() {
        detailContainer.findViewById<StatsView>(R.id.statsDay)?.stopAnimation()
        detailContainer.findViewById<StatsView>(R.id.statsWeek)?.stopAnimation()
        detailContainer.findViewById<StatsView>(R.id.statsMonth)?.stopAnimation()
        detailContainer.findViewById<StatsView>(R.id.statsQuarter)?.stopAnimation()
        detailContainer.findViewById<StatsView>(R.id.statsYear)?.stopAnimation()
        detailContainer.findViewById<StatsView>(R.id.statsAll)?.stopAnimation()
    }

    private fun setupDetailStatsData() {
        val total = categories.sumOf { it.amount.toDouble() }.toFloat()
        if (total == 0f) return

        val dayData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount * 0.2f, category.color)
        }

        val weekData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount * 0.35f, category.color)
        }

        val monthData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount, category.color)
        }

        val quarterData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount * 0.8f, category.color)
        }

        val yearData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount * 1.2f, category.color)
        }

        val allData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount, category.color)
        }

        detailContainer.findViewById<StatsView>(R.id.statsDay)?.let {
            it.isSmallMode = true
            it.data = dayData
            it.startAnimation()
        }
        detailContainer.findViewById<StatsView>(R.id.statsWeek)?.let {
            it.isSmallMode = true
            it.data = weekData
            it.startAnimation()
        }
        detailContainer.findViewById<StatsView>(R.id.statsMonth)?.let {
            it.isSmallMode = true
            it.data = monthData
            it.startAnimation()
        }
        detailContainer.findViewById<StatsView>(R.id.statsQuarter)?.let {
            it.isSmallMode = true
            it.data = quarterData
            it.startAnimation()
        }
        detailContainer.findViewById<StatsView>(R.id.statsYear)?.let {
            it.isSmallMode = true
            it.data = yearData
            it.startAnimation()
        }
        detailContainer.findViewById<StatsView>(R.id.statsAll)?.let {
            it.isSmallMode = true
            it.data = allData
            it.startAnimation()
        }

        detailContainer.requestLayout()
    }

    private fun updateStatsView() {
        Log.d("MainFragment", "=== updateStatsView ===")
        val statsData = categories.map { category ->
            Log.d("MainFragment", "  ${category.name}: ${category.amount}")
            StatsView.CategoryData(
                name = category.name,
                amount = category.amount,
                color = category.color
            )
        }
        statsView.isSmallMode = false
        statsView.data = statsData
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<ImageButton>(R.id.deleteCategoryButton).setOnClickListener {
            if (categories.isNotEmpty()) {
                val categoryToDelete = categories.last()
                viewModel.deleteCategory(categoryToDelete)  // ← удаляем через ViewModel
                if (isDetailMode) {
                    setupDetailStatsData()
                }
                Toast.makeText(requireContext(), "Категория удалена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Нет категорий для удаления", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        view.findViewById<ImageButton>(R.id.addCategoryButton).setOnClickListener {
            val newId = (categories.maxOfOrNull { it.id } ?: 0) + 1
            val colors = listOf(
                Color.parseColor("#FFEAA7"),
                Color.parseColor("#DFE6E9"),
                Color.parseColor("#74B9FF"),
                Color.parseColor("#A29BFE"),
                Color.parseColor("#FDCB6E")
            )
            val newCategoryEntity = CategoryEntity(
                0,  // id = 0 для автогенерации
                "Категория ${newId}",
                500f,
                colors[newId % colors.size]
            )
            viewModel.addCategory(newCategoryEntity)  // ← добавляем через ViewModel

            if (isDetailMode) {
                setupDetailStatsData()
            }
            Toast.makeText(
                requireContext(),
                "Добавлена: ${newCategoryEntity.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        view.findViewById<ImageButton>(R.id.incomeButton).setOnClickListener {
            // Открываем фрагмент редактирования в режиме дохода
            val bundle = Bundle().apply {
                putBoolean("is_income_mode", true)
                putString("category_name", "Доход")
                putInt("category_color", Color.parseColor("#4ECDC4"))
                putFloat("category_amount", 0f)
            }
            findNavController().navigate(R.id.categoryEditFragment, bundle)
        }
    }
}