package ru.github.debitcredit.domain.usecase.category

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import ru.github.debitcredit.domain.model.Category
import ru.github.debitcredit.domain.repository.ICategoryRepository

class AddCategoryUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: AddCategoryUseCase

    @Before
    fun setup() {
        repository = mock(ICategoryRepository::class.java)
        useCase = AddCategoryUseCase(repository)
    }

    @Test
    fun `should insert category`() = runBlocking {
        // Используем Category из domain слоя
        val category = Category(
            id = 1,
            name = "test_category",
            amount = 100f,
            color = 0xFF0000,
            iconRes = android.R.drawable.ic_menu_edit
        )

        useCase(category)

        val expectedEntity = category.toEntity()
        verify(repository).insert(expectedEntity)
    }

    @Test
    fun `should call repository insert`() = runBlocking {
        val category = Category(
            id = 0,
            name = "products",
            amount = 0f,
            color = 0xFFFF0000.toInt(),
            iconRes = android.R.drawable.ic_menu_edit
        )

        useCase(category)

        val expectedEntity = category.toEntity()
        verify(repository).insert(expectedEntity)
    }
}