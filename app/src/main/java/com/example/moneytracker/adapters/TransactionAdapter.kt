package com.example.moneytracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.databinding.ItemTransactionBinding
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.DateUtils

/**
 * 交易记录适配器
 */
class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Boolean
) : ListAdapter<TransactionWithCategory, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionWithCategory) {
            val transaction = item.transaction
            val category = item.category

            // 设置分类名称
            binding.tvCategory.text = category?.name ?: "未知分类"

            // 设置备注
            binding.tvDescription.text = if (transaction.description.isNotEmpty()) {
                transaction.description
            } else {
                category?.name ?: ""
            }

            // 设置金额
            val amountText = if (transaction.isIncome()) {
                "+${CurrencyUtils.formatSimple(transaction.amount)}"
            } else {
                "-${CurrencyUtils.formatSimple(transaction.amount)}"
            }
            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (transaction.isIncome()) R.color.income else R.color.expense
                )
            )

            // 设置日期
            binding.tvDate.text = DateUtils.formatShortDate(transaction.date)

            // 设置图标
            val context = binding.root.context
            category?.let { cat ->
                val resourceId = context.resources.getIdentifier(
                    cat.icon,
                    "drawable",
                    context.packageName
                )
                if (resourceId != 0) {
                    val drawable = ContextCompat.getDrawable(context, resourceId)
                    binding.ivCategoryIcon.setImageDrawable(drawable)
                    binding.ivCategoryIcon.setColorFilter(cat.color)
                }
            }

            // 设置点击事件
            binding.root.setOnClickListener {
                onTransactionClick(transaction)
            }
            binding.root.setOnLongClickListener {
                onTransactionLongClick(transaction)
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithCategory>() {
        override fun areItemsTheSame(
            oldItem: TransactionWithCategory,
            newItem: TransactionWithCategory
        ): Boolean {
            return oldItem.transaction.id == newItem.transaction.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionWithCategory,
            newItem: TransactionWithCategory
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * 交易记录与分类的组合数据类
 */
data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category?
)
