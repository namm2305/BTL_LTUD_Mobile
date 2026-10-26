package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantTable::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tableId"]),
        Index(value = ["status"]),
        Index(value = ["completedAt"])
    ]
)
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableId: Long,
    val tableName: String, // Tên bàn cached để tiện hiển thị
    val totalAmount: Double, // Tổng tiền trước giảm
    val discountCode: String? = null,
    val discountAmount: Double = 0.0, // Số tiền được giảm
    val finalAmount: Double, // Số tiền thanh toán cuối cùng
    val paymentMethod: String? = null, // "CASH" (Tiền mặt) hoặc "TRANSFER" (Chuyển khoản)
    val status: String = STATUS_PENDING, // "PENDING", "SERVED", "COMPLETED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"     // Chờ phục vụ (Khách vừa đặt món)
        const val STATUS_SERVED = "SERVED"       // Đã phục vụ món
        const val STATUS_COMPLETED = "COMPLETED" // Đã thanh toán & hoàn thành
        const val STATUS_CANCELLED = "CANCELLED" // Đã hủy đơn
    }
}
