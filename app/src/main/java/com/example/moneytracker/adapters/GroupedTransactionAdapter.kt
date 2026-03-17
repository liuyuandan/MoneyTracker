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
import com.example.moneytracker.utils.CurrencyUtils
import com.example.moneytracker.utils.DateUtils
import java.util.Calendar

sealed class TransactionListItem {
    class DateHeader(val date: Long, val displayDate: String) : TransactionListItem()
    class TransactionItem(val transaction: Transaction, val category: Category?) : TransactionListItem()
}

class GroupedTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
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
            is TransactionListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is TransactionListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
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
            is TransactionListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is TransactionListItem.TransactionItem -> (holder as TransactionViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionListItem.DateHeader) {
            binding.tvDateHeader.text = item.displayDate
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
        var currentDate: Long? = null
        for (item in transactions) {
            val transactionDate = getDayStart(item.transaction.date)
            if (currentDate == null || currentDate != transactionDate) {
                currentDate = transactionDate
                result.add(TransactionListItem.DateHeader(transactionDate, formatDisplayDate(transactionDate)))
            }
            result.add(TransactionListItem.TransactionItem(item.transaction, item.category))
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
