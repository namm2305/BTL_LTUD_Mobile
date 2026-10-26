package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.MenuItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem): Long

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Delete
    suspend fun deleteMenuItem(item: MenuItem)

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): MenuItem?

    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE isAvailable = 1 ORDER BY category ASC, name ASC")
    fun getAvailableMenuItems(): Flow<List<MenuItem>>
}
