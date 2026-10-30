package com.example.myapplication.ui.customer

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.DialogOrderEditorBinding
import com.example.myapplication.ui.adapter.EditableOrderAdapter
import com.example.myapplication.ui.adapter.EditableOrderRow
import com.example.myapplication.ui.staff.StaffActivity
import com.example.myapplication.ui.viewmodel.CustomerViewModel
import com.example.myapplication.ui.viewmodel.OrderEditViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class OrderEditorDialog : DialogFragment() {
    private val viewModel: OrderEditViewModel by viewModels { ViewModelFactory(requireContext()) }
    private var reasonDialog: AlertDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogOrderEditorBinding.inflate(layoutInflater)
        val staff = activity as? StaffActivity
        val tableId = if (staff != null) requireArguments().getLong("tableId") else {
            ViewModelProvider(requireActivity(), ViewModelFactory(requireContext()))
                .get(CustomerViewModel::class.java).selectedTable.value?.id ?: -1
        }
        viewModel.open(requireArguments().getLong("orderId"), tableId, staff?.staffUserId)
        val adapter = EditableOrderAdapter(
            onQuantity = { item, quantity -> viewModel.change(item.id, quantity) },
            onCancel = { item ->
                if (staff == null) {
                    viewModel.change(item.id, 0)
                } else {
                    val input = EditText(requireContext()).apply { hint = "Lý do hủy (tùy chọn)" }
                    reasonDialog = AlertDialog.Builder(requireContext())
                        .setTitle("Hủy ${item.menuItemName}")
                        .setView(input)
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Hủy món") { _, _ ->
                            viewModel.change(item.id, 0, input.text.toString())
                        }.show()
                }
            }
        )
        binding.rvEditableItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEditableItems.adapter = adapter
        binding.btnSendUpdate.setOnClickListener { viewModel.save() }
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val current = state.snapshot
                    if (current == null) {
                        binding.tvOrderNotice.text = "Đang tải đơn hàng…"
                        return@collect
                    }
                    val editable = viewModel.editable(current.order.status)
                    binding.tvOrderInfo.text = "${current.order.tableName} · Đơn #${current.order.id} · ${current.order.status}"
                    binding.tvOrderNotice.text = if (!editable && staff == null && current.order.status != "CANCELLED") {
                        "Đơn hàng đã được xác nhận, vui lòng báo Nhân viên để hỗ trợ hủy/sửa món"
                    } else state.message.ifBlank {
                        when {
                            current.order.status == "CANCELLED" -> "Đơn đã hủy."
                            !editable -> "Đơn đã thanh toán, không thể chỉnh sửa."
                            staff != null -> "Thay đổi được lưu ngay. Có thể thêm lý do khi hủy món."
                            else -> "Chỉnh số lượng hoặc hủy món, sau đó bấm Gửi cập nhật đơn."
                        }
                    }
                    val total = current.items.filter { it.status != "CANCELLED" }.sumOf {
                        it.price * (state.quantities[it.id] ?: it.quantity)
                    }
                    binding.tvOrderTotal.text = "Tổng tiền: ${format.format(total)}" +
                        if (state.quantities.isNotEmpty()) " (chưa gửi)" else ""
                    adapter.submitList(current.items.map {
                        EditableOrderRow(it, state.quantities[it.id] ?: it.quantity, editable && !state.busy)
                    })
                    binding.btnSendUpdate.visibility = if (staff == null && editable) View.VISIBLE else View.GONE
                    binding.btnSendUpdate.isEnabled = !state.busy && state.quantities.isNotEmpty()
                }
            }
        }
        return AlertDialog.Builder(requireContext()).setTitle("Chi tiết đơn đã gọi")
            .setView(binding.root).setPositiveButton("Đóng", null).create()
    }

    override fun onDestroyView() {
        reasonDialog?.dismiss()
        reasonDialog = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(orderId: Long, tableId: Long) = OrderEditorDialog().apply {
            arguments = Bundle().apply { putLong("orderId", orderId); putLong("tableId", tableId) }
        }
    }
}
