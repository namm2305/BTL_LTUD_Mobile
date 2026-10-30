package com.example.myapplication.data.model

import com.example.myapplication.data.entity.ShiftEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object OtPolicy {
    const val THRESHOLD_MINUTES = 15L

    fun actualMinutes(expectedEnd: Long, checkOut: Long): Long =
        ((checkOut - expectedEnd).coerceAtLeast(0L)) / 60_000L

    fun status(expectedEnd: Long, checkOut: Long): String =
        if (checkOut - expectedEnd > THRESHOLD_MINUTES * 60_000L) "PENDING" else "NONE"

    fun expectedEnd(shift: ShiftEntity, attendanceDate: String): Long {
        val date = shift.workDate.ifBlank { attendanceDate }
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            isLenient = false
        }
        val end = requireNotNull(format.parse("$date ${shift.expectedEndTime}"))
        return Calendar.getInstance().apply {
            time = end
            if (shift.isOvernight || shift.expectedEndTime <= shift.expectedStartTime) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }
}
