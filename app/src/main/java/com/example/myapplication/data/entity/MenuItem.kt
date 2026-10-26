package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String = "Món chính", // ví dụ: "Món chính", "Đồ uống", "Tráng miệng"
    val isAvailable: Boolean = true,
    val imageUrl: String? = null,
    val imageUri: String? = null        // Đường dẫn URI ảnh cục bộ trong thiết bị
)
