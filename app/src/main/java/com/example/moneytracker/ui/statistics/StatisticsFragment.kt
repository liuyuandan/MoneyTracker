package com.example.moneytracker.ui.statistics

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
import com.example.moneytracker.utils.CurrencyUtils
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

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
        // 直接为每个 Chip 设置选中状态监听器
        binding.chipMonth.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked && !isUpdatingFromViewModel) {
                viewModel.setViewMode(false)
            }
        }

        binding.chipYear.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked && !isUpdatingFromViewModel) {
                viewModel.setViewMode(true)
            }
        }
    }

    private fun setupCharts() {
        // 设置饼图
        setupPieChart(binding.pieChartExpense)
        setupPieChart(binding.pieChartIncome)

        // 设置折线图
        setupLineChart()
    }

    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart) {
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
        }
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
            val isYearView = viewModel.isYearView.value ?: false
            updateLineChart(expenseTotals, incomeTotals, isYearView)
        }

        viewModel.periodIncomeTotals.observe(viewLifecycleOwner) { incomeTotals ->
            val expenseTotals = viewModel.periodExpenseTotals.value ?: emptyList()
            val isYearView = viewModel.isYearView.value ?: false
            updateLineChart(expenseTotals, incomeTotals, isYearView)
        }

        // 监听视图模式变化，更新 ChipGroup 选中状态
        viewModel.isYearView.observe(viewLifecycleOwner) { isYear ->
            isUpdatingFromViewModel = true
            if (isYear) {
                binding.chipGroupPeriod.check(R.id.chip_year)
            } else {
                binding.chipGroupPeriod.check(R.id.chip_month)
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
        isYearView: Boolean
    ) {
        if (expenseTotals.isEmpty() && incomeTotals.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        // 添加支出数据
        val expenseEntries = mutableListOf<Entry>()
        expenseTotals.forEachIndexed { index, total ->
            expenseEntries.add(Entry(index.toFloat(), total.totalAmount.toFloat()))
        }

        val expenseDataSet = LineDataSet(expenseEntries, getString(R.string.expense)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.expense)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        // 添加收入数据
        val incomeEntries = mutableListOf<Entry>()
        incomeTotals.forEachIndexed { index, total ->
            incomeEntries.add(Entry(index.toFloat(), total.totalAmount.toFloat()))
        }

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
        binding.lineChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
            if (isYearView) {
                listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
            } else {
                (1..31).map { "${it}日" }
            }
        )
        
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
