package ru.github.debitcredit.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.repository.ITransactionRepository
import ru.github.debitcredit.domain.usecase.category.AddCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.DeleteCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.GetCategoriesUseCase
import ru.github.debitcredit.domain.usecase.transaction.AddTransactionUseCase
import ru.github.debitcredit.domain.usecase.transaction.GetStatisticsUseCase
import ru.github.debitcredit.presentation.state.UiState
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val transactionRepository: ITransactionRepository
) : ViewModel() {

    data class MainUiData(
        val categories: List<CategoryEntity>,
        val balance: Float,
        val totalIncome: Float,
        val totalExpenses: Float
    )

    private val _uiState = MutableLiveData<UiState<MainUiData>>(UiState.Loading)
    val uiState: LiveData<UiState<MainUiData>> = _uiState

    private val _categories = MutableLiveData<List<CategoryEntity>>(emptyList())
    val categories: LiveData<List<CategoryEntity>> = _categories

    private val _transactions = MutableLiveData<List<TransactionEntity>>(emptyList())
    val transactions: LiveData<List<TransactionEntity>> = _transactions

    init {
        loadData()
        loadTransactions()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    getCategoriesUseCase(),
                    getStatisticsUseCase()
                ) { categories, statistics ->
                    _categories.value = categories
                    MainUiData(
                        categories = categories,
                        balance = statistics.balance,
                        totalIncome = statistics.totalIncome,
                        totalExpenses = statistics.totalExpenses
                    )
                }
                    .onEach { data ->
                        _uiState.value = UiState.Success(data)
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            try {
                transactionRepository.getAllTransactions()
                    .onEach { transactions ->
                        _transactions.value = transactions
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun refreshData() {
        loadData()
        loadTransactions()
    }

    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                addCategoryUseCase(category)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun deleteCategory(categoryId: Int, categoryName: String) {
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(categoryId, categoryName)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun addTransaction(categoryName: String, amount: Float, type: String = "expense") {
        viewModelScope.launch {
            try {
                addTransactionUseCase(categoryName, amount, type)
                loadTransactions()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun updateCategory(categoryName: String, newAmount: Float) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val category = currentState.data.categories.find { it.name == categoryName }
            category?.let {
                val updatedCategory = it.copy(amount = newAmount)
                viewModelScope.launch {
                    try {
                        addCategoryUseCase(updatedCategory)
                    } catch (e: Exception) {
                        // Обработка ошибки
                    }
                }
            }
        }
    }
}