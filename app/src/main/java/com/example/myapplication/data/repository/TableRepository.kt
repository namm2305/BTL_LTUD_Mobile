package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.TableDao
import com.example.myapplication.data.entity.RestaurantTable
import kotlinx.coroutines.flow.Flow

class TableRepository(private val tableDao: TableDao) {

    fun getAllTables(): Flow<List<RestaurantTable>> = tableDao.getAllTables()

    suspend fun getTableById(id: Long): RestaurantTable? = tableDao.getTableById(id)

    suspend fun getTableByQrCode(qrCode: String): RestaurantTable? = tableDao.getTableByQrCode(qrCode)

    suspend fun insertTable(table: RestaurantTable): Long = tableDao.insertTable(table)

    suspend fun updateTable(table: RestaurantTable) = tableDao.updateTable(table)

    suspend fun deleteTable(table: RestaurantTable) = tableDao.deleteTable(table)

    suspend fun updateTableStatus(tableId: Long, status: String) = tableDao.updateTableStatus(tableId, status)
}
