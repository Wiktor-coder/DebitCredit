package ru.github.debitcredit.presentation.adapter

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.ui.statistics.InfoItem

class StatisticsItemsAdapter : RecyclerView.Adapter<StatisticsItemsAdapter.ViewHolder>() {

    private var items: List<InfoItem> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.categoryAmountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_statistics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameText.text = item.title
        holder.amountText.text = item.value

        // Определяем темную тему
        val isNightMode = (holder.itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isNightMode) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.black)
        }

        // Если это "Остаток" и значение отрицательное, красим в красный
        if (item.title == holder.itemView.context.getString(R.string.remaining) &&
            item.value.startsWith("-")
        ) {
            holder.amountText.setTextColor("#FF6B6B".toColorInt())
        } else {
            holder.amountText.setTextColor(textColor)
        }
    }

    override fun getItemCount() = items.size

    @Suppress("NotifyDataSetChanged")
    fun updateData(newItems: List<InfoItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}