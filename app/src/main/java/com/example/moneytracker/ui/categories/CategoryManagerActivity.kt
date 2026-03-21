package com.example.moneytracker.ui.categories

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.adapters.CategoryAdapter
import com.example.moneytracker.adapters.ColorPickerAdapter
import com.example.moneytracker.adapters.IconPickerAdapter
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.databinding.ActivityCategoryManagerBinding
import com.google.android.material.tabs.TabLayout

class CategoryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManagerBinding
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    // 可选图标列表
    private val availableIcons = listOf(
        "ic_restaurant", "ic_transport", "ic_shopping", "ic_entertainment",
        "ic_medical", "ic_education", "ic_home", "ic_communication",
        "ic_clothing", "ic_other_expense", "ic_salary", "ic_parttime",
        "ic_investment", "ic_bonus", "ic_other_income", "ic_category"
    )

    // 可选颜色列表
    private val availableColors = listOf(
        0xFFFF9800.toInt(), // 橙色
        0xFF2196F3.toInt(), // 蓝色
        0xFFE91E63.toInt(), // 粉色
        0xFF9C27B0.toInt(), // 紫色
        0xFFF44336.toInt(), // 红色
        0xFF00BCD4.toInt(), // 青色
        0xFF4CAF50.toInt(), // 绿色
        0xFF795548.toInt(), // 棕色
        0xFF607D8B.toInt(), // 灰蓝
        0xFFFFEB3B.toInt(), // 黄色
        0xFF9E9E9E.toInt(), // 灰色
        0xFF8BC34A.toInt()  // 浅绿
    )

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
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                // 点击分类可以编辑（暂不实现）
            },
            isManageMode = true
        )
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

        // 设置拖拽排序
        val itemTouchHelper = ItemTouchHelper(categoryAdapter.getItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(binding.rvCategories)
        categoryAdapter.setItemTouchHelper(itemTouchHelper)

        // 监听拖拽完成事件
        categoryAdapter.setOnDragCompleteListener { categories ->
            viewModel.updateSortOrder(categories)
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
        val rvIcons = dialogView.findViewById<RecyclerView>(R.id.rv_icons)
        val rvColors = dialogView.findViewById<RecyclerView>(R.id.rv_colors)

        // 设置图标选择
        var selectedIcon = availableIcons[0]
        val iconAdapter = IconPickerAdapter(availableIcons) { icon ->
            selectedIcon = icon
        }
        rvIcons.layoutManager = GridLayoutManager(this, 8)
        rvIcons.adapter = iconAdapter

        // 设置颜色选择
        var selectedColor = availableColors[0]
        val colorAdapter = ColorPickerAdapter(availableColors) { color ->
            selectedColor = color
        }
        rvColors.layoutManager = GridLayoutManager(this, 8)
        rvColors.adapter = colorAdapter

        AlertDialog.Builder(this)
            .setTitle("添加分类")
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val type = viewModel.currentType.value ?: Category.TYPE_EXPENSE
                    val category = Category(
                        name = name,
                        icon = selectedIcon,
                        color = selectedColor,
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
