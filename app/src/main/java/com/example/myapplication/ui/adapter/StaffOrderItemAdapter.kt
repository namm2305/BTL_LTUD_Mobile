package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.databinding.ItemStaffOrderSubitemBinding
import java.text.NumberFormat
import java.util.Locale

class StaffOrderItemAdapter(private val items: List<OrderItem>) :
    RecyclerView.Adapter<StaffOrderItemAdapter.SubItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubItemViewHolder {
        val binding = ItemStaffOrderSubitemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SubItemViewHolder(private val binding: ItemStaffOrderSubitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderItem) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvItemNameQuantity.text = "${item.quantity}x ${item.menuItemName}"
            binding.tvItemTotalPrice.text = formatter.format(item.price * item.quantity)
        }
    }
}
