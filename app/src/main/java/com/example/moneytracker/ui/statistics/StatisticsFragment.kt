package com.example.moneytracker.ui.statistics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentStatisticsBinding
import com.example.moneytracker.data.database.entities.CategoryTotal
import com.example.moneytracker.data.database.entities.DailyTotal
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.ThemeManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()
    
    private val logBuilder = StringBuilder()
    
    private fun log(message: String) {
        android.util.Log.d("StatisticsFragment", message)
        logBuilder.append(message).append("\n")
    }
    
    private fun saveLogToFile() {
        try {
            val logFile = File(requireContext().getExternalFilesDir(null), "statistics_debug.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            logFile.appendText("=== $timestamp ===\n${logBuilder.toString()}\n\n")
            logBuilder.clear()
        } catch (e: Exception) {
            android.util.Log.e("StatisticsFragment", "Failed to save log", e)
        }
    }

    // 用于避免循环触发的标志位
    private var isUpdatingFromViewModel = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPeriodNavigation()
        setupPeriodToggle()
        setupCharts()
        observeData()
        applyTheme()
    }

    private fun setupPeriodNavigation() {
        binding.ivPrevMonth.setOnClickListener {
            viewModel.goToPreviousPeriod()
        }

        binding.ivNextMonth.setOnClickListener {
            viewModel.goToNextPeriod()
        }
    }

    private fun setupPeriodToggle() {
        // 周视图
        binding.chipWeek.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked && !isUpdatingFromViewModel) {
                viewModel.setViewMode(StatisticsViewModel.VIEW_MODE_WEEK)
            }
        }

        // 月视图
        binding.chipMonth.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked && !isUpdatingFromViewModel) {
                viewModel.setViewMode(StatisticsViewModel.VIEW_MODE_MONTH)
            }
        }

        // 年视图
        binding.chipYear.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked && !isUpdatingFromViewModel) {
                viewModel.setViewMode(StatisticsViewModel.VIEW_MODE_YEAR)
            }
        }
    }

    private fun setupCharts() {
        // 设置饼图
        setupPieChart(binding.pieChartExpense, Transaction.TYPE_EXPENSE)
        setupPieChart(binding.pieChartIncome, Transaction.TYPE_INCOME)

        // 设置折线图
        setupLineChart()
    }

    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart, transactionType: Int) {
        chart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 55f
            legend.apply {
                isEnabled = true
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                textSize = 12f
            }
            setNoDataText(getString(R.string.no_data))
            
            // 设置点击监听
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is PieEntry) {
                        val dataSet = chart.data?.getDataSetByIndex(0) as? PieDataSet
                        val index = dataSet?.values?.indexOf(e) ?: -1
                        if (index >= 0) {
                            val totals = if (transactionType == Transaction.TYPE_EXPENSE) {
                                viewModel.expenseCategoryTotals.value
                            } else {
                                viewModel.incomeCategoryTotals.value
                            }
                            totals?.getOrNull(index)?.let { categoryTotal ->
                                openCategoryTransactions(categoryTotal, transactionType)
                            }
                        }
                    }
                }
                
                override fun onNothingSelected() {}
            })
        }
    }

    private fun openCategoryTransactions(categoryTotal: CategoryTotal, transactionType: Int) {
        val viewMode = viewModel.viewMode.value ?: StatisticsViewModel.VIEW_MODE_MONTH
        val startTime: Long
        val endTime: Long

        when (viewMode) {
            StatisticsViewModel.VIEW_MODE_WEEK -> {
                startTime = com.example.moneytracker.utils.DateUtils.getWeekStart(viewModel.currentTimestamp)
                endTime = com.example.moneytracker.utils.DateUtils.getWeekEnd(viewModel.currentTimestamp)
            }
            StatisticsViewModel.VIEW_MODE_YEAR -> {
                startTime = com.example.moneytracker.utils.DateUtils.getYearStart(viewModel.currentTimestamp)
                endTime = com.example.moneytracker.utils.DateUtils.getYearEnd(viewModel.currentTimestamp)
            }
            else -> {
                startTime = com.example.moneytracker.utils.DateUtils.getMonthStart(viewModel.currentTimestamp)
                endTime = com.example.moneytracker.utils.DateUtils.getMonthEnd(viewModel.currentTimestamp)
            }
        }

        val timeRangeText = viewModel.currentPeriod.value ?: ""

        val intent = Intent(requireContext(), CategoryTransactionsActivity::class.java).apply {
            putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_ID, categoryTotal.id)
            putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_NAME, categoryTotal.name)
            putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_ICON, categoryTotal.icon)
            putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_COLOR, categoryTotal.color)
            putExtra(CategoryTransactionsActivity.EXTRA_TRANSACTION_TYPE, transactionType)
            putExtra(CategoryTransactionsActivity.EXTRA_START_TIME, startTime)
            putExtra(CategoryTransactionsActivity.EXTRA_END_TIME, endTime)
            putExtra(CategoryTransactionsActivity.EXTRA_TIME_RANGE_TEXT, timeRangeText)
        }
        startActivity(intent)
    }

    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.apply {
                isEnabled = true
                textSize = 12f
            }
            setNoDataText(getString(R.string.no_data))
        }
    }

    private fun observeData() {
        viewModel.currentPeriod.observe(viewLifecycleOwner) { period ->
            binding.tvCurrentMonth.text = period
        }

        viewModel.periodIncome.observe(viewLifecycleOwner) { income ->
            binding.tvIncome.text = CurrencyUtils.format(income)
        }

        viewModel.periodExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvExpense.text = CurrencyUtils.format(expense)
        }

        viewModel.periodBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = CurrencyUtils.format(balance)
        }

        viewModel.expenseCategoryTotals.observe(viewLifecycleOwner) { totals ->
            updatePieChart(binding.pieChartExpense, totals)
        }

        viewModel.incomeCategoryTotals.observe(viewLifecycleOwner) { totals ->
            updatePieChart(binding.pieChartIncome, totals)
        }

        // 监听趋势数据
        viewModel.periodExpenseTotals.observe(viewLifecycleOwner) { expenseTotals ->
            val incomeTotals = viewModel.periodIncomeTotals.value ?: emptyList()
            val viewMode = viewModel.viewMode.value ?: StatisticsViewModel.VIEW_MODE_MONTH
            updateLineChart(expenseTotals, incomeTotals, viewMode)
        }

        viewModel.periodIncomeTotals.observe(viewLifecycleOwner) { incomeTotals ->
            val expenseTotals = viewModel.periodExpenseTotals.value ?: emptyList()
            val viewMode = viewModel.viewMode.value ?: StatisticsViewModel.VIEW_MODE_MONTH
            updateLineChart(expenseTotals, incomeTotals, viewMode)
        }

        // 监听视图模式变化，更新 ChipGroup 选中状态
        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            isUpdatingFromViewModel = true
            when (mode) {
                StatisticsViewModel.VIEW_MODE_WEEK -> binding.chipGroupPeriod.check(R.id.chip_week)
                StatisticsViewModel.VIEW_MODE_MONTH -> binding.chipGroupPeriod.check(R.id.chip_month)
                StatisticsViewModel.VIEW_MODE_YEAR -> binding.chipGroupPeriod.check(R.id.chip_year)
            }
            isUpdatingFromViewModel = false
        }
    }

    private fun updatePieChart(
        chart: com.github.mikephil.charting.charts.PieChart,
        totals: List<CategoryTotal>
    ) {
        if (totals.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = totals.map { total ->
            // 图例显示：分类名称 + 金额，如 "午餐 ¥1000.00"
            val labelWithAmount = "${total.name} ${CurrencyUtils.format(total.totalAmount)}"
            PieEntry(total.totalAmount.toFloat(), labelWithAmount)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = totals.map { it.color }
            sliceSpace = 2f
            selectionShift = 5f
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f%%", value)
                }
            })
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        chart.data = data
        chart.invalidate()
    }

    private fun updateLineChart(
        expenseTotals: List<DailyTotal>,
        incomeTotals: List<DailyTotal>,
        viewMode: Int
    ) {
        val isYearView = viewMode == StatisticsViewModel.VIEW_MODE_YEAR
        val isWeekView = viewMode == StatisticsViewModel.VIEW_MODE_WEEK
        if (expenseTotals.isEmpty() && incomeTotals.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        // 辅助函数：将时间戳归一化到当天开始（去掉时分秒）
        fun normalizeToDay(timestamp: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // 辅助函数：将时间戳归一化到月初
        fun normalizeToMonth(timestamp: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // 获取有交易数据的日期列表（归一化后）
        val transactionDays = (expenseTotals + incomeTotals).map { 
            if (isYearView) normalizeToMonth(it.day) else normalizeToDay(it.day)
        }.distinct().sorted()
        
        log("=== updateLineChart Debug ===")
        log("isYearView: $isYearView")
        log("expenseTotals count: ${expenseTotals.size}")
        expenseTotals.forEach { 
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.day
            log("expense - day: ${cal.get(java.util.Calendar.DAY_OF_MONTH)}日, raw: ${it.day}, amount: ${it.totalAmount}")
        }
        log("incomeTotals count: ${incomeTotals.size}")
        incomeTotals.forEach { 
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.day
            log("income - day: ${cal.get(java.util.Calendar.DAY_OF_MONTH)}日, raw: ${it.day}, amount: ${it.totalAmount}")
        }
        log("transactionDays (normalized): ${transactionDays.map { 
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it
            if (isYearView) "${cal.get(java.util.Calendar.MONTH) + 1}月" else "${cal.get(java.util.Calendar.DAY_OF_MONTH)}日"
        }}")
        
        // 找出第一笔交易和最后一笔交易的日期
        val firstDay = transactionDays.first()
        val lastDay = transactionDays.last()
        
        // 生成从第一笔交易到最后一笔交易之间的连续日期
        val allDays = mutableListOf<Long>()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = firstDay
        
        if (isYearView) {
            // 年度视图：按月生成
            val lastMonthCalendar = java.util.Calendar.getInstance()
            lastMonthCalendar.timeInMillis = lastDay
            
            while (calendar.before(lastMonthCalendar) || calendar.timeInMillis == lastMonthCalendar.timeInMillis) {
                allDays.add(calendar.timeInMillis)
                calendar.add(java.util.Calendar.MONTH, 1)
            }
        } else {
            // 月度视图：按日生成
            val lastDayCalendar = java.util.Calendar.getInstance()
            lastDayCalendar.timeInMillis = lastDay
            // 确保最后一天也归一化
            lastDayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            lastDayCalendar.set(java.util.Calendar.MINUTE, 0)
            lastDayCalendar.set(java.util.Calendar.SECOND, 0)
            lastDayCalendar.set(java.util.Calendar.MILLISECOND, 0)
            
            while (calendar.before(lastDayCalendar) || calendar.timeInMillis == lastDayCalendar.timeInMillis) {
                allDays.add(calendar.timeInMillis)
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        // 创建日期到索引的映射（使用归一化后的时间戳）
        val dayToIndex = allDays.withIndex().associate { it.value to it.index }
        
        // 生成X轴标签
        val xLabels = allDays.map { day ->
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = day
            if (isYearView) {
                "${cal.get(java.util.Calendar.MONTH) + 1}月"
            } else {
                "${cal.get(java.util.Calendar.DAY_OF_MONTH)}日"
            }
        }
        
        log("allDays count: ${allDays.size}, labels: $xLabels")
        log("dayToIndex mapping: ${dayToIndex.map { (day, index) ->
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = day
            "${cal.get(java.util.Calendar.DAY_OF_MONTH)}日 -> $index"
        }}")

        // 添加支出数据 - 先按日期合并金额
        val expenseByDay = mutableMapOf<Long, Double>()
        expenseTotals.forEach { total ->
            val normalizedDay = if (isYearView) normalizeToMonth(total.day) else normalizeToDay(total.day)
            expenseByDay[normalizedDay] = (expenseByDay[normalizedDay] ?: 0.0) + total.totalAmount
        }
        
        val expenseEntries = mutableListOf<Entry>()
        expenseByDay.forEach { (day, amount) ->
            val index = dayToIndex[day] ?: 0
            log("expense entry - day: ${java.util.Calendar.getInstance().apply { timeInMillis = day }.get(java.util.Calendar.DAY_OF_MONTH)}日, index: $index, amount: $amount")
            expenseEntries.add(Entry(index.toFloat(), amount.toFloat()))
        }
        
        // 按索引排序，确保数据点按日期顺序显示
        expenseEntries.sortBy { it.x }
        log("expenseEntries after sort: ${expenseEntries.map { "x=${it.x}, y=${it.y}" }}")

        val expenseDataSet = LineDataSet(expenseEntries, getString(R.string.expense)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.expense)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        // 添加收入数据 - 先按日期合并金额
        val incomeByDay = mutableMapOf<Long, Double>()
        incomeTotals.forEach { total ->
            val normalizedDay = if (isYearView) normalizeToMonth(total.day) else normalizeToDay(total.day)
            incomeByDay[normalizedDay] = (incomeByDay[normalizedDay] ?: 0.0) + total.totalAmount
        }
        
        val incomeEntries = mutableListOf<Entry>()
        incomeByDay.forEach { (day, amount) ->
            val index = dayToIndex[day] ?: 0
            log("income entry - day: ${java.util.Calendar.getInstance().apply { timeInMillis = day }.get(java.util.Calendar.DAY_OF_MONTH)}日, index: $index, amount: $amount")
            incomeEntries.add(Entry(index.toFloat(), amount.toFloat()))
        }
        
        // 按索引排序，确保数据点按日期顺序显示
        incomeEntries.sortBy { it.x }
        log("incomeEntries after sort: ${incomeEntries.map { "x=${it.x}, y=${it.y}" }}")

        val incomeDataSet = LineDataSet(incomeEntries, getString(R.string.income)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.income)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.income))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        val lineData = LineData(expenseDataSet, incomeDataSet)
        binding.lineChart.data = lineData
        
        // 设置X轴标签
        binding.lineChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(xLabels)
        binding.lineChart.xAxis.labelCount = xLabels.size
        binding.lineChart.xAxis.setLabelCount(xLabels.size, true)  // 强制显示所有标签
        binding.lineChart.xAxis.granularity = 1f  // 设置最小间隔为1，确保每个标签都能显示
        binding.lineChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        binding.lineChart.xAxis.setDrawGridLines(false)
        
        // 设置Y轴
        binding.lineChart.axisLeft.setDrawGridLines(true)
        binding.lineChart.axisRight.isEnabled = false
        
        // 设置图表其他属性
        binding.lineChart.description.isEnabled = false
        binding.lineChart.setDrawGridBackground(false)
        binding.lineChart.legend.isEnabled = true
        
        binding.lineChart.invalidate()
        
        // 保存日志到文件
        saveLogToFile()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun applyTheme() {
        ThemeManager.applyTheme(requireContext(), binding.rootLayout)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
