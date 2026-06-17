package ru.github.debitcredit.adapter

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
    private val onAddClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var isSelectionMode = false
    private var selectedIds = mutableSetOf<Int>()
    private var onSelectionChanged: ((Set<Int>) -> Unit)? = null
    private var isSelectMode = false

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelect)
        val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
        val iconContainer: View = itemView.findViewById(R.id.iconContainer)
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
    }

    fun setSelectionMode(enabled: Boolean, ids: MutableSet<Int>, onChanged: (Set<Int>) -> Unit) {
        isSelectionMode = enabled
        selectedIds = ids
        onSelectionChanged = onChanged
        notifyDataSetChanged()
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

        holder.nameText.text = category.name
        holder.amountText.text = String.format("%.2f ₽", category.amount)

        // Создаем круглый фон программно
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(category.color)
            setSize(48, 48)
        }
        holder.iconContainer.background = drawable
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

    private fun showPopupMenu(view: View, category: CategoryEntity) {
        val popupMenu = PopupMenu(view.context, view)

        if (isSelectMode) {
            popupMenu.menu.add(0, 1, 0, "Добавить")
        } else {
            popupMenu.menu.add(0, 1, 0, "Добавить")
            popupMenu.menu.add(0, 2, 1, "Удалить")
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

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}