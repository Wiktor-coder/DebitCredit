package ru.github.debitcredit.utils

import android.content.Context
import androidx.core.graphics.toColorInt
import ru.github.debitcredit.R
import ru.github.debitcredit.data.model.CategoryEntity

object CategoryMapper {
    private val categoryNameMap = mapOf(
        "products" to R.string.products,
        "utilities" to R.string.utilities,
        "transport" to R.string.transport,
        "health" to R.string.health,
        "clothing" to R.string.clothing,
        "entertainment" to R.string.entertainment,
        "other" to R.string.other,
        "income" to R.string.income
    )

    private val iconMap = mapOf(
        "products" to R.drawable.ic_trolley,
        "utilities" to R.drawable.ic_house,
        "transport" to R.drawable.ic_car,
        "health" to R.drawable.ic_heart,
        "clothing" to R.drawable.ic_clothes,
        "entertainment" to R.drawable.ic_amusement,
        "other" to R.drawable.ic_yin_yang,
        "income" to R.drawable.ic_ruble
    )

    private val colorMap = mapOf(
        "products" to "#d91023",      // Красный - продукты
        "utilities" to "#fa2f70",     // Розовый (ЖКХ - utilities)
        "transport" to "#fc8f30",     // Оранжевый (Транспорт)
        "health" to "#18b51e",        // Зеленый
        "clothing" to "#9C27B0",      // Фиолетовый
        "entertainment" to "#081fa1", // Синий
        "other" to "#52636b",         // Серый
        "income" to "#16f0e0"         // Бирюзовый
    )

    fun getLocalizedName(context: Context, key: String): String {
        return categoryNameMap[key]?.let { context.getString(it) } ?: key
    }

    fun getIconRes(key: String): Int = iconMap[key] ?: android.R.drawable.ic_menu_edit

    fun getColor(key: String): Int = colorMap[key]?.toColorInt() ?: "#78909C".toColorInt()

    fun getPredefinedCategories(): List<CategoryEntity> {
        return categoryNameMap.keys.filter { it != "income" }.map { key ->
            CategoryEntity(
                id = 0,
                name = key,
                amount = 0f,
                color = getColor(key),
                iconRes = getIconRes(key)
            )
        }
    }

    fun getCategoryDisplayName(context: Context, category: CategoryEntity): String {
        return getLocalizedName(context, category.name)
    }

    fun getAllCategoryKeys(): List<String> = categoryNameMap.keys.toList()
}