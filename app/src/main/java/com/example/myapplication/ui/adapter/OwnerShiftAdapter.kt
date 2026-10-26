package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.databinding.ItemOwnerShiftBinding
import java.text.NumberFormat
import java.util.Locale

class OwnerShiftAdapter(
    private val onDeleteClick: (ShiftEntity) -> Unit
) : ListAdapter<ShiftEntity, OwnerShiftAdapter.ShiftViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftViewHolder {
        val binding = ItemOwnerShiftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ShiftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShiftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShiftViewHolder(private val binding: ItemOwnerShiftBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shift: ShiftEntity) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

            binding.tvShiftName.text = shift.shiftName
            binding.tvWorkDate.text = "Ngày làm: ${if (shift.workDate.isNotBlank()) shift.workDate else "Tất cả các ngày"}"
            binding.tvShiftTime.text = "Thời gian: ${shift.expectedStartTime} - ${shift.expectedEndTime}"
            binding.tvHourlyRate.text = "Lương/giờ: ${formatter.format(shift.hourlyRate)}/h"
            binding.tvOvertimeMultiplier.text = "Hệ số tăng ca: x${shift.overtimeMultiplier}"

            binding.btnDeleteShift.setOnClickListener { onDeleteClick(shift) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ShiftEntity>() {
            override fun areItemsTheSame(oldItem: ShiftEntity, newItem: ShiftEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShiftEntity, newItem: ShiftEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
