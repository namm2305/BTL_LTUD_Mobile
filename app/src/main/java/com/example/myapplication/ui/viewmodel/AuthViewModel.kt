package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entity.User
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(username: String, password: String) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập tài khoản và mật khẩu")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // 1. Đăng nhập Chủ quán: Hardcode username = "admin", password = "1234"
            if (trimmedUser == "admin" && trimmedPass == "1234") {
                val ownerUser = User(
                    id = 1,
                    username = "admin",
                    password = "1234",
                    fullName = "Chủ Nhà Hàng",
                    role = "OWNER"
                )
                _currentUser.value = ownerUser
                _authState.value = AuthState.Success(ownerUser)
                return@launch
            }

            // 2. Đăng nhập Nhân viên: Kiểm tra username / password từ Room Database
            val staffUser = userRepository.login(trimmedUser, trimmedPass)
            if (staffUser != null) {
                _currentUser.value = staffUser
                _authState.value = AuthState.Success(staffUser)
            } else {
                _authState.value = AuthState.Error("Tài khoản hoặc mật khẩu không chính xác!")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
}
