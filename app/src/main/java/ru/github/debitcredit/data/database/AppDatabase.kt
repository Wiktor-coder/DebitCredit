package ru.github.debitcredit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.github.debitcredit.data.dao.CategoryDao
import ru.github.debitcredit.data.dao.IncomeDao
import ru.github.debitcredit.data.model.CategoryEntity
import ru.github.debitcredit.data.model.IncomeEntity

@Database(
    entities = [CategoryEntity::class, IncomeEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Миграция с версии 1 до версии 2 (добавление таблицы incomes)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `incomes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """)
            }
        }

        // Миграция с версии 2 до версии 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пока ничего
            }
        }

        // Миграция с версии 3 до версии 4 (добавление колонки iconRes)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем колонку iconRes с значением по умолчанию
                database.execSQL("""
            ALTER TABLE categories ADD COLUMN iconRes INTEGER NOT NULL DEFAULT ${android.R.drawable.ic_menu_edit}
        """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debitcredit.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true) // для разработки
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}