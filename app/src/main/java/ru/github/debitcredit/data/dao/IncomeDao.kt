package ru.github.debitcredit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.model.IncomeEntity

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Insert
    suspend fun insert(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes")
    fun getTotalIncome(): Flow<Float>
}