package com.example.moneytracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.data.database.entities.Transaction
import com.example.moneytracker.databinding.ItemDateHeaderBinding
import com.example.moneytracker.databinding.ItemTransactionBinding
import com.example.moneytracker.databinding.ItemYearHeaderBinding
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.DateUtils
import java.util.Calendar

sealed class TransactionListItem {
    class YearHeader(val year: Int) : TransactionListItem()
    class DateHeader(val date: Long, val displayDate: String, val income: Double = 0.0, val expense: Double = 0.0) : TransactionListItem()
    class TransactionItem(val transaction: Transaction, val category: Category?) : TransactionListItem()
}

class GroupedTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_YEAR_HEADER = 2
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    private var items: List<TransactionListItem> = emptyList()

    fun submitList(transactions: List<TransactionWithCategory>) {
        items = groupTransactions(transactions)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionListItem.YearHeader -> VIEW_TYPE_YEAR_HEADER
            is TransactionListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is TransactionListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_YEAR_HEADER -> YearHeaderViewHolder(
                ItemYearHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_TYPE_DATE_HEADER -> DateHeaderViewHolder(
                ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> TransactionViewHolder(
                ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.YearHeader -> (holder as YearHeaderViewHolder).bind(item)
            is TransactionListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is TransactionListItem.TransactionItem -> (holder as TransactionViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class YearHeaderViewHolder(private val binding: ItemYearHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionListItem.YearHeader) {
            binding.tvYearHeader.text = "${item.year}年"
        }
    }

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionListItem.DateHeader) {
            binding.tvDateHeader.text = item.displayDate
            // 显示当天的支出和收入总额
            if (item.expense > 0) {
                binding.tvDayExpense.text = "支 ${CurrencyUtils.formatSimple(item.expense)}"
                binding.tvDayExpense.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDayExpense.visibility = android.view.View.GONE
            }
            if (item.income > 0) {
                binding.tvDayIncome.text = "收 ${CurrencyUtils.formatSimple(item.income)}"
                binding.tvDayIncome.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDayIncome.visibility = android.view.View.GONE
            }
        }
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionListItem.TransactionItem) {
            val transaction = item.transaction
            val category = item.category
            binding.tvCategory.text = category?.name ?: "未知分类"
            binding.tvDescription.text = if (transaction.description.isNotEmpty()) transaction.description else category?.name ?: ""
            binding.tvAmount.text = if (transaction.isIncome()) "+${CurrencyUtils.formatSimple(transaction.amount)}" else "-${CurrencyUtils.formatSimple(transaction.amount)}"
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, if (transaction.isIncome()) R.color.income else R.color.expense))
            binding.tvDate.text = DateUtils.formatTime(transaction.date)
            category?.let { cat ->
                val resourceId = binding.root.context.resources.getIdentifier(cat.icon, "drawable", binding.root.context.packageName)
                if (resourceId != 0) {
                    binding.ivCategoryIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, resourceId))
                    binding.ivCategoryIcon.setColorFilter(if (transaction.isIncome()) ContextCompat.getColor(binding.root.context, R.color.income) else ContextCompat.getColor(binding.root.context, R.color.expense))
                }
            }
            binding.root.setOnClickListener { onTransactionClick(transaction) }
            binding.root.setOnLongClickListener { onTransactionLongClick(transaction) }
        }
    }

    private fun groupTransactions(transactions: List<TransactionWithCategory>): List<TransactionListItem> {
        val result = mutableListOf<TransactionListItem>()
        var currentYear: Int? = null
        var currentDate: Long? = null
        var currentIncome = 0.0
        var currentExpense = 0.0
        
        for (item in transactions) {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = item.transaction.date
            val transactionYear = calendar.get(java.util.Calendar.YEAR)
            val transactionDate = getDayStart(item.transaction.date)
            
            // 检查是否需要添加年份分隔头
            if (currentYear == null || currentYear != transactionYear) {
                // 先保存前一天的总额
                if (currentDate != null) {
                    val lastIndex = result.indexOfLast { it is TransactionListItem.DateHeader && (it as TransactionListItem.DateHeader).date == currentDate }
                    if (lastIndex >= 0) {
                        val oldHeader = result[lastIndex] as TransactionListItem.DateHeader
                        result[lastIndex] = TransactionListItem.DateHeader(oldHeader.date, oldHeader.displayDate, currentIncome, currentExpense)
                    }
                }
                currentYear = transactionYear
                currentDate = null
                currentIncome = 0.0
                currentExpense = 0.0
                result.add(TransactionListItem.YearHeader(transactionYear))
            }
            
            if (currentDate == null || currentDate != transactionDate) {
                // 先保存前一天的总额
                if (currentDate != null) {
                    // 更新前一天的收入支出
                    val lastIndex = result.indexOfLast { it is TransactionListItem.DateHeader && (it as TransactionListItem.DateHeader).date == currentDate }
                    if (lastIndex >= 0) {
                        val oldHeader = result[lastIndex] as TransactionListItem.DateHeader
                        result[lastIndex] = TransactionListItem.DateHeader(oldHeader.date, oldHeader.displayDate, currentIncome, currentExpense)
                    }
                }
                currentDate = transactionDate
                currentIncome = 0.0
                currentExpense = 0.0
                result.add(TransactionListItem.DateHeader(transactionDate, formatDisplayDate(transactionDate)))
            }
            // 累加收入和支出
            if (item.transaction.isIncome()) {
                currentIncome += item.transaction.amount
            } else {
                currentExpense += item.transaction.amount
            }
            result.add(TransactionListItem.TransactionItem(item.transaction, item.category))
        }
        
        // 处理最后一天的总额
        if (currentDate != null) {
            val lastIndex = result.indexOfLast { it is TransactionListItem.DateHeader && (it as TransactionListItem.DateHeader).date == currentDate }
            if (lastIndex >= 0) {
                val oldHeader = result[lastIndex] as TransactionListItem.DateHeader
                result[lastIndex] = TransactionListItem.DateHeader(oldHeader.date, oldHeader.displayDate, currentIncome, currentExpense)
            }
        }
        
        return result
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun formatDisplayDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        return if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)) "今天" else "${month}月${day}日"
    }
}
