package com.example.moneytracker.ui.transactions

import android.app.AlertDialog
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
    }

    private lateinit var binding: ActivityAllTransactionsBinding
    private val viewModel: AllTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: GroupedTransactionAdapter
    private var allTransactionsWithCategory: List<TransactionWithCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(TAG, "onCreate: Starting AllTransactionsActivity")

        try {
            binding = ActivityAllTransactionsBinding.inflate(layoutInflater)
            setContentView(binding.root)

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
                showDeleteConfirmDialog(transaction)
                true
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun showDeleteConfirmDialog(transaction: com.example.moneytracker.data.database.entities.Transaction) {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteTransaction(transaction)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            FileLogger.logError(TAG, "showDeleteConfirmDialog: Error", e)
        }
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { transactions ->
            allTransactionsWithCategory = transactions.map { transaction ->
                TransactionWithCategory(
                    transaction = transaction,
                    category = viewModel.categories.value?.get(transaction.categoryId)
                )
            }
            // 如果有搜索词，应用过滤
            val query = binding.etSearch.text?.toString()?.trim() ?: ""
            if (query.isEmpty()) {
                updateTransactionList(allTransactionsWithCategory)
            } else {
                filterTransactions(query)
            }
        }

        viewModel.categories.observe(this) {
            // 当分类变化时，更新列表显示
            allTransactionsWithCategory = (viewModel.transactions.value ?: emptyList()).map { transaction ->
                TransactionWithCategory(
                    transaction = transaction,
                    category = viewModel.categories.value?.get(transaction.categoryId)
                )
            }
            val query = binding.etSearch.text?.toString()?.trim() ?: ""
            if (query.isEmpty()) {
                updateTransactionList(allTransactionsWithCategory)
            } else {
                filterTransactions(query)
            }
        }
    }
}
