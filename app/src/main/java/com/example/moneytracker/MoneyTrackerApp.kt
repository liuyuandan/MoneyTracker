package com.example.moneytracker

import android.app.Application
import android.util.Log
import com.example.moneytracker.data.database.AppDatabase
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.FileLogger
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
        
        // 初始化文件日志
        FileLogger.init(this)
        FileLogger.log(TAG, "========== 应用启动 ==========")
        FileLogger.log(TAG, "onCreate: Application starting")

        // 设置全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            FileLogger.logError(TAG, "未捕获的异常导致崩溃", throwable)
            Log.e(TAG, "Uncaught exception", throwable)
        }

        try {
            // 初始化偏好设置
            val prefs = PreferenceManager(this)
            CurrencyUtils.setCurrencySymbol(prefs.getCurrencySymbol())
            FileLogger.log(TAG, "onCreate: Preferences initialized")

            // 初始化默认分类 - 确保在IO线程执行
            applicationScope.launch(Dispatchers.IO) {
                try {
                    FileLogger.log(TAG, "onCreate: Initializing default categories")
                    val database = AppDatabase.getDatabase(this@MoneyTrackerApp)
                    AppDatabase.initializeDefaultCategories(database.categoryDao())
                    FileLogger.log(TAG, "onCreate: Default categories initialized successfully")
                } catch (e: Exception) {
                    FileLogger.logError(TAG, "onCreate: Error initializing default categories", e)
                }
            }
            
            FileLogger.log(TAG, "onCreate: Application initialized successfully")
            FileLogger.log(TAG, "日志文件路径: ${FileLogger.getLogFilePath()}")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error during application initialization", e)
        }
    }
}
