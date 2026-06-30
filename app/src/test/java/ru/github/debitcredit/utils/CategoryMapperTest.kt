package ru.github.debitcredit.utils

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import ru.github.debitcredit.R

class CategoryMapperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Продукты")
    }

    @Test
    fun `should return localized name for valid key`() {
        val result = CategoryMapper.getLocalizedName(context, "products")
        assertEquals("Продукты", result)
    }

    @Test
    fun `should return key itself for unknown key`() {
        val result = CategoryMapper.getLocalizedName(context, "unknown")
        assertEquals("unknown", result)
    }

    @Test
    fun `should return correct icon resource for valid key`() {
        assertEquals(R.drawable.ic_trolley, CategoryMapper.getIconRes("products"))
        assertEquals(R.drawable.ic_house, CategoryMapper.getIconRes("utilities"))
        assertEquals(R.drawable.ic_car, CategoryMapper.getIconRes("transport"))
        assertEquals(R.drawable.ic_heart, CategoryMapper.getIconRes("health"))
        assertEquals(R.drawable.ic_clothes, CategoryMapper.getIconRes("clothing"))
        assertEquals(R.drawable.ic_amusement, CategoryMapper.getIconRes("entertainment"))
        assertEquals(R.drawable.ic_yin_yang, CategoryMapper.getIconRes("other"))
        assertEquals(R.drawable.ic_ruble, CategoryMapper.getIconRes("income"))
    }

    @Test
    fun `should return default icon for unknown key`() {
        assertEquals(android.R.drawable.ic_menu_edit, CategoryMapper.getIconRes("unknown"))
    }
}