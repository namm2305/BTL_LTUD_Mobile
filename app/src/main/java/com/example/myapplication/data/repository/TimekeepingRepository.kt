package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.AttendanceDao
import com.example.myapplication.data.dao.ShiftDao
import com.example.myapplication.data.dao.ShiftRegistrationDao
import com.example.myapplication.data.entity.AttendanceEntity
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.entity.ShiftRegistrationEntity
import com.example.myapplication.data.model.AttendanceWithDetails
import com.example.myapplication.data.model.ShiftRegistrationWithDetails
import kotlinx.coroutines.flow.Flow

class TimekeepingRepository(
    private val shiftDao: ShiftDao,
    private val attendanceDao: AttendanceDao,
    private val shiftRegistrationDao: ShiftRegistrationDao
) {

    fun getAllShifts(): Flow<List<ShiftEntity>> = shiftDao.getAllShifts()

    fun getShiftsByDate(workDate: String): Flow<List<ShiftEntity>> = shiftDao.getShiftsByDate(workDate)

    suspend fun getShiftById(id: Long): ShiftEntity? = shiftDao.getShiftById(id)

    suspend fun insertShift(shift: ShiftEntity): Long = shiftDao.insertShift(shift)

    suspend fun deleteShift(shift: ShiftEntity) = shiftDao.deleteShift(shift)

    suspend fun checkInShift(staffId: Long, shiftId: Long, date: String): Long {
        val attendance = AttendanceEntity(
            staffId = staffId,
            shiftId = shiftId,
            date = date,
            actualCheckIn = System.currentTimeMillis()
        )
        return attendanceDao.insertAttendance(attendance)
    }

    suspend fun updateAttendance(attendance: AttendanceEntity) {
        attendanceDao.updateAttendance(attendance)
    }

    suspend fun getActiveAttendance(staffId: Long): AttendanceEntity? {
        return attendanceDao.getActiveAttendance(staffId)
    }

    fun getAttendanceByStaff(staffId: Long): Flow<List<AttendanceWithDetails>> {
        return attendanceDao.getAttendanceByStaff(staffId)
    }

    fun getAllAttendanceWithDetails(): Flow<List<AttendanceWithDetails>> {
        return attendanceDao.getAllAttendanceWithDetails()
    }

    fun getTotalSalaryByStaff(staffId: Long): Flow<Double?> {
        return attendanceDao.getTotalSalaryByStaff(staffId)
    }

    suspend fun markSalaryAsPaid(staffId: Long) {
        attendanceDao.markSalaryAsPaid(staffId)
    }

    // --- Đăng ký ca làm ---
    suspend fun getRegistration(staffId: Long, shiftId: Long, date: String): ShiftRegistrationEntity? {
        return shiftRegistrationDao.getRegistration(staffId, shiftId, date)
    }

    suspend fun registerShift(staffId: Long, shiftId: Long, date: String): Long {
        val existing = shiftRegistrationDao.getRegistration(staffId, shiftId, date)
        if (existing != null) {
            return existing.id
        }
        val reg = ShiftRegistrationEntity(
            staffId = staffId,
            shiftId = shiftId,
            date = date
        )
        return shiftRegistrationDao.insertRegistration(reg)
    }

    suspend fun cancelRegistration(staffId: Long, shiftId: Long, date: String) {
        shiftRegistrationDao.deleteRegistrationByStaffAndShift(staffId, shiftId, date)
    }

    fun getRegistrationsByStaff(staffId: Long): Flow<List<ShiftRegistrationWithDetails>> {
        return shiftRegistrationDao.getRegistrationsByStaff(staffId)
    }

    fun getRegisteredShiftsForStaffOnDate(staffId: Long, date: String): Flow<List<ShiftRegistrationWithDetails>> {
        return shiftRegistrationDao.getRegisteredShiftsForStaffOnDate(staffId, date)
    }

    fun getCompletedShiftIdsForStaffOnDate(staffId: Long, date: String): Flow<List<Long>> {
        return attendanceDao.getCompletedShiftIdsForStaffOnDate(staffId, date)
    }
}
