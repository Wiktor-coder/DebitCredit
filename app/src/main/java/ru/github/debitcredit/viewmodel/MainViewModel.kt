package ru.github.debitcredit.viewmodel

import android.app.Application
import android.util.Log
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.data.database.AppDatabase
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.IncomeEntity
import ru.github.debitcredit.data.model.TransactionEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val categoryDao = database.categoryDao()
    private val incomeDao = database.incomeDao()
    private val transactionDao = database.transactionDao()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _allIncomes = MutableStateFlow<List<IncomeEntity>>(emptyList())
    val allIncomes: StateFlow<List<IncomeEntity>> = _allIncomes.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _totalIncome = MutableStateFlow(0f)
    val totalIncome: StateFlow<Float> = _totalIncome.asStateFlow()

    private val _totalExpenses = MutableStateFlow(0f)
    val totalExpenses: StateFlow<Float> = _totalExpenses.asStateFlow()

    private val _balance = MutableStateFlow(0f)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private var isDefaultDataLoaded = false

    init {
        // Загружаем категории
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { list ->
                Log.d("MainViewModel", "Categories loaded: ${list.size}")
                _categories.value = list
                updateExpensesFromTransactions()
                updateBalance()
            }
        }

        // Загружаем транзакции
        viewModelScope.launch {
            transactionDao.getAllTransactions().collect { list: List<TransactionEntity> ->
                _transactions.value = list
                updateExpensesFromTransactions()
                updateBalance()
            }
        }

        // Загружаем доходы из транзакций
        viewModelScope.launch {
            transactionDao.getTotalIncome().collect { total: Float ->
                _totalIncome.value = total
                updateBalance()
            }
        }

        // Загружаем все доходы (для совместимости)
        viewModelScope.launch {
            incomeDao.getAllIncomes().collect { list ->
                _allIncomes.value = list
            }
        }

        // Запускаем инициализацию после загрузки данных
        viewModelScope.launch {
            // Ждем первой загрузки категорий
            val initialCategories = categoryDao.getAllCategories().first()
            if (initialCategories.isEmpty() && !isDefaultDataLoaded) {
                initializeDefaultData()
            }
        }
    }

    private fun updateExpensesFromTransactions() {
        val expenses = _transactions.value
            .filter { it.type == "expense" }
            .sumOf { it.amount.toDouble() }
            .toFloat()
        _totalExpenses.value = expenses
        Log.d("MainViewModel", "Expenses updated: $expenses")
    }

    private fun updateBalance() {
        _balance.value = _totalIncome.value - _totalExpenses.value
        Log.d("MainViewModel", "Balance updated: ${_balance.value}")
    }

    private suspend fun initializeDefaultData() {
        // Проверяем, что данные еще не загружены
        if (isDefaultDataLoaded) {
            Log.d("MainViewModel", "initializeDefaultData - SKIPPED (already loaded)")
            return
        }

        // Проверяем, есть ли уже категории в БД
        val existingCategories = categoryDao.getAllCategories().first()
        if (existingCategories.isNotEmpty()) {
            Log.d("MainViewModel", "initializeDefaultData - SKIPPED (categories already exist: ${existingCategories.size})")
            isDefaultDataLoaded = true
            return
        }

        isDefaultDataLoaded = true
        Log.d("MainViewModel", "initializeDefaultData - START")

        val defaultCategories = listOf(
            CategoryEntity(0, "products", 0f, "#FF5252".toColorInt(), R.drawable.ic_trolley),
            CategoryEntity(0, "utilities", 0f, "#FFB74D".toColorInt(), R.drawable.ic_house),
            CategoryEntity(0, "transport", 0f, "#FF4081".toColorInt(), R.drawable.ic_car),
            CategoryEntity(0, "health", 0f, "#4CAF50".toColorInt(), R.drawable.ic_heart),
            CategoryEntity(0, "clothing", 0f, "#9C27B0".toColorInt(), R.drawable.ic_clothes),
            CategoryEntity(0, "entertainment", 0f, "#2196F3".toColorInt(), R.drawable.ic_amusement),
            CategoryEntity(0, "other", 0f, "#78909C".toColorInt(), R.drawable.ic_yin_yang)
        )

        defaultCategories.forEach {
            categoryDao.insert(it)
        }

        // Принудительно обновляем состояние после вставки
        val updatedCategories = categoryDao.getAllCategories().first()
        _categories.value = updatedCategories.toList()

        Log.d("MainViewModel", "initializeDefaultData - END, categories: ${updatedCategories.size}")
    }

    // Добавляем транзакцию (расход или доход)
    fun addTransaction(categoryName: String, amount: Float, type: String = "expense") {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                categoryName = categoryName,
                amount = amount,
                date = System.currentTimeMillis(),
                type = type
            )
            transactionDao.insert(transaction)

            // Если это расход, обновляем сумму в категории
            if (type == "expense") {
                updateCategoryAmount(categoryName, amount)
            }

            Log.d("MainViewModel", "Transaction added: $categoryName, $amount, $type")
        }
    }

    // Обновляем сумму категории
    private suspend fun updateCategoryAmount(categoryName: String, amount: Float) {
        val category = _categories.value.find { it.name == categoryName }
        category?.let {
            val updatedCategory = it.copy(
                amount = it.amount + amount,
                date = System.currentTimeMillis()
            )
            categoryDao.update(updatedCategory)
        }
    }

    // Обновляем категорию (для обратной совместимости)
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
            // Получаем категорию для удаления транзакций
            val category = _categories.value.find { it.id == categoryId }
            category?.let {
                // Удаляем все транзакции по этой категории
                transactionDao.deleteByCategory(it.name)
                // Удаляем саму категорию
                categoryDao.deleteById(categoryId)
            }
        }
    }
}