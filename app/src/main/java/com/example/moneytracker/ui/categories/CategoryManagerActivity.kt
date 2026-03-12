package com.example.moneytracker.ui.categories

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moneytracker.adapters.CategoryAdapter
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.databinding.ActivityCategoryManagerBinding
import com.google.android.material.tabs.TabLayout

class CategoryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManagerBinding
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabLayout()
        setupButtons()
        observeData()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            // 点击分类可以编辑（暂不实现）
        }
        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val type = when (tab?.position) {
                    0 -> Category.TYPE_EXPENSE
                    1 -> Category.TYPE_INCOME
                    else -> Category.TYPE_EXPENSE
                }
                viewModel.setType(type)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.ivAdd.setOnClickListener {
            // 添加自定义分类（暂不实现，可以后续扩展）
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        // 简单的添加分类对话框，实际项目中可以做得更完善
        // 这里暂时不实现，预留接口
    }

    private fun observeData() {
        viewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }
    }
}
