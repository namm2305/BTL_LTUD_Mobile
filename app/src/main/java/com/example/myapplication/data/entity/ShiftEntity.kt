package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shifts",
    indices = [
        Index(value = ["workDate"])
    ]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftName: String,          // Ví dụ: "Ca Sáng", "Ca Đêm"
    val workDate: String = "",      // Ngày bắt đầu ca "YYYY-MM-DD"
    val expectedStartTime: String,  // Ví dụ: "20:00"
    val expectedEndTime: String,    // Ví dụ: "04:00"
    val hourlyRate: Double = 30000.0,      // Mức lương theo giờ (VNĐ/giờ)
    val overtimeMultiplier: Double = 1.5,  // Hệ số tăng ca (mặc định 1.5)
    val isOvernight: Boolean = false       // Đánh dấu ca đêm xuyên nửa đêm (Start > End)
)
