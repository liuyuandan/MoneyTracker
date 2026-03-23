package com.example.moneytracker.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

        // 备份数据（点击）- 智能处理
        binding.layoutBackup.setOnClickListener {
            handleBackupClick()
        }

        // 恢复数据
        binding.layoutRestore.setOnClickListener {
            showRestoreDialog()
        }
    }

    /**
     * 处理备份点击事件
     * 如果没有备份 -> 直接备份
     * 如果有备份 -> 显示备份列表（可新增/删除）
     */
    private fun handleBackupClick() {
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

        // 检查是否有备份文件
        val backupFiles = getBackupFiles()

        if (backupFiles.isEmpty()) {
            // 没有备份，直接创建备份
            performBackup()
        } else {
            // 有备份，显示备份列表
            showBackupListDialog(backupFiles)
        }
    }

    /**
     * 显示备份列表对话框（可新增/删除）
     */
    private fun showBackupListDialog(backupFiles: List<File>) {
        // 创建自定义布局
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        // 添加每条备份记录
        backupFiles.forEach { file ->
            val itemView = createBackupItemView(file)
            container.addView(itemView)
        }

        // 创建滚动视图
        val scrollView = ScrollView(requireContext()).apply {
            addView(container)
        }

        // 创建对话框
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("备份列表")
            .setView(scrollView)
            .setPositiveButton("新增备份") { dialog, _ ->
                dialog.dismiss()
                performBackup()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    /**
     * 创建备份项视图
     */
    private fun createBackupItemView(backupFile: File): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }

            // 备份名称
            val nameView = TextView(context).apply {
                text = formatBackupFileName(backupFile.name)
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_primary, null))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            // 删除按钮
            val deleteBtn = Button(context).apply {
                text = "删除"
                setTextColor(resources.getColor(R.color.expense, null))
                background = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    // 显示删除确认对话框
                    showDeleteBackupConfirmDialog(backupFile)
                }
            }

            addView(nameView)
            addView(deleteBtn)
        }
    }

    /**
     * 显示删除备份确认对话框
     */
    private fun showDeleteBackupConfirmDialog(backupFile: File) {
        val displayName = formatBackupFileName(backupFile.name)
        AlertDialog.Builder(requireContext())
            .setTitle("删除备份")
            .setMessage("确定要删除以下备份吗？\n\n$displayName\n\n此操作不可恢复！")
            .setPositiveButton("删除") { dialog, _ ->
                dialog.dismiss()
                deleteBackup(backupFile)
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    /**
     * 删除备份文件
     */
    private fun deleteBackup(backupFile: File) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 删除主备份文件
                    val mainDeleted = backupFile.delete()

                    // 删除关联的 WAL 和 SHM 文件
                    val walFile = File(backupFile.parentFile, backupFile.name + "-wal")
                    val shmFile = File(backupFile.parentFile, backupFile.name + "-shm")

                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()

                    mainDeleted
                }

                if (result) {
                    Toast.makeText(requireContext(), "备份已删除", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                FileLogger.logError(TAG, "删除备份失败", e)
                Toast.makeText(requireContext(), "删除失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 格式化备份文件名显示
     * 从 money_tracker_20260323_225137.db 转换为 2026年03月23日 22时51分
     */
    private fun formatBackupFileName(fileName: String): String {
        return try {
            // 提取时间戳部分: money_tracker_20260323_225137.db -> 20260323_225137
            val timestamp = fileName
                .removePrefix("money_tracker_")
                .removeSuffix(".db")

            // 解析: 20260323_225137
            val parts = timestamp.split("_")
            if (parts.size == 2) {
                val datePart = parts[0] // 20260323
                val timePart = parts[1] // 225137

                val year = datePart.substring(0, 4)
                val month = datePart.substring(4, 6)
                val day = datePart.substring(6, 8)

                val hour = timePart.substring(0, 2)
                val minute = timePart.substring(2, 4)

                "${year}年${month}月${day}日 ${hour}时${minute}分"
            } else {
                fileName
            }
        } catch (e: Exception) {
            fileName
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleBackupClick()
            } else {
                Toast.makeText(requireContext(), "需要存储权限才能备份数据", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 获取备份文件列表
     * 只返回主数据库备份文件（不包含 -wal 和 -shm 文件）
     */
    private fun getBackupFiles(): List<File> {
        val backupDir = File(requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "MoneyTracker")
        FileLogger.log(TAG, "查找备份目录: ${backupDir.absolutePath}")

        if (!backupDir.exists()) {
            FileLogger.log(TAG, "备份目录不存在")
            return emptyList()
        }

        // 列出目录下所有文件
        val allFiles = backupDir.listFiles()?.toList() ?: emptyList()
        FileLogger.log(TAG, "目录下所有文件: ${allFiles.map { it.name }}")

        // 过滤出主备份文件（.db 结尾，但不是 .db-wal 或 .db-shm）
        val backupFiles = allFiles
            .filter { file ->
                val name = file.name
                name.startsWith("money_tracker_") &&
                name.endsWith(".db") &&
                !name.endsWith(".db-wal") &&
                !name.endsWith(".db-shm")
            }
            .sortedByDescending { it.lastModified() }

        FileLogger.log(TAG, "找到 ${backupFiles.size} 个备份文件: ${backupFiles.map { it.name }}")

        return backupFiles
    }

    private fun showRestoreDialog() {
        val backupFiles = getBackupFiles()

        if (backupFiles.isEmpty()) {
            Toast.makeText(requireContext(), "没有找到备份文件，请先备份数据", Toast.LENGTH_SHORT).show()
            return
        }

        // 格式化显示文件名
        val displayNames = backupFiles.map { formatBackupFileName(it.name) }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("恢复数据")
            .setMessage("请选择要恢复的备份记录：")
            .setItems(displayNames) { dialog, which ->
                dialog.dismiss()
                // 延迟显示确认对话框，确保前一个对话框已关闭
                binding.root.post {
                    showRestoreConfirmDialog(backupFiles[which])
                }
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    private fun showRestoreConfirmDialog(backupFile: File) {
        val displayName = formatBackupFileName(backupFile.name)
        AlertDialog.Builder(requireContext())
            .setTitle("确认恢复")
            .setMessage("确定要从以下备份恢复数据吗？\n\n$displayName\n\n警告：当前数据将被覆盖！")
            .setPositiveButton("恢复") { dialog, _ ->
                dialog.dismiss()
                performRestore(backupFile)
            }
            .setNegativeButton("取消", null)
            .create()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
