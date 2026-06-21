package ru.github.debitcredit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryName: String,
    val amount: Float,
    val date: Long = System.currentTimeMillis(),
    val type: String
)