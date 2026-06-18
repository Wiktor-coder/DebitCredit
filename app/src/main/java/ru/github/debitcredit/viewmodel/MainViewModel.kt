package ru.github.debitcredit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.data.database.AppDatabase
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.IncomeEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val categoryDao = database.categoryDao()
    private val incomeDao = database.incomeDao()

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
        // Загружаем категории
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { list ->
                Log.d("MainViewModel", "Categories loaded: ${list.size}")
                _categories.value = list
                _expenses.value = list.sumOf { it.amount.toDouble() }.toFloat()
                Log.d("MainViewModel", "Expenses calculated: ${_expenses.value}")
                updateBalance()

                if (list.isEmpty() && !isDataInitialized) {
                    initializeDefaultData()
                }
            }
        }

        // Загружаем доходы
        viewModelScope.launch {
            incomeDao.getTotalIncome().collect { total ->
                Log.d("MainViewModel", "Total income loaded: $total")
                _totalIncome.value = total
                updateBalance()
            }
        }
    }

    private fun updateBalance() {
        _balance.value = _totalIncome.value - _expenses.value
        Log.d("MainViewModel", "Balance updated: ${_balance.value} (income: ${_totalIncome.value}, expenses: ${_expenses.value})")
    }

    private suspend fun initializeDefaultData() {
        isDataInitialized = true
        Log.d("MainViewModel", "initializeDefaultData - START")

        val defaultCategories = listOf(
            CategoryEntity(0, "products", 2500f, android.graphics.Color.parseColor("#FF5252"), R.drawable.ic_trolley),
            CategoryEntity(0, "utilities", 3500f, android.graphics.Color.parseColor("#FF4081"), R.drawable.ic_house),
            CategoryEntity(0, "transport", 2000f, android.graphics.Color.parseColor("#FFB74D"), R.drawable.ic_car),
            CategoryEntity(0, "health", 1500f, android.graphics.Color.parseColor("#4CAF50"), R.drawable.ic_heart),
            CategoryEntity(0, "clothing", 1800f, android.graphics.Color.parseColor("#9C27B0"), R.drawable.ic_clothes),
            CategoryEntity(0, "entertainment", 1200f, android.graphics.Color.parseColor("#2196F3"), R.drawable.ic_amusement),
            CategoryEntity(0, "other", 800f, android.graphics.Color.parseColor("#78909C"), R.drawable.ic_yin_yang)
        )

        Log.d("MainViewModel", "initializeDefaultData - categories count: ${defaultCategories.size}")
        defaultCategories.forEach {
            Log.d("MainViewModel", "Inserting category: ${it.name}, amount: ${it.amount}")
            categoryDao.insert(it)
        }
        Log.d("MainViewModel", "initializeDefaultData - END")
    }

    fun addIncome(amount: Float, note: String = "") {
        Log.d("MainViewModel", "=== addIncome ===")
        Log.d("MainViewModel", "Amount: $amount, Note: $note")
        viewModelScope.launch {
            val income = IncomeEntity(amount = amount, note = note)
            incomeDao.insert(income)
            Log.d("MainViewModel", "Income inserted successfully")
            // После вставки дохода, он автоматически обновится через Flow
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

    fun deleteCategoryById(categoryId: Int) {
        viewModelScope.launch {
            categoryDao.deleteById(categoryId)
        }
    }
}