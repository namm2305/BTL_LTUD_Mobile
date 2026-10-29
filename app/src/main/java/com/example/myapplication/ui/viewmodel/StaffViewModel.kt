package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entity.AttendanceEntity
import com.example.myapplication.data.entity.Discount
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.data.model.ShiftSalaryDetails
import com.example.myapplication.data.model.OtPolicy
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.data.model.ShiftItemWithRegistrationState
import com.example.myapplication.data.model.ShiftRegistrationStatus
import com.example.myapplication.data.model.ShiftRegistrationWithDetails
import com.example.myapplication.data.repository.DiscountRepository
import com.example.myapplication.data.repository.OrderRepository
import com.example.myapplication.data.repository.TableRepository
import com.example.myapplication.data.repository.TimekeepingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StaffViewModel(
    private val timekeepingRepository: TimekeepingRepository,
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository,
    private val discountRepository: DiscountRepository
) : ViewModel() {

    // --- Danh sách Ca làm việc ---
    val shifts: StateFlow<List<ShiftEntity>> = timekeepingRepository.getAllShifts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getShiftsByDate(workDate: String): Flow<List<ShiftEntity>> {
        return timekeepingRepository.getShiftsByDate(workDate)
    }

    fun getRegisteredShiftsToday(staffId: Long, todayDate: String): Flow<List<ShiftRegistrationWithDetails>> {
        return timekeepingRepository.getRegisteredShiftsForStaffOnDate(staffId, todayDate)
    }

    // Kết hợp Danh sách Ca làm việc theo ngày, Trạng thái Đã Đăng Ký và Trạng thái Đã Hoàn Thành
    fun getShiftsWithRegistrationState(staffId: Long, dateStr: String): Flow<List<ShiftItemWithRegistrationState>> {
        return combine(
            timekeepingRepository.getShiftsByDate(dateStr),
            timekeepingRepository.getRegisteredShiftsForStaffOnDate(staffId, dateStr),
            timekeepingRepository.getCompletedShiftIdsForStaffOnDate(staffId, dateStr),
            timekeepingRepository.getAttendanceByStaff(staffId),
            flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    delay(1_000)
                }
            }
        ) { shiftList, registeredList, completedShiftIds, attendanceList, now ->
            val registeredShiftIds = registeredList.mapNotNull { it.shift?.id }.toSet()
            val completedSet = completedShiftIds.toSet()

            shiftList.map { shift ->
                val status = when {
                    completedSet.contains(shift.id) -> ShiftRegistrationStatus.COMPLETED
                    registeredShiftIds.contains(shift.id) &&
                        (attendanceList.any { it.attendance.shiftId == shift.id && it.attendance.date == dateStr } ||
                            isCancellationClosed(shift, dateStr, now)) -> ShiftRegistrationStatus.LOCKED
                    registeredShiftIds.contains(shift.id) -> ShiftRegistrationStatus.REGISTERED
                    else -> ShiftRegistrationStatus.AVAILABLE
                }
                ShiftItemWithRegistrationState(
                    shift = shift,
                    status = status
                )
            }
        }
    }

    // Hạn hủy ca là lúc mở Check-in: 15 phút trước giờ bắt đầu, theo ngày đăng ký.
    private fun isCancellationClosed(
        shift: ShiftEntity,
        dateStr: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            isLenient = false
        }
        val startTime = try {
            format.parse("$dateStr ${shift.expectedStartTime}")?.time
        } catch (e: java.text.ParseException) {
            null
        } ?: return true
        return now >= startTime - 15 * 60 * 1000L
    }

    // Toggle Đăng ký / Hủy đăng ký ca làm
    fun toggleShiftRegistration(staffId: Long, shiftId: Long, dateStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = timekeepingRepository.getRegistration(staffId, shiftId, dateStr)
            if (existing != null) {
                val shift = timekeepingRepository.getShiftById(shiftId)
                if (shift == null) {
                    onResult(false, "Ca làm không còn tồn tại.")
                    return@launch
                }
                val hasCheckedIn = timekeepingRepository.getAttendanceByStaff(staffId).first()
                    .any { it.attendance.shiftId == shiftId && it.attendance.date == dateStr }
                if (hasCheckedIn) {
                    onResult(false, "Không thể hủy ca đã Check-in.")
                    return@launch
                }
                if (isCancellationClosed(shift, dateStr)) {
                    onResult(false, "Không thể hủy ca từ 15 phút trước giờ bắt đầu ca.")
                    return@launch
                }
                timekeepingRepository.cancelRegistration(staffId, shiftId, dateStr)
                onResult(true, "Đã hủy đăng ký ca làm việc thành công!")
            } else {
                // Đăng ký ca mới (Ngăn chặn duplicate)
                timekeepingRepository.registerShift(staffId, shiftId, dateStr)
                onResult(true, "Đăng ký ca làm việc thành công!")
            }
        }
    }

    // Kiểm tra giờ hiện tại thực tế có đủ điều kiện Check-in ca làm không (Cho phép trước 15 phút, hỗ trợ ca đêm isOvernight)
    fun canCheckInShift(shift: ShiftEntity): Boolean {
        val now = Calendar.getInstance()
        val startParts = shift.expectedStartTime.split(":")
        val endParts = shift.expectedEndTime.split(":")

        // Giờ bắt đầu ca - 15 phút
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startParts.getOrNull(0)?.toIntOrNull() ?: 7)
            set(Calendar.MINUTE, startParts.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -15) // Cho phép Check-in trước 15 phút
        }

        // Giờ kết thúc ca (Nếu là ca đêm isOvernight == true, thuộc về sáng hôm sau)
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endParts.getOrNull(0)?.toIntOrNull() ?: 15)
            set(Calendar.MINUTE, endParts.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (shift.isOvernight) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return now.timeInMillis >= startCal.timeInMillis && now.timeInMillis <= endCal.timeInMillis
    }

    // --- Chấm công & Thu nhập cá nhân ---
    private val _activeAttendance = MutableStateFlow<AttendanceEntity?>(null)
    val activeAttendance: StateFlow<AttendanceEntity?> = _activeAttendance.asStateFlow()

    fun loadActiveAttendance(staffId: Long) {
        viewModelScope.launch {
            _activeAttendance.value = timekeepingRepository.getActiveAttendance(staffId)
        }
    }

    private val accumulatedSalaryFlows = mutableMapOf<Long, StateFlow<Double?>>()

    fun getStaffTotalSalary(staffId: Long): StateFlow<Double?> =
        accumulatedSalaryFlows.getOrPut(staffId) {
            timekeepingRepository.getAccumulatedSalaryByStaff(staffId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    fun checkInShift(staffId: Long, shiftId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = timekeepingRepository.getActiveAttendance(staffId)
            if (existing != null) {
                onResult(false, "Bạn đang trong ca làm việc rồi, vui lòng Check-out trước!")
                return@launch
            }
            val shift = timekeepingRepository.getShiftById(shiftId)
            if (shift == null) {
                onResult(false, "Ca làm không còn tồn tại.")
                return@launch
            }
            if (!canCheckInShift(shift)) {
                onResult(false, "Chỉ được Check-in từ 15 phút trước giờ bắt đầu đến giờ kết thúc ca.")
                return@launch
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = dateFormat.format(Date())

            timekeepingRepository.checkInShift(staffId, shiftId, todayDate)
            _activeAttendance.value = timekeepingRepository.getActiveAttendance(staffId)
            onResult(true, "Check-in ca làm thành công!")
        }
    }

    // LOGIC CHECK-OUT VÀ TÍNH LƯƠNG CHÍNH XÁC (HỖ TRỢ CA ĐÊM ISOVERNIGHT)
    fun checkOutShift(staffId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val active = timekeepingRepository.getActiveAttendance(staffId)
            if (active == null) {
                onResult(false, "Không tìm thấy thông tin Check-in!")
                return@launch
            }

            val shift = timekeepingRepository.getShiftById(active.shiftId)
            val checkInTime = active.actualCheckIn
            val checkOutTime = System.currentTimeMillis()

            // 1. Tính tổng số giờ làm thực tế
            val totalWorkedHours = (checkOutTime - checkInTime) / (1000.0 * 3600.0)

            // 2. Tính thời lượng ca làm quy định (Hỗ trợ ca đêm isOvernight)
            val hourlyRate = shift?.hourlyRate ?: 30000.0
            val overtimeMultiplier = shift?.overtimeMultiplier ?: 1.5

            var expectedShiftDuration = 8.0
            if (shift != null) {
                val startParts = shift.expectedStartTime.split(":")
                val endParts = shift.expectedEndTime.split(":")
                val startH = (startParts.getOrNull(0)?.toDoubleOrNull() ?: 7.0) + (startParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 60.0
                var endH = (endParts.getOrNull(0)?.toDoubleOrNull() ?: 15.0) + (endParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 60.0

                if (shift.isOvernight || endH <= startH) {
                    endH += 24.0 // Cộng 24 giờ cho mốc kết thúc ngày hôm sau
                }
                expectedShiftDuration = endH - startH
            }

            // 3. Kiểm tra nếu Check-out sớm hơn giờ kết thúc ca -> Phạt 50,000 đ
            val expectedEnd = shift?.let { OtPolicy.expectedEnd(it, active.date) }
            val isEarlyCheckOut = expectedEnd != null &&
                checkOutTime < expectedEnd - 5 * 60 * 1000L
            val otMinutes = expectedEnd?.let { OtPolicy.actualMinutes(it, checkOutTime) } ?: 0L
            val otStatus = expectedEnd?.let { OtPolicy.status(it, checkOutTime) } ?: "NONE"
            val earlyPenalty = if (isEarlyCheckOut) 50000.0 else 0.0

            val details = ShiftSalaryDetails.calculate(
                workedHours = totalWorkedHours.coerceAtLeast(0.0),
                expectedHours = expectedShiftDuration,
                hourlyRate = hourlyRate,
                overtimeMultiplier = overtimeMultiplier,
                fineAmount = earlyPenalty
            ).copy(overtimeSalary = 0.0)
            val totalSalary = details.totalSalary
            timekeepingRepository.updateShiftSalary(
                active.copy(
                    actualCheckOut = checkOutTime,
                    otMinutes = otMinutes,
                    otStatus = otStatus,
                    approvedOtMinutes = 0,
                    otRatePerHour = hourlyRate * overtimeMultiplier
                ),
                details
            )
            _activeAttendance.value = null

            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val salaryStr = formatter.format(totalSalary)
            var msg = "Check-out thành công! Lương ca này: $salaryStr"
            if (otStatus == "PENDING") msg += " · $otMinutes phút OT đang chờ duyệt"
            if (isEarlyCheckOut) {
                msg += " (Bị trừ 50k do về sớm)"
            }
            onResult(true, msg)
        }
    }

    // --- Đăng ký Ca làm việc ---
    fun registerShift(staffId: Long, shiftId: Long, date: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            timekeepingRepository.registerShift(staffId, shiftId, date)
            onResult(true, "Đăng ký ca làm ngày $date thành công!")
        }
    }

    fun getRegistrationsByStaff(staffId: Long): Flow<List<ShiftRegistrationWithDetails>> {
        return timekeepingRepository.getRegistrationsByStaff(staffId)
    }

    fun getAttendanceByStaff(staffId: Long): Flow<List<AttendanceWithDetails>> {
        return timekeepingRepository.getAttendanceByStaff(staffId)
    }

    // --- Danh sách bàn & đơn hàng hoạt động ---
    val tables: StateFlow<List<RestaurantTable>> = tableRepository.getAllTables()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeOrders: StateFlow<List<OrderWithItems>> = orderRepository.getActiveOrders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Thanh toán & Mã giảm giá ---
    suspend fun validateAndApplyDiscount(code: String, subTotal: Double): Pair<Discount?, String?> {
        val discount = discountRepository.getDiscountByCode(code.uppercase())
        return when {
            discount == null -> Pair(null, "Mã giảm giá không tồn tại hoặc đã hết hạn!")
            subTotal < discount.minOrderAmount -> Pair(null, "Đơn hàng tối thiểu ${discount.minOrderAmount}đ để dùng mã này!")
            else -> Pair(discount, null)
        }
    }

    fun processPayment(
        orderId: Long,
        paymentMethod: String,
        discountCode: String?,
        discountAmount: Double,
        finalAmount: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val success = orderRepository.completePayment(
                orderId = orderId,
                paymentMethod = paymentMethod,
                discountCode = discountCode,
                discountAmount = discountAmount,
                finalAmount = finalAmount
            )
            if (success) {
                onResult(true, "Thanh toán thành công!")
            } else {
                onResult(false, "Không tìm thấy đơn hàng!")
            }
        }
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrder(order.copy(status = newStatus))
        }
    }
}
