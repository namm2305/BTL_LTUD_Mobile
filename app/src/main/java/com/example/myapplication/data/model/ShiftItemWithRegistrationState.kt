package com.example.myapplication.data.model

import com.example.myapplication.data.entity.ShiftEntity

enum class ShiftRegistrationStatus {
    AVAILABLE,   // Chưa đăng ký -> Nút "Đăng ký" (Màu Tím)
    REGISTERED,  // Đã đăng ký -> Nút "Hủy đăng ký" (Màu Đỏ)
    LOCKED,     // Đã Check-in hoặc đã đến hạn khóa hủy ca
    COMPLETED    // Đã hoàn thành -> Nút "Đã hoàn thành" (Màu Xám, Vô hiệu hóa)
}

data class ShiftItemWithRegistrationState(
    val shift: ShiftEntity,
    val status: ShiftRegistrationStatus
)
