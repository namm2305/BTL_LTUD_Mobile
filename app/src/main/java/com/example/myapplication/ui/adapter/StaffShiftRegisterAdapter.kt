package com.example.myapplication.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.ShiftItemWithRegistrationState
import com.example.myapplication.data.model.ShiftRegistrationStatus
import com.example.myapplication.databinding.ItemStaffRegisterShiftBinding
import java.text.NumberFormat
import java.util.Locale

class StaffShiftRegisterAdapter(
    private val onToggleClick: (ShiftItemWithRegistrationState) -> Unit
) : ListAdapter<ShiftItemWithRegistrationState, StaffShiftRegisterAdapter.ShiftViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftViewHolder {
        val binding = ItemStaffRegisterShiftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ShiftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShiftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShiftViewHolder(private val binding: ItemStaffRegisterShiftBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShiftItemWithRegistrationState) {
            val shift = item.shift
            val status = item.status
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

            binding.tvShiftName.text = shift.shiftName
            binding.tvShiftTime.text = "Thời gian: ${shift.expectedStartTime} - ${shift.expectedEndTime}"
            binding.tvHourlyRate.text = "Lương/giờ: ${formatter.format(shift.hourlyRate)}/h"
            binding.tvOvertimeMultiplier.text = "Hệ số tăng ca: x${shift.overtimeMultiplier}"

            when (status) {
                ShiftRegistrationStatus.COMPLETED -> {
                    binding.btnRegisterShift.text = "Đã hoàn thành"
                    binding.btnRegisterShift.setBackgroundColor(Color.parseColor("#757575")) // Màu Xám
                    binding.btnRegisterShift.isEnabled = false
                    binding.btnRegisterShift.setOnClickListener(null)
                }
                ShiftRegistrationStatus.REGISTERED -> {
                    binding.btnRegisterShift.text = "Hủy đăng ký"
                    binding.btnRegisterShift.setBackgroundColor(Color.parseColor("#D32F2F")) // Màu Đỏ
                    binding.btnRegisterShift.isEnabled = true
                    binding.btnRegisterShift.setOnClickListener { onToggleClick(item) }
                }
                ShiftRegistrationStatus.AVAILABLE -> {
                    binding.btnRegisterShift.text = "Đăng ký"
                    binding.btnRegisterShift.setBackgroundColor(Color.parseColor("#6200EE")) // Màu Tím
                    binding.btnRegisterShift.isEnabled = true
                    binding.btnRegisterShift.setOnClickListener { onToggleClick(item) }
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ShiftItemWithRegistrationState>() {
            override fun areItemsTheSame(
                oldItem: ShiftItemWithRegistrationState,
                newItem: ShiftItemWithRegistrationState
            ): Boolean {
                return oldItem.shift.id == newItem.shift.id
            }

            override fun areContentsTheSame(
                oldItem: ShiftItemWithRegistrationState,
                newItem: ShiftItemWithRegistrationState
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
