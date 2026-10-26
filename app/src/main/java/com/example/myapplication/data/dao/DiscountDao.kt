package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.Discount
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscount(discount: Discount): Long

    @Update
    suspend fun updateDiscount(discount: Discount)

    @Delete
    suspend fun deleteDiscount(discount: Discount)

    @Query("SELECT * FROM discounts WHERE code = :code AND isActive = 1 LIMIT 1")
    suspend fun getDiscountByCode(code: String): Discount?

    @Query("SELECT * FROM discounts ORDER BY id DESC")
    fun getAllDiscounts(): Flow<List<Discount>>

    @Query("SELECT * FROM discounts WHERE isActive = 1 ORDER BY code ASC")
    fun getActiveDiscounts(): Flow<List<Discount>>
}
