package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timekeeping",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Timekeeping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val staffName: String,
    val checkInTime: Long = System.currentTimeMillis(),
    val checkOutTime: Long? = null,
    val workDate: String // Định dạng YYYY-MM-DD để dễ truy vấn báo cáo theo ngày
)
