package com.example.myapplication.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.databinding.ItemOwnerAttendanceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OwnerAttendanceAdapter :
    ListAdapter<AttendanceWithDetails, OwnerAttendanceAdapter.AttendanceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemOwnerAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AttendanceViewHolder(private val binding: ItemOwnerAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceWithDetails) {
            val attendance = item.attendance
            val staff = item.staff
            val shift = item.shift

            val timeFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

            binding.tvStaffName.text = staff?.fullName ?: "Nhân viên ID ${attendance.staffId}"
            binding.tvShiftName.text = shift?.shiftName ?: "Ca làm"
            binding.tvShiftTimeExpected.text = "Giờ quy định: ${shift?.expectedStartTime ?: ""} - ${shift?.expectedEndTime ?: ""}"

            binding.tvActualCheckIn.text = "Check-in: ${timeFormat.format(Date(attendance.actualCheckIn))}"

            if (attendance.actualCheckOut != null) {
                binding.tvActualCheckOut.text = "Check-out: ${timeFormat.format(Date(attendance.actualCheckOut))}"
                binding.tvActualCheckOut.setTextColor(Color.parseColor("#388E3C"))
            } else {
                binding.tvActualCheckOut.text = "Trạng thái: Đang trong ca làm việc"
                binding.tvActualCheckOut.setTextColor(Color.parseColor("#E65100"))
            }
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
