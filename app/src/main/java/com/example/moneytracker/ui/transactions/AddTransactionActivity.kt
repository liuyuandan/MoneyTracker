package com.example.moneytracker.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
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
import com.example.moneytracker.utils.ThemeManager
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

    // 计算器相关变量
    private var storedAmount: Double = 0.0  // 存储的第一个数值
    private var currentOperator: String? = null  // 当前操作符 (+, -, ×, ÷)
    private var waitingForSecondNumber = false  // 是否等待输入第二个数

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
        
        // 设置键盘弹出时的滚动处理
        setupKeyboardScrolling()
        
        // 应用强调色
        applyAccentColor()
            
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

        // 计算器操作按钮
        binding.btnAdd.setOnClickListener { setOperator("+") }
        binding.btnSubtract.setOnClickListener { setOperator("-") }
        binding.btnMultiply.setOnClickListener { setOperator("×") }
        binding.btnDivide.setOnClickListener { setOperator("÷") }
        binding.btnEquals.setOnClickListener { calculateResult() }
    }

    private fun setOperator(operator: String) {
        val currentAmountValue = currentAmount.toDoubleOrNull() ?: 0.0

        // 如果已经有操作符在等待，先计算结果
        if (currentOperator != null && waitingForSecondNumber) {
            // 当前正在等待第二个数，但用户又按了操作符，用当前显示的数作为第二个数计算
            if (currentAmount.isNotEmpty()) {
                calculateResult()
            }
        }

        storedAmount = currentAmount.toDoubleOrNull() ?: 0.0
        currentOperator = operator
        waitingForSecondNumber = true
        currentAmount = ""
        updateOperatorDisplay(operator)
    }

    private fun calculateResult() {
        val secondNumber = currentAmount.toDoubleOrNull() ?: 0.0
        val result = when (currentOperator) {
            "+" -> storedAmount + secondNumber
            "-" -> storedAmount - secondNumber
            "×" -> storedAmount * secondNumber
            "÷" -> {
                if (secondNumber != 0.0) {
                    storedAmount / secondNumber
                } else {
                    0.0
                }
            }
            else -> secondNumber
        }

        // 格式化结果
        currentAmount = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.2f", result).removeSuffix(".00").ifEmpty { "0" }
        }

        // 重置操作符状态
        currentOperator = null
        waitingForSecondNumber = false
        storedAmount = 0.0
        updateAmountDisplay()
        updateOperatorDisplay(null)
    }

    private fun updateOperatorDisplay(operator: String?) {
        // 更新金额显示区域，显示当前表达式
        if (operator != null) {
            // 显示：第一个数 + 运算符
            val firstNumber = if (storedAmount == storedAmount.toLong().toDouble()) {
                storedAmount.toLong().toString()
            } else {
                String.format("%.2f", storedAmount)
            }
            binding.tvAmountDisplay.text = "$firstNumber $operator"
        }
        // 如果 operator 为 null，由 updateAmountDisplay 负责更新显示
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
        
        // 如果正在等待第二个数，显示完整的表达式
        if (waitingForSecondNumber && currentOperator != null) {
            val firstNumber = if (storedAmount == storedAmount.toLong().toDouble()) {
                storedAmount.toLong().toString()
            } else {
                String.format("%.2f", storedAmount)
            }
            binding.tvAmountDisplay.text = "$firstNumber $currentOperator $currentAmount"
        } else {
            updateAmountDisplay()
        }
    }

    private fun deleteLastDigit() {
        if (currentAmount.isNotEmpty()) {
            currentAmount = currentAmount.dropLast(1)
            
            // 如果正在等待第二个数，显示完整的表达式
            if (waitingForSecondNumber && currentOperator != null) {
                val firstNumber = if (storedAmount == storedAmount.toLong().toDouble()) {
                    storedAmount.toLong().toString()
                } else {
                    String.format("%.2f", storedAmount)
                }
                val displayText = if (currentAmount.isEmpty()) {
                    "$firstNumber $currentOperator"
                } else {
                    "$firstNumber $currentOperator $currentAmount"
                }
                binding.tvAmountDisplay.text = displayText
            } else {
                updateAmountDisplay()
            }
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

    private fun setupKeyboardScrolling() {
        // 获取数字键盘布局（保存按钮的父布局）
        val numberKeyboardLayout = binding.btnSave.parent as? android.view.ViewGroup

        // 监听布局变化，当系统键盘弹出时隐藏数字键盘
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardVisible = false
            private var lastScrollY = 0

            override fun onGlobalLayout() {
                val rect = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // 如果键盘刚刚弹出
                if (isKeyboardVisible && !wasKeyboardVisible) {
                    // 保存当前滚动位置
                    lastScrollY = binding.scrollViewContent.scrollY

                    // 隐藏数字键盘，让出空间
                    numberKeyboardLayout?.visibility = android.view.View.GONE

                    // 如果备注栏有焦点，滚动到备注栏
                    if (binding.etDescription.hasFocus()) {
                        binding.scrollViewContent.postDelayed({
                            scrollToDescription()
                        }, 100)
                    }
                } else if (!isKeyboardVisible && wasKeyboardVisible) {
                    // 键盘收起时，显示数字键盘并恢复滚动位置
                    numberKeyboardLayout?.visibility = android.view.View.VISIBLE
                    binding.scrollViewContent.post {
                        binding.scrollViewContent.smoothScrollTo(0, lastScrollY)
                    }
                }
                wasKeyboardVisible = isKeyboardVisible
            }
        })

        // 备注栏获得焦点时的处理
        binding.etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 隐藏数字键盘
                numberKeyboardLayout?.visibility = android.view.View.GONE

                // 滚动到备注栏
                binding.scrollViewContent.postDelayed({
                    scrollToDescription()
                }, 200)
            } else {
                // 失去焦点时显示数字键盘
                numberKeyboardLayout?.visibility = android.view.View.VISIBLE
            }
        }

        // 备注栏内容变化时的处理 - 确保光标可见
        binding.etDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.etDescription.postDelayed({
                    val selectionStart = binding.etDescription.selectionStart
                    if (selectionStart >= 0) {
                        val layout = binding.etDescription.layout
                        if (layout != null) {
                            val line = layout.getLineForOffset(selectionStart)
                            val lineTop = layout.getLineTop(line)
                            val lineBottom = layout.getLineBottom(line)
                            val scrollY = binding.etDescription.scrollY
                            val height = binding.etDescription.height

                            if (lineTop < scrollY) {
                                binding.etDescription.scrollTo(0, lineTop)
                            } else if (lineBottom > scrollY + height) {
                                binding.etDescription.scrollTo(0, lineBottom - height)
                            }
                        }
                    }
                }, 50)
            }
        })
    }

    private fun scrollToDescription() {
        val rect = android.graphics.Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val location = IntArray(2)
        binding.etDescription.getLocationInWindow(location)
        val etTop = location[1]

        // 计算需要滚动的距离：让备注栏顶部对齐到可见区域上方留出间距
        val scrollTo = etTop - rect.top - 100 // 留出100dp的顶部间距
        if (scrollTo > 0) {
            binding.scrollViewContent.smoothScrollTo(0, scrollTo)
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
    
    private fun applyAccentColor() {
        // 统一使用白色背景，不再应用主题色
        // 保存按钮使用灰色背景
        binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primary)
        )

        // 状态栏使用白色
        window.statusBarColor = ContextCompat.getColor(this, R.color.card_background)
    }
    
    override fun onResume() {
        super.onResume()
        applyAccentColor()
    }
}
