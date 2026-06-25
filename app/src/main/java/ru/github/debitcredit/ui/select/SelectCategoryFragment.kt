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

data class PredefinedCategory(
    val key: String,
    val displayNameKey: Int,
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

    private val predefinedCategories = listOf(
        PredefinedCategory("products", R.string.products, "#FF5252"),
        PredefinedCategory("utilities", R.string.utilities, "#FF4081"),
        PredefinedCategory("transport", R.string.transport, "#FFB74D"),
        PredefinedCategory("health", R.string.health, "#4CAF50"),
        PredefinedCategory("clothing", R.string.clothing, "#9C27B0"),
        PredefinedCategory("entertainment", R.string.entertainment, "#2196F3"),
        PredefinedCategory("other", R.string.other, "#78909C")
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
        val categoryList = predefinedCategories.map { category ->
            CategoryEntity(
                id = 0,
                name = category.key,
                amount = 0f,
                color = Color.parseColor(category.colorHex),
                iconRes = getIconResByKey(category.key)
            )
        }
        // ✅ В адаптере показываем локализованные названия
        val displayAdapter = SelectCategoryAdapter(categoryList, requireContext()) { category ->
            checkAndAddCategory(category)
        }
        recyclerView.adapter = displayAdapter
    }

    private fun getIconResByKey(key: String): Int {
        return when (key) {
            "products" -> R.drawable.ic_trolley
            "utilities" -> R.drawable.ic_house
            "transport" -> R.drawable.ic_car
            "health" -> R.drawable.ic_heart
            "clothing" -> R.drawable.ic_clothes
            "entertainment" -> R.drawable.ic_amusement
            "other" -> R.drawable.ic_yin_yang
            else -> android.R.drawable.ic_menu_edit
        }
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
        val existingKeys = existingCategories.map { it.name }.toSet()
        val availableCategories = predefinedCategories
            .filter { it.key !in existingKeys }
            .map { category ->
                CategoryEntity(
                    id = 0,
                    name = category.key,
                    amount = 0f,
                    color = Color.parseColor(category.colorHex),
                    iconRes = getIconResByKey(category.key)
                )
            }
        (recyclerView.adapter as? SelectCategoryAdapter)?.updateCategories(availableCategories)

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