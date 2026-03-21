package com.example.moneytracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import java.util.Calendar

/**
 * 列表项类型
 */
sealed class TransactionListItem {
    data class YearHeader(val year: Int) : TransactionListItem()
    data class TransactionItem(val data: TransactionWithCategory) : TransactionListItem()
}

/**
 * 交易记录适配器（支持年份分隔）
 */
class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Boolean
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    companion object {
        private const val TYPE_YEAR_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.YearHeader -> TYPE_YEAR_HEADER
            is TransactionListItem.TransactionItem -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_YEAR_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_year_header, parent, false)
                YearHeaderViewHolder(view)
            }
            else -> {
                val binding = ItemTransactionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TransactionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.YearHeader -> {
                (holder as YearHeaderViewHolder).bind(item.year)
            }
            is TransactionListItem.TransactionItem -> {
                (holder as TransactionViewHolder).bind(item.data)
            }
        }
    }

    inner class YearHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvYear: TextView = view.findViewById(R.id.tvYear)
        
        fun bind(year: Int) {
            tvYear.text = "${year}年"
        }
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionWithCategory) {
            val transaction = item.transaction
            val category = item.category

            binding.tvCategory.text = category?.name ?: "未知分类"

            binding.tvDescription.text = if (transaction.description.isNotEmpty()) {
                transaction.description
            } else {
                category?.name ?: ""
            }

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

            binding.tvDate.text = DateUtils.formatFullDateTime(transaction.date)

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
                    val iconColor = if (transaction.isIncome()) {
                        ContextCompat.getColor(context, R.color.income)
                    } else {
                        ContextCompat.getColor(context, R.color.expense)
                    }
                    binding.ivCategoryIcon.setColorFilter(iconColor)
                }
            }

            binding.root.setOnClickListener {
                onTransactionClick(transaction)
            }
            binding.root.setOnLongClickListener {
                onTransactionLongClick(transaction)
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean {
            return when {
                oldItem is TransactionListItem.YearHeader && newItem is TransactionListItem.YearHeader -> 
                    oldItem.year == newItem.year
                oldItem is TransactionListItem.TransactionItem && newItem is TransactionListItem.TransactionItem -> 
                    oldItem.data.transaction.id == newItem.data.transaction.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
    
    /**
     * 将交易列表转换为带年份分隔的列表
     */
    fun submitListWithYearHeaders(list: List<TransactionWithCategory>?) {
        if (list.isNullOrEmpty()) {
            submitList(emptyList())
            return
        }
        
        val items = mutableListOf<TransactionListItem>()
        var currentYear = -1
        
        list.forEach { item ->
            val calendar = Calendar.getInstance()
            calendar.time = item.transaction.date
            val year = calendar.get(Calendar.YEAR)
            
            if (year != currentYear) {
                items.add(TransactionListItem.YearHeader(year))
                currentYear = year
            }
            
            items.add(TransactionListItem.TransactionItem(item))
        }
        
        submitList(items)
    }
}

/**
 * 交易记录与分类的组合数据类
 */
data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category?
)
