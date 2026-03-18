package com.example.moneytracker.ui.transactions

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AddTransactionViewModel"
    }

    private val transactionRepository: TransactionRepository
    private val categoryRepository: CategoryRepository

    private val _transactionType = MutableLiveData<Int>()
    val transactionType: LiveData<Int> = _transactionType

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _selectedCategory = MutableLiveData<Category?>()
    val selectedCategory: LiveData<Category?> = _selectedCategory

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    private val _loadedTransaction = MutableLiveData<Transaction?>()
    val loadedTransaction: LiveData<Transaction?> = _loadedTransaction

    init {
        FileLogger.log(TAG, "init: Initializing ViewModel")
        try {
            val database = AppDatabase.getDatabase(application)
            transactionRepository = TransactionRepository(database.transactionDao())
            categoryRepository = CategoryRepository(database.categoryDao())

            _transactionType.value = Transaction.TYPE_EXPENSE
            _selectedDate.value = System.currentTimeMillis()
            loadCategories()
            FileLogger.log(TAG, "init: ViewModel initialized successfully")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "init: Error initializing ViewModel", e)
            throw e
        }
    }

    fun setTransactionType(type: Int) {
        FileLogger.log(TAG, "setTransactionType: type = $type")
        _transactionType.value = type
        _selectedCategory.value = null
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = _transactionType.value ?: Transaction.TYPE_EXPENSE
                FileLogger.log(TAG, "loadCategories: Loading categories for type = $type")
                
                categoryRepository.getCategoriesByType(type).collect { categoryList ->
                    FileLogger.log(TAG, "loadCategories: Loaded ${categoryList.size} categories")
                    _categories.postValue(categoryList)
                }
            } catch (e: Exception) {
                FileLogger.logError(TAG, "loadCategories: Error loading categories", e)
                _categories.postValue(emptyList())
            }
        }
    }

    fun setSelectedCategory(category: Category) {
        FileLogger.log(TAG, "setSelectedCategory: category = ${category.name}")
        _selectedCategory.value = category
    }

    fun setSelectedDate(timestamp: Long) {
        FileLogger.log(TAG, "setSelectedDate: timestamp = $timestamp")
        _selectedDate.value = timestamp
    }

    fun saveTransaction(amount: Double, description: String): Boolean {
        FileLogger.log(TAG, "saveTransaction: amount = $amount, description = $description")
        
        val category = _selectedCategory.value
        if (amount <= 0) {
            FileLogger.log(TAG, "saveTransaction: Invalid amount")
            return false
        }
        if (category == null) {
            FileLogger.log(TAG, "saveTransaction: No category selected")
            return false
        }

        val transaction = Transaction(
            amount = amount,
            type = _transactionType.value ?: Transaction.TYPE_EXPENSE,
            categoryId = category.id,
            description = description,
            date = _selectedDate.value ?: System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FileLogger.log(TAG, "saveTransaction: Inserting transaction")
                transactionRepository.insert(transaction)
                FileLogger.log(TAG, "saveTransaction: Transaction inserted successfully")
            } catch (e: Exception) {
                FileLogger.logError(TAG, "saveTransaction: Error inserting transaction", e)
            }
        }

        return true
    }

    fun updateTransaction(transactionId: Long, amount: Double, description: String): Boolean {
        FileLogger.log(TAG, "updateTransaction: transactionId = $transactionId, amount = $amount")
        
        val category = _selectedCategory.value
        if (amount <= 0 || category == null) {
            FileLogger.log(TAG, "updateTransaction: Invalid parameters")
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FileLogger.log(TAG, "updateTransaction: Updating transaction")
                transactionRepository.update(transaction)
                FileLogger.log(TAG, "updateTransaction: Transaction updated successfully")
            } catch (e: Exception) {
                FileLogger.logError(TAG, "updateTransaction: Error updating transaction", e)
            }
        }

        return true
    }

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transaction = transactionRepository.getTransactionById(transactionId)
                transaction?.let {
                    _transactionType.postValue(it.type)
                    _selectedDate.postValue(it.date)
                    _loadedTransaction.postValue(it)
                    // 先获取分类列表的第一个值
                    val categoryList = categoryRepository.getCategoriesByType(it.type).first()
                    _categories.postValue(categoryList)
                    // 然后设置选中的分类
                    val category = categoryRepository.getCategoryById(it.categoryId)
                    _selectedCategory.postValue(category)
                    FileLogger.log(TAG, "loadTransaction: Transaction loaded, type = ${it.type}, categoryId = ${it.categoryId}, categories size = ${categoryList.size}")
                }
            } catch (e: Exception) {
                FileLogger.logError(TAG, "loadTransaction: Error loading transaction", e)
            }
        }
    }
}
