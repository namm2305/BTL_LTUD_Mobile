package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.ShiftRegistrationEntity
import com.example.myapplication.data.model.ShiftRegistrationWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftRegistrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: ShiftRegistrationEntity): Long

    @Delete
    suspend fun deleteRegistration(registration: ShiftRegistrationEntity)

    @Query("SELECT * FROM shift_registrations WHERE staffId = :staffId AND shiftId = :shiftId AND date = :date LIMIT 1")
    suspend fun getRegistration(staffId: Long, shiftId: Long, date: String): ShiftRegistrationEntity?

    @Query("DELETE FROM shift_registrations WHERE staffId = :staffId AND shiftId = :shiftId AND date = :date")
    suspend fun deleteRegistrationByStaffAndShift(staffId: Long, shiftId: Long, date: String)

    @Transaction
    @Query("""
        SELECT * FROM shift_registrations 
        WHERE staffId = :staffId 
        AND id NOT IN (
            SELECT sr.id FROM shift_registrations sr
            INNER JOIN attendance a ON a.staffId = sr.staffId AND a.shiftId = sr.shiftId AND a.date = sr.date
            WHERE a.actualCheckOut IS NOT NULL
        )
        ORDER BY date DESC
    """)
    fun getRegistrationsByStaff(staffId: Long): Flow<List<ShiftRegistrationWithDetails>>

    @Transaction
    @Query("SELECT * FROM shift_registrations WHERE date = :date ORDER BY staffId ASC")
    fun getRegistrationsByDate(date: String): Flow<List<ShiftRegistrationWithDetails>>

    @Transaction
    @Query("SELECT * FROM shift_registrations WHERE staffId = :staffId AND date = :date")
    fun getRegisteredShiftsForStaffOnDate(staffId: Long, date: String): Flow<List<ShiftRegistrationWithDetails>>

    @Transaction
    @Query("SELECT * FROM shift_registrations ORDER BY date DESC")
    fun getAllRegistrations(): Flow<List<ShiftRegistrationWithDetails>>
}
