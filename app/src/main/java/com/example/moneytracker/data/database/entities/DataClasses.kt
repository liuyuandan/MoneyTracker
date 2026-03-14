package com.example.moneytracker.data.database.entities

/**
 * 分类统计结果
 * 用于DAO查询返回的统计结果
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
 * 用于DAO查询返回的统计结果
 */
data class DailyTotal(
    val day: Long,
    val totalAmount: Double
)
