package com.example.myapplication.ui.owner

import android.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.MenuItem
import com.example.myapplication.databinding.DialogAddEditMenuItemBinding
import com.example.myapplication.databinding.FragmentOwnerMenuBinding
import com.example.myapplication.ui.adapter.OwnerMenuAdapter
import com.example.myapplication.ui.viewmodel.OwnerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OwnerMenuFragment : Fragment() {

    private var _binding: FragmentOwnerMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerViewModel by activityViewModels()
    private lateinit var adapter: OwnerMenuAdapter

    private var selectedImageUri: Uri? = null
    private var currentDialogBinding: DialogAddEditMenuItemBinding? = null

    // Launcher mở thư viện chọn ảnh thiết bị
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                Toast.makeText(
                    requireContext(),
                    "Không thể giữ quyền đọc ảnh. Vui lòng chọn ảnh khác.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            selectedImageUri = uri
            currentDialogBinding?.ivMenuImagePreview?.let { preview ->
                try {
                    preview.setImageURI(null)
                    preview.setImageURI(uri)
                    if (preview.drawable == null) {
                        preview.setImageResource(R.drawable.ic_menu_gallery)
                    }
                } catch (e: Exception) {
                    preview.setImageResource(R.drawable.ic_menu_gallery)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddMenuItem.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerMenuAdapter(
            onEditClick = { menuItem -> showAddEditDialog(menuItem) },
            onDeleteClick = { menuItem -> confirmDelete(menuItem) }
        )
        binding.rvMenuItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMenuItems.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuItems.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmptyMenu.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddEditDialog(menuItem: MenuItem?) {
        val dialogBinding = DialogAddEditMenuItemBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding

        selectedImageUri = menuItem?.imageUri?.let { Uri.parse(it) }

        if (menuItem != null) {
            dialogBinding.tvDialogTitle.text = "Sửa Món Ăn"
            dialogBinding.etMenuName.setText(menuItem.name)
            dialogBinding.etMenuPrice.setText(menuItem.price.toString())
            dialogBinding.etMenuCategory.setText(menuItem.category)

            if (selectedImageUri != null) {
                try {
                    dialogBinding.ivMenuImagePreview.setImageURI(selectedImageUri)
                } catch (e: Exception) {
                    dialogBinding.ivMenuImagePreview.setImageResource(R.drawable.ic_menu_gallery)
                }
            }
        } else {
            dialogBinding.tvDialogTitle.text = "Thêm Món Ăn Mới"
        }

        // Nút "Chọn ảnh từ Thư viện"
        dialogBinding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu") { _, _ ->
                val name = dialogBinding.etMenuName.text.toString().trim()
                val priceStr = dialogBinding.etMenuPrice.text.toString().trim()
                val category = dialogBinding.etMenuCategory.text.toString().trim().ifEmpty { "Món chính" }
                val imageUriStr = selectedImageUri?.toString()

                if (name.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val price = priceStr.toDoubleOrNull() ?: 0.0
                if (menuItem != null) {
                    viewModel.updateMenuItem(menuItem.copy(name = name, price = price, category = category, imageUri = imageUriStr))
                    Toast.makeText(requireContext(), "Đã cập nhật món ăn", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addMenuItem(name, price, category, imageUriStr)
                    Toast.makeText(requireContext(), "Đã thêm món ăn mới", Toast.LENGTH_SHORT).show()
                }
                currentDialogBinding = null
            }
            .setNegativeButton("Hủy") { _, _ ->
                currentDialogBinding = null
            }
            .show()
    }

    private fun confirmDelete(menuItem: MenuItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa món ăn")
            .setMessage("Bạn có chắc chắn muốn xóa ${menuItem.name}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteMenuItem(menuItem)
                Toast.makeText(requireContext(), "Đã xóa món ăn", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentDialogBinding = null
        _binding = null
    }
}
