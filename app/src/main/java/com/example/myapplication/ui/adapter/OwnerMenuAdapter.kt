package com.example.myapplication.ui.adapter

import android.R
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.MenuItem
import com.example.myapplication.databinding.ItemOwnerMenuBinding
import java.text.NumberFormat
import java.util.Locale

class OwnerMenuAdapter(
    private val onEditClick: (MenuItem) -> Unit,
    private val onDeleteClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, OwnerMenuAdapter.MenuViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemOwnerMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuViewHolder(private val binding: ItemOwnerMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvMenuName.text = item.name
            binding.tvMenuCategory.text = "Danh mục: ${item.category}"
            binding.tvMenuPrice.text = formatter.format(item.price)

            val uriStr = item.imageUri ?: item.imageUrl
            if (!uriStr.isNullOrEmpty()) {
                try {
                    binding.ivMenuItemImage.setImageURI(Uri.parse(uriStr))
                } catch (e: Exception) {
                    binding.ivMenuItemImage.setImageResource(R.drawable.ic_menu_gallery)
                }
            } else {
                binding.ivMenuItemImage.setImageResource(R.drawable.ic_menu_gallery)
            }

            binding.btnEditMenuItem.setOnClickListener { onEditClick(item) }
            binding.btnDeleteMenuItem.setOnClickListener { onDeleteClick(item) }
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
