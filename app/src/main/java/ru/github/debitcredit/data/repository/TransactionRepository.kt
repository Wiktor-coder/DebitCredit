package ru.github.debitcredit.data.repository

import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.dao.TransactionDao
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.repository.ITransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) : ITransactionRepository {
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startDate, endDate)

    override fun getTransactionsByCategory(categoryName: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategory(categoryName)

    override fun getTotalExpenses(): Flow<Float> = transactionDao.getTotalExpenses()

    override fun getTotalIncome(): Flow<Float> = transactionDao.getTotalIncome()

    override suspend fun getExpensesBetween(startDate: Long, endDate: Long): Float =
        transactionDao.getExpensesBetween(startDate, endDate)

    override suspend fun getIncomeBetween(startDate: Long, endDate: Long): Float =
        transactionDao.getIncomeBetween(startDate, endDate)

    override suspend fun insert(transaction: TransactionEntity) = transactionDao.insert(transaction)

    override suspend fun deleteById(id: Int) = transactionDao.deleteById(id)

    override suspend fun deleteByCategory(categoryName: String) = transactionDao.deleteByCategory(categoryName)

    override suspend fun updateTransactionTime(id: Int, newTime: Long) {
        transactionDao.updateTransactionTimeById(id, newTime)
    }
}