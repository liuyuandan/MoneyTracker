package com.example.moneytracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 交易记录实体类
 * @param id 主键，自增
 * @param amount 金额
 * @param type 类型：0=支出，1=收入
 * @param categoryId 分类ID
 * @param description 备注
 * @param date 交易日期时间戳
 * @param createdAt 创建时间
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: Int, // 0 = 支出, 1 = 收入
    val categoryId: Long,
    val description: String = "",
    val date: Long, // 时间戳
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1
    }

    fun isExpense(): Boolean = type == TYPE_EXPENSE
    fun isIncome(): Boolean = type == TYPE_INCOME
}
