package com.example.moneytracker.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.adapters.GroupedTransactionAdapter
import com.example.moneytracker.adapters.TransactionWithCategory
import com.example.moneytracker.databinding.ActivityAllTransactionsBinding
import com.example.moneytracker.utils.FileLogger

class AllTransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AllTransactionsActivity"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_PERIOD_NAME = "period_name"
    }

    private lateinit var binding: ActivityAllTransactionsBinding
    private val viewModel: AllTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: GroupedTransactionAdapter
    private var allTransactionsWithCategory: List<TransactionWithCategory> = emptyList()
    private var filterCategoryId: Long = -1
    private var filterCategoryName: String? = null
    private var filterStartTime: Long = -1
    private var filterEndTime: Long = -1
    private var filterPeriodName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(TAG, "onCreate: Starting AllTransactionsActivity")

        try {
            binding = ActivityAllTransactionsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 获取传递的分类ID和名称
            filterCategoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1)
            filterCategoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
            // 获取时间范围
            filterStartTime = intent.getLongExtra(EXTRA_START_TIME, -1)
            filterEndTime = intent.getLongExtra(EXTRA_END_TIME, -1)
            filterPeriodName = intent.getStringExtra(EXTRA_PERIOD_NAME)

            setupToolbar()
            setupSearch()
            setupRecyclerView()
            observeData()
        } catch (e: Exception) {
            FileLogger.logError(TAG, "onCreate: Error", e)
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 如果有过滤分类，更新标题
        val titleBuilder = StringBuilder()
        filterCategoryName?.let { name ->
            titleBuilder.append(name)
        }
        filterPeriodName?.let { period ->
            if (titleBuilder.isNotEmpty()) {
                titleBuilder.append(" - ")
            }
            titleBuilder.append(period)
        }
        if (titleBuilder.isNotEmpty()) {
            binding.toolbar.title = titleBuilder.toString()
        }
    }

    private fun setupSearch() {
        // 搜索框文本变化监听
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            filterTransactions(query)
            binding.ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // 清除搜索按钮
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        // 键盘搜索按钮
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString()?.trim() ?: ""
                filterTransactions(query)
                true
            } else {
                false
            }
        }
    }

    private fun filterTransactions(query: String) {
        if (query.isEmpty()) {
            // 显示全部记录
            updateTransactionList(allTransactionsWithCategory)
        } else {
            // 过滤记录
            val filtered = allTransactionsWithCategory.filter { item ->
                val categoryName = item.category?.name ?: ""
                val description = item.transaction.description
                categoryName.contains(query, ignoreCase = true) || 
                description.contains(query, ignoreCase = true)
            }
            updateTransactionList(filtered)
        }
    }

    private fun updateTransactionList(items: List<TransactionWithCategory>) {
        if (items.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvTransactions.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
            transactionAdapter.submitList(items)
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = GroupedTransactionAdapter(
            onTransactionClick = { transaction ->
                try {
                    val intent = Intent(this, AddTransactionActivity::class.java)
                    intent.putExtra("transaction_id", transaction.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    FileLogger.logError(TAG, "onTransactionClick: Error", e)
                }
            },
            onTransactionLongClick = { transaction ->
                viewModel.deleteTransaction(transaction)
                true
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { transactions ->
            allTransactionsWithCategory = transactions.map { transaction ->
                TransactionWithCategory(
                    transaction = transaction,
                    category = viewModel.categories.value?.get(transaction.categoryId)
                )
            }
            applyFilter()
        }

        viewModel.categories.observe(this) {
            // 当分类变化时，更新列表显示
            allTransactionsWithCategory = (viewModel.transactions.value ?: emptyList()).map { transaction ->
                TransactionWithCategory(
                    transaction = transaction,
                    category = viewModel.categories.value?.get(transaction.categoryId)
                )
            }
            applyFilter()
        }
    }
    
    private fun applyFilter() {
        var filteredList = allTransactionsWithCategory
        
        // 如果有分类过滤，先按分类过滤
        if (filterCategoryId != -1L) {
            filteredList = filteredList.filter { it.transaction.categoryId == filterCategoryId }
        }
        
        // 如果有时间范围过滤，按时间过滤
        if (filterStartTime != -1L && filterEndTime != -1L) {
            filteredList = filteredList.filter { 
                it.transaction.timestamp in filterStartTime..filterEndTime 
            }
        }
        
        // 如果有搜索词，再按搜索词过滤
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { item ->
                val categoryName = item.category?.name ?: ""
                val description = item.transaction.description
                categoryName.contains(query, ignoreCase = true) || 
                description.contains(query, ignoreCase = true)
            }
        }
        
        updateTransactionList(filteredList)
    }
}
