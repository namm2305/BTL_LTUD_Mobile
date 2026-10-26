package com.example.myapplication.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.Discount
import com.example.myapplication.databinding.DialogAddDiscountBinding
import com.example.myapplication.databinding.FragmentOwnerDiscountBinding
import com.example.myapplication.ui.adapter.OwnerDiscountAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OwnerDiscountFragment : Fragment() {

    private var _binding: FragmentOwnerDiscountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerDiscountAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerDiscountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddDiscount.setOnClickListener {
            showAddDiscountDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerDiscountAdapter(
            onDeleteClick = { discount -> confirmDelete(discount) }
        )
        binding.rvDiscounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDiscounts.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discounts.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyDiscount.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddDiscountDialog() {
        val dialogBinding = DialogAddDiscountBinding.inflate(layoutInflater)

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Tạo mã") { _, _ ->
                val code = dialogBinding.etDiscountCode.text.toString().trim()
                val percentStr = dialogBinding.etDiscountPercent.text.toString().trim()
                val minOrderStr = dialogBinding.etMinOrderAmount.text.toString().trim()

                if (code.isEmpty() || percentStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập Mã và % giảm giá", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val percent = percentStr.toDoubleOrNull() ?: 0.0
                val minOrder = minOrderStr.toDoubleOrNull() ?: 0.0

                viewModel.addDiscount(code, percent, minOrder)
                Toast.makeText(requireContext(), "Đã tạo mã giảm giá mới", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDelete(discount: Discount) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa mã giảm giá")
            .setMessage("Bạn có chắc muốn xóa mã ${discount.code}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteDiscount(discount)
                Toast.makeText(requireContext(), "Đã xóa mã giảm giá", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
