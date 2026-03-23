package com.example.moneytracker.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(TAG, "onCreate: Starting AllTransactionsActivity")

        try {
            binding = ActivityAllTransactionsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
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
