package com.example.myapplication.data.model

data class OrderItemChange(
    val itemId: Long,
    val quantity: Int, // Zero cancels the item without deleting its history.
    val cancellationReason: String? = null
)
