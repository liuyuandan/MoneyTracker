package com.example.moneytracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.utils.DateUtils
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val categoryRepository: CategoryRepository

    // 本月时间范围
    private var currentMonthStart: Long = DateUtils.getMonthStart()
    private var currentMonthEnd: Long = DateUtils.getMonthEnd()

    // 本月收入
    private val _monthlyIncome = MutableLiveData<Double>()
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    // 本月支出
    private val _monthlyExpense = MutableLiveData<Double>()
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    // 本月结余
    private val _monthlyBalance = MutableLiveData<Double>()
    val monthlyBalance: LiveData<Double> = _monthlyBalance

    // 当前月份显示
    private val _currentMonth = MutableLiveData<String>()
    val currentMonth: LiveData<String> = _currentMonth

    // 最近交易记录
    private val _recentTransactions = MutableLiveData<List<Transaction>>()
    val recentTransactions: LiveData<List<Transaction>> = _recentTransactions

    // 分类映射
    private val _categories = MutableLiveData<Map<Long, Category>>()
    val categories: LiveData<Map<Long, Category>> = _categories

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        categoryRepository = CategoryRepository(database.categoryDao())

        loadCurrentMonth()
        loadCategories()
        loadRecentTransactions()
    }

    private fun loadCurrentMonth() {
        _currentMonth.value = DateUtils.formatMonth(System.currentTimeMillis())

        viewModelScope.launch {
            transactionRepository.getTotalAmountByTypeAndDateRange(
                Transaction.TYPE_INCOME,
                currentMonthStart,
                currentMonthEnd
            ).collect { income ->
                _monthlyIncome.postValue(income ?: 0.0)
                updateBalance()
            }
        }

        viewModelScope.launch {
            transactionRepository.getTotalAmountByTypeAndDateRange(
                Transaction.TYPE_EXPENSE,
                currentMonthStart,
                currentMonthEnd
            ).collect { expense ->
                _monthlyExpense.postValue(expense ?: 0.0)
                updateBalance()
            }
        }
    }

    private fun updateBalance() {
        val income = _monthlyIncome.value ?: 0.0
        val expense = _monthlyExpense.value ?: 0.0
        _monthlyBalance.postValue(income - expense)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categoryList ->
                val categoryMap = categoryList.associateBy { it.id }
                _categories.postValue(categoryMap)
            }
        }
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            transactionRepository.getRecentTransactions(20).collect { transactions ->
                _recentTransactions.postValue(transactions)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }
}
