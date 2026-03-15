package com.example.moneytracker

import android.app.Application
import android.util.Log
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

    companion object {
        private const val TAG = "MoneyTrackerApp"
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Application starting")

        try {
            // 初始化偏好设置
            val prefs = PreferenceManager(this)
            CurrencyUtils.setCurrencySymbol(prefs.getCurrencySymbol())
            Log.d(TAG, "onCreate: Preferences initialized")

            // 初始化默认分类 - 确保在IO线程执行
            applicationScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "onCreate: Initializing default categories")
                    val database = AppDatabase.getDatabase(this@MoneyTrackerApp)
                    AppDatabase.initializeDefaultCategories(database.categoryDao())
                    Log.d(TAG, "onCreate: Default categories initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "onCreate: Error initializing default categories", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error during application initialization", e)
        }
    }
}
