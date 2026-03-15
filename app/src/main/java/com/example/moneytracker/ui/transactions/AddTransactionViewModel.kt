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
import kotlinx.coroutines.Dispatchers
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

    init {
        Log.d(TAG, "init: Initializing ViewModel")
        try {
            val database = AppDatabase.getDatabase(application)
            transactionRepository = TransactionRepository(database.transactionDao())
            categoryRepository = CategoryRepository(database.categoryDao())

            _transactionType.value = Transaction.TYPE_EXPENSE
            _selectedDate.value = System.currentTimeMillis()
            loadCategories()
            Log.d(TAG, "init: ViewModel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "init: Error initializing ViewModel", e)
            throw e
        }
    }

    fun setTransactionType(type: Int) {
        Log.d(TAG, "setTransactionType: type = $type")
        _transactionType.value = type
        _selectedCategory.value = null
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = _transactionType.value ?: Transaction.TYPE_EXPENSE
                Log.d(TAG, "loadCategories: Loading categories for type = $type")
                
                categoryRepository.getCategoriesByType(type).collect { categoryList ->
                    Log.d(TAG, "loadCategories: Loaded ${categoryList.size} categories")
                    _categories.postValue(categoryList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCategories: Error loading categories", e)
                _categories.postValue(emptyList())
            }
        }
    }

    fun setSelectedCategory(category: Category) {
        Log.d(TAG, "setSelectedCategory: category = ${category.name}")
        _selectedCategory.value = category
    }

    fun setSelectedDate(timestamp: Long) {
        Log.d(TAG, "setSelectedDate: timestamp = $timestamp")
        _selectedDate.value = timestamp
    }

    fun saveTransaction(amount: Double, description: String): Boolean {
        Log.d(TAG, "saveTransaction: amount = $amount, description = $description")
        
        val category = _selectedCategory.value
        if (amount <= 0) {
            Log.w(TAG, "saveTransaction: Invalid amount")
            return false
        }
        if (category == null) {
            Log.w(TAG, "saveTransaction: No category selected")
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
                Log.d(TAG, "saveTransaction: Inserting transaction")
                transactionRepository.insert(transaction)
                Log.d(TAG, "saveTransaction: Transaction inserted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "saveTransaction: Error inserting transaction", e)
            }
        }

        return true
    }

    fun updateTransaction(transactionId: Long, amount: Double, description: String): Boolean {
        Log.d(TAG, "updateTransaction: transactionId = $transactionId, amount = $amount")
        
        val category = _selectedCategory.value
        if (amount <= 0 || category == null) {
            Log.w(TAG, "updateTransaction: Invalid parameters")
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
                Log.d(TAG, "updateTransaction: Updating transaction")
                transactionRepository.update(transaction)
                Log.d(TAG, "updateTransaction: Transaction updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "updateTransaction: Error updating transaction", e)
            }
        }

        return true
    }
}
