package com.example.moneytracker.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.R
import com.example.moneytracker.databinding.ActivityThemeSettingsBinding
import com.example.moneytracker.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThemeSettingsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
        
        // 主题色选项（用于显示）
        val THEME_COLORS = listOf(
            "#4A90D9" to "default",      // 默认蓝色
            "#9C27B0" to "purple",       // 紫色
            "#4CAF50" to "green",        // 绿色
            "#FF9800" to "orange",       // 橙色
            "#F44336" to "red"           // 红色
        )
    }

    private lateinit var binding: ActivityThemeSettingsBinding
    private var selectedThemeIndex = 0
    private var selectedAccentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupThemeColors()
        setupBackgroundImage()
        setupAccentColors()
        loadCurrentTheme()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupThemeColors() {
        val themeIds = listOf(
            R.id.theme_default to R.id.check_default,
            R.id.theme_purple to R.id.check_purple,
            R.id.theme_green to R.id.check_green,
            R.id.theme_orange to R.id.check_orange,
            R.id.theme_red to R.id.check_red
        )

        themeIds.forEachIndexed { index, (themeId, checkId) ->
            val themeLayout = findViewById<FrameLayout>(themeId)
            themeLayout.setOnClickListener {
                selectThemeColor(index)
            }
        }
    }

    private fun selectThemeColor(index: Int) {
        selectedThemeIndex = index

        // 更新选中状态
        val checkIds = listOf(
            R.id.check_default,
            R.id.check_purple,
            R.id.check_green,
            R.id.check_orange,
            R.id.check_red
        )

        checkIds.forEachIndexed { i, checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = if (i == index) ImageView.VISIBLE else ImageView.GONE
        }

        // 使用 ThemeManager 选择主题色（会自动清除背景图片）
        ThemeManager.selectThemeColor(this, index)
        
        // 更新 UI 显示当前模式
        updateModeIndicator(ThemeManager.MODE_COLOR)
        
        // 清除背景图片预览
        clearBackgroundPreview()

        Toast.makeText(this, "主题色已设置", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackgroundImage() {
        val btnSelect = findViewById<Button>(R.id.btn_select_background)
        val btnRemove = findViewById<Button>(R.id.btn_remove_background)

        btnSelect.setOnClickListener {
            openImagePicker()
        }

        btnRemove.setOnClickListener {
            removeBackgroundImage()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 持久化 URI 权限
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                // 保存并设置背景图片模式
                saveAndApplyBackground(uri)
            }
        }
    }

    private fun saveAndApplyBackground(uri: Uri) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 复制图片到应用内部存储
                    val inputStream = contentResolver.openInputStream(uri)
                    val outputFile = File(filesDir, "background_image.jpg")
                    val outputStream = FileOutputStream(outputFile)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                // 使用 ThemeManager 选择背景图片模式（会自动清除主题色模式）
                ThemeManager.selectBackgroundImage(this@ThemeSettingsActivity)
                ThemeManager.setBackgroundUri(this@ThemeSettingsActivity, uri)

                // 更新预览（增强亮度）
                val savedFile = File(filesDir, "background_image.jpg")
                if (savedFile.exists()) {
                    loadEnhancedPreview(savedFile)
                    
                    // 更新模式指示器
                    updateModeIndicator(ThemeManager.MODE_BACKGROUND)
                    
                    // 清除主题色选中状态
                    clearThemeColorSelection()
                    
                    Toast.makeText(this@ThemeSettingsActivity, "背景图片已设置", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ThemeSettingsActivity, "加载图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 加载增强后的预览图片（提高亮度和对比度）
     */
    private fun loadEnhancedPreview(imageFile: File) {
        lifecycleScope.launch {
            try {
                val enhancedBitmap = withContext(Dispatchers.IO) {
                    // 加载原始图片
                    val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@withContext null
                    
                    // 调整图片大小以适应预览
                    val targetWidth = 800
                    val scaledBitmap = if (originalBitmap.width > targetWidth) {
                        val ratio = originalBitmap.width.toFloat() / targetWidth
                        val targetHeight = (originalBitmap.height / ratio).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true).also {
                            if (it != originalBitmap) originalBitmap.recycle()
                        }
                    } else {
                        originalBitmap
                    }
                    
                    // 增强图片（提高亮度和对比度）
                    enhanceBitmap(scaledBitmap, brightness = 1.2f, contrast = 1.1f)
                }
                
                if (enhancedBitmap != null) {
                    findViewById<ImageView>(R.id.iv_background_preview).setImageBitmap(enhancedBitmap)
                    findViewById<TextView>(R.id.tv_no_background).visibility = TextView.GONE
                    findViewById<Button>(R.id.btn_remove_background).isEnabled = true
                } else {
                    // 如果增强失败，使用原始图片
                    findViewById<ImageView>(R.id.iv_background_preview).setImageURI(Uri.fromFile(imageFile))
                    findViewById<TextView>(R.id.tv_no_background).visibility = TextView.GONE
                    findViewById<Button>(R.id.btn_remove_background).isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 出错时使用原始图片
                findViewById<ImageView>(R.id.iv_background_preview).setImageURI(Uri.fromFile(imageFile))
                findViewById<TextView>(R.id.tv_no_background).visibility = TextView.GONE
                findViewById<Button>(R.id.btn_remove_background).isEnabled = true
            }
        }
    }

    /**
     * 增强图片的亮度和对比度
     */
    private fun enhanceBitmap(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // 创建颜色矩阵（亮度 + 对比度）
        val colorMatrix = ColorMatrix()
        
        // 设置对比度（对比度 > 1 增加对比度）
        colorMatrix.setConcat(
            ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )),
            colorMatrix
        )
        
        // 设置亮度（亮度 > 1 增加亮度）
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, (brightness - 1f) * 255,
            0f, 1f, 0f, 0f, (brightness - 1f) * 255,
            0f, 0f, 1f, 0f, (brightness - 1f) * 255,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(brightnessMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }

    private fun removeBackgroundImage() {
        // 使用 ThemeManager 清除背景图片
        ThemeManager.clearBackground(this)
        
        // 如果当前是背景图片模式，则切换到无模式
        if (ThemeManager.getThemeMode(this) == ThemeManager.MODE_BACKGROUND) {
            ThemeManager.setThemeMode(this, ThemeManager.MODE_NONE)
        }
        
        clearBackgroundPreview()
        updateModeIndicator(ThemeManager.MODE_NONE)
        
        Toast.makeText(this, "背景图片已移除", Toast.LENGTH_SHORT).show()
    }

    private fun clearBackgroundPreview() {
        findViewById<ImageView>(R.id.iv_background_preview).setImageDrawable(null)
        findViewById<TextView>(R.id.tv_no_background).visibility = TextView.VISIBLE
        findViewById<Button>(R.id.btn_remove_background).isEnabled = false
    }

    private fun clearThemeColorSelection() {
        val checkIds = listOf(
            R.id.check_default,
            R.id.check_purple,
            R.id.check_green,
            R.id.check_orange,
            R.id.check_red
        )

        checkIds.forEach { checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = ImageView.GONE
        }
    }

    private fun updateModeIndicator(mode: Int) {
        val tvColorMode = findViewById<TextView>(R.id.tv_color_mode_status)
        val tvBackgroundMode = findViewById<TextView>(R.id.tv_background_mode_status)
        
        when (mode) {
            ThemeManager.MODE_COLOR -> {
                tvColorMode?.text = "当前使用"
                tvColorMode?.setTextColor(Color.parseColor("#4CAF50"))
                tvBackgroundMode?.text = "未使用"
                tvBackgroundMode?.setTextColor(Color.GRAY)
            }
            ThemeManager.MODE_BACKGROUND -> {
                tvColorMode?.text = "未使用"
                tvColorMode?.setTextColor(Color.GRAY)
                tvBackgroundMode?.text = "当前使用"
                tvBackgroundMode?.setTextColor(Color.parseColor("#4CAF50"))
            }
            else -> {
                tvColorMode?.text = "未使用"
                tvColorMode?.setTextColor(Color.GRAY)
                tvBackgroundMode?.text = "未使用"
                tvBackgroundMode?.setTextColor(Color.GRAY)
            }
        }
    }

    // ==================== 强调色相关方法 ====================

    private fun setupAccentColors() {
        val accentIds = listOf(
            R.id.accent_default to R.id.accent_check_default,
            R.id.accent_purple to R.id.accent_check_purple,
            R.id.accent_green to R.id.accent_check_green,
            R.id.accent_orange to R.id.accent_check_orange,
            R.id.accent_red to R.id.accent_check_red
        )

        accentIds.forEachIndexed { index, (accentId, checkId) ->
            val accentLayout = findViewById<FrameLayout>(accentId)
            accentLayout.setOnClickListener {
                selectAccentColor(index)
            }
        }
    }

    private fun selectAccentColor(index: Int) {
        selectedAccentIndex = index

        // 更新选中状态
        val checkIds = listOf(
            R.id.accent_check_default,
            R.id.accent_check_purple,
            R.id.accent_check_green,
            R.id.accent_check_orange,
            R.id.accent_check_red
        )

        checkIds.forEachIndexed { i, checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = if (i == index) ImageView.VISIBLE else ImageView.GONE
        }

        // 保存强调色设置
        ThemeManager.setAccentColorIndex(this, index)
        
        // 更新状态显示
        updateAccentColorStatus(index)

        Toast.makeText(this, "强调色已设置", Toast.LENGTH_SHORT).show()
    }

    private fun updateAccentColorStatus(index: Int) {
        val tvAccentStatus = findViewById<TextView>(R.id.tv_accent_color_status)
        tvAccentStatus?.text = ThemeManager.ACCENT_COLOR_NAMES.getOrElse(index) { "默认蓝色" }
    }

    private fun loadAccentColorSetting() {
        selectedAccentIndex = ThemeManager.getAccentColorIndex(this)
        
        val checkIds = listOf(
            R.id.accent_check_default,
            R.id.accent_check_purple,
            R.id.accent_check_green,
            R.id.accent_check_orange,
            R.id.accent_check_red
        )

        checkIds.forEachIndexed { index, checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = if (index == selectedAccentIndex) ImageView.VISIBLE else ImageView.GONE
        }

        updateAccentColorStatus(selectedAccentIndex)
    }

    private fun loadCurrentTheme() {
        // 获取当前主题模式
        val currentMode = ThemeManager.getThemeMode(this)
        
        // 加载主题色选中状态
        selectedThemeIndex = ThemeManager.getThemeColorIndex(this)
        
        val checkIds = listOf(
            R.id.check_default,
            R.id.check_purple,
            R.id.check_green,
            R.id.check_orange,
            R.id.check_red
        )

        // 只有在颜色模式下才显示选中状态
        checkIds.forEachIndexed { index, checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = if (currentMode == ThemeManager.MODE_COLOR && index == selectedThemeIndex) {
                ImageView.VISIBLE
            } else {
                ImageView.GONE
            }
        }

        // 加载背景图片预览（增强亮度）
        val savedFile = File(filesDir, "background_image.jpg")
        if (savedFile.exists()) {
            loadEnhancedPreview(savedFile)
        } else {
            clearBackgroundPreview()
        }
        
        // 更新模式指示器
        updateModeIndicator(currentMode)
        
        // 加载强调色设置
        loadAccentColorSetting()
    }
}
