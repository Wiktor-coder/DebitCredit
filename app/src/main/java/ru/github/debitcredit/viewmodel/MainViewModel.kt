package ru.github.debitcredit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.github.debitcredit.data.database.AppDatabase
import ru.github.debitcredit.data.model.CategoryEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val categoryDao = database.categoryDao()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    init {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { list ->
                _categories.value = list
            }
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.update(category)
        }
    }

    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.insert(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.deleteById(category.id)
        }
    }
}