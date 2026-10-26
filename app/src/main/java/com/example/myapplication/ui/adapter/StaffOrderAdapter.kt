package com.example.myapplication.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.databinding.ItemStaffOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StaffOrderAdapter(
    private val onServeClick: (OrderWithItems) -> Unit,
    private val onPaymentClick: (OrderWithItems) -> Unit
) : ListAdapter<OrderWithItems, StaffOrderAdapter.OrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemStaffOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemStaffOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(orderWithItems: OrderWithItems) {
            val order = orderWithItems.order
            val items = orderWithItems.items

            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val timeFormatter = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())

            binding.tvTableName.text = order.tableName
            binding.tvOrderTime.text = "Thời gian đặt: ${timeFormatter.format(Date(order.createdAt))}"
            binding.tvTotalAmount.text = "Tổng: ${currencyFormatter.format(order.totalAmount)}"

            if (order.status == Order.STATUS_PENDING) {
                binding.tvOrderStatus.text = "Chờ phục vụ"
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FF9800"))
                binding.btnServeOrder.visibility = View.VISIBLE
            } else {
                binding.tvOrderStatus.text = "Đã phục vụ"
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                binding.btnServeOrder.visibility = View.GONE
            }

            // Setup sub RecyclerView cho danh sách món
            val subAdapter = StaffOrderItemAdapter(items)
            binding.rvOrderItems.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvOrderItems.adapter = subAdapter

            binding.btnServeOrder.setOnClickListener { onServeClick(orderWithItems) }
            binding.btnPayment.setOnClickListener { onPaymentClick(orderWithItems) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<OrderWithItems>() {
            override fun areItemsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems): Boolean {
                return oldItem.order.id == newItem.order.id
            }

            override fun areContentsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems): Boolean {
                return oldItem == newItem
            }
        }
    }
}
