package ru.github.debitcredit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Float = 0f,
    val color: Int,
    val iconRes: Int = android.R.drawable.ic_menu_edit,
    val date: Long = System.currentTimeMillis(), // Дата транзакции
)