package com.example.moneytracker.data.repository

import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.dao.CategoryTotal
import com.example.moneytracker.data.database.dao.DailyTotal
import com.example.moneytracker.data.database.dao.TransactionDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录仓库
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    // 获取所有交易记录
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // 获取最近交易记录
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    // 根据ID获取交易记录
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    // 根据日期范围获取交易记录
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    // 根据类型获取交易记录
    fun getTransactionsByType(type: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    // 根据分类获取交易记录
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)

    // 插入交易记录
    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    // 更新交易记录
    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)

    // 删除交易记录
    suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction)

    // 获取指定类型和日期范围的总金额
    fun getTotalAmountByTypeAndDateRange(type: Int, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalAmountByTypeAndDateRange(type, startTime, endTime)

    // 获取指定类型的总金额
    fun getTotalAmountByType(type: Int): Flow<Double?> =
        transactionDao.getTotalAmountByType(type)

    // 获取分类统计
    fun getCategoryTotalsByTypeAndDateRange(
        type: Int,
        startTime: Long,
        endTime: Long
    ): Flow<List<CategoryTotal>> =
        transactionDao.getCategoryTotalsByTypeAndDateRange(type, startTime, endTime)

    // 获取每日统计
    fun getDailyTotalsByType(type: Int, startTime: Long, endTime: Long): Flow<List<DailyTotal>> =
        transactionDao.getDailyTotalsByType(type, startTime, endTime)

    // 删除所有交易记录
    suspend fun deleteAll() = transactionDao.deleteAll()
}
