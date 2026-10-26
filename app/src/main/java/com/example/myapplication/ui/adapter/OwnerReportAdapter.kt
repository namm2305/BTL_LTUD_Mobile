package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Order
import com.example.myapplication.databinding.ItemOwnerReportOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OwnerReportAdapter(
    private val onItemClick: (Order) -> Unit
) : ListAdapter<Order, OwnerReportAdapter.OrderReportViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderReportViewHolder {
        val binding = ItemOwnerReportOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderReportViewHolder(private val binding: ItemOwnerReportOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val dateFormatter = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

            binding.tvOrderTable.text = order.tableName
            binding.tvOrderAmount.text = currencyFormatter.format(order.finalAmount)

            val methodText = when (order.paymentMethod) {
                "TRANSFER" -> "Chuyển khoản"
                "CASH" -> "Tiền mặt"
                else -> order.paymentMethod ?: "Khác"
            }
            binding.tvPaymentMethod.text = "Thanh toán: $methodText"

            val time = order.completedAt ?: order.createdAt
            binding.tvCompletedTime.text = dateFormatter.format(Date(time))

            binding.root.setOnClickListener { onItemClick(order) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem == newItem
            }
        }
    }
}
