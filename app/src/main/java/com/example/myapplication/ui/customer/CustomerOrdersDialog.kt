package com.example.myapplication.ui.customer

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.ui.viewmodel.CustomerViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerOrdersDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity(), ViewModelFactory(requireContext()))
            .get(CustomerViewModel::class.java)
        val list = ListView(requireContext())
        var orders = emptyList<OrderWithItems>()
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
        val date = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        list.setOnItemClickListener { _, _, position, _ ->
            val selected = orders.getOrNull(position) ?: return@setOnItemClickListener
            if (parentFragmentManager.findFragmentByTag("order_editor") == null) {
                OrderEditorDialog.newInstance(selected.order.id, selected.order.tableId)
                    .show(parentFragmentManager, "order_editor")
            }
        }
        val dialog = AlertDialog.Builder(requireContext()).setTitle("Đơn đã gọi của bàn")
            .setView(list).setPositiveButton("Đóng", null).create()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tableOrders.collect { current ->
                    orders = current
                    val labels = current.map {
                        val status = when (it.order.status) {
                            "PENDING" -> "Mới gọi · Có thể sửa"
                            "CONFIRMED" -> "Đã xác nhận"
                            "SERVED" -> "Đã phục vụ"
                            "COMPLETED", "PAID" -> "Đã thanh toán"
                            else -> "Đã hủy"
                        }
                        "Đơn #${it.order.id} · ${date.format(Date(it.order.createdAt))}\n$status · ${format.format(it.order.totalAmount)}"
                    }
                    list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                        if (labels.isEmpty()) listOf("Bàn chưa có đơn đã gọi") else labels)
                }
            }
        }
        return dialog
    }
}
