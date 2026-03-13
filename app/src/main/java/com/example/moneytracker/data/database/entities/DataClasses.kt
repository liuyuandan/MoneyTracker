package com.example.moneytracker.data.database.entities

import androidx.room.Embedded

/**
 * 分类统计结果
 */
@Embedded
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
@Embedded
data class DailyTotal(
    val day: Long,
    val totalAmount: Double
)
