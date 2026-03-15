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

            setupCategoryRecyclerView()
            setupTabLayout()
            setupDatePicker()
            setupNumberKeyboard()
            setupSaveButton()
            setupBackButton()
            observeData()
            
            FileLogger.log(TAG, "onCreate: Setup completed successfully")
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error during initialization", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupCategoryRecyclerView() {
        FileLogger.log(TAG, "setupCategoryRecyclerView: Setting up category adapter")
        categoryAdapter = CategoryAdapter { category ->
            viewModel.setSelectedCategory(category)
        }
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

    private fun observeData() {
        viewModel.transactionType.observe(this) { type ->
            val isExpense = type == Transaction.TYPE_EXPENSE
            val color = if (isExpense) {
                ContextCompat.getColor(this, R.color.expense)
            } else {
                ContextCompat.getColor(this, R.color.income)
            }
            binding.tvAmountDisplay.setTextColor(color)
        }

        viewModel.categories.observe(this) { categories ->
            FileLogger.log(TAG, "observeData: categories updated, size = ${categories.size}")
            categoryAdapter.submitList(categories)
        }

        viewModel.selectedCategory.observe(this) { category ->
            category?.let {
                categoryAdapter.setSelectedCategory(it.id)
            }
        }

        viewModel.selectedDate.observe(this) { timestamp ->
            binding.tvDate.text = DateUtils.formatDate(timestamp)
        }
    }
}
