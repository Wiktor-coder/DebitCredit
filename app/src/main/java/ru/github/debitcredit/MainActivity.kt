package ru.github.debitcredit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.adapter.CategoryAdapter
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.Category

class MainActivity : AppCompatActivity() {

    private lateinit var statsView: StatsView
    private lateinit var statsContainer: ViewGroup
    private lateinit var detailContainer: ViewGroup
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private var categories = mutableListOf<Category>()

    private var isDetailMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupSampleData()
        setupClickListeners()
        setupStatsViewClick()
        setupBackButtonHandler()
    }

    private fun initializeViews() {
        statsView = findViewById(R.id.statsView)
        statsContainer = findViewById(R.id.statsContainer)
        detailContainer = findViewById(R.id.detailContainer)
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)

        categoryRecyclerView.layoutManager = GridLayoutManager(this, 2)
        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(this, "Выбрана категория: ${category.name}", Toast.LENGTH_SHORT).show()
        }
        categoryRecyclerView.adapter = categoryAdapter

        detailContainer.visibility = View.GONE
        detailContainer.alpha = 0f
        detailContainer.scaleX = 0f
        detailContainer.scaleY = 0f
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

        // Получаем текущие размеры в пикселях
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

                // Убеждаемся, что размеры установлены в 0
                val params = statsView.layoutParams
                params.width = 0
                params.height = 0
                statsView.layoutParams = params

                // Показываем детальный контейнер
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

        // Анимируем исчезновение детального контейнера
        detailContainer.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction {
                detailContainer.visibility = View.GONE

                // Полностью останавливаем анимацию StatsView
                statsView.stopAnimation()

                // Устанавливаем правильные размеры для StatsView
                val targetSize = dpToPx(300)
                val params = statsView.layoutParams
                params.width = targetSize
                params.height = targetSize
                statsView.layoutParams = params

                // Сбрасываем все трансформации
                statsView.scaleX = 1f
                statsView.scaleY = 1f
                statsView.alpha = 1f  // Сразу ставим 1, без анимации
                statsView.visibility = View.VISIBLE

                // Обновляем данные (это вызовет startAnimation, который начнет с 0)
                updateStatsView()

                // Принудительно перерисовываем
                statsView.requestLayout()
                statsView.invalidate()
            }
            .start()
    }

    // Добавьте вспомогательный метод для конвертации dp в px
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
        val total = categories.sumOf { it.amount }.toFloat()
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
            StatsView.CategoryData(category.name, category.amount.toFloat(), category.color)
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
            StatsView.CategoryData(category.name, category.amount.toFloat(), category.color)
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

        // Принудительно обновляем layout для детального контейнера
        detailContainer.requestLayout()
    }

    private fun setupSampleData() {
        categories.addAll(listOf(
            Category(1, "Продукты", 2500.0, Color.parseColor("#FF6B6B")),
            Category(2, "Развлечения", 1200.0, Color.parseColor("#4ECDC4")),
            Category(3, "Транспорт", 800.0, Color.parseColor("#45B7D1")),
            Category(4, "Кафе", 1500.0, Color.parseColor("#96CEB4"))
        ))
        categoryAdapter.updateCategories(categories)
        updateStatsView()
    }

    private fun updateStatsView() {
        val statsData = categories.map { category ->
            StatsView.CategoryData(
                name = category.name,
                amount = category.amount.toFloat(),
                color = category.color
            )
        }
        statsView.isSmallMode = false
        statsView.data = statsData
        // data сеттер сам вызывает startAnimation()
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.deleteCategoryButton).setOnClickListener {
            if (categories.isNotEmpty()) {
                categories.removeAt(categories.size - 1)
                categoryAdapter.updateCategories(categories)
                updateStatsView()

                if (isDetailMode) {
                    setupDetailStatsData()
                }
                Toast.makeText(this, "Категория удалена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Нет категорий для удаления", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.addCategoryButton).setOnClickListener {
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
                500.0,
                colors[newId % colors.size]
            )
            categories.add(newCategory)
            categoryAdapter.updateCategories(categories)
            updateStatsView()

            if (isDetailMode) {
                setupDetailStatsData()
            }
            Toast.makeText(this, "Добавлена: ${newCategory.name}", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.incomeButton).setOnClickListener {
            Toast.makeText(this, "Доход (скоро)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDetailMode) {
                    collapseToSingleMode()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        statsView.stopAnimation()
        stopAllDetailAnimations()
    }
}