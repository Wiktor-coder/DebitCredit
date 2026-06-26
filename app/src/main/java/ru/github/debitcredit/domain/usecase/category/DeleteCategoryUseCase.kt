package ru.github.debitcredit.domain.usecase.category

import ru.github.debitcredit.domain.repository.ICategoryRepository
import ru.github.debitcredit.domain.repository.ITransactionRepository

class DeleteCategoryUseCase(
    private val categoryRepository: ICategoryRepository,
    private val transactionRepository: ITransactionRepository
) {
    suspend operator fun invoke(categoryId: Int, categoryName: String) {
        transactionRepository.deleteByCategory(categoryName)
        categoryRepository.deleteById(categoryId)
    }
}