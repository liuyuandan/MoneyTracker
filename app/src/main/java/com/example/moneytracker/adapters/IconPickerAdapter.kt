package com.example.moneytracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R

/**
 * 图标选择适配器
 */
class IconPickerAdapter(
    private val icons: List<String>,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    private var selectedPosition = 0

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    fun getSelectedIcon(): String = icons[selectedPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_picker, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = icons.size

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)

        init {
            itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onIconSelected(icons[selectedPosition])
            }
        }

        fun bind(iconName: String, isSelected: Boolean) {
            val context = itemView.context
            val resourceId = context.resources.getIdentifier(
                iconName,
                "drawable",
                context.packageName
            )
            if (resourceId != 0) {
                val drawable = ContextCompat.getDrawable(context, resourceId)
                ivIcon.setImageDrawable(drawable)
            }

            ivIcon.setBackgroundResource(
                if (isSelected) R.drawable.bg_icon_picker_selected
                else R.drawable.bg_icon_picker
            )
        }
    }
}
