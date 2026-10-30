package com.example.myapplication.ui.staff

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.DialogShiftSalaryDetailBinding
import com.example.myapplication.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShiftSalaryDetailDialog : DialogFragment() {
    private val viewModel: StaffViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogShiftSalaryDetailBinding.inflate(layoutInflater)
        val staffId = requireArguments().getLong("staffId")
        val attendanceId = requireArguments().getLong("attendanceId")
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val result = AlertDialog.Builder(requireContext())
            .setTitle("Chi tiết lương ca làm việc")
            .setView(binding.root)
            .setPositiveButton("Đóng", null)
            .create()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAttendanceByStaff(staffId).collect { list ->
                    val item = list.find { it.attendance.id == attendanceId }
                    if (item == null) {
                        dismiss()
                        return@collect
                    }
                    val salary = item.attendance
                    binding.tvShiftInfo.text = "${item.shift?.shiftName ?: "Ca làm"} · ${salary.date}"
                    binding.tvBaseSalary.text = "Lương ca làm việc: +${formatter.format(salary.baseSalary)} VNĐ"
                    binding.tvOvertimeSalary.text = when (salary.otStatus) {
                        "PENDING" -> "Chờ chủ nhà hàng duyệt OT (${salary.otMinutes} phút) · Chưa tính vào lương"
                        "APPROVED" -> "Lương tăng ca (OT): +${formatter.format(salary.overtimeSalary)} VNĐ (Đã duyệt ${salary.approvedOtMinutes} phút OT)"
                        "REJECTED" -> "Yêu cầu OT bị từ chối · OT: 0 VNĐ"
                        else -> "Lương tăng ca (OT): 0 VNĐ"
                    }
                    binding.tvFineAmount.text = "Tiền phạt: -${formatter.format(salary.fineAmount)} VNĐ"
                    binding.tvTotalSalary.text = "Tổng thực nhận ca này: ${formatter.format(salary.totalSalary)} VNĐ"
                    binding.tvTotalSalary.setTextColor(
                        Color.parseColor(if (salary.totalSalary < 0) "#D32F2F" else "#2E7D32")
                    )
                    binding.tvSalaryStatus.text = when {
                        salary.actualCheckOut == null -> "Đang làm việc · Lương chưa chốt"
                        salary.isPaid -> "Đã thanh toán"
                        else -> "Chưa thanh toán"
                    }
                }
            }
        }
        return result
    }

    companion object {
        fun newInstance(staffId: Long, attendanceId: Long) = ShiftSalaryDetailDialog().apply {
            arguments = Bundle().apply {
                putLong("staffId", staffId)
                putLong("attendanceId", attendanceId)
            }
        }
    }
}
