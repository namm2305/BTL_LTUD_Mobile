package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.*

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val database by lazy { AppDatabase.getInstance(context) }

    private val userRepository by lazy { UserRepository(database.userDao()) }
    private val menuRepository by lazy { MenuRepository(database.menuItemDao()) }
    private val tableRepository by lazy { TableRepository(database.tableDao()) }
    private val discountRepository by lazy { DiscountRepository(database.discountDao()) }
    private val orderRepository by lazy {
        OrderRepository(database.orderDao(), database.orderItemDao(), database.tableDao())
    }
    private val timekeepingRepository by lazy {
        TimekeepingRepository(database.shiftDao(), database.attendanceDao(), database.shiftRegistrationDao())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(userRepository) as T
            }
            modelClass.isAssignableFrom(OwnerViewModel::class.java) -> {
                OwnerViewModel(menuRepository, discountRepository, tableRepository, orderRepository, userRepository, timekeepingRepository) as T
            }
            modelClass.isAssignableFrom(StaffViewModel::class.java) -> {
                StaffViewModel(timekeepingRepository, orderRepository, tableRepository, discountRepository) as T
            }
            modelClass.isAssignableFrom(OrderEditViewModel::class.java) -> {
                OrderEditViewModel(orderRepository, timekeepingRepository) as T
            }
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                CustomerViewModel(tableRepository, menuRepository, orderRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
