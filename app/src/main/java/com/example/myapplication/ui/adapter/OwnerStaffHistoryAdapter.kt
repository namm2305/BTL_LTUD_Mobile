package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.databinding.ItemOwnerStaffHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OwnerStaffHistoryAdapter :
    ListAdapter<AttendanceWithDetails, OwnerStaffHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemOwnerStaffHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemOwnerStaffHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceWithDetails) {
            val attendance = item.attendance
            val shift = item.shift

            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            binding.tvShiftName.text = shift?.shiftName ?: "Ca làm"
            binding.tvWorkDate.text = "Ngày: ${attendance.date}"
            binding.tvActualCheckIn.text = "Check-in: ${timeFormat.format(Date(attendance.actualCheckIn))}"

            if (attendance.actualCheckOut != null) {
                binding.tvActualCheckOut.text = "Check-out: ${timeFormat.format(Date(attendance.actualCheckOut))}"
            } else {
                binding.tvActualCheckOut.text = "Check-out: Chưa"
            }

            binding.tvShiftSalary.text = "Lương ca: ${currencyFormatter.format(attendance.totalSalary)}"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AttendanceWithDetails>() {
            override fun areItemsTheSame(
                oldItem: AttendanceWithDetails,
                newItem: AttendanceWithDetails
            ): Boolean {
                return oldItem.attendance.id == newItem.attendance.id
            }

            override fun areContentsTheSame(
                oldItem: AttendanceWithDetails,
                newItem: AttendanceWithDetails
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
