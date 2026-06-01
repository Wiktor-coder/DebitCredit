package ru.github.debitcredit

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.adapter.CategoryAdapter
import ru.github.debitcredit.customview.StatsView
import ru.github.debitcredit.data.Category

class MainActivity : AppCompatActivity() {

    private lateinit var statsView: StatsView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private var categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupSampleData()
        setupClickListeners()
    }

    private fun initializeViews() {
        statsView = findViewById(R.id.statsView)
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)

        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(this, "Выбрана категория: ${category.name}", Toast.LENGTH_SHORT).show()
        }
        categoryRecyclerView.adapter = categoryAdapter
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
        statsView.data = statsData
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.deleteCategoryButton).setOnClickListener {
            if (categories.isNotEmpty()) {
                categories.removeAt(categories.size - 1)
                categoryAdapter.updateCategories(categories)
                updateStatsView()
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
            Toast.makeText(this, "Добавлена: ${newCategory.name}", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.incomeButton).setOnClickListener {
            Toast.makeText(this, "Доход (скоро)", Toast.LENGTH_SHORT).show()
        }

        setupTimeButtons()
    }

    private fun setupTimeButtons() {
        val buttons = mapOf(
            R.id.dayButton to "День",
            R.id.weekButton to "Неделя",
            R.id.monthButton to "Месяц",
            R.id.quarterButton to "Квартал",
            R.id.yearButton to "Год",
            R.id.allButton to "Общее"
        )

        buttons.forEach { (id, period) ->
            findViewById<Button>(id).setOnClickListener {
                Toast.makeText(this, "Период: $period", Toast.LENGTH_SHORT).show()
                // Здесь будет загрузка данных за период
            }
        }
    }
}