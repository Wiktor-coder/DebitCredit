package ru.github.debitcredit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity

class CategoryAdapter(
    private var categories: List<CategoryEntity>,
    private val onItemClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var isSelectionMode = false
    private var selectedIds = mutableSetOf<Int>()
    private var onSelectionChanged: ((Set<Int>) -> Unit)? = null

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.categoryCard)
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelect)
    }

    fun setSelectionMode(enabled: Boolean, ids: MutableSet<Int>, onChanged: (Set<Int>) -> Unit) {
        isSelectionMode = enabled
        selectedIds = ids
        onSelectionChanged = onChanged
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.nameText.text = category.name
        holder.amountText.text = String.format("%.2f ₽", category.amount)
        holder.cardView.setCardBackgroundColor(category.color)

        if (isSelectionMode) {
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.isChecked = selectedIds.contains(category.id)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIds.add(category.id)
                } else {
                    selectedIds.remove(category.id)
                }
                onSelectionChanged?.invoke(selectedIds)
            }
        } else {
            holder.checkBox.visibility = View.GONE
            holder.checkBox.setOnCheckedChangeListener(null)
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            } else {
                onItemClick(category)
            }
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}