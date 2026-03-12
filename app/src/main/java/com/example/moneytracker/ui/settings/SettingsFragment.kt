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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
