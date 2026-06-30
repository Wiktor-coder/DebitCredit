package ru.github.debitcredit.domain.usecase.transaction

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository
import ru.github.debitcredit.domain.repository.ITransactionRepository

class AddTransactionUseCaseTest {

    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setUp() {
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        useCase = AddTransactionUseCase(transactionRepository, categoryRepository)
    }

    @Test
    fun `should add expense transaction and update category`() = runTest {
        // Подготовка
        val categoryName = "products"
        val amount = 100f
        val type = "expense"
        val existingCategory = CategoryEntity(
            id = 1,
            name = categoryName,
            amount = 50f,
            color = 0xFF5252,
            iconRes = 0
        )

        coEvery { categoryRepository.getAllCategories() } returns flowOf(listOf(existingCategory))

        // Выполнение
        useCase.invoke(categoryName, amount, type)

        // Проверка
        coVerify { transactionRepository.insert(any<TransactionEntity>()) }
        coVerify { categoryRepository.update(any<CategoryEntity>()) }
    }

    @Test
    fun `should add income transaction without updating category`() = runTest {
        // Подготовка
        val categoryName = "income"
        val amount = 100f
        val type = "income"

        // Выполнение
        useCase.invoke(categoryName, amount, type)

        // Проверка
        coVerify { transactionRepository.insert(any<TransactionEntity>()) }
        coVerify(exactly = 0) { categoryRepository.update(any<CategoryEntity>()) }
    }

    @Test
    fun `should add expense to non-existent category`() = runTest {
        // Подготовка
        val categoryName = "unknown"
        val amount = 100f
        val type = "expense"

        coEvery { categoryRepository.getAllCategories() } returns flowOf(emptyList())

        // Выполнение
        useCase.invoke(categoryName, amount, type)

        // Проверка
        coVerify { transactionRepository.insert(any<TransactionEntity>()) }
        coVerify(exactly = 0) { categoryRepository.update(any<CategoryEntity>()) }
    }
}