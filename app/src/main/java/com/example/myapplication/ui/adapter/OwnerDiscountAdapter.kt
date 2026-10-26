package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Discount
import com.example.myapplication.databinding.ItemOwnerDiscountBinding
import java.text.NumberFormat
import java.util.Locale

class OwnerDiscountAdapter(
    private val onDeleteClick: (Discount) -> Unit
) : ListAdapter<Discount, OwnerDiscountAdapter.DiscountViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val binding = ItemOwnerDiscountBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DiscountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiscountViewHolder(private val binding: ItemOwnerDiscountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(discount: Discount) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvDiscountCode.text = discount.code
            binding.tvDiscountPercent.text = "Giảm ${discount.discountPercent}%"
            binding.tvMinOrderAmount.text = "Đơn tối thiểu: ${formatter.format(discount.minOrderAmount)}"

            binding.btnDeleteDiscount.setOnClickListener { onDeleteClick(discount) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Discount>() {
            override fun areItemsTheSame(oldItem: Discount, newItem: Discount): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Discount, newItem: Discount): Boolean {
                return oldItem == newItem
            }
        }
    }
}
