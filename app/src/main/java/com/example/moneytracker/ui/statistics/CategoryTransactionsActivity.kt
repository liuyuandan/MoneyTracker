package com.example.moneytracker.ui.statistics

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.adapters.GroupedTransactionAdapter
import com.example.moneytracker.adapters.TransactionWithCategory
import com.example.moneytracker.databinding.ActivityCategoryTransactionsBinding
import com.example.moneytracker.ui.transactions.AddTransactionActivity
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.FileLogger

class CategoryTransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CategoryTransactionsActivity"
        
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_CATEGORY_ICON = "category_icon"
        const val EXTRA_CATEGORY_COLOR = "category_color"
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_TIME_RANGE_TEXT = "time_range_text"
    }

    private lateinit var binding: ActivityCategoryTransactionsBinding
    private val viewModel: CategoryTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: GroupedTransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(TAG, "onCreate: Starting CategoryTransactionsActivity")

        try {
            binding = ActivityCategoryTransactionsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRecyclerView()
            loadCategoryInfo()
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

    private fun loadCategoryInfo() {
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "分类"
        val categoryIcon = intent.getIntExtra(EXTRA_CATEGORY_ICON, R.drawable.ic_category)
        val categoryColor = intent.getIntExtra(EXTRA_CATEGORY_COLOR, -1)
        val transactionType = intent.getIntExtra(EXTRA_TRANSACTION_TYPE, 0)
        val startTime = intent.getLongExtra(EXTRA_START_TIME, 0)
        val endTime = intent.getLongExtra(EXTRA_END_TIME, 0)
        val timeRangeText = intent.getStringExtra(EXTRA_TIME_RANGE_TEXT) ?: ""

        // 设置分类信息
        binding.tvCategoryName.text = categoryName
        binding.ivCategoryIcon.setImageResource(categoryIcon)
        if (categoryColor != -1) {
            binding.ivCategoryIcon.setColorFilter(categoryColor)
        }
        binding.tvTimeRange.text = timeRangeText

        // 加载数据
        viewModel.loadTransactions(categoryId, transactionType, startTime, endTime)
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
            if (transactions.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvTransactions.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvTransactions.visibility = View.VISIBLE

                val items = transactions.map { transaction ->
                    TransactionWithCategory(
                        transaction = transaction,
                        category = viewModel.categories.value?.get(transaction.categoryId)
                    )
                }
                transactionAdapter.submitList(items)
            }
        }

        viewModel.totalAmount.observe(this) { amount ->
            binding.tvTotalAmount.text = CurrencyUtils.format(amount)
        }

        viewModel.categories.observe(this) {
            // 当分类变化时，更新列表显示
            val transactions = viewModel.transactions.value ?: emptyList()
            if (transactions.isNotEmpty()) {
                val items = transactions.map { transaction ->
                    TransactionWithCategory(
                        transaction = transaction,
                        category = viewModel.categories.value?.get(transaction.categoryId)
                    )
                }
                transactionAdapter.submitList(items)
            }
        }
    }
}
