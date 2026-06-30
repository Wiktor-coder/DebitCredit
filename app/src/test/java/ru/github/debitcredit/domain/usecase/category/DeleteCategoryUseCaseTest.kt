package ru.github.debitcredit.domain.usecase.category

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.github.debitcredit.domain.repository.ICategoryRepository
import ru.github.debitcredit.domain.repository.ITransactionRepository

class DeleteCategoryUseCaseTest {

    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var useCase: DeleteCategoryUseCase

    @Before
    fun setUp() {
        categoryRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        useCase = DeleteCategoryUseCase(categoryRepository, transactionRepository)
    }

    @Test
    fun `should delete category and its transactions`() = runTest {
        // Подготовка
        val categoryId = 1
        val categoryName = "products"

        // Выполнение
        useCase.invoke(categoryId, categoryName)

        // Проверка
        coVerify { transactionRepository.deleteByCategory(categoryName) }
        coVerify { categoryRepository.deleteById(categoryId) }
    }

    @Test
    fun `should handle deletion with different category name`() = runTest {
        // Подготовка
        val categoryId = 2
        val categoryName = "transport"

        // Выполнение
        useCase.invoke(categoryId, categoryName)

        // Проверка
        coVerify { transactionRepository.deleteByCategory(categoryName) }
        coVerify { categoryRepository.deleteById(categoryId) }
    }
}