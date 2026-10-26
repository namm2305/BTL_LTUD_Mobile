package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.ShiftRegistrationWithDetails
import com.example.myapplication.databinding.ItemStaffRegisteredShiftBinding
import java.text.NumberFormat
import java.util.Locale

class StaffRegisteredShiftAdapter :
    ListAdapter<ShiftRegistrationWithDetails, StaffRegisteredShiftAdapter.RegistrationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        val binding = ItemStaffRegisteredShiftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RegistrationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RegistrationViewHolder(private val binding: ItemStaffRegisteredShiftBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShiftRegistrationWithDetails) {
            val reg = item.registration
            val shift = item.shift

            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

            binding.tvRegistrationDate.text = "Ngày làm: ${reg.date}"
            binding.tvShiftName.text = shift?.shiftName ?: "Ca làm"
            binding.tvShiftTime.text = "Thời gian quy định: ${shift?.expectedStartTime ?: ""} - ${shift?.expectedEndTime ?: ""}"
            binding.tvHourlyRate.text = "Mức lương: ${formatter.format(shift?.hourlyRate ?: 30000.0)}/h"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ShiftRegistrationWithDetails>() {
            override fun areItemsTheSame(
                oldItem: ShiftRegistrationWithDetails,
                newItem: ShiftRegistrationWithDetails
            ): Boolean {
                return oldItem.registration.id == newItem.registration.id
            }

            override fun areContentsTheSame(
                oldItem: ShiftRegistrationWithDetails,
                newItem: ShiftRegistrationWithDetails
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
