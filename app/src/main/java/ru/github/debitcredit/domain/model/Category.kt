package ru.github.debitcredit.domain.model

import ru.github.debitcredit.data.model.CategoryEntity

data class Category(
    val id: Int = 0,
    val name: String,
    val amount: Float = 0f,
    val color: Int,
    val iconRes: Int = android.R.drawable.ic_menu_edit,
    val date: Long = System.currentTimeMillis()
) {
    fun toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            amount = amount,
            color = color,
            iconRes = iconRes,
            date = date
        )
    }
}

// Extension function для преобразования
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        amount = amount,
        color = color,
        iconRes = iconRes,
        date = date
    )
}
