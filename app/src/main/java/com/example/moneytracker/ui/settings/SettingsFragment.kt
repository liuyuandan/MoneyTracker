package com.example.moneytracker.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.R
import com.example.moneytracker.data.database.AppDatabase
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

    companion object {
        private const val TAG = "SettingsFragment"
        private const val REQUEST_STORAGE_PERMISSION = 1001
    }

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
        // 检查是否需要请求存储权限（Android 9及以下需要）
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
                return
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_data)
            .setMessage("确定要备份数据吗？\n\n备份文件将保存到应用专属目录，可通过文件管理器查看。")
            .setPositiveButton(R.string.confirm) { _, _ ->
                performBackup()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBackupDialog()
            } else {
                Toast.makeText(requireContext(), "需要存储权限才能备份数据", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreDialog() {
        // 使用应用专属外部存储目录
        val backupDir = File(requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "MoneyTracker")
        FileLogger.log(TAG, "查找备份目录: ${backupDir.absolutePath}")

        if (!backupDir.exists()) {
            Toast.makeText(requireContext(), "没有找到备份目录", Toast.LENGTH_SHORT).show()
            return
        }

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
                    // Room 数据库文件名是 "money_tracker_database"
                    // Room 会创建以下文件：
                    // 1. money_tracker_database (主数据库文件)
                    // 2. money_tracker_database-wal (Write-Ahead Log)
                    // 3. money_tracker_database-shm (Shared Memory)
                    val dbFile = requireContext().getDatabasePath("money_tracker_database")
                    FileLogger.log(TAG, "数据库路径: ${dbFile.absolutePath}, exists: ${dbFile.exists()}")

                    if (!dbFile.exists()) {
                        FileLogger.logError(TAG, "数据库文件不存在: ${dbFile.absolutePath}", null)
                        return@withContext "数据库文件不存在"
                    }

                    // 创建备份目录
                    val backupDir = File(requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "MoneyTracker")
                    FileLogger.log(TAG, "备份目录: ${backupDir.absolutePath}")

                    if (!backupDir.exists()) {
                        val created = backupDir.mkdirs()
                        FileLogger.log(TAG, "创建备份目录: $created")
                    }

                    // 生成备份文件名
                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val timestamp = dateFormat.format(Date())

                    // 备份主数据库文件
                    val backupFileName = "money_tracker_$timestamp.db"
                    val backupFile = File(backupDir, backupFileName)
                    FileLogger.log(TAG, "备份文件: ${backupFile.absolutePath}")

                    FileInputStream(dbFile).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 同时备份 wal 和 shm 文件（如果存在）
                    val walFile = requireContext().getDatabasePath("money_tracker_database-wal")
                    val shmFile = requireContext().getDatabasePath("money_tracker_database-shm")

                    if (walFile.exists()) {
                        val walBackup = File(backupDir, "money_tracker_$timestamp.db-wal")
                        FileInputStream(walFile).use { input ->
                            FileOutputStream(walBackup).use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileLogger.log(TAG, "已备份 WAL 文件")
                    }

                    if (shmFile.exists()) {
                        val shmBackup = File(backupDir, "money_tracker_$timestamp.db-shm")
                        FileInputStream(shmFile).use { input ->
                            FileOutputStream(shmBackup).use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileLogger.log(TAG, "已备份 SHM 文件")
                    }

                    FileLogger.log(TAG, "备份成功")
                    backupFile.absolutePath
                }

                Toast.makeText(requireContext(), "备份成功：$result", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                FileLogger.logError(TAG, "备份失败", e)
                e.printStackTrace()
                Toast.makeText(requireContext(), "备份失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun performRestore(backupFile: File) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Room 数据库文件名是 "money_tracker_database"
                    val dbFile = requireContext().getDatabasePath("money_tracker_database")
                    val walFile = requireContext().getDatabasePath("money_tracker_database-wal")
                    val shmFile = requireContext().getDatabasePath("money_tracker_database-shm")

                    FileLogger.log(TAG, "恢复数据库从: ${backupFile.absolutePath} 到: ${dbFile.absolutePath}")

                    // 1. 先关闭数据库连接，确保所有数据已写入
                    try {
                        AppDatabase.closeDatabase()
                        FileLogger.log(TAG, "已关闭数据库连接")
                    } catch (e: Exception) {
                        FileLogger.logError(TAG, "关闭数据库连接失败", e)
                    }

                    // 2. 删除现有的 WAL 和 SHM 文件（避免数据不一致）
                    if (walFile.exists()) {
                        val deleted = walFile.delete()
                        FileLogger.log(TAG, "删除现有 WAL 文件: $deleted")
                    }
                    if (shmFile.exists()) {
                        val deleted = shmFile.delete()
                        FileLogger.log(TAG, "删除现有 SHM 文件: $deleted")
                    }

                    // 3. 复制备份的主数据库文件
                    FileInputStream(backupFile).use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    FileLogger.log(TAG, "已恢复主数据库文件")

                    // 4. 恢复 WAL 和 SHM 文件（如果存在）
                    val backupDir = backupFile.parentFile!!
                    val walBackup = File(backupDir, backupFile.name + "-wal")
                    val shmBackup = File(backupDir, backupFile.name + "-shm")

                    if (walBackup.exists()) {
                        FileInputStream(walBackup).use { input ->
                            FileOutputStream(walFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileLogger.log(TAG, "已恢复 WAL 文件")
                    }

                    if (shmBackup.exists()) {
                        FileInputStream(shmBackup).use { input ->
                            FileOutputStream(shmFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileLogger.log(TAG, "已恢复 SHM 文件")
                    }

                    FileLogger.log(TAG, "恢复成功")
                    "success"
                }

                if (result == "success") {
                    Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show()

                    // 提示重启应用
                    AlertDialog.Builder(requireContext())
                        .setTitle("恢复成功")
                        .setMessage("数据已恢复，需要重启应用以生效。\n\n是否立即重启？")
                        .setPositiveButton("立即重启") { _, _ ->
                            // 重启应用
                            val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                requireActivity().finish()
                                // 杀掉进程，确保完全重启
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }
                        .setNegativeButton("稍后重启", null)
                        .show()
                }
            } catch (e: Exception) {
                FileLogger.logError(TAG, "恢复失败", e)
                e.printStackTrace()
                Toast.makeText(requireContext(), "恢复失败：${e.message}", Toast.LENGTH_LONG).show()
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
