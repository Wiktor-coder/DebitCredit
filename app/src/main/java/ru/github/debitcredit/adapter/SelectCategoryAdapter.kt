package ru.github.debitcredit.adapter

import android.content.Context
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
    private val context: Context,
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<SelectCategoryAdapter.SelectCategoryViewHolder>() {

    // Маппинг ключей на ресурсы строк
    private val categoryNameMap = mapOf(
        "products" to R.string.products,
        "utilities" to R.string.utilities,
        "transport" to R.string.transport,
        "health" to R.string.health,
        "clothing" to R.string.clothing,
        "entertainment" to R.string.entertainment,
        "other" to R.string.other
    )

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
        // ✅ Показываем локализованное имя
        val displayName = categoryNameMap[category.name]?.let { context.getString(it) } ?: category.name
        holder.nameText.text = displayName
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