package ru.github.debitcredit.domain.usecase.category

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.github.debitcredit.domain.model.Category
import ru.github.debitcredit.domain.model.toDomain
import ru.github.debitcredit.domain.repository.ICategoryRepository

class GetCategoriesUseCase(
    private val repository: ICategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}