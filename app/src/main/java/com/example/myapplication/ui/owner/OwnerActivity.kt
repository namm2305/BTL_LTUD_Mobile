package com.example.myapplication.ui.owner

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
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.databinding.ActivityOwnerBinding
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator

class OwnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBinding

    private val viewModel: OwnerViewModel by viewModels {
        ViewModelFactory(this)
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = ViewModelFactory(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupWindowInsets()

        setupViewPagerAndTabs()
    }

    private fun setupWindowInsets() {
        val root = binding.root
        val header = binding.toolbarOwner
        val headerHeight = header.layoutParams.height
        val headerPaddingTop = header.paddingTop
        val rootPaddingLeft = root.paddingLeft
        val rootPaddingRight = root.paddingRight
        val rootPaddingBottom = root.paddingBottom

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
                right = rootPaddingRight + maxOf(insets.right, cutout.right),
                bottom = rootPaddingBottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun setupViewPagerAndTabs() {
        val adapter = OwnerPagerAdapter(this)
        binding.viewPagerOwner.adapter = adapter

        val tabTitles = arrayOf("Thực Đơn", "Mã Giảm Giá", "Quản Lý Bàn", "Nhân Viên", "Ca Làm", "Chấm Công", "Báo Cáo")

        TabLayoutMediator(binding.tabLayoutOwner, binding.viewPagerOwner) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private inner class OwnerPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 7

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OwnerMenuFragment()
                1 -> OwnerDiscountFragment()
                2 -> OwnerTableFragment()
                3 -> OwnerStaffFragment()
                4 -> OwnerShiftFragment()
                5 -> OwnerAttendanceFragment()
                6 -> OwnerReportFragment()
                else -> OwnerMenuFragment()
            }
        }
    }
}
