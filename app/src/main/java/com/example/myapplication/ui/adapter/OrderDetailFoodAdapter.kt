package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.databinding.ItemOrderDetailFoodBinding
import java.text.NumberFormat
import java.util.Locale

class OrderDetailFoodAdapter :
    ListAdapter<OrderItem, OrderDetailFoodAdapter.FoodViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemOrderDetailFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(private val binding: ItemOrderDetailFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderItem) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvFoodName.text = item.menuItemName
            binding.tvQuantityPrice.text = "Số lượng: x${item.quantity} (Đơn giá: ${formatter.format(item.price)})"
            binding.tvFoodTotal.text = formatter.format(item.price * item.quantity)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<OrderItem>() {
            override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
