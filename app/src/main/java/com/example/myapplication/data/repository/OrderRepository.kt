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
        // Chèn đơn hàng mới
        val orderId = orderDao.insertOrder(order)
        // Chèn danh sách món kèm theo orderId
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
        orderItemDao.insertOrderItems(itemsWithOrderId)
        // Đổi trạng thái bàn thành OCCUPIED (có khách)
        tableDao.updateTableStatus(order.tableId, RestaurantTable.STATUS_OCCUPIED)
        return orderId
    }

    fun getActiveOrders(): Flow<List<OrderWithItems>> = orderDao.getActiveOrdersWithItems()

    fun getActiveOrderByTable(tableId: Long): Flow<OrderWithItems?> = orderDao.getActiveOrderByTable(tableId)

    fun getOrderWithItemsById(orderId: Long): Flow<OrderWithItems?> = orderDao.getOrderWithItemsById(orderId)

    suspend fun getOrderWithItemsByIdSync(orderId: Long): OrderWithItems? = orderDao.getOrderWithItemsByIdSync(orderId)

    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)

    suspend fun completePayment(orderId: Long, paymentMethod: String, discountCode: String? = null, discountAmount: Double = 0.0, finalAmount: Double): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        val updatedOrder = order.copy(
            paymentMethod = paymentMethod,
            discountCode = discountCode,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            status = Order.STATUS_COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        orderDao.updateOrder(updatedOrder)
        // Chuyển trạng thái bàn về trống
        tableDao.updateTableStatus(order.tableId, RestaurantTable.STATUS_EMPTY)
        return true
    }

    fun getCompletedOrdersBetween(startTime: Long, endTime: Long): Flow<List<Order>> =
        orderDao.getCompletedOrdersBetween(startTime, endTime)

    fun getTotalRevenueBetween(startTime: Long, endTime: Long): Flow<Double?> =
        orderDao.getTotalRevenueBetween(startTime, endTime)
}
