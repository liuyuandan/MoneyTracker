package com.example.moneytracker.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.database.entities.Category
import com.example.moneytracker.databinding.ItemCategoryBinding

/**
 * 分类选择适配器
 */
class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: Long = -1

    fun setSelectedCategory(categoryId: Long) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvName.text = category.name

            // 设置图标
            val context = binding.root.context
            val resourceId = context.resources.getIdentifier(
                category.icon,
                "drawable",
                context.packageName
            )
            if (resourceId != 0) {
                val drawable = ContextCompat.getDrawable(context, resourceId)
                binding.ivIcon.setImageDrawable(drawable)
            }

            // 设置图标颜色
            binding.ivIcon.setColorFilter(category.color)

            // 设置选中状态
            val isSelected = category.id == selectedCategoryId
            binding.root.alpha = if (isSelected) 1.0f else 0.6f

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
