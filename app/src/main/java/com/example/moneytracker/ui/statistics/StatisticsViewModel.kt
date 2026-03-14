package com.example.moneytracker.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.CategoryTotal
import com.example.moneytracker.data.database.entities.DailyTotal
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.utils.DateUtils
import kotlinx.coroutines.launch

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository

    // 当前时间戳
    private var currentTimestamp: Long = System.currentTimeMillis()

    // 当前月份显示
    private val _currentMonth = MutableLiveData<String>()
    val currentMonth: LiveData<String> = _currentMonth

    // 本月收入
    private val _monthlyIncome = MutableLiveData<Double>()
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    // 本月支出
    private val _monthlyExpense = MutableLiveData<Double>()
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    // 本月结余
    private val _monthlyBalance = MutableLiveData<Double>()
    val monthlyBalance: LiveData<Double> = _monthlyBalance

    // 支出分类统计
    private val _expenseCategoryTotals = MutableLiveData<List<CategoryTotal>>()
    val expenseCategoryTotals: LiveData<List<CategoryTotal>> = _expenseCategoryTotals

    // 收入分类统计
    private val _incomeCategoryTotals = MutableLiveData<List<CategoryTotal>>()
    val incomeCategoryTotals: LiveData<List<CategoryTotal>> = _incomeCategoryTotals

    // 每日支出统计
    private val _dailyExpenseTotals = MutableLiveData<List<DailyTotal>>()
    val dailyExpenseTotals: LiveData<List<DailyTotal>> = _dailyExpenseTotals

    // 每日收入统计
    private val _dailyIncomeTotals = MutableLiveData<List<DailyTotal>>()
    val dailyIncomeTotals: LiveData<List<DailyTotal>> = _dailyIncomeTotals

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())

        loadData()
    }

    private fun loadData() {
        val monthStart = DateUtils.getMonthStart(currentTimestamp)
        val monthEnd = DateUtils.getMonthEnd(currentTimestamp)

        _currentMonth.value = DateUtils.formatMonth(currentTimestamp)

        // 加载收支总额
        viewModelScope.launch {
            transactionRepository.getTotalAmountByTypeAndDateRange(
                Transaction.TYPE_INCOME,
                monthStart,
                monthEnd
            ).collect { income ->
                _monthlyIncome.postValue(income ?: 0.0)
                updateBalance()
            }
        }

        viewModelScope.launch {
            transactionRepository.getTotalAmountByTypeAndDateRange(
                Transaction.TYPE_EXPENSE,
                monthStart,
                monthEnd
            ).collect { expense ->
                _monthlyExpense.postValue(expense ?: 0.0)
                updateBalance()
            }
        }

        // 加载分类统计
        viewModelScope.launch {
            transactionRepository.getCategoryTotalsByTypeAndDateRange(
                Transaction.TYPE_EXPENSE,
                monthStart,
                monthEnd
            ).collect { totals ->
                _expenseCategoryTotals.postValue(totals)
            }
        }

        viewModelScope.launch {
            transactionRepository.getCategoryTotalsByTypeAndDateRange(
                Transaction.TYPE_INCOME,
                monthStart,
                monthEnd
            ).collect { totals ->
                _incomeCategoryTotals.postValue(totals)
            }
        }

        // 加载每日统计
        viewModelScope.launch {
            transactionRepository.getDailyTotalsByType(
                Transaction.TYPE_EXPENSE,
                monthStart,
                monthEnd
            ).collect { totals ->
                _dailyExpenseTotals.postValue(totals)
            }
        }

        viewModelScope.launch {
            transactionRepository.getDailyTotalsByType(
                Transaction.TYPE_INCOME,
                monthStart,
                monthEnd
            ).collect { totals ->
                _dailyIncomeTotals.postValue(totals)
            }
        }
    }

    private fun updateBalance() {
        val income = _monthlyIncome.value ?: 0.0
        val expense = _monthlyExpense.value ?: 0.0
        _monthlyBalance.postValue(income - expense)
    }

    fun goToPreviousMonth() {
        currentTimestamp = DateUtils.getPreviousMonth(currentTimestamp)
        loadData()
    }

    fun goToNextMonth() {
        currentTimestamp = DateUtils.getNextMonth(currentTimestamp)
        loadData()
    }
}
