package com.example.myapplication.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.OrderItem

data class OrderWithItems(
    @Embedded
    val order: Order,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)
