package ru.github.debitcredit.data.repository

import kotlinx.coroutines.flow.Flow
import ru.github.debitcredit.data.dao.CategoryDao
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.domain.repository.ICategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) : ICategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun insert(category: CategoryEntity) = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    override suspend fun deleteById(id: Int) = categoryDao.deleteById(id)

    override suspend fun isCategoryExists(name: String): Boolean = categoryDao.isCategoryExists(name)
}