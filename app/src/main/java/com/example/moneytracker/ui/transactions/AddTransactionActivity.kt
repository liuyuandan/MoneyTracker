package com.example.moneytracker.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.adapters.CategoryAdapter
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.databinding.ActivityAddTransactionBinding
import com.example.moneytracker.utils.DateUtils
import com.example.moneytracker.utils.FileLogger
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddTransactionActivity"
    }

    private lateinit var binding: ActivityAddTransactionBinding
    private val viewModel: AddTransactionViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    private var currentAmount = ""
    private var editingTransactionId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(TAG, "onCreate: Starting AddTransactionActivity")
        
        try {
            binding = ActivityAddTransactionBinding.inflate(layoutInflater)
            setContentView(binding.root)

            editingTransactionId = intent.getLongExtra("transaction_id", -1)
            FileLogger.log(TAG, "onCreate: editingTransactionId = $editingTransactionId")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error inflating layout", e)
            showErrorAndFinish("布局加载失败", e)
            return
        }
        
        try {
            setupCategoryRecyclerView()
            FileLogger.log(TAG, "onCreate: CategoryRecyclerView setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up category recycler view", e)
            showErrorAndFinish("分类列表初始化失败", e)
            return
        }
        
        try {
            setupTabLayout()
            FileLogger.log(TAG, "onCreate: TabLayout setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up tab layout", e)
            showErrorAndFinish("标签页初始化失败", e)
            return
        }
        
        try {
            setupDatePicker()
            FileLogger.log(TAG, "onCreate: DatePicker setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up date picker", e)
            showErrorAndFinish("日期选择初始化失败", e)
            return
        }
        
        try {
            setupNumberKeyboard()
            FileLogger.log(TAG, "onCreate: NumberKeyboard setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up number keyboard", e)
            showErrorAndFinish("数字键盘初始化失败", e)
            return
        }
        
        try {
            setupSaveButton()
            setupBackButton()
            FileLogger.log(TAG, "onCreate: Buttons setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up buttons", e)
            showErrorAndFinish("按钮初始化失败", e)
            return
        }
        
        try {
            observeData()
            FileLogger.log(TAG, "onCreate: observeData setup done")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error setting up data observation", e)
            showErrorAndFinish("数据监听初始化失败", e)
            return
        }
        
        // 如果是编辑模式，设置标题并加载交易详情
        if (editingTransactionId > 0) {
            binding.tvTitle.text = getString(R.string.edit_transaction)
            loadTransactionDetails()
        } else {
            binding.tvTitle.text = getString(R.string.add_transaction)
        }
            
        FileLogger.log(TAG, "onCreate: All setup completed successfully")
    }
    
    private fun showErrorAndFinish(message: String, e: Exception) {
        android.app.AlertDialog.Builder(this)
            .setTitle("发生错误")
            .setMessage("$message\n\n错误信息: ${e.message}\n\n日志已保存到文件，请查看设置页面获取日志路径。")
            .setPositiveButton("确定") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun setupCategoryRecyclerView() {
        FileLogger.log(TAG, "setupCategoryRecyclerView: Setting up category adapter")
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                viewModel.setSelectedCategory(category)
            },
            isManageMode = false
        )
        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupTabLayout() {
        FileLogger.log(TAG, "setupTabLayout: Setting up tab layout")
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val type = when (tab?.position) {
                    0 -> Transaction.TYPE_EXPENSE
                    1 -> Transaction.TYPE_INCOME
                    else -> Transaction.TYPE_EXPENSE
                }
                viewModel.setTransactionType(type)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val timestamp = calendar.timeInMillis
            viewModel.setSelectedDate(timestamp)
            binding.tvDate.text = DateUtils.formatDate(timestamp)
        }

        binding.layoutDate.setOnClickListener {
            DatePickerDialog(
                this,
                dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.tvDate.text = DateUtils.formatDate(System.currentTimeMillis())
    }

    private fun setupNumberKeyboard() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9, binding.btnDot
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                val digit = (it as TextView).text.toString()
                appendDigit(digit)
            }
        }

        binding.btnDelete.setOnClickListener {
            deleteLastDigit()
        }
    }

    private fun appendDigit(digit: String) {
        if (digit == "." && currentAmount.contains(".")) {
            return
        }
        if (currentAmount.contains(".") &&
            currentAmount.substringAfter(".").length >= 2) {
            return
        }
        currentAmount += digit
        updateAmountDisplay()
    }

    private fun deleteLastDigit() {
        if (currentAmount.isNotEmpty()) {
            currentAmount = currentAmount.dropLast(1)
            updateAmountDisplay()
        }
    }

    private fun updateAmountDisplay() {
        val displayText = if (currentAmount.isEmpty()) {
            "0.00"
        } else {
            currentAmount
        }
        binding.tvAmountDisplay.text = displayText
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            try {
                val amount = currentAmount.toDoubleOrNull() ?: 0.0
                val description = binding.etDescription.text.toString()

                if (amount <= 0) {
                    Toast.makeText(this, R.string.please_input_amount, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (viewModel.selectedCategory.value == null) {
                    Toast.makeText(this, R.string.please_select_category, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val success = if (editingTransactionId > 0) {
                    viewModel.updateTransaction(editingTransactionId, amount, description)
                } else {
                    viewModel.saveTransaction(amount, description)
                }

                if (success) {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                FileLogger.logError(TAG, "setupSaveButton: Error saving transaction", e)
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBackButton() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadTransactionDetails() {
        FileLogger.log(TAG, "loadTransactionDetails: Loading transaction $editingTransactionId")
        viewModel.loadTransaction(editingTransactionId)
    }

    private fun observeData() {
        viewModel.transactionType.observe(this) { type ->
            type?.let {
                val isExpense = it == Transaction.TYPE_EXPENSE
                val color = if (isExpense) {
                    ContextCompat.getColor(this, R.color.expense)
                } else {
                    ContextCompat.getColor(this, R.color.income)
                }
                binding.tvAmountDisplay.setTextColor(color)
                
                // 更新 TabLayout 选中状态
                val tabIndex = if (isExpense) 0 else 1
                if (binding.tabLayout.selectedTabPosition != tabIndex) {
                    binding.tabLayout.getTabAt(tabIndex)?.select()
                }
            }
        }

        viewModel.categories.observe(this) { categories ->
            FileLogger.log(TAG, "observeData: categories updated, size = ${categories?.size ?: 0}")
            categoryAdapter.submitList(categories ?: emptyList())
        }

        viewModel.selectedCategory.observe(this) { category ->
            category?.let {
                categoryAdapter.setSelectedCategory(it.id)
            }
        }

        viewModel.selectedDate.observe(this) { timestamp ->
            timestamp?.let {
                binding.tvDate.text = DateUtils.formatDate(it)
            }
        }

        // 监听加载的交易记录，更新金额显示
        viewModel.loadedTransaction.observe(this) { transaction ->
            transaction?.let {
                currentAmount = it.amount.toString()
                updateAmountDisplay()
                binding.etDescription.setText(it.description)
                FileLogger.log(TAG, "observeData: Loaded transaction amount = ${it.amount}")
            }
        }
    }
}
