package com.example.myapplication.ui.owner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.databinding.DialogAddShiftBinding
import com.example.myapplication.databinding.FragmentOwnerShiftBinding
import com.example.myapplication.ui.adapter.OwnerShiftAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OwnerShiftFragment : Fragment() {

    private var _binding: FragmentOwnerShiftBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerShiftAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerShiftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddShift.setOnClickListener {
            showAddShiftDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerShiftAdapter(
            onDeleteClick = { shift -> confirmDelete(shift) }
        )
        binding.rvShifts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShifts.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shifts.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyShift.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddShiftDialog() {
        val dialogBinding = DialogAddShiftBinding.inflate(layoutInflater)
        val calendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dialogBinding.etWorkDate.setText(dateFormat.format(Date()))

        // 1. Mở DatePickerDialog chọn Ngày làm việc (Khóa các ngày quá khứ)
        dialogBinding.etWorkDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dialogBinding.etWorkDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // Khóa các ngày quá khứ (minDate = hôm nay)
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        // 2. Mở TimePickerDialog chọn Giờ bắt đầu
        dialogBinding.etStartTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    dialogBinding.etStartTime.setText(timeStr)
                },
                7, 0, true
            ).show()
        }

        // 3. Mở TimePickerDialog chọn Giờ kết thúc
        dialogBinding.etEndTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    dialogBinding.etEndTime.setText(timeStr)
                },
                15, 0, true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Tạo ca làm") { _, _ ->
                val name = dialogBinding.etShiftName.text.toString().trim()
                val workDate = dialogBinding.etWorkDate.text.toString().trim()
                val startTime = dialogBinding.etStartTime.text.toString().trim()
                val endTime = dialogBinding.etEndTime.text.toString().trim()
                val rateStr = dialogBinding.etHourlyRate.text.toString().trim()
                val multiplierStr = dialogBinding.etOvertimeMultiplier.text.toString().trim()

                if (name.isEmpty() || workDate.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || rateStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng chọn ngày, giờ bắt đầu/kết thúc và mức lương", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val hourlyRate = rateStr.toDoubleOrNull() ?: 30000.0
                val multiplier = multiplierStr.toDoubleOrNull() ?: 1.5

                // Kiểm tra ca đêm: Giờ kết thúc nhỏ hơn Giờ bắt đầu (Ví dụ "04:00" < "20:00")
                val isOvernight = endTime < startTime

                viewModel.addShift(name, workDate, startTime, endTime, hourlyRate, multiplier, isOvernight)

                val msg = if (isOvernight) {
                    "Đã tạo ca đêm $name (xuyên qua nửa đêm) ngày $workDate thành công!"
                } else {
                    "Đã tạo ca $name ngày $workDate thành công!"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDelete(shift: ShiftEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa ca làm việc")
            .setMessage("Bạn có chắc muốn xóa ca ${shift.shiftName} ngày ${shift.workDate}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteShift(shift)
                Toast.makeText(requireContext(), "Đã xóa ca làm việc", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
