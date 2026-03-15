package com.example.moneytracker.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.adapters.TransactionAdapter
import com.example.moneytracker.adapters.TransactionWithCategory
import com.example.moneytracker.databinding.FragmentHomeBinding
import com.example.moneytracker.ui.transactions.AddTransactionActivity
import com.example.moneytracker.utils.CurrencyUtils

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Creating view")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: View created")

        try {
            setupRecyclerView()
            setupFab()
            observeData()
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated: Error setting up view", e)
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Setting up recycler view")
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                try {
                    val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                    intent.putExtra("transaction_id", transaction.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "onTransactionClick: Error", e)
                }
            },
            onTransactionLongClick = { transaction ->
                showDeleteConfirmDialog(transaction)
                true
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun showDeleteConfirmDialog(transaction: com.example.moneytracker.data.database.entities.Transaction) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteTransaction(transaction)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "showDeleteConfirmDialog: Error", e)
        }
    }

    private fun setupFab() {
        Log.d(TAG, "setupFab: Setting up FAB")
        binding.fabAdd.setOnClickListener {
            try {
                Log.d(TAG, "setupFab: FAB clicked, starting AddTransactionActivity")
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "setupFab: Error starting activity", e)
                android.widget.Toast.makeText(requireContext(), "打开失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeData() {
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.tvMonth.text = month
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

        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            val categories = viewModel.categories.value ?: emptyMap()

            if (transactions.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvTransactions.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvTransactions.visibility = View.VISIBLE

                val items = transactions.map { transaction ->
                    TransactionWithCategory(
                        transaction = transaction,
                        category = categories[transaction.categoryId]
                    )
                }
                transactionAdapter.submitList(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Destroying view")
        _binding = null
    }
}
