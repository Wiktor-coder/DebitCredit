package ru.github.debitcredit.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.utils.CategoryMapper

class SelectCategoryAdapter(
    private val context: Context,
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<SelectCategoryAdapter.SelectCategoryViewHolder>() {

    private var categories: List<CategoryEntity> = emptyList()

    class SelectCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.categoryCard)
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_category, parent, false)
        return SelectCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectCategoryViewHolder, position: Int) {
        val category = categories[position]
        val displayName = CategoryMapper.getLocalizedName(context, category.name)
        holder.nameText.text = displayName
        holder.cardView.setCardBackgroundColor(category.color)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size

    @Suppress("NotifyDataSetChanged")
    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}