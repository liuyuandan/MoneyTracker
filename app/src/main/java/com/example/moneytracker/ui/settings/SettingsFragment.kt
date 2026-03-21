package com.example.moneytracker.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentSettingsBinding
import com.example.moneytracker.ui.categories.CategoryManagerActivity
import com.example.moneytracker.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupVersionDisplay()
    }

    private fun setupVersionDisplay() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = versionName
        } catch (e: Exception) {
            binding.tvVersion.text = "1.0"
        }
    }

    private fun setupClickListeners() {
        // 分类管理
        binding.layoutCategory.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryManagerActivity::class.java))
        }

        // 备份数据
        binding.layoutBackup.setOnClickListener {
            showBackupDialog()
        }

        // 恢复数据
        binding.layoutRestore.setOnClickListener {
            showRestoreDialog()
        }
        
        // 查看日志
        binding.layoutBackup.setOnLongClickListener {
            showLogDialog()
            true
        }
    }
    
    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_data)
            .setMessage("确定要备份数据吗？备份文件将保存到 Download/MoneyTracker 目录")
            .setPositiveButton(R.string.confirm) { _, _ ->
                performBackup()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRestoreDialog() {
        val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "MoneyTracker")
        val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".db") }?.sortedByDescending { it.lastModified() }
        
        if (backupFiles.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "没有找到备份文件", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileNames = backupFiles.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.restore_data)
            .setItems(fileNames) { _, which ->
                showRestoreConfirmDialog(backupFiles[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showRestoreConfirmDialog(backupFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认恢复")
            .setMessage("确定要从 ${backupFile.name} 恢复数据吗？\n\n警告：当前数据将被覆盖！")
            .setPositiveButton(R.string.confirm) { _, _ ->
                performRestore(backupFile)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performBackup() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 获取数据库路径
                    val dbPath = requireContext().getDatabasePath("money_tracker.db")
                    
                    // 创建备份目录
                    val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "MoneyTracker")
                    if (!backupDir.exists()) {
                        backupDir.mkdirs()
                    }
                    
                    // 生成备份文件名
                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val backupFileName = "money_tracker_${dateFormat.format(Date())}.db"
                    val backupFile = File(backupDir, backupFileName)
                    
                    // 复制数据库文件
                    FileInputStream(dbPath).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    backupFile.absolutePath
                }
                
                Toast.makeText(requireContext(), "备份成功：$result", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "备份失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun performRestore(backupFile: File) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 获取数据库路径
                    val dbPath = requireContext().getDatabasePath("money_tracker.db")
                    
                    // 复制备份文件到数据库位置
                    FileInputStream(backupFile).use { input ->
                        FileOutputStream(dbPath).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                
                Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show()
                
                // 提示重启应用
                AlertDialog.Builder(requireContext())
                    .setTitle("恢复成功")
                    .setMessage("数据已恢复，请重启应用以生效")
                    .setPositiveButton("确定") { _, _ ->
                        // 重启应用
                        val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "恢复失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLogDialog() {
        val logContent = FileLogger.getLogContent()
        val logPath = FileLogger.getLogFilePath()
        
        AlertDialog.Builder(requireContext())
            .setTitle("应用日志")
            .setMessage("日志文件路径:\n$logPath\n\n日志内容:\n\n${logContent.take(2000)}${if (logContent.length > 2000) "\n...(已截断)" else ""}")
            .setPositiveButton("复制日志路径") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("日志路径", logPath)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "日志路径已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .setNeutralButton("清空日志") { _, _ ->
                FileLogger.clearLog()
                Toast.makeText(requireContext(), "日志已清空", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
