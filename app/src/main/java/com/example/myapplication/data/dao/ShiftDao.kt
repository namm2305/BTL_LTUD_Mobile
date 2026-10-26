package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY id ASC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE workDate = :workDate OR workDate = '' ORDER BY expectedStartTime ASC")
    fun getShiftsByDate(workDate: String): Flow<List<ShiftEntity>>
}
