package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.OrderItemChange
import com.example.myapplication.data.model.OrderWithItems
import com.example.myapplication.data.repository.OrderRepository
import com.example.myapplication.data.repository.TimekeepingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderEditorState(
    val snapshot: OrderWithItems? = null,
    val quantities: Map<Long, Int> = emptyMap(),
    val reasons: Map<Long, String?> = emptyMap(),
    val busy: Boolean = false,
    val message: String = ""
)

class OrderEditViewModel(
    private val repository: OrderRepository,
    private val timekeeping: TimekeepingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OrderEditorState())
    val state = _state.asStateFlow()
    private var observer: Job? = null
    private var orderId = 0L
    private var tableId = 0L
    private var staffId: Long? = null

    fun open(id: Long, table: Long, staff: Long?) {
        if (observer != null) return
        orderId = id
        tableId = table
        staffId = staff
        observer = viewModelScope.launch {
            repository.getOrderWithItemsById(id).collect { current ->
                val old = _state.value
                if (old.snapshot?.order?.revision != current?.order?.revision) {
                    _state.value = old.copy(snapshot = current, quantities = emptyMap(), reasons = emptyMap(),
                        message = if (old.quantities.isNotEmpty() && !old.busy)
                            "Đơn vừa thay đổi. Danh sách đã được tải lại, vui lòng kiểm tra trước khi sửa."
                        else old.message)
                } else _state.value = old.copy(snapshot = current)
            }
        }
    }

    fun change(itemId: Long, quantity: Int, reason: String? = null) {
        val current = _state.value
        val order = current.snapshot ?: return
        if (current.busy || !editable(order.order.status)) return
        val item = order.items.find { it.id == itemId && it.status != "CANCELLED" } ?: return
        if (quantity !in 0..9999) return
        val quantities = current.quantities.toMutableMap()
        if (quantity == item.quantity) quantities.remove(itemId) else quantities[itemId] = quantity
        _state.value = current.copy(quantities = quantities,
            reasons = current.reasons + (itemId to reason), message = "")
        if (staffId != null) save()
    }

    fun editable(status: String): Boolean =
        status == "PENDING" || (staffId != null && status in listOf("CONFIRMED", "SERVED"))

    fun save() {
        val current = _state.value
        val order = current.snapshot ?: return
        if (current.busy || current.quantities.isEmpty()) return
        _state.value = current.copy(busy = true, message = "")
        viewModelScope.launch {
            try {
                val staff = staffId
                if (staff != null) check(timekeeping.getActiveAttendance(staff) != null) {
                    "Bạn cần Check-in ca làm trước khi chỉnh sửa đơn."
                }
                repository.editItems(orderId, tableId, staff != null, order.order.revision,
                    current.quantities.map { (id, quantity) ->
                        OrderItemChange(id, quantity, current.reasons[id])
                    })
                _state.value = _state.value.copy(busy = false, quantities = emptyMap(),
                    reasons = emptyMap(), message = "Đã cập nhật đơn hàng.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false,
                    message = e.message ?: "Không thể cập nhật đơn. Vui lòng thử lại.")
            }
        }
    }
}
