package ru.github.debitcredit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.model.TransactionEntity

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryName = :categoryName ORDER BY date DESC")
    fun getTransactionsByCategory(categoryName: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'expense'")
    fun getTotalExpenses(): Flow<Float>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'income'")
    fun getTotalIncome(): Flow<Float>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'expense' AND date BETWEEN :startDate AND :endDate")
    suspend fun getExpensesBetween(startDate: Long, endDate: Long): Float

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'income' AND date BETWEEN :startDate AND :endDate")
    suspend fun getIncomeBetween(startDate: Long, endDate: Long): Float

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transactions WHERE categoryName = :categoryName")
    suspend fun deleteByCategory(categoryName: String)
}