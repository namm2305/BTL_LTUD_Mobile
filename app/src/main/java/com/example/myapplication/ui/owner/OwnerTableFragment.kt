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
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.databinding.DialogAddTableBinding
import com.example.myapplication.databinding.DialogViewTableQrBinding
import com.example.myapplication.databinding.FragmentOwnerTablesBinding
import com.example.myapplication.ui.adapter.OwnerTableAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OwnerTableFragment : Fragment() {

    private var _binding: FragmentOwnerTablesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerTableAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddTable.setOnClickListener {
            showAddTableDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerTableAdapter(
            onQrClick = { table -> showViewQrDialog(table) },
            onDeleteClick = { table -> confirmDelete(table) }
        )
        binding.rvTables.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTables.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tables.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyTables.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddTableDialog() {
        val dialogBinding = DialogAddTableBinding.inflate(layoutInflater)
        var generatedQrData: String? = null

        // Nút "Tự Tạo & Xem Mã QR"
        dialogBinding.btnGeneratePreview.setOnClickListener {
            val tableName = dialogBinding.etTableName.text.toString().trim()
            if (tableName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập Tên bàn trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sanitized = tableName.replace(" ", "_").uppercase()
            generatedQrData = "TABLE_${sanitized}_${(100..999).random()}"

            // Tự động vẽ hình ảnh QR
            val bitmap = viewModel.generateQrBitmap(generatedQrData!!, 300, 300)
            if (bitmap != null) {
                dialogBinding.layoutQrPreview.visibility = View.VISIBLE
                dialogBinding.ivQrPreview.setImageBitmap(bitmap)
                dialogBinding.tvQrCodeDataInfo.text = "Mã QR tự động: $generatedQrData"
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu bàn") { _, _ ->
                val tableName = dialogBinding.etTableName.text.toString().trim()
                if (tableName.isEmpty()) {
                    Toast.makeText(requireContext(), "Tên bàn không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Nếu chưa nhấn Tạo trước, tự động sinh mã QR dữ liệu
                if (generatedQrData == null) {
                    val sanitized = tableName.replace(" ", "_").uppercase()
                    generatedQrData = "TABLE_${sanitized}_${(100..999).random()}"
                }

                viewModel.addTableAutoQr(tableName, generatedQrData)
                Toast.makeText(requireContext(), "Đã thêm $tableName thành công!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showViewQrDialog(table: RestaurantTable) {
        val dialogBinding = DialogViewTableQrBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTableName.text = "Mã QR - ${table.tableName}"
        dialogBinding.tvDialogQrCodeData.text = "Mã định danh: ${table.qrCodeData}"

        val bitmap = viewModel.generateQrBitmap(table.qrCodeData, 500, 500)
        if (bitmap != null) {
            dialogBinding.ivFullQrCode.setImageBitmap(bitmap)
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun confirmDelete(table: RestaurantTable) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa bàn ăn")
            .setMessage("Bạn có chắc muốn xóa ${table.tableName}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTable(table)
                Toast.makeText(requireContext(), "Đã xóa bàn", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
