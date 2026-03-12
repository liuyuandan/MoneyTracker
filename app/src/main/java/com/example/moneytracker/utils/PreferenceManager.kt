package com.example.moneytracker.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 偏好设置管理
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取货币符号
     */
    fun getCurrencySymbol(): String {
        return prefs.getString(KEY_CURRENCY_SYMBOL, "¥") ?: "¥"
    }

    /**
     * 设置货币符号
     */
    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    /**
     * 获取首月日期
     */
    fun getFirstDayOfMonth(): Int {
        return prefs.getInt(KEY_FIRST_DAY_OF_MONTH, 1)
    }

    /**
     * 设置首月日期
     */
    fun setFirstDayOfMonth(day: Int) {
        prefs.edit().putInt(KEY_FIRST_DAY_OF_MONTH, day).apply()
    }

    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 设置已启动
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "money_tracker_prefs"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_FIRST_DAY_OF_MONTH = "first_day_of_month"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
