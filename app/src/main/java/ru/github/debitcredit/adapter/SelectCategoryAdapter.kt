package ru.github.debitcredit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity

class SelectCategoryAdapter(
    private var categories: List<CategoryEntity>,
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<SelectCategoryAdapter.SelectCategoryViewHolder>() {

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
        holder.nameText.text = category.name
        holder.cardView.setCardBackgroundColor(category.color)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}