package com.example.moneytracker.data.repository

import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.DefaultCategories
import kotlinx.coroutines.flow.Flow

/**
 * 分类仓库
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    // 获取所有分类
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    // 根据类型获取分类
    fun getCategoriesByType(type: Int): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    // 根据ID获取分类
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    // 插入分类
    suspend fun insert(category: Category): Long = categoryDao.insert(category)

    // 更新分类
    suspend fun update(category: Category) = categoryDao.update(category)

    // 删除分类
    suspend fun delete(category: Category) = categoryDao.delete(category)

    // 删除所有自定义分类
    suspend fun deleteCustomCategories() = categoryDao.deleteCustomCategories()

    // 初始化默认分类
    suspend fun initializeDefaultCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            categoryDao.insertAll(DefaultCategories.getAllDefaultCategories())
        }
    }

    // 根据名称和类型查找分类
    suspend fun getCategoryByNameAndType(name: String, type: Int): Category? =
        categoryDao.getCategoryByNameAndType(name, type)

    // 获取分类数量
    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()
}
