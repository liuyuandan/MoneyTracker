package com.example.moneytracker.ui.statistics

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryTransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val categoryRepository: CategoryRepository

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _totalAmount = MutableLiveData(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    private val _categories = MutableLiveData<Map<Long, Category>>()
    val categories: LiveData<Map<Long, Category>> = _categories

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        categoryRepository = CategoryRepository(database.categoryDao())

        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                categoryRepository.getAllCategories().collect { categoryList ->
                    val categoryMap = categoryList.associateBy { it.id }
                    _categories.postValue(categoryMap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadTransactions(categoryId: Long, transactionType: Int, startTime: Long, endTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getTransactionsByCategoryAndDateRange(
                    categoryId,
                    transactionType,
                    startTime,
                    endTime
                ).collect { transactionList ->
                    _transactions.postValue(transactionList)
                    
                    // 计算总金额
                    val total = transactionList.sumOf { it.amount }
                    _totalAmount.postValue(total)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.delete(transaction)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
