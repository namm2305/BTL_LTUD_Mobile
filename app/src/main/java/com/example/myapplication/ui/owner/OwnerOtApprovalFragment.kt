package com.example.myapplication.ui.owner

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.databinding.FragmentOwnerOtApprovalBinding
import com.example.myapplication.ui.adapter.OwnerOtApprovalAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.launch

class OwnerOtApprovalFragment : Fragment() {
    private var _binding: FragmentOwnerOtApprovalBinding? = null
    private val viewModel: OwnerViewModel by activityViewModels()
    private var decisionDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val binding = FragmentOwnerOtApprovalBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = requireNotNull(_binding)
        val adapter = OwnerOtApprovalAdapter(::approve, ::reject)
        binding.rvOtRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOtRequests.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingOt.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmptyOt.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun approve(item: AttendanceWithDetails) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Số phút OT được duyệt"
            setText(item.attendance.otMinutes.toString())
            selectAll()
            setPadding(32, 16, 32, 16)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Duyệt tăng ca")
            .setMessage("Nhập từ 1 đến ${item.attendance.otMinutes} phút.")
            .setView(input)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Đồng ý", null)
            .create()
        decisionDialog = dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val minutes = input.text.toString().toLongOrNull()
                if (minutes == null || minutes !in 1..item.attendance.otMinutes) {
                    input.error = "Số phút phải từ 1 đến ${item.attendance.otMinutes}"
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                viewModel.decideOt(item.attendance.id, true, minutes) { success, message ->
                    context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
                    if (success) dialog.dismiss()
                    else dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
            }
        }
        dialog.show()
    }

    private fun reject(item: AttendanceWithDetails) {
        decisionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Từ chối tăng ca")
            .setMessage("Từ chối ${item.attendance.otMinutes} phút OT của ${item.staff?.fullName ?: "nhân viên"}? Lương cơ bản và tiền phạt được giữ nguyên.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Từ chối") { _, _ ->
                viewModel.decideOt(item.attendance.id, false, 0) { _, message ->
                    context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
                }
            }.show()
    }

    override fun onDestroyView() {
        decisionDialog?.dismiss()
        decisionDialog = null
        _binding?.rvOtRequests?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
