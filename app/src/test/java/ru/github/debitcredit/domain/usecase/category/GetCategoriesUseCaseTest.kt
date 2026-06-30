package ru.github.debitcredit.domain.usecase.category

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository

class GetCategoriesUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: GetCategoriesUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCategoriesUseCase(repository)
    }

    @Test
    fun `should return categories from repository`() = runTest {
        // Подготовка
        val categories = listOf(
            CategoryEntity(1, "products", 100f, 0xFF5252),
            CategoryEntity(2, "transport", 50f, 0xFFB74D)
        )
        coEvery { repository.getAllCategories() } returns flowOf(categories)

        // Выполнение
        val result = useCase.invoke()

        // Проверка
        result.collect { collected ->
            assertEquals(categories.size, collected.size)
            assertEquals(categories[0].name, collected[0].name)
            assertEquals(categories[1].name, collected[1].name)
        }
    }

    @Test
    fun `should return empty list when repository returns empty`() = runTest {
        // Подготовка
        coEvery { repository.getAllCategories() } returns flowOf(emptyList())

        // Выполнение
        val result = useCase.invoke()

        // Проверка
        result.collect { collected ->
            assertTrue(collected.isEmpty())
        }
    }
}