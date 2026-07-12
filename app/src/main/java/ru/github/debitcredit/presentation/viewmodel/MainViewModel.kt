package ru.github.debitcredit.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.model.Category
import ru.github.debitcredit.domain.repository.ICategoryRepository
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
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    data class MainUiData(
        val categories: List<Category>,
        val balance: Float,
        val totalIncome: Float,
        val totalExpenses: Float
    )

    private val _uiState = MutableLiveData<UiState<MainUiData>>(UiState.Loading)
    val uiState: LiveData<UiState<MainUiData>> = _uiState

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _transactions = MutableLiveData<List<TransactionEntity>>(emptyList())
    val transactions: LiveData<List<TransactionEntity>> = _transactions

//    private var isIconsUpdated = false

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
                    // Обновляем иконки в фоновом потоке
//                    if (!isIconsUpdated) {
//                        viewModelScope.launch(Dispatchers.IO) {
//                            updateCategoryIcons(categories)
//                        }
//                    }

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

    fun updateCategoryIcon(categoryId: Int, newIconRes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val category = currentState.data.categories.find { it.id == categoryId }
                    category?.let {
                        val categoryEntity = CategoryEntity(
                            id = it.id,
                            name = it.name,
                            amount = it.amount,
                            color = it.color,
                            iconRes = newIconRes,
                            date = it.date
                        )
                        categoryRepository.update(categoryEntity)
                        android.util.Log.d(
                            "MainViewModel",
                            "Updated icon for: ${it.name} -> $newIconRes"
                        )
                        refreshData()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error updating icon", e)
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
                e.stackTrace
            }
        }
    }

    fun refreshData() {
        loadData()
        loadTransactions()
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                addCategoryUseCase(category)
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    fun deleteCategory(categoryId: Int, categoryName: String) {
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(categoryId, categoryName)
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    fun addTransaction(categoryName: String, amount: Float, type: String = "expense") {
        viewModelScope.launch {
            try {
                addTransactionUseCase(categoryName, amount, type)
                loadTransactions()
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    fun updateCategory(categoryName: String, newAmount: Float) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val category = currentState.data.categories.find { it.name == categoryName }
            category?.let {
                val updatedCategory = it.copy(amount = newAmount)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val categoryEntity = CategoryEntity(
                            id = updatedCategory.id,
                            name = updatedCategory.name,
                            amount = updatedCategory.amount,
                            color = updatedCategory.color,
                            iconRes = updatedCategory.iconRes,
                            date = updatedCategory.date
                        )
                        categoryRepository.update(categoryEntity)
                        refreshData()
                    } catch (e: Exception) {
                        e.stackTrace
                    }
                }
            }
        }
    }
}