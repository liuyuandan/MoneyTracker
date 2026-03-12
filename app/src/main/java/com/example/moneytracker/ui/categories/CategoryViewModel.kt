package com.example.moneytracker.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.repository.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository

    // 分类列表
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // 当前类型
    private val _currentType = MutableLiveData<Int>()
    val currentType: LiveData<Int> = _currentType

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CategoryRepository(database.categoryDao())

        _currentType.value = Category.TYPE_EXPENSE
        loadCategories()
    }

    fun setType(type: Int) {
        _currentType.value = type
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val type = _currentType.value ?: Category.TYPE_EXPENSE
            repository.getCategoriesByType(type).collect { categoryList ->
                _categories.postValue(categoryList)
            }
        }
    }

    fun addCategory(name: String, icon: String, color: Int) {
        viewModelScope.launch {
            val type = _currentType.value ?: Category.TYPE_EXPENSE
            val category = Category(
                name = name,
                icon = icon,
                color = color,
                type = type,
                isDefault = false
            )
            repository.insert(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.update(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (!category.isDefault) {
                repository.delete(category)
            }
        }
    }
}
