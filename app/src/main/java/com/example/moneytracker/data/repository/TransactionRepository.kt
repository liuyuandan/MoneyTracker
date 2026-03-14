package com.example.moneytracker.data.repository

import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.dao.TransactionDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.CategoryTotal
import com.example.moneytracker.data.database.entities.DailyTotal
import com.example.moneytracker.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录仓库
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    fun update(transaction: Transaction) = transactionDao.update(transaction)

    fun delete(transaction: Transaction) = transactionDao.delete(transaction)

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionById(id: Long) = transactionDao.getTransactionById(id)

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    fun getTransactionsByType(type: Int): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(categoryId)

    fun getTotalAmountByTypeAndDateRange(type: Int, startTime: Long, endTime: Long): Flow<Double?> = 
        transactionDao.getTotalAmountByTypeAndDateRange(type, startTime, endTime)

    fun getTotalAmountByType(type: Int): Flow<Double?> = 
        transactionDao.getTotalAmountByType(type)

    fun getCategoryTotalsByTypeAndDateRange(
        type: Int,
        startTime: Long,
        endTime: Long
    ): Flow<List<CategoryTotal>> = 
        transactionDao.getCategoryTotalsByTypeAndDateRange(type, startTime, endTime)

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = 
        transactionDao.getRecentTransactions(limit)

    fun deleteAll() = transactionDao.deleteAll()

    fun getDailyTotalsByType(type: Int, startTime: Long, endTime: Long): Flow<List<DailyTotal>> = 
        transactionDao.getDailyTotalsByType(type, startTime, endTime)
}
