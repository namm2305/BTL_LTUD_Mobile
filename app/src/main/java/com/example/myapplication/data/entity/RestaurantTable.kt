package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class RestaurantTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableName: String, // ví dụ: "Bàn 01", "Bàn 02"
    val qrCodeData: String, // mã định danh chứa trong QR code, ví dụ: "TABLE_1"
    val status: String = STATUS_EMPTY // "EMPTY", "OCCUPIED"
) {
    companion object {
        const val STATUS_EMPTY = "EMPTY"       // Bàn trống
        const val STATUS_OCCUPIED = "OCCUPIED" // Đang có khách
    }
}
