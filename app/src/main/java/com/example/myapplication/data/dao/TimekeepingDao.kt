package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.Timekeeping
import kotlinx.coroutines.flow.Flow

@Dao
interface TimekeepingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimekeeping(timekeeping: Timekeeping): Long

    @Update
    suspend fun updateTimekeeping(timekeeping: Timekeeping)

    @Query("SELECT * FROM timekeeping WHERE userId = :userId AND checkOutTime IS NULL ORDER BY checkInTime DESC LIMIT 1")
    suspend fun getActiveCheckIn(userId: Long): Timekeeping?

    @Query("SELECT * FROM timekeeping WHERE userId = :userId ORDER BY checkInTime DESC")
    fun getTimekeepingByStaff(userId: Long): Flow<List<Timekeeping>>

    @Query("SELECT * FROM timekeeping ORDER BY checkInTime DESC")
    fun getAllTimekeeping(): Flow<List<Timekeeping>>
}
