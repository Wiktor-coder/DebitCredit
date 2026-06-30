package ru.github.debitcredit.data.model

import org.junit.Assert.*
import org.junit.Test

class TransactionEntityTest {

    @Test
    fun `should create expense transaction`() {
        val transaction = TransactionEntity(
            categoryName = "products",
            amount = 100f,
            type = "expense"
        )

        assertEquals("products", transaction.categoryName)
        assertEquals(100f, transaction.amount)
        assertEquals("expense", transaction.type)
        assertTrue(transaction.date > 0)
        assertEquals(0, transaction.id)
    }

    @Test
    fun `should create income transaction`() {
        val transaction = TransactionEntity(
            categoryName = "income",
            amount = 500f,
            type = "income"
        )

        assertEquals("income", transaction.categoryName)
        assertEquals(500f, transaction.amount)
        assertEquals("income", transaction.type)
        assertTrue(transaction.date > 0)
    }

    @Test
    fun `should create transaction with all values`() {
        val transaction = TransactionEntity(
            id = 1,
            categoryName = "transport",
            amount = 250f,
            date = 123456789L,
            type = "expense"
        )

        assertEquals(1, transaction.id)
        assertEquals("transport", transaction.categoryName)
        assertEquals(250f, transaction.amount)
        assertEquals(123456789L, transaction.date)
        assertEquals("expense", transaction.type)
    }

    @Test
    fun `should copy transaction with new amount`() {
        val original = TransactionEntity(
            categoryName = "products",
            amount = 100f,
            type = "expense"
        )

        val updated = original.copy(amount = 150f)

        assertEquals(150f, updated.amount)
        assertEquals(original.categoryName, updated.categoryName)
        assertEquals(original.type, updated.type)
    }

    @Test
    fun `two transactions with same data should be equal`() {
        val transaction1 = TransactionEntity(
            id = 1,
            categoryName = "products",
            amount = 100f,
            date = 123456789L,
            type = "expense"
        )
        val transaction2 = TransactionEntity(
            id = 1,
            categoryName = "products",
            amount = 100f,
            date = 123456789L,
            type = "expense"
        )

        assertEquals(transaction1, transaction2)
        assertEquals(transaction1.hashCode(), transaction2.hashCode())
    }
}