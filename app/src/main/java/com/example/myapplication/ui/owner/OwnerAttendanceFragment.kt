package com.example.myapplication.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentOwnerAttendanceBinding
import com.example.myapplication.ui.adapter.OwnerAttendanceAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OwnerAttendanceFragment : Fragment() {

    private var _binding: FragmentOwnerAttendanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerAttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = OwnerAttendanceAdapter()
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendanceList.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyAttendance.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
