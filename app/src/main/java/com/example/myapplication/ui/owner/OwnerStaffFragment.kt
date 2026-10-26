package com.example.myapplication.ui.owner

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
import com.example.myapplication.data.entity.User
import com.example.myapplication.databinding.DialogAddStaffBinding
import com.example.myapplication.databinding.DialogOwnerStaffHistoryBinding
import com.example.myapplication.databinding.FragmentOwnerStaffBinding
import com.example.myapplication.ui.adapter.OwnerStaffAdapter
import com.example.myapplication.ui.adapter.OwnerStaffHistoryAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class OwnerStaffFragment : Fragment() {

    private var _binding: FragmentOwnerStaffBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerStaffAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddStaff.setOnClickListener {
            showAddStaffDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerStaffAdapter(
            onItemClick = { user -> showStaffSalaryDetailDialog(user) },
            onDeleteClick = { user -> confirmDelete(user) }
        )
        binding.rvStaffList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStaffList.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.staffList.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyStaff.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showStaffSalaryDetailDialog(user: User) {
        val dialogBinding = DialogOwnerStaffHistoryBinding.inflate(layoutInflater)
        dialogBinding.tvStaffHistoryTitle.text = "Lịch Sử Ca Làm - ${user.fullName}"

        val historyAdapter = OwnerStaffHistoryAdapter()
        dialogBinding.rvStaffHistoryList.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvStaffHistoryList.adapter = historyAdapter

        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        var currentUnpaidSalary = 0.0

        // Quan sát tổng tiền lương CHƯA TRẢ (isPaid = 0) của nhân viên này thời gian thực
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTotalSalaryByStaff(user.id).collectLatest { totalSalary ->
                currentUnpaidSalary = totalSalary ?: 0.0
                dialogBinding.tvTotalSalaryPaid.text = "LƯƠNG CẦN TRẢ: ${formatter.format(currentUnpaidSalary)}"
                dialogBinding.btnPayAllSalary.isEnabled = currentUnpaidSalary > 0
            }
        }

        // Quan sát danh sách lịch sử chấm công các ca
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAttendanceByStaff(user.id).collectLatest { history ->
                historyAdapter.submitList(history)
                dialogBinding.tvEmptyStaffHistory.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Nút "Thanh toán toàn bộ lương"
        dialogBinding.btnPayAllSalary.setOnClickListener {
            if (currentUnpaidSalary <= 0) {
                Toast.makeText(requireContext(), "Hiện không có lương cần thanh toán", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Thanh toán toàn bộ lương")
                .setMessage("Bạn có chắc chắn muốn xác nhận ĐÃ THANH TOÁN TOÀN BỘ LƯƠNG cho ${user.fullName} (${formatter.format(currentUnpaidSalary)}) không?\n\nSố tiền thu nhập chưa trả của nhân viên sẽ tự động reset về 0đ.")
                .setPositiveButton("Xác nhận đã trả") { _, _ ->
                    viewModel.markSalaryAsPaid(user.id) { success, msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showAddStaffDialog() {
        val dialogBinding = DialogAddStaffBinding.inflate(layoutInflater)

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Tạo tài khoản") { _, _ ->
                val fullName = dialogBinding.etStaffFullName.text.toString().trim()
                val username = dialogBinding.etStaffUsername.text.toString().trim()
                val password = dialogBinding.etStaffPassword.text.toString().trim()

                if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ Họ tên, Username và Password", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addStaff(username, password, fullName) { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa tài khoản nhân viên")
            .setMessage("Bạn có chắc muốn xóa tài khoản của ${user.fullName}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteStaff(user)
                Toast.makeText(requireContext(), "Đã xóa tài khoản nhân viên", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
