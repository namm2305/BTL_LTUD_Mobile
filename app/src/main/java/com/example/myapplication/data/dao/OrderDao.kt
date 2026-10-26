package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

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
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status IN ('PENDING', 'SERVED') ORDER BY createdAt DESC LIMIT 1")
    fun getActiveOrderByTable(tableId: Long): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status IN ('PENDING', 'SERVED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveOrderEntityByTable(tableId: Long): Order?

    @Transaction
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersWithItemsByStatus(status: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'SERVED') ORDER BY createdAt DESC")
    fun getActiveOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Query("SELECT * FROM orders WHERE status = 'COMPLETED' AND completedAt BETWEEN :startTime AND :endTime ORDER BY completedAt DESC")
    fun getCompletedOrdersBetween(startTime: Long, endTime: Long): Flow<List<Order>>

    @Query("SELECT SUM(finalAmount) FROM orders WHERE status = 'COMPLETED' AND completedAt BETWEEN :startTime AND :endTime")
    fun getTotalRevenueBetween(startTime: Long, endTime: Long): Flow<Double?>
}
