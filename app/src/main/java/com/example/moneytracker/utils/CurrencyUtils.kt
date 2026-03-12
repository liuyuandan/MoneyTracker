package com.example.moneytracker.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * 货币格式化工具
 */
object CurrencyUtils {

    private val formatter = DecimalFormat("#,##0.00")
    private var currencySymbol = "¥"

    /**
     * 设置货币符号
     */
    fun setCurrencySymbol(symbol: String) {
        currencySymbol = symbol
    }

    /**
     * 格式化金额显示
     */
    fun format(amount: Double): String {
        return "$currencySymbol${formatter.format(amount)}"
    }

    /**
     * 格式化金额显示（带正负号）
     */
    fun formatWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign$currencySymbol${formatter.format(kotlin.math.abs(amount))}"
    }

    /**
     * 解析金额字符串
     */
    fun parse(formattedAmount: String): Double {
        return try {
            formattedAmount
                .replace(currencySymbol, "")
                .replace(",", "")
                .replace(" ", "")
                .toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 格式化简单金额（无符号）
     */
    fun formatSimple(amount: Double): String {
        return formatter.format(amount)
    }
}
