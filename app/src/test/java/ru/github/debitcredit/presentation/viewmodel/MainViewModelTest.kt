package ru.github.debitcredit.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.TransactionEntity
import ru.github.debitcredit.domain.repository.ITransactionRepository
import ru.github.debitcredit.domain.usecase.category.AddCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.DeleteCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.GetCategoriesUseCase
import ru.github.debitcredit.domain.usecase.transaction.AddTransactionUseCase
import ru.github.debitcredit.domain.usecase.transaction.GetStatisticsUseCase
import ru.github.debitcredit.domain.usecase.transaction.Statistics
import ru.github.debitcredit.presentation.state.UiState
import ru.github.debitcredit.utils.MainCoroutineRule

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: MainViewModel
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var addCategoryUseCase: AddCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase
    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var getStatisticsUseCase: GetStatisticsUseCase
    private lateinit var transactionRepository: ITransactionRepository

    @Before
    fun setUp() {
        getCategoriesUseCase = mockk(relaxed = true)
        addCategoryUseCase = mockk(relaxed = true)
        deleteCategoryUseCase = mockk(relaxed = true)
        addTransactionUseCase = mockk(relaxed = true)
        getStatisticsUseCase = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)

        coEvery { getCategoriesUseCase.invoke() } returns flowOf(emptyList())
        coEvery { getStatisticsUseCase.invoke() } returns flowOf(Statistics(0f, 0f, 0f, 0f))
        coEvery { transactionRepository.getAllTransactions() } returns flowOf(emptyList())
    }

    @Test
    fun `should load data successfully`() = runTest {
        // Подготовка
        val categories = listOf(
            CategoryEntity(1, "products", 100f, 0xFF5252),
            CategoryEntity(2, "transport", 50f, 0xFFB74D)
        )
        val statistics = Statistics(500f, 300f, 200f, 60f)

        coEvery { getCategoriesUseCase.invoke() } returns flowOf(categories)
        coEvery { getStatisticsUseCase.invoke() } returns flowOf(statistics)

        // Выполнение
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Проверка
        val result = viewModel.uiState.value
        assertTrue(result is UiState.Success)
        if (result is UiState.Success) {
            assertEquals(categories, result.data.categories)
            assertEquals(200f, result.data.balance)
            assertEquals(500f, result.data.totalIncome)
            assertEquals(300f, result.data.totalExpenses)
        }
    }

    @Test
    fun `should handle error when loading data`() = runTest {
        // Подготовка
        val errorMessage = "Network error"
        coEvery { getCategoriesUseCase.invoke() } throws RuntimeException(errorMessage)

        // Выполнение
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Проверка
        val result = viewModel.uiState.value
        assertTrue(result is UiState.Error)
        if (result is UiState.Error) {
            assertTrue(result.message.contains(errorMessage))
        }
    }

    @Test
    fun `should add expense transaction`() = runTest {
        // Подготовка
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.addTransaction("products", 100f, "expense")

        // Проверка
        coVerify { addTransactionUseCase.invoke("products", 100f, "expense") }
    }

    @Test
    fun `should add income transaction`() = runTest {
        // Подготовка
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.addTransaction("income", 500f, "income")

        // Проверка
        coVerify { addTransactionUseCase.invoke("income", 500f, "income") }
    }

    @Test
    fun `should add category`() = runTest {
        // Подготовка
        val category = CategoryEntity(0, "entertainment", 0f, 0x2196F3)

        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.addCategory(category)

        // Проверка
        coVerify { addCategoryUseCase.invoke(category) }
    }

    @Test
    fun `should delete category and its transactions`() = runTest {
        // Подготовка
        val categoryId = 1
        val categoryName = "products"

        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.deleteCategory(categoryId, categoryName)

        // Проверка
        coVerify { deleteCategoryUseCase.invoke(categoryId, categoryName) }
    }

    @Test
    fun `should update category amount`() = runTest {
        // Подготовка
        val category = CategoryEntity(1, "products", 100f, 0xFF5252)
        val categories = listOf(category)
        val statistics = Statistics(0f, 0f, 0f, 0f)

        coEvery { getCategoriesUseCase.invoke() } returns flowOf(categories)
        coEvery { getStatisticsUseCase.invoke() } returns flowOf(statistics)
        coEvery { transactionRepository.getAllTransactions() } returns flowOf(emptyList())

        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.updateCategory("products", 200f)

        // Проверка
        coVerify { addCategoryUseCase.invoke(category.copy(amount = 200f)) }
    }

    @Test
    fun `should refresh data`() = runTest {
        // Подготовка
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Выполнение
        viewModel.refreshData()

        // Проверка
        coVerify(atLeast = 1) { getCategoriesUseCase.invoke() }
        coVerify(atLeast = 1) { getStatisticsUseCase.invoke() }
    }

    @Test
    fun `should load transactions`() = runTest {
        // Подготовка
        val transactions = listOf(
            TransactionEntity(1, "products", 100f, 123456789L, "expense"),
            TransactionEntity(2, "transport", 50f, 123456790L, "expense")
        )
        coEvery { transactionRepository.getAllTransactions() } returns flowOf(transactions)

        // Выполнение
        viewModel = MainViewModel(
            getCategoriesUseCase,
            addCategoryUseCase,
            deleteCategoryUseCase,
            addTransactionUseCase,
            getStatisticsUseCase,
            transactionRepository
        )

        // Проверка
        val result = viewModel.transactions.value
        assertEquals(transactions, result)
    }
}