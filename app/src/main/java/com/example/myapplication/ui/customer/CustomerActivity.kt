package com.example.myapplication.ui.customer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.databinding.ActivityCustomerBinding
import com.example.myapplication.databinding.DialogCustomerCartBinding
import com.example.myapplication.ui.adapter.CustomerCartAdapter
import com.example.myapplication.ui.adapter.CustomerMenuAdapter
import com.example.myapplication.ui.viewmodel.CustomerViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerBinding

    private val viewModel: CustomerViewModel by viewModels {
        ViewModelFactory(this)
    }

    private lateinit var menuAdapter: CustomerMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupWindowInsets()

        val tableId = intent.getLongExtra("EXTRA_TABLE_ID", 1)
        val tableName = intent.getStringExtra("EXTRA_TABLE_NAME") ?: "Bàn 01"
        val qrCodeData = intent.getStringExtra("EXTRA_QR_CODE") ?: "TABLE_1"

        viewModel.setTableDirectly(
            RestaurantTable(id = tableId, tableName = tableName, qrCodeData = qrCodeData)
        )

        binding.tvCurrentTable.text = "Đang phục vụ: $tableName"

        setupRecyclerView()
        observeData()

        binding.btnViewCart.setOnClickListener {
            showCartDialog()
        }
    }

    private fun setupWindowInsets() {
        val root = binding.root
        val header = binding.toolbarCustomer
        val cart = binding.cardCartSummary
        val headerHeight = header.layoutParams.height
        val headerPaddingTop = header.paddingTop
        val rootPaddingLeft = root.paddingLeft
        val rootPaddingRight = root.paddingRight
        val cartBottomMargin =
            (cart.layoutParams as android.view.ViewGroup.MarginLayoutParams).bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val safeTop = maxOf(insets.top, cutout.top)

            header.updatePadding(top = headerPaddingTop + safeTop)
            header.updateLayoutParams {
                height = headerHeight + safeTop
            }
            root.updatePadding(
                left = rootPaddingLeft + maxOf(insets.left, cutout.left),
                right = rootPaddingRight + maxOf(insets.right, cutout.right)
            )
            cart.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = cartBottomMargin + insets.bottom
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun setupRecyclerView() {
        menuAdapter = CustomerMenuAdapter(
            onAddToCart = { menuItem ->
                viewModel.addToCart(menuItem)
                Toast.makeText(this, "Đã thêm ${menuItem.name} vào giỏ", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvCustomerMenu.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerMenu.adapter = menuAdapter
    }

    private fun observeData() {
        // Quan sát danh sách món ăn
        lifecycleScope.launch {
            viewModel.availableMenuItems.collectLatest { list ->
                menuAdapter.submitList(list)
            }
        }

        // Quan sát giỏ hàng
        lifecycleScope.launch {
            viewModel.cartItems.collectLatest { cartList ->
                if (cartList.isNotEmpty()) {
                    binding.cardCartSummary.visibility = View.VISIBLE
                    val totalQty = cartList.sumOf { it.quantity }
                    val totalPrice = viewModel.calculateTotalAmount()
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

                    binding.tvCartCount.text = "$totalQty món trong giỏ"
                    binding.tvCartTotal.text = formatter.format(totalPrice)
                } else {
                    binding.cardCartSummary.visibility = View.GONE
                }
            }
        }
    }

    private fun showCartDialog() {
        val cartList = viewModel.cartItems.value
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogCustomerCartBinding.inflate(layoutInflater)
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        val cartAdapter = CustomerCartAdapter(
            onIncrease = { cartItem ->
                viewModel.addToCart(cartItem.menuItem)
            },
            onDecrease = { cartItem ->
                viewModel.removeFromCart(cartItem.menuItem)
            }
        )

        dialogBinding.rvCartItems.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvCartItems.adapter = cartAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Quan sát giỏ hàng thời gian thực trong Dialog
        lifecycleScope.launch {
            viewModel.cartItems.collectLatest { currentList ->
                cartAdapter.submitList(currentList)
                val total = viewModel.calculateTotalAmount()
                dialogBinding.tvCartTotalAmount.text = currencyFormatter.format(total)

                if (currentList.isEmpty() && dialog.isShowing) {
                    dialog.dismiss()
                    Toast.makeText(this@CustomerActivity, "Giỏ hàng đã hết món", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.btnPlaceOrder.setOnClickListener {
            val note = dialogBinding.etOrderNote.text.toString().trim().ifEmpty { null }
            viewModel.placeOrder(
                note = note,
                onResult = { success, msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        dialog.dismiss()
                    }
                }
            )
        }

        dialog.show()
    }
}
