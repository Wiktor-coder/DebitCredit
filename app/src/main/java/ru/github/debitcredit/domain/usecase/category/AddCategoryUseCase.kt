package ru.github.debitcredit.domain.usecase.category

import ru.github.debitcredit.domain.model.Category
import ru.github.debitcredit.domain.repository.ICategoryRepository

class AddCategoryUseCase(
    private val repository: ICategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        val entity = category.toEntity()
        repository.insert(entity)
    }
}