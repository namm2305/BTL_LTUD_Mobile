package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.MenuItemDao
import com.example.myapplication.data.entity.MenuItem
import kotlinx.coroutines.flow.Flow

class MenuRepository(private val menuItemDao: MenuItemDao) {

    fun getAllMenuItems(): Flow<List<MenuItem>> = menuItemDao.getAllMenuItems()

    fun getAvailableMenuItems(): Flow<List<MenuItem>> = menuItemDao.getAvailableMenuItems()

    suspend fun getItemById(id: Long): MenuItem? = menuItemDao.getItemById(id)

    suspend fun insertMenuItem(item: MenuItem): Long = menuItemDao.insertMenuItem(item)

    suspend fun updateMenuItem(item: MenuItem) = menuItemDao.updateMenuItem(item)

    suspend fun deleteMenuItem(item: MenuItem) = menuItemDao.deleteMenuItem(item)
}
