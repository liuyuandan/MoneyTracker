package com.example.moneytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.dao.TransactionDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.DefaultCategories
import com.example.moneytracker.data.database.entities.Transaction

@Database(
    entities = [Transaction::class, Category::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库迁移：从版本1升级到版本2，添加sortOrder字段
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 初始化默认分类数据
         */
        suspend fun initializeDefaultCategories(categoryDao: CategoryDao) {
            val count = categoryDao.getCategoryCount()
            if (count == 0) {
                categoryDao.insertAll(DefaultCategories.getAllDefaultCategories())
            }
        }
    }
}
