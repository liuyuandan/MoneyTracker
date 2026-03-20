package com.example.moneytracker.data.repository

import android.util.Log
import com.example.moneytracker.data.database.dao.CategoryDao
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * 分类仓库
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    companion object {
        private const val TAG = "CategoryRepository"
    }

    fun getAllCategories(): Flow<List<Category>> = 
        categoryDao.getAllCategories()
            .catch { e -> 
                Log.e(TAG, "getAllCategories: Error", e)
                emit(emptyList())
            }

    fun getCategoriesByType(type: Int): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)
            .catch { e -> 
                Log.e(TAG, "getCategoriesByType: Error", e)
                emit(emptyList())
            }

    suspend fun getCategoryById(id: Long): Category? = 
        try {
            categoryDao.getCategoryById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryById: Error", e)
            null
        }

    suspend fun insert(category: Category): Long = 
        try {
            categoryDao.insert(category)
        } catch (e: Exception) {
            Log.e(TAG, "insert: Error", e)
            throw e
        }

    suspend fun update(category: Category) {
        try {
            categoryDao.update(category)
        } catch (e: Exception) {
            Log.e(TAG, "update: Error", e)
            throw e
        }
    }

    suspend fun delete(category: Category) {
        try {
            categoryDao.delete(category)
        } catch (e: Exception) {
            Log.e(TAG, "delete: Error", e)
            throw e
        }
    }

    suspend fun deleteCustomCategories() {
        try {
            categoryDao.deleteCustomCategories()
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomCategories: Error", e)
            throw e
        }
    }

    suspend fun initializeDefaultCategories() {
        try {
            val count = categoryDao.getCategoryCount()
            if (count == 0) {
                Log.d(TAG, "initializeDefaultCategories: Inserting default categories")
                categoryDao.insertAll(DefaultCategories.getAllDefaultCategories())
            }
        } catch (e: Exception) {
            Log.e(TAG, "initializeDefaultCategories: Error", e)
            throw e
        }
    }

    suspend fun getCategoryByNameAndType(name: String, type: Int): Category? =
        try {
            categoryDao.getCategoryByNameAndType(name, type)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryByNameAndType: Error", e)
            null
        }

    suspend fun getCategoryCount(): Int = 
        try {
            categoryDao.getCategoryCount()
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryCount: Error", e)
            0
        }

    suspend fun updateSortOrder(categories: List<Category>) {
        try {
            categoryDao.updateSortOrderAll(categories)
        } catch (e: Exception) {
            Log.e(TAG, "updateSortOrder: Error", e)
        }
    }
}
