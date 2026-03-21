package com.example.moneytracker.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
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
    private var onLongClickListener: ((Category) -> Boolean)? = null
    private var dragListener: OnDragListener? = null
    private var onDragCompleteListener: ((List<Category>) -> Unit)? = null
    // 保存当前拖拽中的列表，避免 currentList 不同步问题
    private var currentDragList: MutableList<Category>? = null

    fun setSelectedCategory(categoryId: Long) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    fun setOnLongClickListener(listener: (Category) -> Boolean) {
        onLongClickListener = listener
    }

    fun setOnDragListener(listener: OnDragListener) {
        dragListener = listener
    }

    fun setOnDragCompleteListener(listener: (List<Category>) -> Unit) {
        onDragCompleteListener = listener
    }

    fun getItemTouchHelperCallback(): ItemTouchHelper.Callback {
        return object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false
                }
                return onItemMove(fromPosition, toPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 不需要滑动删除
            }

            override fun isLongPressDragEnabled(): Boolean = true

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                // 开始拖拽时，初始化拖拽列表
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && currentDragList == null) {
                    currentDragList = currentList.toMutableList()
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 拖拽完成，更新排序
                currentDragList?.let { list ->
                    val updatedList = list.mapIndexed { index, category ->
                        category.copy(sortOrder = index)
                    }
                    onDragCompleteListener?.invoke(updatedList)
                }
                currentDragList = null
            }
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        currentDragList?.let { list ->
            val item = list.removeAt(fromPosition)
            list.add(toPosition, item)
        }
        // 同时更新显示列表
        val displayList = currentList.toMutableList()
        val item = displayList.removeAt(fromPosition)
        displayList.add(toPosition, item)
        submitList(displayList)
        return true
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
            binding.selectedIndicator.visibility = if (isSelected) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            binding.root.alpha = if (isSelected) 1.0f else 0.6f

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }

            binding.root.setOnLongClickListener {
                onLongClickListener?.invoke(category) ?: false
            }

            // 设置拖拽触摸监听
            binding.root.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragListener?.onDragStart(this)
                }
                false
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

    interface OnDragListener {
        fun onDragStart(viewHolder: RecyclerView.ViewHolder)
    }
}
