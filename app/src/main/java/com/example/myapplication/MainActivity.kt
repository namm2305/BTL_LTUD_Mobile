package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.TableRepository
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.DialogAccountLoginBinding
import com.example.myapplication.ui.customer.CustomerActivity
import com.example.myapplication.ui.owner.OwnerActivity
import com.example.myapplication.ui.staff.StaffActivity
import com.example.myapplication.ui.viewmodel.AuthState
import com.example.myapplication.ui.viewmodel.AuthViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val authViewModel: AuthViewModel by viewModels {
        ViewModelFactory(this)
    }

    private val tableRepository by lazy {
        TableRepository(AppDatabase.getInstance(this).tableDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo trước database để pre-populate dữ liệu mẫu
        AppDatabase.getInstance(this)

        setupRoleClickListeners()
        observeAuthState()
    }

    private fun setupRoleClickListeners() {
        // 1. Vai trò Khách Hàng -> Quét mã QR
        binding.cardRoleCustomer.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setOrientationLocked(false)
            integrator.setPrompt("Hướng camera vào mã QR trên bàn ăn")
            integrator.initiateScan()
        }

        // Quick demo chọn bàn trực tiếp (Thử nghiệm không cần Camera)
        binding.tvDemoChoice.setOnClickListener {
            showQuickTableChoiceDialog()
        }

        // 2. Vai trò Nhân Viên -> Đăng nhập bằng Username & Password
        binding.cardRoleStaff.setOnClickListener {
            showAccountLoginDialog("NHÂN VIÊN", defaultUser = "nv1")
        }

        // 3. Vai trò Chủ Nhà Hàng -> Đăng nhập bằng Username: admin / Password: 1234
        binding.cardRoleOwner.setOnClickListener {
            showAccountLoginDialog("CHỦ NHÀ HÀNG", defaultUser = "admin")
        }
    }

    private fun showAccountLoginDialog(roleName: String, defaultUser: String) {
        val dialogBinding = DialogAccountLoginBinding.inflate(layoutInflater)
        dialogBinding.tvLoginDialogTitle.text = "Đăng Nhập $roleName"
        dialogBinding.tvLoginDialogSub.text = "Vui lòng nhập tài khoản và mật khẩu"
        dialogBinding.etUsername.setText(defaultUser)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Đăng nhập") { _, _ ->
                val username = dialogBinding.etUsername.text.toString().trim()
                val password = dialogBinding.etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Gọi ViewModel xử lý đăng nhập bằng Username & Password
                authViewModel.login(username, password)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthState.Success -> {
                        val user = state.user
                        Toast.makeText(this@MainActivity, "Đăng nhập thành công: ${user.fullName}", Toast.LENGTH_SHORT).show()
                        if (user.role == "OWNER") {
                            startActivity(Intent(this@MainActivity, OwnerActivity::class.java))
                        } else if (user.role == "STAFF") {
                            val intent = Intent(this@MainActivity, StaffActivity::class.java)
                            intent.putExtra("EXTRA_USER_ID", user.id)
                            intent.putExtra("EXTRA_USER_NAME", user.fullName)
                            startActivity(intent)
                        }
                        authViewModel.logout()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                        authViewModel.logout()
                    }
                    else -> {}
                }
            }
        }
    }

    // Xử lý kết quả quét QR Code
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val qrCode = result.contents
                lifecycleScope.launch {
                    val table = tableRepository.getTableByQrCode(qrCode)
                    if (table != null) {
                        Toast.makeText(this@MainActivity, "Quét thành công: ${table.tableName}", Toast.LENGTH_SHORT).show()
                        openCustomerScreen(table.id, table.tableName, table.qrCodeData)
                    } else {
                        Toast.makeText(this@MainActivity, "Mã QR '$qrCode' không khớp với bàn nào trong hệ thống!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Đã hủy quét QR", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showQuickTableChoiceDialog() {
        lifecycleScope.launch {
            tableRepository.getAllTables().collectLatest { tables ->
                if (tables.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Chưa có dữ liệu bàn ăn", Toast.LENGTH_SHORT).show()
                    return@collectLatest
                }
                val tableNames = tables.map { it.tableName }.toTypedArray()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Chọn bàn ăn để đặt món (Demo)")
                    .setItems(tableNames) { _, which ->
                        val selectedTable = tables[which]
                        openCustomerScreen(selectedTable.id, selectedTable.tableName, selectedTable.qrCodeData)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        }
    }

    private fun openCustomerScreen(tableId: Long, tableName: String, qrCode: String) {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            putExtra("EXTRA_TABLE_ID", tableId)
            putExtra("EXTRA_TABLE_NAME", tableName)
            putExtra("EXTRA_QR_CODE", qrCode)
        }
        startActivity(intent)
    }
}
