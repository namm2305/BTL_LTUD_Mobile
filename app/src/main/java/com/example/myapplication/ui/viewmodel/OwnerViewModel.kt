package com.example.myapplication.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entity.Discount
import com.example.myapplication.data.entity.MenuItem
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.entity.User
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.data.repository.DiscountRepository
import com.example.myapplication.data.repository.MenuRepository
import com.example.myapplication.data.repository.OrderRepository
import com.example.myapplication.data.repository.TableRepository
import com.example.myapplication.data.repository.TimekeepingRepository
import com.example.myapplication.data.repository.UserRepository
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OwnerViewModel(
    private val menuRepository: MenuRepository,
    private val discountRepository: DiscountRepository,
    private val tableRepository: TableRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val timekeepingRepository: TimekeepingRepository
) : ViewModel() {

    // --- Thực đơn món ăn ---
    val menuItems: StateFlow<List<MenuItem>> = menuRepository.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addMenuItem(name: String, price: Double, category: String, imageUri: String? = null) {
        viewModelScope.launch {
            val item = MenuItem(name = name, price = price, category = category, imageUri = imageUri)
            menuRepository.insertMenuItem(item)
        }
    }

    fun updateMenuItem(item: MenuItem) {
        viewModelScope.launch {
            menuRepository.updateMenuItem(item)
        }
    }

    fun deleteMenuItem(item: MenuItem) {
        viewModelScope.launch {
            menuRepository.deleteMenuItem(item)
        }
    }

    // --- Mã giảm giá ---
    val discounts: StateFlow<List<Discount>> = discountRepository.getAllDiscounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addDiscount(code: String, discountPercent: Double, minOrderAmount: Double) {
        viewModelScope.launch {
            val discount = Discount(
                code = code.uppercase(),
                discountPercent = discountPercent,
                minOrderAmount = minOrderAmount
            )
            discountRepository.insertDiscount(discount)
        }
    }

    fun updateDiscount(discount: Discount) {
        viewModelScope.launch {
            discountRepository.updateDiscount(discount)
        }
    }

    fun deleteDiscount(discount: Discount) {
        viewModelScope.launch {
            discountRepository.deleteDiscount(discount)
        }
    }

    // --- Quản lý bàn & Tự động tạo mã QR ---
    val tables: StateFlow<List<RestaurantTable>> = tableRepository.getAllTables()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTableAutoQr(tableName: String, customQrData: String? = null) {
        viewModelScope.launch {
            val sanitized = tableName.trim().replace(" ", "_").uppercase()
            val qrCodeData = customQrData?.ifBlank { null } ?: "TABLE_${sanitized}_${(100..999).random()}"
            val table = RestaurantTable(tableName = tableName, qrCodeData = qrCodeData)
            tableRepository.insertTable(table)
        }
    }

    fun deleteTable(table: RestaurantTable) {
        viewModelScope.launch {
            tableRepository.deleteTable(table)
        }
    }

    fun generateQrBitmap(qrCodeData: String, width: Int = 400, height: Int = 400): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(qrCodeData, BarcodeFormat.QR_CODE, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Quản lý Ca Làm Việc ---
    val shifts: StateFlow<List<ShiftEntity>> = timekeepingRepository.getAllShifts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addShift(
        shiftName: String,
        workDate: String,
        startTime: String,
        endTime: String,
        hourlyRate: Double,
        overtimeMultiplier: Double,
        isOvernight: Boolean = false
    ) {
        viewModelScope.launch {
            val newShift = ShiftEntity(
                shiftName = shiftName,
                workDate = workDate,
                expectedStartTime = startTime,
                expectedEndTime = endTime,
                hourlyRate = hourlyRate,
                overtimeMultiplier = overtimeMultiplier,
                isOvernight = isOvernight
            )
            timekeepingRepository.insertShift(newShift)
        }
    }

    fun deleteShift(shift: ShiftEntity) {
        viewModelScope.launch {
            timekeepingRepository.deleteShift(shift)
        }
    }

    // --- Quản lý Nhân viên & Lương ---
    val staffList: StateFlow<List<User>> = userRepository.getAllStaff()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addStaff(username: String, password: String, fullName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = userRepository.getUserByUsername(username)
            if (existing != null) {
                onResult(false, "Tên tài khoản '$username' đã tồn tại!")
                return@launch
            }
            val newStaff = User(
                username = username,
                password = password,
                fullName = fullName,
                role = "STAFF"
            )
            userRepository.insertUser(newStaff)
            onResult(true, "Đã tạo tài khoản nhân viên $fullName thành công!")
        }
    }

    fun deleteStaff(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user)
        }
    }

    fun getAttendanceByStaff(staffId: Long): Flow<List<AttendanceWithDetails>> {
        return timekeepingRepository.getAttendanceByStaff(staffId)
    }

    fun getTotalSalaryByStaff(staffId: Long): Flow<Double?> {
        return timekeepingRepository.getTotalSalaryByStaff(staffId)
    }

    fun markSalaryAsPaid(staffId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            timekeepingRepository.markSalaryAsPaid(staffId)
            onResult(true, "Đã thanh toán các ca đã chốt. Ca chờ duyệt OT được giữ lại.")
        }
    }

    val pendingOt: StateFlow<List<AttendanceWithDetails>> = timekeepingRepository.getPendingOt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun decideOt(id: Long, approved: Boolean, minutes: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = timekeepingRepository.decideOt(id, approved, minutes)
                onResult(success, if (success) {
                    if (approved) "Đã duyệt tăng ca" else "Đã từ chối tăng ca"
                } else "Yêu cầu không còn chờ duyệt hoặc số phút không hợp lệ.")
            } catch (_: Exception) {
                onResult(false, "Không thể cập nhật OT. Vui lòng thử lại.")
            }
        }
    }

    // --- Báo cáo Chấm công Nhân viên theo Ca ---
    val attendanceList: StateFlow<List<AttendanceWithDetails>> = timekeepingRepository.getAllAttendanceWithDetails()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Báo cáo thu chi & Chi tiết đơn hàng ---
    fun getCompletedOrdersBetween(startTime: Long, endTime: Long): Flow<List<Order>> {
        return orderRepository.getCompletedOrdersBetween(startTime, endTime)
    }

    fun getTotalRevenueBetween(startTime: Long, endTime: Long): Flow<Double?> {
        return orderRepository.getTotalRevenueBetween(startTime, endTime)
    }

    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems? {
        return orderRepository.getOrderWithItemsByIdSync(orderId)
    }
}
