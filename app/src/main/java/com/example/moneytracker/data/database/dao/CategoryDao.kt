package com.example.moneytracker.data.database.dao

import androidx.room.*
import com.example.moneytracker.data.database.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: Int): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    fun getDefaultCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteCustomCategories()

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByNameAndType(name: String, type: Int): Category?
}
