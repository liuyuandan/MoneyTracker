package com.example.moneytracker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 */
object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"
    private const val MONTH_FORMAT = "yyyy年MM月"
    private const val SHORT_DATE_FORMAT = "MM-dd"

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
    private val monthFormatter = SimpleDateFormat(MONTH_FORMAT, Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat(SHORT_DATE_FORMAT, Locale.getDefault())

    /**
     * 获取当前时间戳
     */
    fun now(): Long = System.currentTimeMillis()

    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormatter.format(Date(timestamp))
    }

    /**
     * 格式化月份
     */
    fun formatMonth(timestamp: Long): String {
        return monthFormatter.format(Date(timestamp))
    }

    /**
     * 格式化短日期
     */
    fun formatShortDate(timestamp: Long): String {
        return shortDateFormatter.format(Date(timestamp))
    }

    /**
     * 解析日期字符串
     */
    fun parseDate(dateString: String): Long {
        return try {
            dateFormatter.parse(dateString)?.time ?: now()
        } catch (e: Exception) {
            now()
        }
    }

    /**
     * 获取月份开始时间戳
     */
    fun getMonthStart(timestamp: Long = now()): Long {
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
     * 获取月份结束时间戳
     */
    fun getMonthEnd(timestamp: Long = now()): Long {
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
     * 获取上个月同一天的时间戳
     */
    fun getPreviousMonth(timestamp: Long = now()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, -1)
        return calendar.timeInMillis
    }

    /**
     * 获取下个月同一天的时间戳
     */
    fun getNextMonth(timestamp: Long = now()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, 1)
        return calendar.timeInMillis
    }

    /**
     * 获取当前月份的天数
     */
    fun getDaysInMonth(timestamp: Long = now()): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取年份
     */
    fun getYear(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR)
    }

    /**
     * 获取月份
     */
    fun getMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) + 1
    }

    /**
     * 获取日
     */
    fun getDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_MONTH)
    }
}
