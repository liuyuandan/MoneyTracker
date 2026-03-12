package com.example.moneytracker

import android.app.Application
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用程序入口类
 */
class MoneyTrackerApp : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 初始化偏好设置
        val prefs = PreferenceManager(this)
        CurrencyUtils.setCurrencySymbol(prefs.getCurrencySymbol())

        // 初始化默认分类
        applicationScope.launch {
            val database = AppDatabase.getDatabase(this@MoneyTrackerApp)
            AppDatabase.initializeDefaultCategories(database.categoryDao())
        }
    }
}
