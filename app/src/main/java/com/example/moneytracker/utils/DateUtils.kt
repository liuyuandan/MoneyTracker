package com.example.moneytracker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 日期工具类
 */
object DateUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * 格式化日期（只显示日期）
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(timestamp)
    }
    
    /**
     * 格式化日期时间（显示日期和时分）
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(timestamp)
    }
    
    /**
     * 格式化完整日期时间（年月日时分秒）
     */
    fun formatFullDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(timestamp)
    }
    
    /**
     * 格式化短日期（月-日）
     */
    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(timestamp)
    }
    
    /**
     * 格式化月份
     */
    fun formatMonth(timestamp: Long): String {
        return monthFormat.format(timestamp)
    }
    
    /**
     * 格式化时间（只显示时分）
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(timestamp)
    }
    
    /**
     * 获取本月开始时间戳
     */
    fun getMonthStart(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本月结束时间戳
     */
    fun getMonthEnd(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 获取上个月同一时间
     */
    fun getPreviousMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, -1)
        return calendar.timeInMillis
    }
    
    /**
     * 获取下个月同一时间
     */
    fun getNextMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, 1)
        return calendar.timeInMillis
    }

    /**
     * 格式化年份
     */
    fun formatYear(timestamp: Long): String {
        val yearFormat = SimpleDateFormat("yyyy年", Locale.getDefault())
        return yearFormat.format(timestamp)
    }

    /**
     * 获取本年开始时间戳
     */
    fun getYearStart(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 获取本年结束时间戳
     */
    fun getYearEnd(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * 获取上一年同一时间
     */
    fun getPreviousYear(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.YEAR, -1)
        return calendar.timeInMillis
    }

    /**
     * 获取下一年同一时间
     */
    fun getNextYear(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.YEAR, 1)
        return calendar.timeInMillis
    }

    /**
     * 获取指定年份的某个月
     */
    fun getMonthInYear(timestamp: Long, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.MONTH, month) // 0-11
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }
}
