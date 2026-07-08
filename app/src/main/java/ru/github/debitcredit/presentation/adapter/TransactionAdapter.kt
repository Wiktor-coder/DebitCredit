package ru.github.debitcredit.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.utils.CategoryMapper
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val context: Context,
    private val onItemClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryNameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val amountText: TextView = itemView.findViewById(R.id.amountText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val typeIndicator: View = itemView.findViewById(R.id.typeIndicator)
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)

        val displayName = CategoryMapper.getLocalizedName(context, transaction.categoryName)
        holder.categoryNameText.text = displayName

        // Сумма
        val amountText = if (transaction.type == "expense") {
            "- ${String.format(Locale.US, "%.2f", transaction.amount)}"
        } else {
            "+ ${String.format(Locale.US, "%.2f", transaction.amount)}"
        }
        holder.amountText.text = amountText
        holder.amountText.setTextColor(
            if (transaction.type == "expense") {
                ContextCompat.getColor(context, R.color.expense_color)
            } else {
                ContextCompat.getColor(context, R.color.income_color)
            }
        )

        // Дата
        val date = Date(transaction.date)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(date)

        // Индикатор типа (кружок слева)
        holder.typeIndicator.setBackgroundColor(
            if (transaction.type == "expense") {
                ContextCompat.getColor(context, R.color.expense_color)
            } else {
                ContextCompat.getColor(context, R.color.income_color)
            }
        )

        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }
}