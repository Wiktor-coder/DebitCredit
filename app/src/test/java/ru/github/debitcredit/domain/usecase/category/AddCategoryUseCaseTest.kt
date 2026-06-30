package ru.github.debitcredit.domain.usecase.category

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository

class AddCategoryUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: AddCategoryUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddCategoryUseCase(repository)
    }

    @Test
    fun `should add category to repository`() = runTest {
        // Подготовка
        val category = CategoryEntity(
            name = "products",
            color = 0xFF5252
        )

        // Выполнение
        useCase.invoke(category)

        // Проверка
        coVerify { repository.insert(category) }
    }

    @Test
    fun `should add category with amount`() = runTest {
        // Подготовка
        val category = CategoryEntity(
            name = "transport",
            amount = 500f,
            color = 0xFFB74D
        )

        // Выполнение
        useCase.invoke(category)

        // Проверка
        coVerify { repository.insert(category) }
    }
}