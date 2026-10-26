package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discounts")
data class Discount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String, // Ví dụ: "GIAM10", "WELCOME"
    val discountPercent: Double, // Phần trăm giảm, ví dụ 10.0 = 10%
    val minOrderAmount: Double = 0.0, // Giá trị đơn tối thiểu để áp dụng
    val isActive: Boolean = true
)
