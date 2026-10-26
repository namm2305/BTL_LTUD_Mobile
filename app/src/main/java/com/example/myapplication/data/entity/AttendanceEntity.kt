package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["staffId"]),
        Index(value = ["shiftId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val shiftId: Long,
    val date: String, // "YYYY-MM-DD"
    val actualCheckIn: Long = System.currentTimeMillis(),
    val actualCheckOut: Long? = null,
    val earlyPenalty: Double = 50000.0, // Phạt về sớm/đến muộn (mặc định 50,000 đ)
    val overtimePay: Double = 0.0,      // Tiền lương tăng ca thêm
    val totalSalary: Double = 0.0,      // Tổng lương thực nhận cho ca làm này
    val isPaid: Boolean = false         // Trạng thái đã thanh toán lương hay chưa (mặc định false)
)
