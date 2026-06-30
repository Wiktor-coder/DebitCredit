package ru.github.debitcredit.data.model

import org.junit.Assert.*
import org.junit.Test
import ru.github.debitcredit.R

class CategoryEntityTest {

    @Test
    fun `should create category with default values`() {
        val category = CategoryEntity(
            name = "products",
            color = 0xFF5252
        )

        assertEquals(0, category.id)
        assertEquals("products", category.name)
        assertEquals(0f, category.amount)
        assertEquals(0xFF5252, category.color)
        assertEquals(android.R.drawable.ic_menu_edit, category.iconRes)
        assertTrue(category.date > 0)
    }

    @Test
    fun `should create category with all values`() {
        val category = CategoryEntity(
            id = 1,
            name = "transport",
            amount = 500f,
            color = 0xFFB74D,
            iconRes = R.drawable.ic_car,
            date = 123456789L
        )

        assertEquals(1, category.id)
        assertEquals("transport", category.name)
        assertEquals(500f, category.amount)
        assertEquals(0xFFB74D, category.color)
        assertEquals(R.drawable.ic_car, category.iconRes)
        assertEquals(123456789L, category.date)
    }

    @Test
    fun `should copy category with new amount`() {
        val original = CategoryEntity(
            name = "products",
            amount = 100f,
            color = 0xFF5252
        )

        val updated = original.copy(amount = 200f)

        assertEquals(200f, updated.amount)
        assertEquals(original.name, updated.name)
        assertEquals(original.color, updated.color)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun `should copy category with new name`() {
        val original = CategoryEntity(
            name = "products",
            amount = 100f,
            color = 0xFF5252
        )

        val updated = original.copy(name = "utilities")

        assertEquals("utilities", updated.name)
        assertEquals(original.amount, updated.amount)
        assertEquals(original.color, updated.color)
    }

    @Test
    fun `should have correct color for predefined categories`() {
        val products = CategoryEntity(name = "products", color = 0xFF5252)
        val utilities = CategoryEntity(name = "utilities", color = 0xFFB74D)

        assertNotEquals(products.color, utilities.color)
    }

    @Test
    fun `two categories with same data should be equal`() {
        val category1 = CategoryEntity(id = 1, name = "products", amount = 100f, color = 0xFF5252)
        val category2 = CategoryEntity(id = 1, name = "products", amount = 100f, color = 0xFF5252)

        assertEquals(category1, category2)
        assertEquals(category1.hashCode(), category2.hashCode())
    }

    @Test
    fun `two categories with different data should not be equal`() {
        val category1 = CategoryEntity(id = 1, name = "products", amount = 100f, color = 0xFF5252)
        val category2 = CategoryEntity(id = 2, name = "transport", amount = 50f, color = 0xFFB74D)

        assertNotEquals(category1, category2)
    }
}