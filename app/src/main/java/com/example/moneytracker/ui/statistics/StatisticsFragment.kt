package com.example.moneytracker.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentStatisticsBinding
import com.example.moneytracker.utils.CurrencyUtils
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

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

        setupMonthNavigation()
        setupCharts()
        observeData()
    }

    private fun setupMonthNavigation() {
        binding.ivPrevMonth.setOnClickListener {
            viewModel.goToPreviousMonth()
        }

        binding.ivNextMonth.setOnClickListener {
            viewModel.goToNextMonth()
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
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.tvCurrentMonth.text = month
        }

        viewModel.monthlyIncome.observe(viewLifecycleOwner) { income ->
            binding.tvIncome.text = CurrencyUtils.format(income)
        }

        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvExpense.text = CurrencyUtils.format(expense)
        }

        viewModel.monthlyBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = CurrencyUtils.format(balance)
        }

        viewModel.expenseCategoryTotals.observe(viewLifecycleOwner) { totals ->
            updatePieChart(binding.pieChartExpense, totals)
        }

        viewModel.incomeCategoryTotals.observe(viewLifecycleOwner) { totals ->
            updatePieChart(binding.pieChartIncome, totals)
        }

        // 同时监听每日支出和收入数据来更新折线图
        viewModel.dailyExpenseTotals.observe(viewLifecycleOwner) { expenseTotals ->
            val incomeTotals = viewModel.dailyIncomeTotals.value ?: emptyList()
            updateLineChart(expenseTotals, incomeTotals)
        }

        viewModel.dailyIncomeTotals.observe(viewLifecycleOwner) { incomeTotals ->
            val expenseTotals = viewModel.dailyExpenseTotals.value ?: emptyList()
            updateLineChart(expenseTotals, incomeTotals)
        }
    }

    private fun updatePieChart(
        chart: com.github.mikephil.charting.charts.PieChart,
        totals: List<com.example.moneytracker.data.database.dao.CategoryTotal>
    ) {
        if (totals.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = totals.map { total ->
            PieEntry(total.totalAmount.toFloat(), total.name)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = totals.map { it.color }
            sliceSpace = 2f
            selectionShift = 5f
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        chart.data = data
        chart.invalidate()
    }

    private fun updateLineChart(
        expenseTotals: List<com.example.moneytracker.data.database.dao.DailyTotal>,
        incomeTotals: List<com.example.moneytracker.data.database.dao.DailyTotal>
    ) {
        if (expenseTotals.isEmpty() && incomeTotals.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        val entries = mutableListOf<Entry>()

        // 添加支出数据
        expenseTotals.forEachIndexed { index, total ->
            entries.add(Entry(index.toFloat(), total.totalAmount.toFloat()))
        }

        val expenseDataSet = LineDataSet(entries, getString(R.string.expense)).apply {
            color = Color.parseColor("#FF5252")
            setCircleColor(Color.parseColor("#FF5252"))
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
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        val lineData = LineData(expenseDataSet, incomeDataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
