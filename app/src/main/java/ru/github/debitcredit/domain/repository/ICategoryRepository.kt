package ru.github.debitcredit.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.model.CategoryEntity

interface ICategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun insert(category: CategoryEntity)
    suspend fun update(category: CategoryEntity)
    suspend fun deleteById(id: Int)
    suspend fun isCategoryExists(name: String): Boolean
}