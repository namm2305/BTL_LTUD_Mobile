package com.example.myapplication

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalaryMigrationTest {
    @Test fun migrationRestoresNegativeUnpaidSalaryAndPreservesPaidAmounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""CREATE TABLE shifts (
                        id INTEGER PRIMARY KEY, expectedStartTime TEXT, expectedEndTime TEXT,
                        hourlyRate REAL, isOvernight INTEGER)""")
                    db.execSQL("""CREATE TABLE attendance (
                        id INTEGER PRIMARY KEY, shiftId INTEGER, actualCheckIn INTEGER,
                        actualCheckOut INTEGER, earlyPenalty REAL, overtimePay REAL,
                        totalSalary REAL, isPaid INTEGER)""")
                    db.execSQL("INSERT INTO shifts VALUES (1, '20:00', '04:00', 30000.0, 1)")
                    // Thirty minutes worked: the old implementation clamped -35,000 to zero.
                    db.execSQL("INSERT INTO attendance VALUES (1, 1, 0, 1800000, 50000.0, 0.0, 0.0, 0)")
                    db.execSQL("INSERT INTO attendance VALUES (2, 1, 0, 1800000, 50000.0, 0.0, 0.0, 1)")
                    db.execSQL("INSERT INTO attendance VALUES (3, 1, 0, NULL, 50000.0, 0.0, 0.0, 0)")
                    db.execSQL("INSERT INTO attendance VALUES (4, 1, 0, 32400000, 0.0, 45000.0, 285000.0, 0)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            val db = helper.writableDatabase
            AppDatabase.MIGRATION_8_9.migrate(db)
            db.query("SELECT baseSalary, totalSalary, earlyPenalty FROM attendance ORDER BY id").use { rows ->
                rows.moveToNext()
                assertEquals(15000.0, rows.getDouble(0), 0.001)
                assertEquals(-35000.0, rows.getDouble(1), 0.001)
                rows.moveToNext()
                assertEquals(0.0, rows.getDouble(1), 0.001) // Paid amount unchanged.
                rows.moveToNext()
                assertEquals(0.0, rows.getDouble(2), 0.001) // Active shift has no assessed fine.
                rows.moveToNext()
                assertEquals(240000.0, rows.getDouble(0), 0.001) // Eight-hour overnight shift.
                assertEquals(285000.0, rows.getDouble(1), 0.001)
                assertEquals(4, rows.count)
            }
        }
    }
}
