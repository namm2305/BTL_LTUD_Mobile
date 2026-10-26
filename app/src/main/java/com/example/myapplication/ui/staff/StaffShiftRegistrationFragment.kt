package com.example.myapplication.ui.staff

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentStaffShiftRegistrationBinding
import com.example.myapplication.ui.adapter.StaffShiftRegisterAdapter
import com.example.myapplication.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StaffShiftRegistrationFragment : Fragment() {

    private var _binding: FragmentStaffShiftRegistrationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffViewModel by activityViewModels()
    private lateinit var adapter: StaffShiftRegisterAdapter

    private var staffUserId: Long = 2
    private var selectedDateStr: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffShiftRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        staffUserId = (activity as? StaffActivity)?.staffUserId ?: 2

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
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvAvailableShifts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAvailableShifts.adapter = adapter

        // Bấm chọn Ngày bằng DatePickerDialog
        binding.etSelectDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getShiftsWithRegistrationState(staffUserId, dateStr).collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyShifts.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
