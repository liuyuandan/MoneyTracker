package com.example.moneytracker.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志工具类 - 将日志写入文件
 */
object FileLogger {
    
    private const val TAG = "FileLogger"
    private const val LOG_DIR = "MoneyTracker"
    private const val LOG_FILE = "app_log.txt"
    
    private var logFile: File? = null
    private var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 初始化日志文件
     */
    fun init(context: Context) {
        try {
            // 使用应用专属外部存储目录，不需要额外权限
            val dir = File(context.getExternalFilesDir(null), LOG_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            logFile = File(dir, LOG_FILE)
            
            // 写入启动日志
            log("App", "========== 应用启动 ==========")
            log("App", "日志文件路径: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "init: Error initializing file logger", e)
        }
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? = logFile?.absolutePath
    
    /**
     * 写入日志
     */
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] [$tag] $message\n"
        
        Log.d(tag, message)
        
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logLine)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "log: Error writing to log file", e)
        }
    }
    
    /**
     * 记录错误日志
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val sb = StringBuilder()
        sb.append("[$timestamp] [$tag] ERROR: $message\n")
        
        throwable?.let {
            sb.append("Stack trace:\n")
            val stringWriter = java.io.StringWriter()
            val printWriter = PrintWriter(stringWriter)
            it.printStackTrace(printWriter)
            sb.append(stringWriter.toString())
            sb.append("\n")
        }
        
        Log.e(tag, message, throwable)
        
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(sb.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "logError: Error writing to log file", e)
        }
    }
    
    /**
     * 清空日志文件
     */
    fun clearLog() {
        try {
            logFile?.delete()
            logFile?.createNewFile()
        } catch (e: Exception) {
            Log.e(TAG, "clearLog: Error clearing log file", e)
        }
    }
    
    /**
     * 获取日志内容
     */
    fun getLogContent(): String {
        return try {
            logFile?.readText() ?: "日志文件不存在"
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }
}
