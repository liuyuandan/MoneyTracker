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

    // 保存原始备注内容，用于回退取消
    private var originalDescription: String = ""
    private var isEditMode: Boolean = false

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
            isEditMode = true
            loadTransactionDetails()
        } else {
            binding.tvTitle.text = getString(R.string.add_transaction)
            isEditMode = false
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
        // nestedScrollingEnabled=false 让 RecyclerView 不拦截滚动事件，交给 ScrollView 处理
        binding.rvCategories.isNestedScrollingEnabled = false
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

    override fun onBackPressed() {
        // 如果是编辑模式且备注内容已修改，恢复原始备注内容
        if (isEditMode) {
            val currentDescription = binding.etDescription.text.toString()
            if (currentDescription != originalDescription) {
                binding.etDescription.setText(originalDescription)
                // 清除焦点，关闭键盘
                binding.etDescription.clearFocus()
                // 显示提示
                Toast.makeText(this, "备注已恢复", Toast.LENGTH_SHORT).show()
                return
            }
        }
        super.onBackPressed()
    }

    private fun setupKeyboardScrolling() {
        // 备注栏获得焦点时，确保其在软键盘弹出后仍然可见
        binding.etDescription.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // 延迟等软键盘完全弹出（adjustResize 触发窗口重新布局）
                binding.scrollViewContent.postDelayed({
                    // 直接滚动到备注栏位置，确保它出现在视口内
                    scrollToDescription()
                }, 300)
            }
        }
    }

    /**
     * 动态计算 RecyclerView 的正确高度。
     * 由于 RecyclerView 嵌套在 ScrollView 中，直接使用 wrap_content
     * 只能测量出可见区域高度，导致只显示部分行。
     * 通过获取第一个 item 的测量高度，再乘以行数，得到正确总高度。
     */
    private fun updateRecyclerViewHeight() {
        val layoutManager = binding.rvCategories.layoutManager as? GridLayoutManager ?: return
        val adapter = binding.rvCategories.adapter ?: return
        if (adapter.itemCount == 0) return

        // 找到第一个可见的 itemView，测量它的实际高度
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible < 0) {
            // 还没测量到，延迟再试
            binding.rvCategories.post { updateRecyclerViewHeight() }
            return
        }
        val itemView = layoutManager.findViewByPosition(firstVisible) ?: return

        // 强制测量，确保拿到正确的宽高
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            binding.rvCategories.width, android.view.View.MeasureSpec.EXACTLY
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        itemView.measure(widthSpec, heightSpec)
        val itemHeight = itemView.measuredHeight

        if (itemHeight <= 0) {
            binding.rvCategories.post { updateRecyclerViewHeight() }
            return
        }

        // 计算总行数：总item数 / 每行span数，向上取整
        val spanCount = layoutManager.spanCount
        val totalRows = (adapter.itemCount + spanCount - 1) / spanCount
        val totalHeight = totalRows * itemHeight

        // 设置 RecyclerView 的高度为计算出的总高度
        val params = binding.rvCategories.layoutParams
        if (params.height != totalHeight) {
            params.height = totalHeight
            binding.rvCategories.layoutParams = params
            FileLogger.log(TAG, "updateRecyclerViewHeight: set height = ${totalHeight}dp ($totalRows rows, $spanCount cols, ${adapter.itemCount} items)")
        }
    }

    /**
     * 滚动 ScrollView，使备注栏可见。
     * 计算备注栏在 ScrollView 内容中的位置，然后滚动到该位置。
     */
    private fun scrollToDescription() {
        val scrollView = binding.scrollViewContent
        val description = binding.etDescription

        // 获取备注栏在其父容器中的顶部位置
        var top = 0
        var current: android.view.View = description
        val parent = current.parent as? android.view.View ?: return

        // 遍历到 ScrollView，累加所有子 View 的高度
        var vp: android.view.View? = parent
        while (vp != null && vp != scrollView) {
            // 对于 LinearLayout 子容器（包含标签、日期、备注等），加上它的 top margin
            top += current.top
            current = vp
            vp = vp.parent as? android.view.View
        }

        // 加上 ScrollView 本身的 padding 和目标上方的额外空间
        val targetScrollY = top - scrollView.paddingTop - 16 // 16dp 的上方间距
        scrollView.scrollTo(0, maxOf(0, targetScrollY))
        FileLogger.log(TAG, "scrollToDescription: scrolling to y = $targetScrollY")
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
            // 等待 RecyclerView 布局完成后，动态计算并设置正确高度
            binding.rvCategories.post {
                updateRecyclerViewHeight()
            }
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
                // 保存原始备注内容，用于回退取消
                originalDescription = it.description ?: ""
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
