package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.RestaurantTable
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RestaurantTable): Long

    @Update
    suspend fun updateTable(table: RestaurantTable)

    @Delete
    suspend fun deleteTable(table: RestaurantTable)

    @Query("SELECT * FROM tables WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: Long): RestaurantTable?

    @Query("SELECT * FROM tables WHERE qrCodeData = :qrCode LIMIT 1")
    suspend fun getTableByQrCode(qrCode: String): RestaurantTable?

    @Query("SELECT * FROM tables ORDER BY tableName ASC")
    fun getAllTables(): Flow<List<RestaurantTable>>

    @Query("UPDATE tables SET status = :status WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: Long, status: String)
}
