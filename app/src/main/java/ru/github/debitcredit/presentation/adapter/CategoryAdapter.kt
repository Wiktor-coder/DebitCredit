package ru.github.debitcredit.presentation.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.utils.CategoryMapper

class CategoryAdapter(
    private val context: Context,
    private val onItemClick: (CategoryEntity) -> Unit,
    private val onDeleteClick: (CategoryEntity) -> Unit,
    private val onAddClick: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Int>()
    var onSelectionChanged: ((Set<Int>) -> Unit)? = null

    class CategoryViewHolder(itemView: View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelect)
        val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
        val iconContainer: View = itemView.findViewById(R.id.iconContainer)
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryEntity>() {
        override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem == newItem
        }
    }

    @Suppress("NotifyDataSetChanged")
    fun setSelectMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedIds.clear()
            onSelectionChanged?.invoke(emptySet())
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)

        val displayName = CategoryMapper.getLocalizedName(context, category.name)
        holder.nameText.text = displayName
        holder.amountText.text = context.getString(
            R.string.amount_format,
            category.amount
        ) //String.format("%.2f ₽", category.amount)

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(category.color)
            setSize(48, 48)
        }
        holder.iconContainer.background = drawable

        holder.categoryIcon.setImageResource(category.iconRes)
        holder.categoryIcon.setColorFilter(
            ContextCompat.getColor(context, android.R.color.white)
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

        if (isSelectionMode) {
            popupMenu.menu.add(0, 1, 0, context.getString(R.string.add))
        } else {
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
}