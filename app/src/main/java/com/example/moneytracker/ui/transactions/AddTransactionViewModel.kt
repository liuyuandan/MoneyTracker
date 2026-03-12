package com.example.moneytracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.utils.DateUtils
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val categoryRepository: CategoryRepository

    // 交易类型
    private val _transactionType = MutableLiveData<Int>()
    val transactionType: LiveData<Int> = _transactionType

    // 分类列表
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // 选中的分类
    private val _selectedCategory = MutableLiveData<Category?>()
    val selectedCategory: LiveData<Category?> = _selectedCategory

    // 选中的日期
    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        categoryRepository = CategoryRepository(database.categoryDao())

        _transactionType.value = Transaction.TYPE_EXPENSE
        _selectedDate.value = System.currentTimeMillis()
        loadCategories()
    }

    fun setTransactionType(type: Int) {
        _transactionType.value = type
        _selectedCategory.value = null
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val type = _transactionType.value ?: Transaction.TYPE_EXPENSE
            categoryRepository.getCategoriesByType(type).collect { categoryList ->
                _categories.postValue(categoryList)
            }
        }
    }

    fun setSelectedCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun setSelectedDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    fun saveTransaction(
        amount: Double,
        description: String
    ): Boolean {
        val category = _selectedCategory.value
        if (amount <= 0) {
            return false
        }
        if (category == null) {
            return false
        }

        val transaction = Transaction(
            amount = amount,
            type = _transactionType.value ?: Transaction.TYPE_EXPENSE,
            categoryId = category.id,
            description = description,
            date = _selectedDate.value ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            transactionRepository.insert(transaction)
        }

        return true
    }

    fun updateTransaction(
        transactionId: Long,
        amount: Double,
        description: String
    ): Boolean {
        val category = _selectedCategory.value
        if (amount <= 0 || category == null) {
            return false
        }

        val transaction = Transaction(
            id = transactionId,
            amount = amount,
            type = _transactionType.value ?: Transaction.TYPE_EXPENSE,
            categoryId = category.id,
            description = description,
            date = _selectedDate.value ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            transactionRepository.update(transaction)
        }

        return true
    }
}
