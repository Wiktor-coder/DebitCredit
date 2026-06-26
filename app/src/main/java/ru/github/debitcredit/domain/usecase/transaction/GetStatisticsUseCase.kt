package ru.github.debitcredit.domain.usecase.transaction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.github.debitcredit.domain.repository.ITransactionRepository

data class Statistics(
    val totalIncome: Float,
    val totalExpenses: Float,
    val balance: Float,
    val spentPercentage: Float
)

class GetStatisticsUseCase(
    private val transactionRepository: ITransactionRepository
) {
    operator fun invoke(): Flow<Statistics> {
        return combine(
            transactionRepository.getTotalIncome(),
            transactionRepository.getTotalExpenses()
        ) { income, expenses ->
            val balance = income - expenses
            val percentage = if (income > 0) {
                kotlin.math.min((expenses / income) * 100, 100f)
            } else {
                0f
            }
            Statistics(income, expenses, balance, percentage)
        }
    }
}