package ru.github.debitcredit.domain.usecase.transaction

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.github.debitcredit.domain.repository.ITransactionRepository

class GetStatisticsUseCaseTest {

    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var useCase: GetStatisticsUseCase

    @Before
    fun setUp() {
        transactionRepository = mockk()
        useCase = GetStatisticsUseCase(transactionRepository)
    }

    @Test
    fun `should calculate correct statistics when income is positive`() = runTest {
        // Подготовка
        val income = 1000f
        val expenses = 750f
        coEvery { transactionRepository.getTotalIncome() } returns flowOf(income)
        coEvery { transactionRepository.getTotalExpenses() } returns flowOf(expenses)

        // Выполнение
        val result = useCase.invoke()

        // Проверка
        result.collect { statistics ->
            assertEquals(income, statistics.totalIncome)
            assertEquals(expenses, statistics.totalExpenses)
            assertEquals(income - expenses, statistics.balance)
            assertEquals(75f, statistics.spentPercentage)
        }
    }

    @Test
    fun `should return zero percentage when income is zero`() = runTest {
        // Подготовка
        val income = 0f
        val expenses = 100f
        coEvery { transactionRepository.getTotalIncome() } returns flowOf(income)
        coEvery { transactionRepository.getTotalExpenses() } returns flowOf(expenses)

        // Выполнение
        val result = useCase.invoke()

        // Проверка
        result.collect { statistics ->
            assertEquals(0f, statistics.spentPercentage)
            assertEquals(-100f, statistics.balance)
        }
    }

    @Test
    fun `should handle zero expenses`() = runTest {
        // Подготовка
        val income = 1000f
        val expenses = 0f
        coEvery { transactionRepository.getTotalIncome() } returns flowOf(income)
        coEvery { transactionRepository.getTotalExpenses() } returns flowOf(expenses)

        // Выполнение
        val result = useCase.invoke()

        // Проверка
        result.collect { statistics ->
            assertEquals(0f, statistics.spentPercentage)
            assertEquals(1000f, statistics.balance)
        }
    }
}