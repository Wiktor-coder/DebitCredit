package ru.github.debitcredit.presentation.ui.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.adapter.SelectCategoryAdapter
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.presentation.viewmodel.MainViewModel
import ru.github.debitcredit.utils.CategoryMapper

data class PredefinedCategory(
    val key: String,
    val displayNameKey: Int,
    val colorHex: String
)

@AndroidEntryPoint
class SelectCategoryFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SelectCategoryAdapter
    private lateinit var backButton: ImageButton

    private var existingCategories = mutableListOf<CategoryEntity>()

    private val predefinedCategories = listOf(
        PredefinedCategory("products", R.string.products, "#d91023"),
        PredefinedCategory("utilities", R.string.utilities, "#fa2f70"),
        PredefinedCategory("transport", R.string.transport, "#fc8f30"),
        PredefinedCategory("health", R.string.health, "#18b51e"),
        PredefinedCategory("clothing", R.string.clothing, "#9C27B0"),
        PredefinedCategory("entertainment", R.string.entertainment, "#081fa1"),
        PredefinedCategory("other", R.string.other, "#52636b")
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

        // Создаем список категорий
        val categoryList = predefinedCategories.map { category ->
            CategoryEntity(
                id = 0,
                name = category.key,
                amount = 0f,
                color = category.colorHex.toColorInt(),
                iconRes = CategoryMapper.getIconRes(category.key)
            )
        }

        // Создаем адаптер с правильным порядком аргументов
        adapter = SelectCategoryAdapter(
            context = requireContext(),
            onCategoryClick = { category ->
                checkAndAddCategory(category)
            }
        )

        // Устанавливаем категории
        adapter.updateCategories(categoryList)
        recyclerView.adapter = adapter
    }

    private fun observeExistingCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categories?.let {
                existingCategories = it.toMutableList()
                updateAvailableCategories()
            }
        }
    }

    private fun updateAvailableCategories() {
        val existingKeys = existingCategories.map { it.name }.toSet()
        val availableCategories = predefinedCategories
            .filter { it.key !in existingKeys }
            .map { category ->
                CategoryEntity(
                    id = 0,
                    name = category.key,
                    amount = 0f,
                    color = category.colorHex.toColorInt(),
                    iconRes = CategoryMapper.getIconRes(category.key)
                )
            }

        adapter.updateCategories(availableCategories)

        if (availableCategories.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.no_categories), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun checkAndAddCategory(category: CategoryEntity) {
        val alreadyExists = existingCategories.any { it.name == category.name }

        if (alreadyExists) {
            val displayName = getDisplayNameByKey(category.name)
            Toast.makeText(requireContext(),
                "$displayName ${getString(R.string.category_selected)}", Toast.LENGTH_SHORT).show()
        } else {
            showAddCategoryDialog(category)
        }
    }

    private fun getDisplayNameByKey(key: String): String {
        val resource = predefinedCategories.find { it.key == key }?.displayNameKey
        return if (resource != null) {
            getString(resource)
        } else {
            key
        }
    }

    private fun showAddCategoryDialog(category: CategoryEntity) {
        val displayName = getDisplayNameByKey(category.name)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_category)
            .setMessage("${getString(R.string.add_category_question)} \"$displayName\"?")
            .setPositiveButton(R.string.add) { _, _ ->
                addCategoryToMain(category)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addCategoryToMain(category: CategoryEntity) {
        val newCategory = CategoryEntity(
            id = 0,
            name = category.name,
            amount = 0f,
            color = category.color,
            iconRes = category.iconRes
        )
        viewModel.addCategory(newCategory)

        val displayName = getDisplayNameByKey(category.name)
        Toast.makeText(requireContext(),
            "${getString(R.string.category_added)}: $displayName", Toast.LENGTH_SHORT).show()

        updateAvailableCategories()
    }
}