package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.data.model.OtPolicy
import com.example.myapplication.databinding.ItemOwnerOtApprovalBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OwnerOtApprovalAdapter(
    private val onApprove: (AttendanceWithDetails) -> Unit,
    private val onReject: (AttendanceWithDetails) -> Unit
) : ListAdapter<AttendanceWithDetails, OwnerOtApprovalAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemOwnerOtApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemOwnerOtApprovalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AttendanceWithDetails) {
            val a = item.attendance
            val format = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            binding.tvStaffName.text = item.staff?.fullName ?: "Nhân viên #${a.staffId}"
            binding.tvShiftInfo.text = "${item.shift?.shiftName ?: "Ca làm"} · ${a.date}"
            binding.tvExpectedEnd.text = item.shift?.let {
                "Giờ ra dự kiến: ${format.format(Date(OtPolicy.expectedEnd(it, a.date)))}"
            } ?: "Không còn thông tin ca làm"
            binding.tvActualEnd.text = "Check-out thực tế: ${a.actualCheckOut?.let { format.format(Date(it)) } ?: "—"}"
            binding.tvOtMinutes.text = "OT chờ duyệt: ${a.otMinutes} phút"
            binding.btnApprove.setOnClickListener { onApprove(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AttendanceWithDetails>() {
        override fun areItemsTheSame(oldItem: AttendanceWithDetails, newItem: AttendanceWithDetails) =
            oldItem.attendance.id == newItem.attendance.id
        override fun areContentsTheSame(oldItem: AttendanceWithDetails, newItem: AttendanceWithDetails) =
            oldItem == newItem
    }
}
