package com.example.myapplication.ui.owner

import android.app.DatePickerDialog
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
import com.example.myapplication.databinding.DialogDailyRevenueReportBinding
import com.example.myapplication.databinding.FragmentOwnerReportBinding
import com.example.myapplication.ui.adapter.DailyRevenueChipAdapter
import com.example.myapplication.ui.adapter.DailyRevenueItem
import com.example.myapplication.ui.adapter.DailyRevenueReportAdapter
import com.example.myapplication.ui.adapter.OrderDetailFoodAdapter
import com.example.myapplication.ui.adapter.OwnerReportAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
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
    private lateinit var dailyChipAdapter: DailyRevenueChipAdapter

    private val selectedCalendar: Calendar = Calendar.getInstance()
    private var monthReportJob: Job? = null
    private var currentMonthDailyItems: List<DailyRevenueItem> = emptyList()

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

        setupRecyclerViews()
        setupMonthSelectors()
        loadTodayRevenue()
        updateMonthUI()
    }

    private fun setupRecyclerViews() {
        adapter = OwnerReportAdapter { order ->
            showOrderDetailDialog(order)
        }
        binding.rvCompletedOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletedOrders.adapter = adapter

        dailyChipAdapter = DailyRevenueChipAdapter { selectedItem ->
            onDailyChipSelected(selectedItem)
        }
        binding.rvDailyRevenueChips.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvDailyRevenueChips.adapter = dailyChipAdapter
    }

    private fun setupMonthSelectors() {
        binding.btnPrevMonth.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, -1)
            updateMonthUI()
        }

        binding.btnNextMonth.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, 1)
            updateMonthUI()
        }

        binding.tvSelectedMonth.setOnClickListener {
            showMonthPickerDialog()
        }

        binding.btnViewDailyTable.setOnClickListener {
            showDailyRevenueTableDialog()
        }
    }

    private fun showMonthPickerDialog() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                selectedCalendar.set(Calendar.YEAR, selectedYear)
                selectedCalendar.set(Calendar.MONTH, selectedMonth)
                updateMonthUI()
            },
            year,
            month,
            1
        ).show()
    }

    private fun loadTodayRevenue() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val endOfToday = System.currentTimeMillis()
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTotalRevenueBetween(startOfToday, endOfToday).collectLatest { revenue ->
                val total = revenue ?: 0.0
                _binding?.tvTotalRevenueToday?.text = "Doanh thu hôm nay: ${formatter.format(total)}"
            }
        }
    }

    private fun updateMonthUI() {
        val now = Calendar.getInstance()

        val isCurrentMonth = selectedCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                selectedCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)

        val isFutureMonth = (selectedCalendar.get(Calendar.YEAR) > now.get(Calendar.YEAR)) ||
                (selectedCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        selectedCalendar.get(Calendar.MONTH) > now.get(Calendar.MONTH))

        binding.btnNextMonth.isEnabled = !isFutureMonth && !isCurrentMonth
        binding.btnNextMonth.alpha = if (!isFutureMonth && !isCurrentMonth) 1.0f else 0.3f

        val monthYearFormatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val monthYearStr = monthYearFormatter.format(selectedCalendar.time)

        binding.tvSelectedMonth.text = "Tháng $monthYearStr"

        val startOfMonth = (selectedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = (selectedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

        monthReportJob?.cancel()
        monthReportJob = viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.getTotalRevenueBetween(startOfMonth, endOfMonth).collectLatest { revenue ->
                    val total = revenue ?: 0.0
                    val monthLabel = if (isCurrentMonth) "tháng này" else "tháng $monthYearStr"
                    _binding?.tvTotalRevenueMonth?.text = "Doanh thu $monthLabel: ${formatter.format(total)}"
                }
            }

            launch {
                viewModel.getCompletedOrdersBetween(startOfMonth, endOfMonth).collectLatest { allOrders ->
                    processAndDisplayDailyBreakdown(allOrders, monthYearStr)
                }
            }
        }
    }

    private fun processAndDisplayDailyBreakdown(allOrders: List<Order>, monthYearStr: String) {
        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatChip = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateFormatFull = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

        val groupedOrders = allOrders.groupBy { order ->
            val time = order.completedAt ?: order.createdAt
            dateFormatKey.format(Date(time))
        }

        val dailyItems = mutableListOf<DailyRevenueItem>()

        val totalMonthRevenue = allOrders.sumOf { it.finalAmount }
        dailyItems.add(
            DailyRevenueItem(
                dateKey = "ALL",
                displayDate = "Tất cả",
                fullDate = "Toàn bộ tháng $monthYearStr",
                totalRevenue = totalMonthRevenue,
                orderCount = allOrders.size,
                orders = allOrders
            )
        )

        val sortedDays = groupedOrders.entries.sortedByDescending { it.key }
        for ((key, dayOrders) in sortedDays) {
            val sampleTime = dayOrders.firstOrNull()?.let { it.completedAt ?: it.createdAt } ?: 0L
            val dayRevenue = dayOrders.sumOf { it.finalAmount }
            dailyItems.add(
                DailyRevenueItem(
                    dateKey = key,
                    displayDate = dateFormatChip.format(Date(sampleTime)),
                    fullDate = dateFormatFull.format(Date(sampleTime)),
                    totalRevenue = dayRevenue,
                    orderCount = dayOrders.size,
                    orders = dayOrders
                )
            )
        }

        currentMonthDailyItems = dailyItems
        dailyChipAdapter.submitList(dailyItems)
        dailyChipAdapter.resetSelection()

        adapter.submitList(allOrders)
        binding.tvCompletedOrdersHeader.text = "Lịch sử đơn hàng tháng $monthYearStr (${allOrders.size} đơn):"
    }

    private fun onDailyChipSelected(item: DailyRevenueItem) {
        adapter.submitList(item.orders)
        val monthYearFormatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val monthYearStr = monthYearFormatter.format(selectedCalendar.time)

        if (item.dateKey == "ALL") {
            binding.tvCompletedOrdersHeader.text = "Lịch sử đơn hàng tháng $monthYearStr (${item.orders.size} đơn):"
        } else {
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            binding.tvCompletedOrdersHeader.text =
                "Lịch sử đơn hàng ${item.fullDate} (${item.orders.size} đơn - ${currencyFormatter.format(item.totalRevenue)}):"
        }
    }

    private fun showDailyRevenueTableDialog() {
        val dialogBinding = DialogDailyRevenueReportBinding.inflate(layoutInflater)
        val monthYearFormatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val monthYearStr = monthYearFormatter.format(selectedCalendar.time)
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

        val allItem = currentMonthDailyItems.firstOrNull { it.dateKey == "ALL" }
        val totalRevenue = allItem?.totalRevenue ?: 0.0
        val totalOrders = allItem?.orderCount ?: 0

        dialogBinding.tvDialogTitle.text = "Chi Tiết Thu Nhập Theo Ngày"
        dialogBinding.tvDialogSubtitle.text =
            "Tháng $monthYearStr - Tổng: ${currencyFormatter.format(totalRevenue)} ($totalOrders đơn)"

        val onlyDays = currentMonthDailyItems.filter { it.dateKey != "ALL" }

        var dialog: BottomSheetDialog? = null

        val reportAdapter = DailyRevenueReportAdapter { selectedItem ->
            dailyChipAdapter.setSelectedKey(selectedItem.dateKey)
            onDailyChipSelected(selectedItem)
            dialog?.dismiss()
        }

        dialogBinding.rvDailyReportList.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvDailyReportList.adapter = reportAdapter
        reportAdapter.submitList(onlyDays)

        dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun showOrderDetailDialog(order: Order) {
        val sheetBinding = BottomSheetOwnerOrderDetailBinding.inflate(layoutInflater)
        val foodAdapter = OrderDetailFoodAdapter()

        sheetBinding.rvOrderDetailItems.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvOrderDetailItems.adapter = foodAdapter

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
        val dateFormatter = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

        sheetBinding.tvDetailTableName.text = "Chi Tiết Đơn Hàng - ${order.tableName}"
        sheetBinding.tvDetailPaymentTime.text =
            "Giờ thanh toán: ${dateFormatter.format(Date(order.completedAt ?: order.createdAt))}"

        val methodText = when (order.paymentMethod) {
            "TRANSFER" -> "Chuyển khoản"
            "CASH" -> "Tiền mặt"
            else -> order.paymentMethod ?: "Khác"
        }
        sheetBinding.tvDetailPaymentMethod.text = "Phương thức: $methodText"

        sheetBinding.tvDetailSubTotal.text = "Tổng tiền gốc: ${currencyFormatter.format(order.totalAmount)}"

        if (!order.discountCode.isNullOrEmpty()) {
            sheetBinding.tvDetailDiscountInfo.visibility = View.VISIBLE
            sheetBinding.tvDetailDiscountInfo.text =
                "Mã giảm giá (${order.discountCode}): -${currencyFormatter.format(order.discountAmount)}"
        } else {
            sheetBinding.tvDetailDiscountInfo.visibility = View.GONE
        }

        sheetBinding.tvDetailFinalTotal.text = "TỔNG THANH TOÁN: ${currencyFormatter.format(order.finalAmount)}"

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

    override fun onDestroyView() {
        monthReportJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
