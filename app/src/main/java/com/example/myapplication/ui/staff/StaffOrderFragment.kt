package com.example.myapplication.ui.staff

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
import com.example.myapplication.data.entity.Discount
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.databinding.DialogStaffPaymentBinding
import com.example.myapplication.databinding.FragmentStaffOrderBinding
import com.example.myapplication.ui.adapter.StaffOrderAdapter
import com.example.myapplication.ui.customer.OrderEditorDialog
import com.example.myapplication.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StaffOrderFragment : Fragment() {

    private var _binding: FragmentStaffOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffViewModel by activityViewModels()
    private lateinit var adapter: StaffOrderAdapter

    private var staffUserId: Long = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        staffUserId = (activity as? StaffActivity)?.staffUserId ?: 2

        setupRecyclerView()
        observeShiftStatusAndActiveOrders()
    }

    private fun setupRecyclerView() {
        adapter = StaffOrderAdapter(
            onServeClick = { orderWithItems ->
                val next = if (orderWithItems.order.status == Order.STATUS_PENDING)
                    Order.STATUS_CONFIRMED else Order.STATUS_SERVED
                viewModel.updateOrderStatus(orderWithItems.order, next) { _, message ->
                    context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
                }
            },
            onPaymentClick = { orderWithItems ->
                showPaymentDialog(orderWithItems)
            },
            onEditClick = { order ->
                if (childFragmentManager.findFragmentByTag("order_editor") == null) {
                    OrderEditorDialog.newInstance(order.order.id, order.order.tableId)
                        .show(childFragmentManager, "order_editor")
                }
            }
        )
        binding.rvActiveOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActiveOrders.adapter = adapter
    }

    private fun observeShiftStatusAndActiveOrders() {
        // Tải trạng thái Check-in active hiện tại của Nhân viên
        viewModel.loadActiveAttendance(staffUserId)

        // 1. Quan sát Trạng thái Ca Làm Việc (Active hay Inactive)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeAttendance.collectLatest { attendance ->
                if (_binding == null) return@collectLatest

                if (attendance != null) {
                    // ĐÃ CHECK-IN (Active Shift) -> Ẩn cảnh báo, hiển thị danh sách đơn hàng
                    binding.layoutInactiveShiftNotice.visibility = View.GONE
                    observeActiveOrdersList()
                } else {
                    // CHƯA CHECK-IN (Inactive Shift) -> Hiển thị cảnh báo, ẩn danh sách
                    binding.layoutInactiveShiftNotice.visibility = View.VISIBLE
                    binding.rvActiveOrders.visibility = View.GONE
                    binding.tvEmptyOrders.visibility = View.GONE
                }
            }
        }
    }

    private fun observeActiveOrdersList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeOrders.collectLatest { list ->
                if (_binding == null) return@collectLatest
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.rvActiveOrders.visibility = View.GONE
                    binding.tvEmptyOrders.visibility = View.VISIBLE
                } else {
                    binding.rvActiveOrders.visibility = View.VISIBLE
                    binding.tvEmptyOrders.visibility = View.GONE
                }
            }
        }
    }

    private fun showPaymentDialog(orderWithItems: OrderWithItems) {
        val dialogBinding = DialogStaffPaymentBinding.inflate(layoutInflater)
        val order = orderWithItems.order
        var subTotal = order.totalAmount

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        dialogBinding.tvPaymentTable.text = order.tableName
        dialogBinding.tvSubTotal.text = "Tổng tiền gốc: ${currencyFormatter.format(subTotal)}"

        var appliedDiscount: Discount? = null
        var discountAmount = 0.0
        var finalAmount = subTotal

        fun updatePaymentTotals() {
            if (appliedDiscount != null) {
                discountAmount = (subTotal * appliedDiscount!!.discountPercent) / 100.0
                finalAmount = (subTotal - discountAmount).coerceAtLeast(0.0)
                dialogBinding.tvDiscountAmount.text = "Giảm giá (${appliedDiscount!!.discountPercent}%): -${currencyFormatter.format(discountAmount)}"
            } else {
                discountAmount = 0.0
                finalAmount = subTotal
                dialogBinding.tvDiscountAmount.text = "Giảm giá: -0 đ"
            }
            dialogBinding.tvFinalAmount.text = "CẦN THANH TOÁN: ${currencyFormatter.format(finalAmount)}"
        }

        updatePaymentTotals()

        // Áp dụng mã giảm giá
        dialogBinding.btnApplyDiscount.setOnClickListener {
            val code = dialogBinding.etDiscountCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val (discount, errorMsg) = viewModel.validateAndApplyDiscount(code, subTotal)
                if (discount != null) {
                    appliedDiscount = discount
                    updatePaymentTotals()
                    dialogBinding.tvDiscountMessage.visibility = View.VISIBLE
                    dialogBinding.tvDiscountMessage.text = "Áp dụng thành công mã ${discount.code}!"
                    Toast.makeText(requireContext(), "Áp dụng mã giảm giá thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    dialogBinding.tvDiscountMessage.visibility = View.GONE
                    Toast.makeText(requireContext(), errorMsg ?: "Mã giảm giá không hợp lệ", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Chọn phương thức thanh toán
        dialogBinding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == dialogBinding.rbTransfer.id) {
                dialogBinding.cardTransferNotice.visibility = View.VISIBLE
            } else {
                dialogBinding.cardTransferNotice.visibility = View.GONE
            }
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnConfirmPayment.setOnClickListener {
            val paymentMethod = if (dialogBinding.rbTransfer.isChecked) "TRANSFER" else "CASH"

            viewModel.processPayment(
                orderId = order.id,
                paymentMethod = paymentMethod,
                discountCode = appliedDiscount?.code,
                discountAmount = discountAmount,
                finalAmount = finalAmount
            ) { success, msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                if (success) {
                    alertDialog.dismiss()
                }
            }
        }

        val paymentObserver = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeOrder(order.id).collectLatest { latest ->
                if (latest == null || latest.order.status !in listOf("PENDING", "CONFIRMED", "SERVED")) {
                    alertDialog.dismiss()
                    return@collectLatest
                }
                subTotal = latest.order.totalAmount
                dialogBinding.tvSubTotal.text = "Tổng tiền gốc: ${currencyFormatter.format(subTotal)}"
                if (appliedDiscount?.let { subTotal < it.minOrderAmount } == true) {
                    appliedDiscount = null
                    dialogBinding.tvDiscountMessage.text = "Đơn thay đổi, không còn đủ điều kiện giảm giá."
                    dialogBinding.tvDiscountMessage.visibility = View.VISIBLE
                }
                updatePaymentTotals()
            }
        }
        alertDialog.setOnDismissListener { paymentObserver.cancel() }
        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
