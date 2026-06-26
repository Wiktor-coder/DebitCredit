package ru.github.debitcredit.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.github.debitcredit.data.database.AppDatabase
import ru.github.debitcredit.data.dao.CategoryDao
import ru.github.debitcredit.data.dao.TransactionDao
import ru.github.debitcredit.data.repository.CategoryRepository
import ru.github.debitcredit.data.repository.TransactionRepository
import ru.github.debitcredit.domain.repository.ICategoryRepository
import ru.github.debitcredit.domain.repository.ITransactionRepository
import ru.github.debitcredit.domain.usecase.category.AddCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.DeleteCategoryUseCase
import ru.github.debitcredit.domain.usecase.category.GetCategoriesUseCase
import ru.github.debitcredit.domain.usecase.transaction.AddTransactionUseCase
import ru.github.debitcredit.domain.usecase.transaction.GetStatisticsUseCase
import ru.github.debitcredit.utils.SettingsManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): ICategoryRepository {
        return CategoryRepository(categoryDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): ITransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(repository: ICategoryRepository): GetCategoriesUseCase {
        return GetCategoriesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddCategoryUseCase(repository: ICategoryRepository): AddCategoryUseCase {
        return AddCategoryUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteCategoryUseCase(
        categoryRepository: ICategoryRepository,
        transactionRepository: ITransactionRepository
    ): DeleteCategoryUseCase {
        return DeleteCategoryUseCase(categoryRepository, transactionRepository)
    }

    @Provides
    @Singleton
    fun provideAddTransactionUseCase(
        transactionRepository: ITransactionRepository,
        categoryRepository: ICategoryRepository
    ): AddTransactionUseCase {
        return AddTransactionUseCase(transactionRepository, categoryRepository)
    }

    @Provides
    @Singleton
    fun provideGetStatisticsUseCase(transactionRepository: ITransactionRepository): GetStatisticsUseCase {
        return GetStatisticsUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}