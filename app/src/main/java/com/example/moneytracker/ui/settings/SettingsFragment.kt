package com.example.moneytracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentSettingsBinding
import com.example.moneytracker.ui.categories.CategoryManagerActivity
import com.example.moneytracker.utils.FileLogger

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
            FileLogger.log("SettingsFragment", "setupVersionDisplay: version = $versionName")
        } catch (e: Exception) {
            FileLogger.logError("SettingsFragment", "setupVersionDisplay: Error getting version", e)
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
            Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show()
        }

        // 恢复数据
        binding.layoutRestore.setOnClickListener {
            Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show()
        }
        
        // 查看日志（长按备份按钮）
        binding.layoutBackup.setOnLongClickListener {
            showLogDialog()
            true
        }
    }
    
    private fun showLogDialog() {
        val logContent = FileLogger.getLogContent()
        val logPath = FileLogger.getLogFilePath()
        
        android.app.AlertDialog.Builder(requireContext())
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
