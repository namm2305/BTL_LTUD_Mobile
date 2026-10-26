package com.example.myapplication.ui.adapter

import android.R
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.MenuItem
import com.example.myapplication.databinding.ItemCustomerMenuBinding
import java.text.NumberFormat
import java.util.Locale

class CustomerMenuAdapter(
    private val onAddToCart: (MenuItem) -> Unit
) : ListAdapter<MenuItem, CustomerMenuAdapter.MenuViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemCustomerMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuViewHolder(private val binding: ItemCustomerMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvItemName.text = item.name
            binding.tvItemCategory.text = item.category
            binding.tvItemPrice.text = formatter.format(item.price)

            val uriStr = item.imageUri ?: item.imageUrl
            if (!uriStr.isNullOrEmpty()) {
                try {
                    binding.ivCustomerMenuItemImage.setImageURI(Uri.parse(uriStr))
                } catch (e: Exception) {
                    binding.ivCustomerMenuItemImage.setImageResource(R.drawable.ic_menu_gallery)
                }
            } else {
                binding.ivCustomerMenuItemImage.setImageResource(R.drawable.ic_menu_gallery)
            }

            binding.btnAddToCart.setOnClickListener { onAddToCart(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MenuItem>() {
            override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
