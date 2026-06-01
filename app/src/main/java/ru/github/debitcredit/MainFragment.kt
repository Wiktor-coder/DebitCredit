package ru.github.debitcredit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.adapter.CategoryAdapter
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.Category

class MainFragment : Fragment() {

    private lateinit var statsView: StatsView
    private lateinit var statsContainer: ViewGroup
    private lateinit var detailContainer: ViewGroup
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private val predefinedCategories = listOf("Продукты", "Развлечения", "Иное")
    private var categories = mutableListOf<Category>()
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
        setupSampleData()
        setupClickListeners(view)
        setupStatsViewClick()
        setupPredefinedCategories(view)

        // Слушаем результат из фрагмента редактирования
        parentFragmentManager.setFragmentResultListener("category_update", viewLifecycleOwner) { _, bundle ->
            val categoryName = bundle.getString("category_name") ?: return@setFragmentResultListener
            val newAmount = bundle.getFloat("new_amount")
            updateCategoryAmount(categoryName, newAmount)
        }

        // Обработка кнопки Back
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
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

    private fun initializeViews(view: View) {
        statsView = view.findViewById(R.id.statsView)
        statsContainer = view.findViewById(R.id.statsContainer)
        detailContainer = view.findViewById(R.id.detailContainer)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)

        categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(requireContext(), "Выбрана категория: ${category.name}", Toast.LENGTH_SHORT).show()
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
                val currentAmount = categories.find { it.name == categoryName }?.amount ?: 0.0
                val categoryColor = getColorForCategory(categoryName)

                // Используем Navigation Component для перехода
                val bundle = bundleOf(
                    "category_name" to categoryName,
                    "category_color" to categoryColor,
                    "category_amount" to currentAmount.toFloat()
                )
                view.findNavController().navigate(R.id.categoryEditFragment, bundle)
            }
        }
    }

    private fun createCategoryView(categoryName: String): View {
        val view = layoutInflater.inflate(R.layout.item_category_predefined, null)
        val nameText = view.findViewById<TextView>(R.id.categoryNameText)
        val amountText = view.findViewById<TextView>(R.id.categoryAmountText)
        val cardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.categoryCard)

        nameText.text = categoryName
        cardView.setCardBackgroundColor(getColorForCategory(categoryName))

        val category = categories.find { it.name == categoryName }
        amountText.text = String.format("%.2f ₽", category?.amount ?: 0.0)

        return view
    }

    private fun getColorForCategory(name: String): Int {
        return when (name) {
            "Продукты" -> Color.parseColor("#FF6B6B")
            "Развлечения" -> Color.parseColor("#4ECDC4")
            else -> Color.parseColor("#96CEB4")
        }
    }

    fun updateCategoryAmount(categoryName: String, newAmount: Float) {
        val index = categories.indexOfFirst { it.name == categoryName }
        if (index != -1) {
            val updatedCategory = Category(
                id = categories[index].id,
                name = categories[index].name,
                amount = newAmount,
                color = categories[index].color
            )
            categories[index] = updatedCategory
            categoryAdapter.updateCategories(categories)
            updateStatsView()

            updatePredefinedCategoryAmount(categoryName, newAmount)
        }
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

        val dayData = listOf(
            StatsView.CategoryData("Продукты", total * 0.15f, Color.parseColor("#FF6B6B")),
            StatsView.CategoryData("Развлечения", total * 0.25f, Color.parseColor("#4ECDC4")),
            StatsView.CategoryData("Транспорт", total * 0.60f, Color.parseColor("#45B7D1"))
        )

        val weekData = listOf(
            StatsView.CategoryData("Продукты", total * 0.35f, Color.parseColor("#FF6B6B")),
            StatsView.CategoryData("Развлечения", total * 0.20f, Color.parseColor("#4ECDC4")),
            StatsView.CategoryData("Транспорт", total * 0.15f, Color.parseColor("#45B7D1")),
            StatsView.CategoryData("Кафе", total * 0.30f, Color.parseColor("#96CEB4"))
        )

        val monthData = categories.map { category ->
            StatsView.CategoryData(category.name, category.amount, category.color)
        }

        val quarterData = listOf(
            StatsView.CategoryData("Продукты", total * 0.40f, Color.parseColor("#FF6B6B")),
            StatsView.CategoryData("Развлечения", total * 0.30f, Color.parseColor("#4ECDC4")),
            StatsView.CategoryData("Транспорт", total * 0.20f, Color.parseColor("#45B7D1")),
            StatsView.CategoryData("Кафе", total * 0.10f, Color.parseColor("#96CEB4"))
        )

        val yearData = listOf(
            StatsView.CategoryData("Продукты", total * 0.50f, Color.parseColor("#FF6B6B")),
            StatsView.CategoryData("Развлечения", total * 0.30f, Color.parseColor("#4ECDC4")),
            StatsView.CategoryData("Транспорт", total * 0.20f, Color.parseColor("#45B7D1"))
        )

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

    private fun setupSampleData() {
        categories.addAll(listOf(
            Category(1, "Продукты", 2500f, Color.parseColor("#FF6B6B")),
            Category(2, "Развлечения", 1200f, Color.parseColor("#4ECDC4")),
            Category(3, "Транспорт", 800f, Color.parseColor("#45B7D1")),
            Category(4, "Кафе", 1500f, Color.parseColor("#96CEB4"))
        ))
        categoryAdapter.updateCategories(categories)
        updateStatsView()
    }

    private fun updateStatsView() {
        val statsData = categories.map { category ->
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
                categories.removeAt(categories.size - 1)
                categoryAdapter.updateCategories(categories)
                updateStatsView()

                if (isDetailMode) {
                    setupDetailStatsData()
                }
                Toast.makeText(requireContext(), "Категория удалена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Нет категорий для удаления", Toast.LENGTH_SHORT).show()
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
            val newCategory = Category(
                newId,
                "Категория ${newId}",
                500f,
                colors[newId % colors.size]
            )
            categories.add(newCategory)
            categoryAdapter.updateCategories(categories)
            updateStatsView()

            if (isDetailMode) {
                setupDetailStatsData()
            }
            Toast.makeText(
                requireContext(),
                "Добавлена: ${newCategory.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        view.findViewById<ImageButton>(R.id.incomeButton).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Доход (скоро)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}