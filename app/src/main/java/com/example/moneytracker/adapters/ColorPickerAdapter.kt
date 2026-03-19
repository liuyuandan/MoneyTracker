package com.example.moneytracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R

/**
 * 颜色选择适配器
 */
class ColorPickerAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    private var selectedPosition = 0

    fun getSelectedColor(): Int = colors[selectedPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_picker, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColor: View = itemView.findViewById(R.id.view_color)

        init {
            itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onColorSelected(colors[selectedPosition])
            }
        }

        fun bind(color: Int, isSelected: Boolean) {
            // 使用 GradientDrawable 设置背景和边框
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(color)
            if (isSelected) {
                drawable.setStroke(4, -0xCCCCCC) // 深灰色边框
            }
            viewColor.background = drawable
        }
    }
}
