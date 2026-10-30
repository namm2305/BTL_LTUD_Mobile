package com.example.myapplication

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.entity.AttendanceEntity
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.entity.User
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OtApprovalTest {
    private inline fun <T> AppDatabase.withDatabase(block: (AppDatabase) -> T): T =
        try { block(this) } finally { close() }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun approvalAtomicallyRecalculatesSalaryAndNotifiesStaffFlow() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().withDatabase { db ->
            val staff = db.userDao().insertUser(User(username = "staff", password = "test",
                fullName = "Staff", role = "STAFF"))
            val shift = db.shiftDao().insertShift(ShiftEntity(shiftName = "Ca", workDate = "2026-10-29",
                expectedStartTime = "08:00", expectedEndTime = "16:00"))
            val dao = db.attendanceDao()
            val id = dao.insertAttendance(AttendanceEntity(staffId = staff, shiftId = shift,
                date = "2026-10-29", actualCheckOut = 100,
                baseSalary = 15000.0, fineAmount = 50000.0, totalSalary = -35000.0,
                otMinutes = 60, otStatus = "PENDING", otRatePerHour = 45000.0))
            assertEquals(1, dao.getPendingOt().first().size)
            assertEquals(0, dao.decideOt(id, "APPROVED", 61))
            assertEquals(0, dao.decideOt(id, "APPROVED", 0))
            coroutineScope {
                val observed = async {
                    withTimeout(5000) {
                        dao.getAttendanceByStaff(staff).first { rows ->
                            rows.any { it.attendance.otStatus == "APPROVED" }
                        }.single().attendance
                    }
                }
                assertEquals(1, dao.decideOt(id, "APPROVED", 30))
                val result = observed.await()
                assertEquals(30L, result.approvedOtMinutes)
                assertEquals(22500.0, result.overtimeSalary, 0.001)
                assertEquals(-12500.0, result.totalSalary, 0.001)
            }
            assertTrue(dao.getPendingOt().first().isEmpty())
            assertEquals(0, dao.decideOt(id, "REJECTED", 0)) // A stale second decision cannot overwrite it.
            assertEquals(-12500.0, dao.getAccumulatedSalaryByStaff(staff).first()!!, 0.001)
        }
    }

    @Test fun rejectionKeepsBaseAndFineAndPendingRowsCannotBePaid() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().withDatabase { db ->
            val staff = db.userDao().insertUser(User(username = "staff", password = "test",
                fullName = "Staff", role = "STAFF"))
            val shift = db.shiftDao().insertShift(ShiftEntity(shiftName = "Ca",
                expectedStartTime = "08:00", expectedEndTime = "16:00"))
            val dao = db.attendanceDao()
            val id = dao.insertAttendance(AttendanceEntity(staffId = staff, shiftId = shift,
                date = "2026-10-29", actualCheckOut = 100,
                baseSalary = 15000.0, fineAmount = 50000.0, totalSalary = -35000.0,
                otMinutes = 60, otStatus = "PENDING", otRatePerHour = 45000.0))
            assertNull(dao.getTotalSalaryByStaff(staff).first())
            dao.markSalaryAsPaid(staff)
            assertFalse(dao.getAttendanceByStaff(staff).first().single().attendance.isPaid)
            assertEquals(1, dao.decideOt(id, "REJECTED", 0))
            val result = dao.getAttendanceByStaff(staff).first().single().attendance
            assertEquals(-35000.0, dao.getTotalSalaryByStaff(staff).first()!!, 0.001)
            assertEquals("REJECTED", result.otStatus)
            assertEquals(0.0, result.overtimeSalary, 0.001)
            assertEquals(15000.0, result.baseSalary, 0.001)
            assertEquals(50000.0, result.fineAmount, 0.001)
            assertEquals(-35000.0, result.totalSalary, 0.001)
        }
    }

    @Test fun migrationKeepsHistoricalSalaryAndAddsOtDefaults() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE shifts (id INTEGER PRIMARY KEY, hourlyRate REAL, overtimeMultiplier REAL)")
                    db.execSQL("CREATE TABLE attendance (id INTEGER PRIMARY KEY, shiftId INTEGER, overtimePay REAL, totalSalary REAL, actualCheckOut INTEGER)")
                    db.execSQL("INSERT INTO shifts VALUES (1, 30000.0, 1.5)")
                    db.execSQL("INSERT INTO attendance VALUES (1, 1, 45000.0, 285000.0, 100)")
                    db.execSQL("INSERT INTO attendance VALUES (2, 1, 0.0, -35000.0, 100)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            val db = helper.writableDatabase
            AppDatabase.MIGRATION_9_10.migrate(db)
            db.query("SELECT otStatus, otMinutes, approvedOtMinutes, totalSalary FROM attendance ORDER BY id").use {
                it.moveToNext()
                assertEquals("APPROVED", it.getString(0))
                assertEquals(60L, it.getLong(1))
                assertEquals(60L, it.getLong(2))
                assertEquals(285000.0, it.getDouble(3), 0.001)
                it.moveToNext()
                assertEquals("NONE", it.getString(0))
                assertEquals(0L, it.getLong(1))
                assertEquals(-35000.0, it.getDouble(3), 0.001)
            }
        }
    }
}
