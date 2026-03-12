package com.example.moneytracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类
 * @param id 主键，自增
 * @param name 分类名称
 * @param icon 图标资源名
 * @param color 颜色值
 * @param type 类型：0=支出，1=收入
 * @param isDefault 是否为预设分类
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val type: Int, // 0 = 支出, 1 = 收入
    val isDefault: Boolean = false
) {
    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1
    }

    fun isExpense(): Boolean = type == TYPE_EXPENSE
    fun isIncome(): Boolean = type == TYPE_INCOME
}

/**
 * 预设分类数据
 */
object DefaultCategories {

    // 支出分类
    val expenseCategories = listOf(
        Category(name = "餐饮", icon = "ic_restaurant", color = 0xFFFF9800.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "交通", icon = "ic_transport", color = 0xFF2196F3.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "购物", icon = "ic_shopping", color = 0xFFE91E63.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "娱乐", icon = "ic_entertainment", color = 0xFF9C27B0.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "医疗", icon = "ic_medical", color = 0xFFF44336.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "教育", icon = "ic_education", color = 0xFF00BCD4.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "居住", icon = "ic_home", color = 0xFF795548.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "通讯", icon = "ic_communication", color = 0xFF607D8B.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "服饰", icon = "ic_clothing", color = 0xFFFFEB3B.toInt(), type = Category.TYPE_EXPENSE, isDefault = true),
        Category(name = "其他支出", icon = "ic_other_expense", color = 0xFF9E9E9E.toInt(), type = Category.TYPE_EXPENSE, isDefault = true)
    )

    // 收入分类
    val incomeCategories = listOf(
        Category(name = "工资", icon = "ic_salary", color = 0xFF4CAF50.toInt(), type = Category.TYPE_INCOME, isDefault = true),
        Category(name = "兼职", icon = "ic_parttime", color = 0xFF8BC34A.toInt(), type = Category.TYPE_INCOME, isDefault = true),
        Category(name = "投资", icon = "ic_investment", color = 0xFF00BCD4.toInt(), type = Category.TYPE_INCOME, isDefault = true),
        Category(name = "红包", icon = "ic_bonus", color = 0xFFF44336.toInt(), type = Category.TYPE_INCOME, isDefault = true),
        Category(name = "其他收入", icon = "ic_other_income", color = 0xFF9E9E9E.toInt(), type = Category.TYPE_INCOME, isDefault = true)
    )

    fun getAllDefaultCategories(): List<Category> = expenseCategories + incomeCategories
}
