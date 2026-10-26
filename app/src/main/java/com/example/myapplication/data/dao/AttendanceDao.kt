package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.AttendanceEntity
import com.example.myapplication.data.model.AttendanceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE staffId = :staffId AND actualCheckOut IS NULL ORDER BY actualCheckIn DESC LIMIT 1")
    suspend fun getActiveAttendance(staffId: Long): AttendanceEntity?

    @Transaction
    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY actualCheckIn DESC")
    fun getAttendanceByStaff(staffId: Long): Flow<List<AttendanceWithDetails>>

    @Transaction
    @Query("SELECT * FROM attendance ORDER BY actualCheckIn DESC")
    fun getAllAttendanceWithDetails(): Flow<List<AttendanceWithDetails>>

    @Transaction
    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate ORDER BY actualCheckIn DESC")
    fun getAttendanceBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceWithDetails>>

    @Query("SELECT shiftId FROM attendance WHERE staffId = :staffId AND date = :date AND actualCheckOut IS NOT NULL")
    fun getCompletedShiftIdsForStaffOnDate(staffId: Long, date: String): Flow<List<Long>>

    // CHỈ tính tổng tiền lương của các ca CHƯA ĐƯỢC thanh toán (isPaid = 0)
    @Query("SELECT SUM(totalSalary) FROM attendance WHERE staffId = :staffId AND isPaid = 0")
    fun getTotalSalaryByStaff(staffId: Long): Flow<Double?>

    @Query("SELECT SUM(totalSalary) FROM attendance WHERE staffId = :staffId AND isPaid = 0 AND date BETWEEN :startDate AND :endDate")
    fun getTotalSalaryByStaffBetweenDates(staffId: Long, startDate: String, endDate: String): Flow<Double?>

    // Đánh dấu tất cả các ca làm của nhân viên đó là ĐÃ thanh toán lương (isPaid = 1)
    @Query("UPDATE attendance SET isPaid = 1 WHERE staffId = :staffId AND isPaid = 0")
    suspend fun markSalaryAsPaid(staffId: Long)
}
