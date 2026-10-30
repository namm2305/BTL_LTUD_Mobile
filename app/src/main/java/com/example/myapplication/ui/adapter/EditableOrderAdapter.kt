package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.databinding.ItemEditableOrderBinding
import java.text.NumberFormat
import java.util.Locale

data class EditableOrderRow(val item: OrderItem, val quantity: Int, val editable: Boolean)

class EditableOrderAdapter(
    private val onQuantity: (OrderItem, Int) -> Unit,
    private val onCancel: (OrderItem) -> Unit
) : ListAdapter<EditableOrderRow, EditableOrderAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemEditableOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemEditableOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: EditableOrderRow) {
            val item = row.item
            val cancelled = item.status == "CANCELLED"
            val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            binding.tvFoodName.text = item.menuItemName + when {
                cancelled -> " · Đã hủy"
                row.quantity == 0 -> " · Sẽ hủy khi gửi cập nhật"
                else -> ""
            }
            binding.tvFoodPrice.text = if (cancelled) {
                "Không tính tiền" + (item.cancellationReason?.takeIf { it.isNotBlank() }?.let { " · Lý do: $it" } ?: "")
            } else "${format.format(item.price)} × ${row.quantity} = ${format.format(item.price * row.quantity)}"
            binding.tvQuantity.text = if (cancelled) "—" else row.quantity.toString()
            binding.btnDecrease.visibility = if (row.editable && !cancelled) View.VISIBLE else View.GONE
            binding.btnIncrease.visibility = binding.btnDecrease.visibility
            binding.btnCancelItem.visibility = binding.btnDecrease.visibility
            binding.btnDecrease.isEnabled = row.quantity > 1
            binding.btnIncrease.isEnabled = row.quantity < 9999
            binding.btnCancelItem.isEnabled = row.quantity > 0
            binding.btnDecrease.setOnClickListener { onQuantity(item, row.quantity - 1) }
            binding.btnIncrease.setOnClickListener { onQuantity(item, row.quantity + 1) }
            binding.btnCancelItem.setOnClickListener { onCancel(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<EditableOrderRow>() {
        override fun areItemsTheSame(oldItem: EditableOrderRow, newItem: EditableOrderRow) =
            oldItem.item.id == newItem.item.id
        override fun areContentsTheSame(oldItem: EditableOrderRow, newItem: EditableOrderRow) = oldItem == newItem
    }
}
