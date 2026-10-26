package com.example.myapplication.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.Order
import com.example.myapplication.databinding.BottomSheetOwnerOrderDetailBinding
import com.example.myapplication.databinding.FragmentOwnerReportBinding
import com.example.myapplication.ui.adapter.OrderDetailFoodAdapter
import com.example.myapplication.ui.adapter.OwnerReportAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OwnerReportFragment : Fragment() {

    private var _binding: FragmentOwnerReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadReports()
    }

    private fun setupRecyclerView() {
        adapter = OwnerReportAdapter { order ->
            showOrderDetailDialog(order)
        }
        binding.rvCompletedOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletedOrders.adapter = adapter
    }

    private fun showOrderDetailDialog(order: Order) {
        val sheetBinding = BottomSheetOwnerOrderDetailBinding.inflate(layoutInflater)
        val foodAdapter = OrderDetailFoodAdapter()

        sheetBinding.rvOrderDetailItems.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvOrderDetailItems.adapter = foodAdapter

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val dateFormatter = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

        sheetBinding.tvDetailTableName.text = "Chi Tiết Đơn Hàng - ${order.tableName}"
        sheetBinding.tvDetailPaymentTime.text = "Giờ thanh toán: ${dateFormatter.format(Date(order.completedAt ?: order.createdAt))}"

        val methodText = when (order.paymentMethod) {
            "TRANSFER" -> "Chuyển khoản"
            "CASH" -> "Tiền mặt"
            else -> order.paymentMethod ?: "Khác"
        }
        sheetBinding.tvDetailPaymentMethod.text = "Phương thức: $methodText"

        sheetBinding.tvDetailSubTotal.text = "Tổng tiền gốc: ${currencyFormatter.format(order.totalAmount)}"

        if (!order.discountCode.isNullOrEmpty()) {
            sheetBinding.tvDetailDiscountInfo.visibility = View.VISIBLE
            sheetBinding.tvDetailDiscountInfo.text = "Mã giảm giá (${order.discountCode}): -${currencyFormatter.format(order.discountAmount)}"
        } else {
            sheetBinding.tvDetailDiscountInfo.visibility = View.GONE
        }

        sheetBinding.tvDetailFinalTotal.text = "TỔNG THANH TOÁN: ${currencyFormatter.format(order.finalAmount)}"

        // Tải danh sách chi tiết các món ăn của đơn hàng này
        viewLifecycleOwner.lifecycleScope.launch {
            val orderWithItems = viewModel.getOrderWithItemsById(order.id)
            if (orderWithItems != null) {
                foodAdapter.submitList(orderWithItems.items)
            }
        }

        BottomSheetDialog(requireContext()).apply {
            setContentView(sheetBinding.root)
            show()
        }
    }

    private fun loadReports() {
        val calendar = Calendar.getInstance()

        // Đầu ngày hôm nay
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        // Cuối ngày hôm nay
        val endOfToday = System.currentTimeMillis()

        // Đầu tháng này
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        // Doanh thu hôm nay
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTotalRevenueBetween(startOfToday, endOfToday).collectLatest { revenue ->
                val total = revenue ?: 0.0
                _binding?.tvTotalRevenueToday?.text = "Doanh thu hôm nay: ${formatter.format(total)}"
            }
        }

        // Doanh thu tháng này
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTotalRevenueBetween(startOfMonth, endOfToday).collectLatest { revenue ->
                val total = revenue ?: 0.0
                _binding?.tvTotalRevenueMonth?.text = "Doanh thu tháng này: ${formatter.format(total)}"
            }
        }

        // Danh sách đơn hàng đã hoàn thành trong tháng
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCompletedOrdersBetween(startOfMonth, endOfToday).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
