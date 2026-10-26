package com.example.myapplication.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.databinding.ItemOwnerTableBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class OwnerTableAdapter(
    private val onQrClick: (RestaurantTable) -> Unit,
    private val onDeleteClick: (RestaurantTable) -> Unit
) : ListAdapter<RestaurantTable, OwnerTableAdapter.TableViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val binding = ItemOwnerTableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TableViewHolder(private val binding: ItemOwnerTableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(table: RestaurantTable) {
            binding.tvTableName.text = table.tableName
            binding.tvQrCodeData.text = "Mã QR: ${table.qrCodeData}"

            // Tự động tạo ảnh Bitmap từ chuỗi QR và hiển thị lên ImageView
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(table.qrCodeData, BarcodeFormat.QR_CODE, 150, 150)
                binding.ivQrIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (table.status == RestaurantTable.STATUS_OCCUPIED) {
                binding.tvTableStatus.text = "Trạng thái: Đang có khách"
                binding.tvTableStatus.setTextColor(Color.RED)
            } else {
                binding.tvTableStatus.text = "Trạng thái: Bàn trống"
                binding.tvTableStatus.setTextColor(Color.parseColor("#388E3C"))
            }

            // Bấm vào mã QR hoặc item để xem chi tiết mã QR phóng to
            binding.ivQrIcon.setOnClickListener { onQrClick(table) }
            binding.btnDeleteTable.setOnClickListener { onDeleteClick(table) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RestaurantTable>() {
            override fun areItemsTheSame(
                oldItem: RestaurantTable,
                newItem: RestaurantTable
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: RestaurantTable,
                newItem: RestaurantTable
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
