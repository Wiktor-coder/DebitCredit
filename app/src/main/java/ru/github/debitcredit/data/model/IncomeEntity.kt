package ru.github.debitcredit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Float,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)