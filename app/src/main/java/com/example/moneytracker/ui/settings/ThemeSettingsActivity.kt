package com.example.moneytracker.ui.settings

import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream

class ThemeSettingsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_BACKGROUND_URI = "background_uri"

        // 主题色选项
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupThemeColors()
        setupBackgroundImage()
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
                selectTheme(index)
            }
        }
    }

    private fun selectTheme(index: Int) {
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

        // 保存主题色
        saveThemeColor(index)

        // 更新主题色
        applyThemeColor(index)

        Toast.makeText(this, "主题色已更新", Toast.LENGTH_SHORT).show()
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
                saveBackgroundUri(uri.toString())
                updateBackgroundPreview(uri)
                findViewById<Button>(R.id.btn_remove_background).isEnabled = true
                Toast.makeText(this, "背景图片已设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeBackgroundImage() {
        saveBackgroundUri("")
        findViewById<ImageView>(R.id.iv_background_preview).setImageDrawable(null)
        findViewById<TextView>(R.id.tv_no_background).visibility = TextView.VISIBLE
        findViewById<Button>(R.id.btn_remove_background).isEnabled = false
        Toast.makeText(this, "背景图片已移除", Toast.LENGTH_SHORT).show()
    }

    private fun updateBackgroundPreview(uri: Uri) {
        val imageView = findViewById<ImageView>(R.id.iv_background_preview)
        val noBackgroundText = findViewById<TextView>(R.id.tv_no_background)

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

                // 显示图片
                val savedFile = File(filesDir, "background_image.jpg")
                if (savedFile.exists()) {
                    imageView.setImageURI(Uri.fromFile(savedFile))
                    noBackgroundText.visibility = TextView.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ThemeSettingsActivity, "加载图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCurrentTheme() {
        // 加载主题色
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        selectedThemeIndex = prefs.getInt(KEY_THEME_COLOR, 0)

        // 更新选中状态
        val checkIds = listOf(
            R.id.check_default,
            R.id.check_purple,
            R.id.check_green,
            R.id.check_orange,
            R.id.check_red
        )

        checkIds.forEachIndexed { index, checkId ->
            val checkView = findViewById<ImageView>(checkId)
            checkView?.visibility = if (index == selectedThemeIndex) ImageView.VISIBLE else ImageView.GONE
        }

        // 加载背景图片
        val backgroundUri = prefs.getString(KEY_BACKGROUND_URI, "")
        if (!backgroundUri.isNullOrEmpty()) {
            try {
                val savedFile = File(filesDir, "background_image.jpg")
                if (savedFile.exists()) {
                    findViewById<ImageView>(R.id.iv_background_preview).setImageURI(Uri.fromFile(savedFile))
                    findViewById<TextView>(R.id.tv_no_background).visibility = TextView.GONE
                    findViewById<Button>(R.id.btn_remove_background).isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveThemeColor(index: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_COLOR, index).apply()
    }

    private fun saveBackgroundUri(uri: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKGROUND_URI, uri).apply()
    }

    private fun applyThemeColor(index: Int) {
        // 这里可以添加应用主题色的逻辑
        // 例如：更新全局主题、发送广播通知其他页面等
    }

    // 静态方法供其他地方使用
    fun getThemeColor(context: android.content.Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val index = prefs.getInt(KEY_THEME_COLOR, 0)
        return android.graphics.Color.parseColor(THEME_COLORS[index].first)
    }

    fun getBackgroundUri(context: android.content.Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getString(KEY_BACKGROUND_URI, null)
    }
}
