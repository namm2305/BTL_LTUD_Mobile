package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.databinding.ItemOwnerShiftBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
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

            val canDelete = !hasShiftEnded(shift)
            binding.btnDeleteShift.visibility = if (canDelete) View.VISIBLE else View.GONE
            binding.btnDeleteShift.setOnClickListener(
                if (canDelete) View.OnClickListener {
                    // Recheck if the row has remained visible past the shift's end.
                    if (hasShiftEnded(shift)) {
                        binding.btnDeleteShift.visibility = View.GONE
                    } else {
                        onDeleteClick(shift)
                    }
                } else null
            )
        }
    }

    private fun hasShiftEnded(shift: ShiftEntity): Boolean {
        // Without a date, the end of a legacy shift cannot be determined safely.
        if (shift.workDate.isBlank()) return true
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            isLenient = false
        }
        val endDate = try {
            format.parse("${shift.workDate} ${shift.expectedEndTime}")
        } catch (_: java.text.ParseException) {
            null
        } ?: return true
        val end = Calendar.getInstance().apply {
            time = endDate
            if (shift.isOvernight || shift.expectedEndTime < shift.expectedStartTime) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return System.currentTimeMillis() >= end.timeInMillis
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
