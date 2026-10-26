package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.User
import com.example.myapplication.databinding.ItemOwnerStaffBinding

class OwnerStaffAdapter(
    private val onItemClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : ListAdapter<User, OwnerStaffAdapter.StaffViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemOwnerStaffBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffViewHolder(private val binding: ItemOwnerStaffBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvStaffFullName.text = user.fullName
            binding.tvStaffUsername.text = "Tài khoản: ${user.username}"
            binding.tvStaffPassword.text = "Mật khẩu: ${user.password}"

            binding.root.setOnClickListener { onItemClick(user) }
            binding.btnDeleteStaff.setOnClickListener { onDeleteClick(user) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}
