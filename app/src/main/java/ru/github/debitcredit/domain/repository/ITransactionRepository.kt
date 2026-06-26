package ru.github.debitcredit.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.model.TransactionEntity

interface ITransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getTransactionsByCategory(categoryName: String): Flow<List<TransactionEntity>>
    fun getTotalExpenses(): Flow<Float>
    fun getTotalIncome(): Flow<Float>
    suspend fun getExpensesBetween(startDate: Long, endDate: Long): Float
    suspend fun getIncomeBetween(startDate: Long, endDate: Long): Float
    suspend fun insert(transaction: TransactionEntity)
    suspend fun deleteById(id: Int)
    suspend fun deleteByCategory(categoryName: String)
    suspend fun updateTransactionTime(id: Int, newTime: Long) // Этот метод должен быть
}