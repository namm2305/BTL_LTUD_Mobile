package com.example.myapplication.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.databinding.DialogSelectShiftBinding
import com.example.myapplication.databinding.FragmentStaffShiftManagementBinding
import com.example.myapplication.ui.adapter.OwnerAttendanceAdapter
import com.example.myapplication.ui.adapter.StaffRegisteredShiftAdapter
import com.example.myapplication.ui.viewmodel.StaffViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StaffShiftManagementFragment : Fragment() {

    private var _binding: FragmentStaffShiftManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffViewModel by activityViewModels()

    private var staffUserId: Long = 2
    private var staffName: String = "Nhân Viên A"

    private lateinit var registeredShiftAdapter: StaffRegisteredShiftAdapter
    private lateinit var attendanceHistoryAdapter: OwnerAttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffShiftManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        staffUserId = (activity as? StaffActivity)?.staffUserId ?: 2
        staffName = (activity as? StaffActivity)?.staffName ?: "Nhân Viên A"

        binding.tvStaffName.text = "Nhân viên: $staffName"

        setupHeaderData()
        setupTabsAndRecyclerView()
    }

    private fun setupHeaderData() {
        // Tải trạng thái Check-in active hiện tại
        viewModel.loadActiveAttendance(staffUserId)

        // 1. Quan sát trạng thái Check-in hiện tại thời gian thực
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeAttendance.collectLatest { attendance ->
                if (_binding == null) return@collectLatest
                if (attendance != null) {
                    val timeFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                    val timeStr = timeFormat.format(Date(attendance.actualCheckIn))
                    val shiftName = viewModel.shifts.value.find { it.id == attendance.shiftId }?.shiftName ?: "Ca làm"
                    binding.tvCheckInStatus.text = "Trạng thái: Đã Check-in [$shiftName] lúc $timeStr"
                    binding.btnCheckIn.isEnabled = false
                    binding.btnCheckOut.isEnabled = true
                } else {
                    binding.tvCheckInStatus.text = "Trạng thái: Chưa Check-in"
                    binding.btnCheckIn.isEnabled = true
                    binding.btnCheckOut.isEnabled = false
                }
            }
        }

        // 2. Quan sát Tổng thu nhập cá nhân tích lũy thời gian thực
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getStaffTotalSalary(staffUserId).collectLatest { totalSalary ->
                if (_binding == null) return@collectLatest
                val total = totalSalary ?: 0.0
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvStaffTotalSalary.text = "Thu nhập tích lũy: ${formatter.format(total)}"
            }
        }

        // 3. Sự kiện Nút Check-in
        binding.btnCheckIn.setOnClickListener {
            showCheckInShiftDialog()
        }

        // 4. Sự kiện Nút Check-out (Kèm AlertDialog xác nhận & Toast thông báo thu nhập)
        binding.btnCheckOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Check-out")
                .setMessage("Bạn có chắc chắn muốn Check-out ca làm việc này không?")
                .setPositiveButton("Check-out") { _, _ ->
                    viewModel.checkOutShift(staffUserId) { success, msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        if (success) {
                            // Tải lại trạng thái active
                            viewModel.loadActiveAttendance(staffUserId)
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun showCheckInShiftDialog() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRegisteredShiftsToday(staffUserId, todayDate).collectLatest { registeredList ->
                if (_binding == null) return@collectLatest
                if (registeredList.isEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Chưa đăng ký ca làm")
                        .setMessage("Bạn chưa đăng ký ca làm việc nào cho ngày hôm nay ($todayDate).\n\nVui lòng sang tab 'Đăng ký ca' để đăng ký ca trước!")
                        .setPositiveButton("Đã hiểu", null)
                        .show()
                    return@collectLatest
                }

                val dialogBinding = DialogSelectShiftBinding.inflate(layoutInflater)
                var selectedShift: ShiftEntity? = null
                var firstSelectableSet = false

                registeredList.forEach { regWithDetails ->
                    val shift = regWithDetails.shift
                    if (shift != null) {
                        val canCheckIn = viewModel.canCheckInShift(shift)
                        val radioButton = RadioButton(requireContext()).apply {
                            id = View.generateViewId()
                            if (canCheckIn) {
                                text = "${shift.shiftName} (${shift.expectedStartTime} - ${shift.expectedEndTime})"
                                isEnabled = true
                            } else {
                                text = "${shift.shiftName} (${shift.expectedStartTime} - ${shift.expectedEndTime}) - (Chưa đến giờ Check-in)"
                                isEnabled = false
                            }
                            textSize = 15f
                            setPadding(10, 20, 10, 20)
                        }
                        dialogBinding.rgShifts.addView(radioButton)

                        if (canCheckIn && !firstSelectableSet) {
                            radioButton.isChecked = true
                            selectedShift = shift
                            firstSelectableSet = true
                        }

                        radioButton.setOnClickListener {
                            if (canCheckIn) {
                                selectedShift = shift
                            }
                        }
                    }
                }

                AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.root)
                    .setPositiveButton("Check-in Ca Này") { _, _ ->
                        if (selectedShift != null) {
                            viewModel.checkInShift(staffUserId, selectedShift!!.id) { _, msg ->
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Vui lòng chọn ca làm đã đăng ký và đến giờ Check-in", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        }
    }

    private fun setupTabsAndRecyclerView() {
        registeredShiftAdapter = StaffRegisteredShiftAdapter()
        attendanceHistoryAdapter = OwnerAttendanceAdapter()

        binding.rvShiftManagementList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShiftManagementList.adapter = registeredShiftAdapter

        binding.tabLayoutShiftManagement.addTab(binding.tabLayoutShiftManagement.newTab().setText("Ca Sắp Tới (Đã Đăng Ký)"))
        binding.tabLayoutShiftManagement.addTab(binding.tabLayoutShiftManagement.newTab().setText("Lịch Sử Ca Đã Làm"))

        fun loadTabData(tabPosition: Int) {
            if (tabPosition == 0) {
                // Tab 0: Ca Sắp Tới (Đã Đăng Ký)
                binding.rvShiftManagementList.adapter = registeredShiftAdapter
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.getRegistrationsByStaff(staffUserId).collectLatest { list ->
                        if (_binding == null) return@collectLatest
                        registeredShiftAdapter.submitList(list)
                        binding.tvEmptyShiftManagementList.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } else {
                // Tab 1: Lịch Sử Ca Đã Làm
                binding.rvShiftManagementList.adapter = attendanceHistoryAdapter
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.getAttendanceByStaff(staffUserId).collectLatest { list ->
                        if (_binding == null) return@collectLatest
                        attendanceHistoryAdapter.submitList(list)
                        binding.tvEmptyShiftManagementList.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }

        binding.tabLayoutShiftManagement.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { loadTabData(it) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadTabData(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
