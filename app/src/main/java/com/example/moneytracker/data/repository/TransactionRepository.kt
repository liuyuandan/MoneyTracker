package com.example.moneytracker.data.repository

import android.util.Log
import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.dao.TransactionDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.CategoryTotal
import com.example.moneytracker.data.database.entities.DailyTotal
import com.example.moneytracker.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * 交易记录仓库
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    companion object {
        private const val TAG = "TransactionRepository"
    }

    suspend fun insert(transaction: Transaction): Long {
        return try {
            Log.d(TAG, "insert: Inserting transaction")
            transactionDao.insert(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "insert: Error inserting transaction", e)
            throw e
        }
    }

    suspend fun update(transaction: Transaction) {
        return try {
            Log.d(TAG, "update: Updating transaction")
            transactionDao.update(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "update: Error updating transaction", e)
            throw e
        }
    }

    suspend fun delete(transaction: Transaction) {
        return try {
            Log.d(TAG, "delete: Deleting transaction")
            transactionDao.delete(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "delete: Error deleting transaction", e)
            throw e
        }
    }

    fun getAllTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions()
            .catch { e -> 
                Log.e(TAG, "getAllTransactions: Error", e)
                emit(emptyList())
            }

    suspend fun getTransactionById(id: Long): Transaction? = 
        try {
            transactionDao.getTransactionById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getTransactionById: Error", e)
            null
        }

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startTime, endTime)
            .catch { e -> 
                Log.e(TAG, "getTransactionsByDateRange: Error", e)
                emit(emptyList())
            }

    fun getTransactionsByType(type: Int): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByType(type)
            .catch { e -> 
                Log.e(TAG, "getTransactionsByType: Error", e)
                emit(emptyList())
            }

    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(categoryId)
            .catch { e -> 
                Log.e(TAG, "getTransactionsByCategory: Error", e)
                emit(emptyList())
            }

    fun getTotalAmountByTypeAndDateRange(type: Int, startTime: Long, endTime: Long): Flow<Double?> = 
        transactionDao.getTotalAmountByTypeAndDateRange(type, startTime, endTime)
            .catch { e -> 
                Log.e(TAG, "getTotalAmountByTypeAndDateRange: Error", e)
                emit(null)
            }

    fun getTotalAmountByType(type: Int): Flow<Double?> = 
        transactionDao.getTotalAmountByType(type)
            .catch { e -> 
                Log.e(TAG, "getTotalAmountByType: Error", e)
                emit(null)
            }

    fun getCategoryTotalsByTypeAndDateRange(
        type: Int,
        startTime: Long,
        endTime: Long
    ): Flow<List<CategoryTotal>> = 
        transactionDao.getCategoryTotalsByTypeAndDateRange(type, startTime, endTime)
            .catch { e -> 
                Log.e(TAG, "getCategoryTotalsByTypeAndDateRange: Error", e)
                emit(emptyList())
            }

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = 
        transactionDao.getRecentTransactions(limit)
            .catch { e -> 
                Log.e(TAG, "getRecentTransactions: Error", e)
                emit(emptyList())
            }

    suspend fun deleteAll() {
        try {
            transactionDao.deleteAll()
        } catch (e: Exception) {
            Log.e(TAG, "deleteAll: Error", e)
            throw e
        }
    }

    fun getDailyTotalsByType(type: Int, startTime: Long, endTime: Long): Flow<List<DailyTotal>> = 
        transactionDao.getDailyTotalsByType(type, startTime, endTime)
            .catch { e -> 
                Log.e(TAG, "getDailyTotalsByType: Error", e)
                emit(emptyList())
            }

    fun getMonthlyTotalsByType(type: Int, startTime: Long, endTime: Long): Flow<List<DailyTotal>> = 
        transactionDao.getMonthlyTotalsByType(type, startTime, endTime)
            .catch { e -> 
                Log.e(TAG, "getMonthlyTotalsByType: Error", e)
                emit(emptyList())
            }

    fun getTransactionsByCategoryAndDateRange(
        categoryId: Long,
        type: Int,
        startTime: Long,
        endTime: Long
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategoryAndDateRange(categoryId, type, startTime, endTime)
            .catch { e ->
                Log.e(TAG, "getTransactionsByCategoryAndDateRange: Error", e)
                emit(emptyList())
            }
}
