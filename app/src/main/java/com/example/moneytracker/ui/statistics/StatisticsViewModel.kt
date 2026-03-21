package com.example.moneytracker.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.database.entities.CategoryTotal
import com.example.moneytracker.data.database.entities.DailyTotal
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository

    // 视图模式：true=年度，false=月度
    private val _isYearView = MutableLiveData(false)
    val isYearView: LiveData<Boolean> = _isYearView

    // 当前时间戳
    private var currentTimestamp: Long = System.currentTimeMillis()

    // 当前月份/年份显示
    private val _currentPeriod = MutableLiveData<String>()
    val currentPeriod: LiveData<String> = _currentPeriod

    // 本期收入
    private val _periodIncome = MutableLiveData(0.0)
    val periodIncome: LiveData<Double> = _periodIncome

    // 本期支出
    private val _periodExpense = MutableLiveData(0.0)
    val periodExpense: LiveData<Double> = _periodExpense

    // 本期结余 - 使用 MediatorLiveData 自动计算
    private val _periodBalance = MediatorLiveData<Double>()
    val periodBalance: LiveData<Double> = _periodBalance

    // 支出分类统计
    private val _expenseCategoryTotals = MutableLiveData<List<CategoryTotal>>()
    val expenseCategoryTotals: LiveData<List<CategoryTotal>> = _expenseCategoryTotals

    // 收入分类统计
    private val _incomeCategoryTotals = MutableLiveData<List<CategoryTotal>>()
    val incomeCategoryTotals: LiveData<List<CategoryTotal>> = _incomeCategoryTotals

    // 每日/月支出统计
    private val _periodExpenseTotals = MutableLiveData<List<DailyTotal>>()
    val periodExpenseTotals: LiveData<List<DailyTotal>> = _periodExpenseTotals

    // 每日/月收入统计
    private val _periodIncomeTotals = MutableLiveData<List<DailyTotal>>()
    val periodIncomeTotals: LiveData<List<DailyTotal>> = _periodIncomeTotals

    // 兼容旧代码
    val currentMonth: LiveData<String> = _currentPeriod
    val monthlyIncome: LiveData<Double> = _periodIncome
    val monthlyExpense: LiveData<Double> = _periodExpense
    val monthlyBalance: LiveData<Double> = _periodBalance
    val dailyExpenseTotals: LiveData<List<DailyTotal>> = _periodExpenseTotals
    val dailyIncomeTotals: LiveData<List<DailyTotal>> = _periodIncomeTotals

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())

        loadData()
    }

    private fun loadData() {
        val isYear = _isYearView.value ?: false
        val startTime: Long
        val endTime: Long

        if (isYear) {
            startTime = DateUtils.getYearStart(currentTimestamp)
            endTime = DateUtils.getYearEnd(currentTimestamp)
            _currentPeriod.value = DateUtils.formatYear(currentTimestamp)
        } else {
            startTime = DateUtils.getMonthStart(currentTimestamp)
            endTime = DateUtils.getMonthEnd(currentTimestamp)
            _currentPeriod.value = DateUtils.formatMonth(currentTimestamp)
        }

        // 加载收支总额
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getTotalAmountByTypeAndDateRange(
                    Transaction.TYPE_INCOME,
                    startTime,
                    endTime
                ).collect { income ->
                    _periodIncome.postValue(income ?: 0.0)
                    updateBalance()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getTotalAmountByTypeAndDateRange(
                    Transaction.TYPE_EXPENSE,
                    startTime,
                    endTime
                ).collect { expense ->
                    _periodExpense.postValue(expense ?: 0.0)
                    updateBalance()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 加载分类统计
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getCategoryTotalsByTypeAndDateRange(
                    Transaction.TYPE_EXPENSE,
                    startTime,
                    endTime
                ).collect { totals ->
                    _expenseCategoryTotals.postValue(totals)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.getCategoryTotalsByTypeAndDateRange(
                    Transaction.TYPE_INCOME,
                    startTime,
                    endTime
                ).collect { totals ->
                    _incomeCategoryTotals.postValue(totals)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 加载每日/月统计
        if (isYear) {
            // 年度视图：按月统计
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    transactionRepository.getMonthlyTotalsByType(
                        Transaction.TYPE_EXPENSE,
                        startTime,
                        endTime
                    ).collect { totals ->
                        _periodExpenseTotals.postValue(totals)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    transactionRepository.getMonthlyTotalsByType(
                        Transaction.TYPE_INCOME,
                        startTime,
                        endTime
                    ).collect { totals ->
                        _periodIncomeTotals.postValue(totals)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // 月度视图：按日统计
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    transactionRepository.getDailyTotalsByType(
                        Transaction.TYPE_EXPENSE,
                        startTime,
                        endTime
                    ).collect { totals ->
                        _periodExpenseTotals.postValue(totals)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    transactionRepository.getDailyTotalsByType(
                        Transaction.TYPE_INCOME,
                        startTime,
                        endTime
                    ).collect { totals ->
                        _periodIncomeTotals.postValue(totals)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateBalance() {
        val income = _periodIncome.value ?: 0.0
        val expense = _periodExpense.value ?: 0.0
        _periodBalance.postValue(income - expense)
    }

    fun toggleViewMode() {
        _isYearView.value = !(_isYearView.value ?: false)
        loadData()
    }

    fun setViewMode(isYear: Boolean) {
        if (_isYearView.value != isYear) {
            _isYearView.value = isYear
            loadData()
        }
    }

    fun goToPreviousPeriod() {
        val isYear = _isYearView.value ?: false
        currentTimestamp = if (isYear) {
            DateUtils.getPreviousYear(currentTimestamp)
        } else {
            DateUtils.getPreviousMonth(currentTimestamp)
        }
        loadData()
    }

    fun goToNextPeriod() {
        val isYear = _isYearView.value ?: false
        currentTimestamp = if (isYear) {
            DateUtils.getNextYear(currentTimestamp)
        } else {
            DateUtils.getNextMonth(currentTimestamp)
        }
        loadData()
    }

    // 兼容旧代码
    fun goToPreviousMonth() = goToPreviousPeriod()
    fun goToNextMonth() = goToNextPeriod()
}
