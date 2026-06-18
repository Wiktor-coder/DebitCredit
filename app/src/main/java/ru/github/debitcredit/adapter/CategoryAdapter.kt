package ru.github.debitcredit.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity

class CategoryAdapter(
    private var categories: List<CategoryEntity>,
    private val onItemClick: (CategoryEntity) -> Unit,
    private val onDeleteClick: (CategoryEntity) -> Unit,
    private val onAddClick: (CategoryEntity) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var isSelectionMode = false
    private var selectedIds = mutableSetOf<Int>()
    private var onSelectionChanged: ((Set<Int>) -> Unit)? = null
    private var isSelectMode = false

    // Маппинг ключей на ресурсы строк
    private val categoryNameMap = mapOf(
        "products" to R.string.products,
        "utilities" to R.string.utilities,
        "transport" to R.string.transport,
        "health" to R.string.health,
        "clothing" to R.string.clothing,
        "entertainment" to R.string.entertainment,
        "other" to R.string.other,
        "income" to R.string.income
    )

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelect)
        val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
        val iconContainer: View = itemView.findViewById(R.id.iconContainer)
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
    }

    fun setSelectMode(enabled: Boolean) {
        isSelectMode = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        Log.d("CategoryAdapter", "onCreateViewHolder - using item_category")
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        Log.d("CategoryAdapter", "Binding category: ${category.name}, amount: ${category.amount}, position: $position")

        // Получаем локализованное имя по ключу
        val displayName = getLocalizedName(category.name)
        holder.nameText.text = displayName
        holder.amountText.text = String.format("%.2f ₽", category.amount)

        // Создаем круглый фон для иконки
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(category.color)
            setSize(48, 48)
        }
        holder.iconContainer.background = drawable

        // Устанавливаем иконку
        holder.categoryIcon.setImageResource(category.iconRes)
        holder.categoryIcon.setColorFilter(
            ContextCompat.getColor(
                holder.itemView.context,
                android.R.color.white
            )
        )

        if (isSelectionMode) {
            holder.checkBox.visibility = View.VISIBLE
            holder.menuButton.visibility = View.GONE
            holder.checkBox.isChecked = selectedIds.contains(category.id)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIds.add(category.id)
                } else {
                    selectedIds.remove(category.id)
                }
                onSelectionChanged?.invoke(selectedIds)
            }
            holder.itemView.setOnClickListener {
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            }
        } else {
            holder.checkBox.visibility = View.GONE
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.menuButton.visibility = View.VISIBLE

            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, category)
            }

            holder.itemView.setOnClickListener {
                onItemClick(category)
            }
        }
    }

    private fun getLocalizedName(key: String): String {
        return categoryNameMap[key]?.let { context.getString(it) } ?: key
    }

    private fun showPopupMenu(view: View, category: CategoryEntity) {
        val popupMenu = PopupMenu(view.context, view)

        // На главном экране показываем только "Удалить"
        if (isSelectMode) {
            // В режиме выбора категорий показываем "Добавить"
            popupMenu.menu.add(0, 1, 0, context.getString(R.string.add))
        } else {
            // На главном экране только "Удалить"
            popupMenu.menu.add(0, 2, 0, context.getString(R.string.delete))
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    onAddClick(category)
                    true
                }
                2 -> {
                    onDeleteClick(category)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    override fun getItemCount(): Int {
        Log.d("CategoryAdapter", "Item count: ${categories.size}")
        return categories.size
    }

    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories = newCategories
        Log.d("CategoryAdapter", "updateCategories - size: ${categories.size}")
        notifyDataSetChanged()
    }
}