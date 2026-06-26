package ru.github.debitcredit.domain.usecase.transaction

import kotlinx.coroutines.flow.first
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository
import ru.github.debitcredit.domain.repository.ITransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository
) {
    suspend operator fun invoke(categoryName: String, amount: Float, type: String = "expense") {
        // Сохраняем время в UTC (без корректировки)
        val currentTime = System.currentTimeMillis()

        val transaction = TransactionEntity(
            categoryName = categoryName,
            amount = amount,
            date = currentTime,
            type = type
        )
        transactionRepository.insert(transaction)

        if (type == "expense") {
            val categories = categoryRepository.getAllCategories().first()
            val category = categories.find { it.name == categoryName }
            category?.let {
                val updatedCategory = it.copy(
                    amount = it.amount + amount,
                    date = currentTime
                )
                categoryRepository.update(updatedCategory)
            }
        }
    }
}