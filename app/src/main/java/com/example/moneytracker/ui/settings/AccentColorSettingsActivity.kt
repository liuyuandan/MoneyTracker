package com.example.moneytracker.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.R
import com.example.moneytracker.databinding.ActivityAccentColorSettingsBinding
import com.example.moneytracker.utils.ThemeManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AccentColorSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccentColorSettingsBinding
    private var selectedColorRes: Int = R.color.accent_blue
    
    // 颜色资源到实际颜色值的映射
    private val colorResToValue = mapOf(
        R.color.accent_blue to 0xFF4A90D9.toInt(),
        R.color.accent_green to 0xFF4CAF50.toInt(),
        R.color.accent_orange to 0xFFFF9800.toInt(),
        R.color.accent_purple to 0xFF9C27B0.toInt(),
        R.color.accent_red to 0xFFF44336.toInt()
    )
    private var accentImageUri: Uri? = null

    private val accentColorButtons = mutableMapOf<Int, Pair<MaterialButton, ImageView>>()
    private val accentColors = listOf(
        R.color.accent_blue,
        R.color.accent_green,
        R.color.accent_orange,
        R.color.accent_purple,
        R.color.accent_red
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleAccentImageSelected(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccentColorSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initAccentColorButtons()
        loadSavedSettings()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initAccentColorButtons() {
        accentColorButtons[R.color.accent_blue] = Pair(binding.colorBlueButton, binding.checkBlue)
        accentColorButtons[R.color.accent_green] = Pair(binding.colorGreenButton, binding.checkGreen)
        accentColorButtons[R.color.accent_orange] = Pair(binding.colorOrangeButton, binding.checkOrange)
        accentColorButtons[R.color.accent_purple] = Pair(binding.colorPurpleButton, binding.checkPurple)
        accentColorButtons[R.color.accent_red] = Pair(binding.colorRedButton, binding.checkRed)
    }

    private fun loadSavedSettings() {
        // 加载保存的强调色资源ID
        selectedColorRes = ThemeManager.getAccentColorRes(this)
        updateAccentColorSelection(selectedColorRes)

        // 加载保存的背景图片
        val savedUri = ThemeManager.getAccentImageUri(this)
        if (savedUri != null) {
            accentImageUri = savedUri
            displayAccentImage(savedUri)
        }

        // 更新当前模式显示
        updateCurrentModeDisplay()
    }

    private fun setupClickListeners() {
        // 颜色按钮点击
        accentColorButtons.forEach { (colorRes, pair) ->
            pair.first.setOnClickListener {
                selectAccentColor(colorRes)
            }
        }

        // 背景图片点击
        binding.accentImagePreviewContainer.setOnClickListener {
            openImagePicker()
        }

        // 清除背景图片
        binding.clearAccentImageButton.setOnClickListener {
            clearAccentImage()
        }
    }

    private fun selectAccentColor(colorRes: Int) {
        selectedColorRes = colorRes

        // 清除背景图片（二选一逻辑）
        accentImageUri = null
        clearAccentImageDisplay()

        updateAccentColorSelection(colorRes)
        ThemeManager.setAccentColor(this, colorRes)
        ThemeManager.setAccentImageEnabled(this, false)

        updateCurrentModeDisplay()
        Toast.makeText(this, "已选择主题色", Toast.LENGTH_SHORT).show()
    }

    private fun updateAccentColorSelection(selectedColor: Int) {
        accentColorButtons.forEach { (colorRes, pair) ->
            val checkView = pair.second
            if (colorRes == selectedColor) {
                checkView.visibility = View.VISIBLE
            } else {
                checkView.visibility = View.GONE
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun handleAccentImageSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                // 复制图片到应用私有目录
                val localUri = copyImageToLocal(uri)

                accentImageUri = localUri

                // 清除主题色选择（二选一逻辑）
                ThemeManager.setAccentImageEnabled(this@AccentColorSettingsActivity, true)

                withContext(Dispatchers.Main) {
                    displayAccentImage(localUri)
                    updateCurrentModeDisplay()
                    Toast.makeText(this@AccentColorSettingsActivity, "已设置背景图片", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccentColorSettingsActivity, "设置图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun copyImageToLocal(sourceUri: Uri): Uri {
        return withContext(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Cannot open input stream")

            val file = File(filesDir, "accent_background.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            Uri.fromFile(file)
        }
    }

    private fun displayAccentImage(uri: Uri) {
        binding.accentImagePreview.setImageURI(uri)
        binding.accentImagePreview.visibility = View.VISIBLE
        binding.accentImagePlaceholder.visibility = View.GONE
        binding.clearAccentImageButton.visibility = View.VISIBLE
    }

    private fun clearAccentImageDisplay() {
        binding.accentImagePreview.setImageDrawable(null)
        binding.accentImagePreview.visibility = View.GONE
        binding.accentImagePlaceholder.visibility = View.VISIBLE
        binding.clearAccentImageButton.visibility = View.GONE
    }

    private fun clearAccentImage() {
        accentImageUri = null
        clearAccentImageDisplay()
        ThemeManager.setAccentImageEnabled(this, false)

        // 恢复默认颜色选择
        updateAccentColorSelection(selectedColorRes)
        updateCurrentModeDisplay()

        Toast.makeText(this, "已清除背景图片", Toast.LENGTH_SHORT).show()
    }

    private fun updateCurrentModeDisplay() {
        val isImageMode = ThemeManager.isAccentImageEnabled(this)
        if (isImageMode && accentImageUri != null) {
            binding.currentModeText.text = "使用背景图片"
            binding.currentModeText.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.currentModeText.text = "使用主题色"
            // 使用颜色值而不是资源ID
            val colorValue = colorResToValue[selectedColorRes] ?: 0xFF4A90D9.toInt()
            binding.currentModeText.setTextColor(colorValue)
        }
    }

    override fun onResume() {
        super.onResume()
        // 应用当前主题
        ThemeManager.applyTheme(this, binding.root)
    }
}
