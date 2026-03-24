package com.example.moneytracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.children
import com.example.moneytracker.R
import java.io.File

/**
 * 主题管理工具类
 * 管理主题色和背景图片的二选一逻辑
 */
object ThemeManager {
    
    const val MODE_NONE = 0
    const val MODE_COLOR = 1
    const val MODE_BACKGROUND = 2
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_COLOR_INDEX = "theme_color_index"
    private const val KEY_BACKGROUND_URI = "background_uri"
    
    // 主题色选项 - 使用浅色渐变，确保文字可读
    val THEME_COLORS = listOf(
        intArrayOf(0xFFE3F2FD.toInt(), 0xFFBBDEFB.toInt()),  // 蓝色浅渐变
        intArrayOf(0xFFF3E5F5.toInt(), 0xFFE1BEE7.toInt()),  // 紫色浅渐变
        intArrayOf(0xFFE8F5E9.toInt(), 0xFFC8E6C9.toInt()),  // 绿色浅渐变
        intArrayOf(0xFFFFF3E0.toInt(), 0xFFFFE0B2.toInt()),  // 橙色浅渐变
        intArrayOf(0xFFFFEBEE.toInt(), 0xFFFFCDD2.toInt())   // 红色浅渐变
    )
    
    // 深色主题色（用于状态栏等）
    val THEME_COLOR_DARK = listOf(
        0xFF4A90D9.toInt(),  // 蓝色
        0xFF9C27B0.toInt(),  // 紫色
        0xFF4CAF50.toInt(),  // 绿色
        0xFFFF9800.toInt(),  // 橙色
        0xFFF44336.toInt()   // 红色
    )
    
    val THEME_COLOR_SINGLE = listOf(
        0xFF4A90D9.toInt(),  // 蓝色
        0xFF9C27B0.toInt(),  // 紫色
        0xFF4CAF50.toInt(),  // 绿色
        0xFFFF9800.toInt(),  // 橙色
        0xFFF44336.toInt()   // 红色
    )
    
    /**
     * 获取当前主题模式
     */
    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, MODE_NONE)
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }
    
    /**
     * 获取主题色索引
     */
    fun getThemeColorIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_COLOR_INDEX, 0)
    }
    
    /**
     * 设置主题色索引
     */
    fun setThemeColorIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_COLOR_INDEX, index).apply()
    }
    
    /**
     * 获取主题色（单色）
     */
    fun getThemeColor(context: Context): Int {
        val index = getThemeColorIndex(context)
        return THEME_COLOR_SINGLE.getOrElse(index) { THEME_COLOR_SINGLE[0] }
    }
    
    /**
     * 获取主题色渐变数组
     */
    fun getThemeGradientColors(context: Context): IntArray {
        val index = getThemeColorIndex(context)
        return THEME_COLORS.getOrElse(index) { THEME_COLORS[0] }
    }
    
    /**
     * 获取深色主题色（用于状态栏）
     */
    fun getThemeColorDark(context: Context): Int {
        val index = getThemeColorIndex(context)
        return THEME_COLOR_DARK.getOrElse(index) { THEME_COLOR_DARK[0] }
    }
    
    /**
     * 获取背景图片 URI
     */
    fun getBackgroundUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_BACKGROUND_URI, null)
        return uriString?.let { Uri.parse(it) }
    }
    
    /**
     * 设置背景图片 URI
     */
    fun setBackgroundUri(context: Context, uri: Uri?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKGROUND_URI, uri?.toString()).apply()
    }
    
    /**
     * 清除背景图片
     */
    fun clearBackground(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BACKGROUND_URI).apply()
        // 删除内部存储的图片文件
        val file = File(context.filesDir, "background_image.jpg")
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * 检查是否有背景图片文件
     */
    fun hasBackgroundImage(context: Context): Boolean {
        val file = File(context.filesDir, "background_image.jpg")
        return file.exists()
    }
    
    /**
     * 获取背景图片文件
     */
    fun getBackgroundFile(context: Context): File {
        return File(context.filesDir, "background_image.jpg")
    }
    
    /**
     * 应用主题到根布局
     */
    fun applyTheme(context: Context, rootView: View) {
        when (getThemeMode(context)) {
            MODE_COLOR -> {
                applyThemeColor(context, rootView)
            }
            MODE_BACKGROUND -> {
                applyBackgroundImage(context, rootView)
            }
            MODE_NONE -> {
                resetToDefault(context, rootView)
            }
        }
    }
    
    /**
     * 应用主题色（浅色渐变背景，保持文字可读性）
     */
    private fun applyThemeColor(context: Context, rootView: View) {
        val colors = getThemeGradientColors(context)
        
        // 创建浅色渐变背景
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            colors
        )
        
        rootView.background = gradient
        
        // 设置状态栏颜色为深色主题色
        if (context is android.app.Activity) {
            context.window?.statusBarColor = getThemeColorDark(context)
        }
    }
    
    /**
     * 应用背景图片（添加遮罩层确保文字可读）
     */
    private fun applyBackgroundImage(context: Context, rootView: View) {
        val file = getBackgroundFile(context)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    // 创建背景图片
                    val bitmapDrawable = BitmapDrawable(context.resources, bitmap)
                    
                    // 创建半透明白色遮罩层（30%不透明度，让背景图片更清晰明亮）
                    val overlay = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(0x4DFFFFFF.toInt(), 0x4DFFFFFF.toInt())
                    )
                    
                    // 组合背景和遮罩
                    val layers = arrayOf(bitmapDrawable, overlay)
                    val layerDrawable = LayerDrawable(layers)
                    
                    rootView.background = layerDrawable
                    
                    // 设置状态栏为半透明深色
                    if (context is android.app.Activity) {
                        context.window?.statusBarColor = 0x55000000.toInt()
                    }
                } else {
                    resetToDefault(context, rootView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resetToDefault(context, rootView)
            }
        } else {
            resetToDefault(context, rootView)
        }
    }
    
    /**
     * 重置为默认背景
     */
    private fun resetToDefault(context: Context, rootView: View) {
        rootView.setBackgroundColor(0xFFFAFAFA.toInt())
        
        // 恢复状态栏颜色
        if (context is android.app.Activity) {
            context.window?.statusBarColor = context.getColor(R.color.primary)
        }
    }
    
    /**
     * 选择主题色（清除背景图片）
     */
    fun selectThemeColor(context: Context, colorIndex: Int) {
        setThemeMode(context, MODE_COLOR)
        setThemeColorIndex(context, colorIndex)
        clearBackground(context)
    }
    
    /**
     * 选择背景图片（清除主题色选择）
     */
    fun selectBackgroundImage(context: Context) {
        setThemeMode(context, MODE_BACKGROUND)
    }
    
    /**
     * 清除所有主题设置
     */
    fun clearAllThemes(context: Context) {
        setThemeMode(context, MODE_NONE)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        clearBackground(context)
    }
}
