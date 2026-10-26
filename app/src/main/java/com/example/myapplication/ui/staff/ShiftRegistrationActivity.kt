package com.example.myapplication.ui.staff

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityShiftRegistrationBinding
import com.example.myapplication.ui.adapter.StaffShiftRegisterAdapter
import com.example.myapplication.ui.viewmodel.StaffViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShiftRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShiftRegistrationBinding

    private val viewModel: StaffViewModel by viewModels {
        ViewModelFactory(this)
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = ViewModelFactory(this)

    private lateinit var adapter: StaffShiftRegisterAdapter
    private var staffUserId: Long = 2
    private var selectedDateStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShiftRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        staffUserId = intent.getLongExtra("EXTRA_USER_ID", 2)

        binding.toolbarShiftRegistration.setNavigationOnClickListener {
            finish()
        }

        setupDatePickerAndShifts()
    }

    private fun setupDatePickerAndShifts() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDateStr = dateFormat.format(Date())

        binding.etSelectDate.setText(selectedDateStr)

        adapter = StaffShiftRegisterAdapter(
            onToggleClick = { item ->
                viewModel.toggleShiftRegistration(staffUserId, item.shift.id, selectedDateStr) { _, msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvAvailableShifts.layoutManager = LinearLayoutManager(this)
        binding.rvAvailableShifts.adapter = adapter

        // Bấm chọn ngày từ DatePickerDialog
        binding.etSelectDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDateStr = dateFormat.format(calendar.time)
                    binding.etSelectDate.setText(selectedDateStr)
                    loadShiftsForDate(selectedDateStr)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        loadShiftsForDate(selectedDateStr)
    }

    private fun loadShiftsForDate(dateStr: String) {
        lifecycleScope.launch {
            viewModel.getShiftsWithRegistrationState(staffUserId, dateStr).collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyShifts.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
