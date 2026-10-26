package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.DiscountDao
import com.example.myapplication.data.entity.Discount
import kotlinx.coroutines.flow.Flow

class DiscountRepository(private val discountDao: DiscountDao) {

    fun getAllDiscounts(): Flow<List<Discount>> = discountDao.getAllDiscounts()

    fun getActiveDiscounts(): Flow<List<Discount>> = discountDao.getActiveDiscounts()

    suspend fun getDiscountByCode(code: String): Discount? = discountDao.getDiscountByCode(code)

    suspend fun insertDiscount(discount: Discount): Long = discountDao.insertDiscount(discount)

    suspend fun updateDiscount(discount: Discount) = discountDao.updateDiscount(discount)

    suspend fun deleteDiscount(discount: Discount) = discountDao.deleteDiscount(discount)
}
