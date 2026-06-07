package ru.github.debitcredit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.github.debitcredit.data.database.AppDatabase
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.IncomeEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val categoryDao = database.categoryDao()
    private val incomeDao = database.incomeDao()  // ← ДОБАВЛЕНО

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _totalIncome = MutableStateFlow(0f)
    val totalIncome: StateFlow<Float> = _totalIncome.asStateFlow()

    private val _expenses = MutableStateFlow(0f)
    val expenses: StateFlow<Float> = _expenses.asStateFlow()

    private val _balance = MutableStateFlow(0f)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private var isDataInitialized = false

    init {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { list ->
                _categories.value = list
                _expenses.value = list.sumOf { it.amount.toDouble() }.toFloat()
                updateBalance()

                if (list.isEmpty() && !isDataInitialized) {
                    initializeDefaultData()
                }
            }
        }

        // Наблюдаем за доходами
        viewModelScope.launch {
            incomeDao.getTotalIncome().collect { total ->
                _totalIncome.value = total
                updateBalance()
            }
        }
    }

    private fun updateBalance() {
        _balance.value = _totalIncome.value - _expenses.value
    }

    private suspend fun initializeDefaultData() {
        isDataInitialized = true
        val defaultCategories = listOf(
            CategoryEntity(0, "Продукты", 2500f, android.graphics.Color.parseColor("#FF6B6B")),
            CategoryEntity(0, "Развлечения", 1200f, android.graphics.Color.parseColor("#4ECDC4")),
            CategoryEntity(0, "Иное", 800f, android.graphics.Color.parseColor("#96CEB4"))
        )
        defaultCategories.forEach { categoryDao.insert(it) }
    }

    fun addIncome(amount: Float, note: String = "") {
        Log.d("MainViewModel", "=== addIncome ===")
        Log.d("MainViewModel", "Amount: $amount, Note: $note")
        viewModelScope.launch {
            val income = IncomeEntity(amount = amount, note = note)
            incomeDao.insert(income)
            Log.d("MainViewModel", "Income inserted successfully")
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