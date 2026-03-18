package com.example.moneytracker.ui.categories

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moneytracker.R
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

        // 长按删除分类
        categoryAdapter.setOnLongClickListener { category ->
            if (category.isDefault) {
                Toast.makeText(this, "默认分类不能删除", Toast.LENGTH_SHORT).show()
            } else {
                showDeleteConfirmDialog(category)
            }
            true
        }
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
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.et_category_name)

        AlertDialog.Builder(this)
            .setTitle("添加分类")
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val type = viewModel.currentType.value ?: Category.TYPE_EXPENSE
                    val category = Category(
                        name = name,
                        icon = "ic_category_other",
                        color = 0xFF9E9E9E.toInt(),
                        type = type,
                        isDefault = false
                    )
                    viewModel.addCategory(category)
                    Toast.makeText(this, "分类添加成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("删除分类")
            .setMessage("确定要删除分类 ${category.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCategory(category)
                Toast.makeText(this, "分类已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }
    }
}
