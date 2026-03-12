package com.example.moneytracker.data.database.dao

import androidx.room.*
import com.example.moneytracker.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startTime AND :endTime")
    fun getTotalAmountByTypeAndDateRange(type: Int, startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalAmountByType(type: Int): Flow<Double?>

    @Query("""
        SELECT c.id, c.name, c.icon, c.color, c.type, c.isDefault, SUM(t.amount) as totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type AND t.date BETWEEN :startTime AND :endTime
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategoryTotalsByTypeAndDateRange(type: Int, startTime: Long, endTime: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // 获取指定月份的每日收支统计
    @Query("""
        SELECT date/86400000 as day, SUM(amount) as totalAmount
        FROM transactions
        WHERE type = :type AND date BETWEEN :startTime AND :endTime
        GROUP BY day
        ORDER BY day
    """)
    fun getDailyTotalsByType(type: Int, startTime: Long, endTime: Long): Flow<List<DailyTotal>>
}

/**
 * 分类统计结果
 */
data class CategoryTotal(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Int,
    val type: Int,
    val isDefault: Boolean,
    val totalAmount: Double
)

/**
 * 每日统计结果
 */
data class DailyTotal(
    val day: Long,
    val totalAmount: Double
)
