package ru.github.debitcredit.domain.usecase.category

import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository

class AddCategoryUseCase(
    private val repository: ICategoryRepository
) {
    suspend operator fun invoke(category: CategoryEntity) {
        repository.insert(category)
    }
}