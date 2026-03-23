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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllTransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val categoryRepository: CategoryRepository

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _categories = MutableLiveData<Map<Long, Category>>()
    val categories: LiveData<Map<Long, Category>> = _categories

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        categoryRepository = CategoryRepository(database.categoryDao())

        loadTransactions()
        loadCategories()
    }

    private fun loadTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getAllTransactions().collect { transactions ->
                    _transactions.postValue(transactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
