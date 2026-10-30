package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.data.model.OrderItemChange
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Transaction
    @Query("SELECT * FROM orders WHERE tableId = :tableId ORDER BY createdAt DESC LIMIT 30")
    fun getOrdersByTable(tableId: Long): Flow<List<OrderWithItems>>

    @Insert
    suspend fun insertItems(items: List<OrderItem>)

    @Query("UPDATE tables SET status = :status WHERE id = :tableId")
    suspend fun setTableStatus(tableId: Long, status: String)

    @Query("SELECT COUNT(*) FROM orders WHERE tableId = :tableId AND status IN ('PENDING', 'CONFIRMED', 'SERVED')")
    suspend fun activeOrderCount(tableId: Long): Int

    @Query("UPDATE order_items SET quantity = :quantity WHERE id = :itemId AND orderId = :orderId AND status != 'CANCELLED'")
    suspend fun updateItemQuantity(orderId: Long, itemId: Long, quantity: Int)

    @Query("UPDATE order_items SET status = 'CANCELLED', cancellationReason = :reason WHERE id = :itemId AND orderId = :orderId")
    suspend fun cancelItem(orderId: Long, itemId: Long, reason: String?)

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM order_items WHERE orderId = :orderId AND status != 'CANCELLED'")
    suspend fun calculateOrderTotal(orderId: Long): Double

    @Query("UPDATE order_items SET status = 'CONFIRMED' WHERE orderId = :orderId AND status = 'PENDING'")
    suspend fun confirmItems(orderId: Long)

    @Transaction
    suspend fun createWithItems(order: Order, items: List<OrderItem>): Long {
        require(items.isNotEmpty() && items.all { it.quantity > 0 && it.price >= 0 && it.price.isFinite() })
        val total = items.sumOf { it.price * it.quantity }
        val id = insertOrder(order.copy(totalAmount = total, finalAmount = total))
        insertItems(items.map { it.copy(orderId = id) })
        setTableStatus(order.tableId, "OCCUPIED")
        return id
    }

    @Transaction
    suspend fun editItems(
        orderId: Long, tableId: Long, staff: Boolean, expectedRevision: Long,
        changes: List<OrderItemChange>
    ) {
        val current = getOrderWithItemsByIdSync(orderId) ?: error("Không tìm thấy đơn hàng.")
        val order = current.order
        check(order.tableId == tableId) { "Đơn hàng không thuộc bàn này." }
        check(order.status in listOf("PENDING", "CONFIRMED", "SERVED")) { "Đơn đã thanh toán hoặc đã hủy, không thể chỉnh sửa." }
        check(staff || order.status == "PENDING") {
            "Đơn hàng đã được xác nhận, vui lòng báo Nhân viên để hỗ trợ hủy/sửa món"
        }
        check(order.revision == expectedRevision) { "Đơn vừa được cập nhật. Vui lòng kiểm tra lại danh sách món." }
        require(changes.isNotEmpty() && changes.map { it.itemId }.distinct().size == changes.size)
        changes.forEach { change ->
            val item = current.items.find { it.id == change.itemId }
                ?: error("Món không thuộc đơn hàng.")
            check(item.status != "CANCELLED") { "Món đã bị hủy." }
            require(change.quantity in 0..9999) { "Số lượng phải từ 0 đến 9999." }
        }
        changes.forEach { change ->
            if (change.quantity == 0) cancelItem(orderId, change.itemId, change.cancellationReason?.trim()?.take(500))
            else updateItemQuantity(orderId, change.itemId, change.quantity)
        }
        val remaining = getOrderWithItemsByIdSync(orderId)!!.items.any { it.status != "CANCELLED" }
        val total = calculateOrderTotal(orderId)
        updateOrder(order.copy(totalAmount = total, finalAmount = total,
            discountCode = null, discountAmount = 0.0,
            status = if (remaining) order.status else "CANCELLED", revision = order.revision + 1))
        if (!remaining && activeOrderCount(tableId) == 0) setTableStatus(tableId, "EMPTY")
    }

    @Transaction
    suspend fun transitionStatus(id: Long, expectedRevision: Long, status: String): Boolean {
        val order = getOrderById(id) ?: return false
        if (order.revision != expectedRevision) return false
        if (!((order.status == "PENDING" && status == "CONFIRMED") ||
                (order.status == "CONFIRMED" && status == "SERVED"))) return false
        confirmItems(id)
        updateOrder(order.copy(status = status, revision = order.revision + 1))
        return true
    }

    @Transaction
    suspend fun payOrder(id: Long, method: String, code: String?, discount: Double, final: Double): Boolean {
        val order = getOrderById(id) ?: return false
        if (order.status !in listOf("PENDING", "CONFIRMED", "SERVED")) return false
        if (!discount.isFinite() || !final.isFinite() || discount < 0 || final < 0 ||
            discount > order.totalAmount || kotlin.math.abs(final + discount - order.totalAmount) > 0.01) return false
        confirmItems(id)
        updateOrder(order.copy(paymentMethod = method, discountCode = code, discountAmount = discount,
            finalAmount = final, status = "COMPLETED", completedAt = System.currentTimeMillis(),
            revision = order.revision + 1))
        if (activeOrderCount(order.tableId) == 0) setTableStatus(order.tableId, "EMPTY")
        return true
    }

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): Order?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun getOrderWithItemsById(id: Long): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderWithItemsByIdSync(id: Long): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status IN ('PENDING', 'CONFIRMED', 'SERVED') ORDER BY createdAt DESC LIMIT 1")
    fun getActiveOrderByTable(tableId: Long): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status IN ('PENDING', 'CONFIRMED', 'SERVED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveOrderEntityByTable(tableId: Long): Order?

    @Transaction
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersWithItemsByStatus(status: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'CONFIRMED', 'SERVED') ORDER BY createdAt DESC")
    fun getActiveOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Query("SELECT * FROM orders WHERE status = 'COMPLETED' AND completedAt BETWEEN :startTime AND :endTime ORDER BY completedAt DESC")
    fun getCompletedOrdersBetween(startTime: Long, endTime: Long): Flow<List<Order>>

    @Query("SELECT SUM(finalAmount) FROM orders WHERE status = 'COMPLETED' AND completedAt BETWEEN :startTime AND :endTime")
    fun getTotalRevenueBetween(startTime: Long, endTime: Long): Flow<Double?>
}
