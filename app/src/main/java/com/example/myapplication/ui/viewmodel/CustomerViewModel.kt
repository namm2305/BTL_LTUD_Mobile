package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entity.MenuItem
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.data.repository.MenuRepository
import com.example.myapplication.data.repository.OrderRepository
import com.example.myapplication.data.repository.TableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int
)

class CustomerViewModel(
    private val tableRepository: TableRepository,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    // --- Bàn ăn hiện tại ---
    private val _selectedTable = MutableStateFlow<RestaurantTable?>(null)
    val selectedTable: StateFlow<RestaurantTable?> = _selectedTable.asStateFlow()

    fun scanAndSelectTable(qrCodeData: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val table = tableRepository.getTableByQrCode(qrCodeData)
            if (table != null) {
                _selectedTable.value = table
                onResult(true, "Nhận diện thành công ${table.tableName}")
            } else {
                onResult(false, "Không tìm thấy bàn phù hợp với mã QR này!")
            }
        }
    }

    fun setTableDirectly(table: RestaurantTable) {
        _selectedTable.value = table
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tableOrders = selectedTable.flatMapLatest { table ->
        if (table == null) flowOf(emptyList()) else orderRepository.getOrdersByTable(table.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Menu món ăn đang khả dụng ---
    val availableMenuItems: StateFlow<List<MenuItem>> = menuRepository.getAvailableMenuItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Giỏ hàng (Cart) ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(menuItem: MenuItem) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.menuItem.id == menuItem.id }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(menuItem, 1))
        }
        _cartItems.value = currentList
    }

    fun removeFromCart(menuItem: MenuItem) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.menuItem.id == menuItem.id }
        if (index != -1) {
            val existing = currentList[index]
            if (existing.quantity > 1) {
                currentList[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
        }
        _cartItems.value = currentList
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun calculateTotalAmount(): Double {
        return _cartItems.value.sumOf { it.menuItem.price * it.quantity }
    }

    // --- Đặt món (Place Order) ---
    fun placeOrder(note: String?, onResult: (Boolean, String) -> Unit) {
        val table = _selectedTable.value
        if (table == null) {
            onResult(false, "Chưa xác định bàn ăn! Vui lòng quét mã QR.")
            return
        }

        val items = _cartItems.value
        if (items.isEmpty()) {
            onResult(false, "Giỏ hàng đang trống! Vui lòng chọn món.")
            return
        }

        viewModelScope.launch {
            val totalAmount = calculateTotalAmount()
            val order = Order(
                tableId = table.id,
                tableName = table.tableName,
                totalAmount = totalAmount,
                finalAmount = totalAmount,
                status = Order.STATUS_PENDING
            )

            val orderItems = items.map { cart ->
                OrderItem(
                    orderId = 0, // Sẽ được tự động cập nhật trong Repository
                    menuItemId = cart.menuItem.id,
                    menuItemName = cart.menuItem.name,
                    price = cart.menuItem.price,
                    quantity = cart.quantity,
                    note = note
                )
            }

            orderRepository.createOrder(order, orderItems)
            clearCart()
            onResult(true, "Đặt món thành công! Vui lòng chờ nhân viên phục vụ.")
        }
    }
}
