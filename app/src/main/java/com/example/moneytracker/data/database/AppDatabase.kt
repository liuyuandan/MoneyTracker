package com.example.moneytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.dao.CategoryTotal
import com.example.moneytracker.data.database.dao.DailyTotal
import com.example.moneytracker.data.database.dao.TransactionDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.DefaultCategories
import com.example.moneytracker.data.database.entities.Transaction

@Database(
    entities = [Transaction::class, Category::class],
    views = [CategoryTotal::class, DailyTotal::class],
    version = 1,
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
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
