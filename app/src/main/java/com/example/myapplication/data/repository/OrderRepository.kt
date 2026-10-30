package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.OrderDao
import com.example.myapplication.data.dao.OrderItemDao
import com.example.myapplication.data.dao.TableDao
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

class OrderRepository(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val tableDao: TableDao
) {

    suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        return orderDao.createWithItems(order, items)
    }

    fun getActiveOrders(): Flow<List<OrderWithItems>> = orderDao.getActiveOrdersWithItems()

    fun getActiveOrderByTable(tableId: Long): Flow<OrderWithItems?> = orderDao.getActiveOrderByTable(tableId)

    fun getOrderWithItemsById(orderId: Long): Flow<OrderWithItems?> = orderDao.getOrderWithItemsById(orderId)

    suspend fun getOrderWithItemsByIdSync(orderId: Long): OrderWithItems? = orderDao.getOrderWithItemsByIdSync(orderId)

    suspend fun updateOrder(order: Order): Boolean =
        orderDao.transitionStatus(order.id, order.revision, order.status)

    fun getOrdersByTable(tableId: Long): Flow<List<OrderWithItems>> = orderDao.getOrdersByTable(tableId)

    suspend fun editItems(orderId: Long, tableId: Long, staff: Boolean, revision: Long,
        changes: List<com.example.myapplication.data.model.OrderItemChange>) =
        orderDao.editItems(orderId, tableId, staff, revision, changes)

    suspend fun completePayment(orderId: Long, paymentMethod: String, discountCode: String? = null, discountAmount: Double = 0.0, finalAmount: Double): Boolean {
        return orderDao.payOrder(orderId, paymentMethod, discountCode, discountAmount, finalAmount)
    }

    fun getCompletedOrdersBetween(startTime: Long, endTime: Long): Flow<List<Order>> =
        orderDao.getCompletedOrdersBetween(startTime, endTime)

    fun getTotalRevenueBetween(startTime: Long, endTime: Long): Flow<Double?> =
        orderDao.getTotalRevenueBetween(startTime, endTime)
}
