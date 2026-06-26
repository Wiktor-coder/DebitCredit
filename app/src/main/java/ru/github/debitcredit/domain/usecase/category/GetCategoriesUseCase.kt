package ru.github.debitcredit.domain.usecase.category

import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository

class GetCategoriesUseCase(
    private val repository: ICategoryRepository
) {
    operator fun invoke(): Flow<List<CategoryEntity>> = repository.getAllCategories()
}