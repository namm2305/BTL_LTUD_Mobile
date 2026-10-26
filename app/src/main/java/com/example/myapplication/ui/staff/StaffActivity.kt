package com.example.myapplication.ui.staff

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityStaffBinding
import com.example.myapplication.ui.viewmodel.StaffViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory

class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding

    private val viewModel: StaffViewModel by viewModels {
        ViewModelFactory(this)
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = ViewModelFactory(this)

    // Thông tin nhân viên đăng nhập
    var staffUserId: Long = 2
    var staffName: String = "Nhân Viên A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupWindowInsets()

        staffUserId = intent.getLongExtra("EXTRA_USER_ID", 2)
        staffName = intent.getStringExtra("EXTRA_USER_NAME") ?: "Nhân Viên A"

        setupBottomNavigation(savedInstanceState)
    }

    private fun setupWindowInsets() {
        val root = binding.root
        val header = binding.toolbarStaff
        val headerHeight = header.layoutParams.height
        val headerPaddingTop = header.paddingTop
        val rootPaddingLeft = root.paddingLeft
        val rootPaddingRight = root.paddingRight
        val bottomNav = binding.bottomNavStaff
        val bottomPadding = bottomNav.paddingBottom
        // Root handles navigation insets; avoid Material applying them a second time.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { _, insets -> insets }

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
            bottomNav.updatePadding(bottom = bottomPadding + insets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun setupBottomNavigation(savedInstanceState: Bundle?) {
        binding.bottomNavStaff.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_staff_order -> {
                    replaceFragment(StaffOrderFragment())
                    true
                }
                R.id.nav_staff_shift_management -> {
                    replaceFragment(StaffShiftManagementFragment())
                    true
                }
                R.id.nav_staff_shift_registration -> {
                    replaceFragment(StaffShiftRegistrationFragment())
                    true
                }
                else -> false
            }
        }

        // Chọn tab đầu tiên (Phục vụ) làm mặc định khi ứng dụng khởi chạy lần đầu
        if (savedInstanceState == null) {
            binding.bottomNavStaff.selectedItemId = R.id.nav_staff_order
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerStaff, fragment)
            .commit()
    }
}
