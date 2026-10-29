package com.example.myapplication.data.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "0.0")
    val baseSalary: Double = 0.0,
    @ColumnInfo(name = "earlyPenalty")
    val fineAmount: Double = 0.0,
    @ColumnInfo(name = "overtimePay")
    val overtimeSalary: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val otMinutes: Long = 0,
    @ColumnInfo(defaultValue = "'NONE'")
    val otStatus: String = "NONE",
    @ColumnInfo(defaultValue = "0")
    val approvedOtMinutes: Long = 0,
    @ColumnInfo(defaultValue = "0.0")
    val otRatePerHour: Double = 0.0,
    val totalSalary: Double = 0.0,      // Tổng lương thực nhận cho ca làm này
    val isPaid: Boolean = false         // Trạng thái đã thanh toán lương hay chưa (mặc định false)
)
