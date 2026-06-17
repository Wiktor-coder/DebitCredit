package ru.github.debitcredit.ui.select

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.adapter.SelectCategoryAdapter
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.viewmodel.MainViewModel

// Класс для хранения информации о предопределенной категории
data class PredefinedCategory(
    val name: String,
    val colorHex: String
)

class SelectCategoryFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SelectCategoryAdapter
    private lateinit var backButton: ImageButton

    private var existingCategories = mutableListOf<CategoryEntity>()

    // Список предопределенных категорий
    private val predefinedCategories = listOf(
        PredefinedCategory("Продукты", "#FF5252"),
        PredefinedCategory("ЖКХ", "#FF4081"),
        PredefinedCategory("Транспорт", "#FFB74D"),
        PredefinedCategory("Здоровье", "#4CAF50"),
        PredefinedCategory("Одежда", "#9C27B0"),
        PredefinedCategory("Развлечения", "#2196F3"),
        PredefinedCategory("Иное", "#78909C")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupBackButton()
        observeExistingCategories()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.categoryRecyclerView)
        backButton = view.findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Показываем предопределенные категории
        val categoryList = predefinedCategories.map { category ->
            CategoryEntity(
                id = 0,
                name = category.name,
                amount = 0f,
                color = Color.parseColor(category.colorHex)
            )
        }
        adapter = SelectCategoryAdapter(categoryList) { category ->
            checkAndAddCategory(category)
        }
        recyclerView.adapter = adapter
    }

    private fun observeExistingCategories() {
        lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                existingCategories = categories.toMutableList()
                updateAvailableCategories()
            }
        }
    }

    private fun updateAvailableCategories() {
        val existingNames = existingCategories.map { it.name }.toSet()
        val availableCategories = predefinedCategories
            .filter { it.name !in existingNames }
            .map { category ->
                CategoryEntity(
                    id = 0,
                    name = category.name,
                    amount = 0f,
                    color = Color.parseColor(category.colorHex)
                )
            }
        adapter.updateCategories(availableCategories)

        if (availableCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Все категории уже добавлены", Toast.LENGTH_SHORT).show()
            // Можно добавить кнопку возврата
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun checkAndAddCategory(category: CategoryEntity) {
        // Проверяем, не добавлена ли уже такая категория
        val alreadyExists = existingCategories.any { it.name == category.name }

        if (alreadyExists) {
            Toast.makeText(requireContext(), "Категория \"${category.name}\" уже добавлена", Toast.LENGTH_SHORT).show()
        } else {
            showAddCategoryDialog(category)
        }
    }

    private fun showAddCategoryDialog(category: CategoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Добавление категории")
            .setMessage("Добавить категорию \"${category.name}\" на главный экран?")
            .setPositiveButton("Да") { _, _ ->
                addCategoryToMain(category)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun addCategoryToMain(category: CategoryEntity) {
        // Создаем новую категорию с ID = 0 для автогенерации
        val newCategory = CategoryEntity(
            id = 0,
            name = category.name,
            amount = 0f,
            color = category.color
        )
        viewModel.addCategory(newCategory)
        Toast.makeText(requireContext(), "Категория \"${category.name}\" добавлена", Toast.LENGTH_SHORT).show()

        // Обновляем список доступных категорий
        updateAvailableCategories()
    }
}