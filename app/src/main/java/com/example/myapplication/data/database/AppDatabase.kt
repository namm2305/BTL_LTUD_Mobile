package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.myapplication.data.model.ShiftSalaryDetails
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        MenuItem::class,
        RestaurantTable::class,
        Discount::class,
        Order::class,
        OrderItem::class,
        Timekeeping::class,
        ShiftEntity::class,
        AttendanceEntity::class,
        ShiftRegistrationEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun tableDao(): TableDao
    abstract fun discountDao(): DiscountDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun timekeepingDao(): TimekeepingDao
    abstract fun shiftDao(): ShiftDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftRegistrationDao(): ShiftRegistrationDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance ADD COLUMN baseSalary REAL NOT NULL DEFAULT 0.0")
                // Backfill salary components without deleting attendance or other app data.
                db.query("""
                    SELECT a.id, a.actualCheckIn, a.actualCheckOut, a.earlyPenalty,
                           a.overtimePay, a.totalSalary, a.isPaid,
                           s.expectedStartTime, s.expectedEndTime, s.hourlyRate, s.isOvernight
                    FROM attendance a LEFT JOIN shifts s ON s.id = a.shiftId
                """.trimIndent()).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        if (cursor.isNull(2)) {
                            db.execSQL("UPDATE attendance SET earlyPenalty = 0.0 WHERE id = ?", arrayOf(id))
                            continue
                        }
                        val fine = cursor.getDouble(3)
                        val overtime = cursor.getDouble(4)
                        // Preserve already paid amounts; recover the original base for unpaid rows.
                        val base = if (cursor.getInt(6) != 0 || cursor.isNull(7)) {
                            cursor.getDouble(5) + fine - overtime
                        } else {
                            fun hours(value: String): Double {
                                val parts = value.split(":")
                                return parts[0].toDouble() + parts[1].toDouble() / 60.0
                            }
                            val start = hours(cursor.getString(7))
                            var end = hours(cursor.getString(8))
                            if (cursor.getInt(10) != 0 || end <= start) end += 24.0
                            val worked = ((cursor.getLong(2) - cursor.getLong(1)) / 3_600_000.0)
                                .coerceAtLeast(0.0)
                            minOf(worked, end - start) * cursor.getDouble(9)
                        }
                        val details = ShiftSalaryDetails(base, overtime, fine)
                        db.execSQL(
                            "UPDATE attendance SET baseSalary = ?, totalSalary = ? WHERE id = ?",
                            arrayOf<Any>(details.baseSalary, details.totalSalary, id)
                        )
                    }
                }
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance ADD COLUMN otMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attendance ADD COLUMN otStatus TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE attendance ADD COLUMN approvedOtMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attendance ADD COLUMN otRatePerHour REAL NOT NULL DEFAULT 0.0")
                // Preserve historical OT and salary; approval applies to new check-outs.
                db.execSQL("""
                    UPDATE attendance SET
                    otRatePerHour = COALESCE((SELECT hourlyRate * overtimeMultiplier
                        FROM shifts WHERE shifts.id = attendance.shiftId), 0.0)
                """.trimIndent())
                db.execSQL("""
                    UPDATE attendance SET otStatus = 'APPROVED',
                    otMinutes = CAST(ROUND(overtimePay * 60.0 / otRatePerHour) AS INTEGER),
                    approvedOtMinutes = CAST(ROUND(overtimePay * 60.0 / otRatePerHour) AS INTEGER)
                    WHERE overtimePay > 0.0 AND otRatePerHour > 0.0 AND actualCheckOut IS NOT NULL
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_database"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Thêm dữ liệu mẫu ban đầu
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            // 1. Tạo tài khoản mặc định (Chủ nhà hàng: admin/1234, Nhân viên: nv1/123)
            val userDao = db.userDao()
            userDao.insertUser(
                User(username = "admin", password = "1234", fullName = "Chủ Nhà Hàng", role = "OWNER")
            )
            userDao.insertUser(
                User(username = "nv1", password = "123", fullName = "Nhân Viên A", role = "STAFF")
            )

            // 2. Tạo các ca làm việc mặc định kèm ngày làm việc
            val shiftDao = db.shiftDao()
            shiftDao.insertShift(
                ShiftEntity(shiftName = "Ca Sáng", workDate = "2025-05-20", expectedStartTime = "07:00", expectedEndTime = "15:00", hourlyRate = 30000.0, overtimeMultiplier = 1.5)
            )
            shiftDao.insertShift(
                ShiftEntity(shiftName = "Ca Chiều", workDate = "2025-05-20", expectedStartTime = "15:00", expectedEndTime = "23:00", hourlyRate = 35000.0, overtimeMultiplier = 1.5)
            )

            // 3. Tạo món ăn mặc định
            val menuItemDao = db.menuItemDao()
            menuItemDao.insertMenuItem(MenuItem(name = "Phở Bò Đặc Biệt", price = 65000.0, category = "Món chính"))
            menuItemDao.insertMenuItem(MenuItem(name = "Bún Chả Hà Nội", price = 55000.0, category = "Món chính"))
            menuItemDao.insertMenuItem(MenuItem(name = "Cơm Tấm Sườn Bì Chả", price = 50000.0, category = "Món chính"))
            menuItemDao.insertMenuItem(MenuItem(name = "Cà Phê Sữa Đá", price = 28000.0, category = "Đồ uống"))
            menuItemDao.insertMenuItem(MenuItem(name = "Trà Cam Sả", price = 35000.0, category = "Đồ uống"))
            menuItemDao.insertMenuItem(MenuItem(name = "Chè Bưởi An Giang", price = 25000.0, category = "Tráng miệng"))

            // 4. Tạo bàn mặc định
            val tableDao = db.tableDao()
            tableDao.insertTable(RestaurantTable(tableName = "Bàn 01", qrCodeData = "TABLE_1"))
            tableDao.insertTable(RestaurantTable(tableName = "Bàn 02", qrCodeData = "TABLE_2"))
            tableDao.insertTable(RestaurantTable(tableName = "Bàn 03", qrCodeData = "TABLE_3"))
            tableDao.insertTable(RestaurantTable(tableName = "Bàn 04", qrCodeData = "TABLE_4"))
            tableDao.insertTable(RestaurantTable(tableName = "Bàn 05", qrCodeData = "TABLE_5"))

            // 5. Tạo mã giảm giá mặc định
            val discountDao = db.discountDao()
            discountDao.insertDiscount(Discount(code = "WELCOME10", discountPercent = 10.0, minOrderAmount = 50000.0))
            discountDao.insertDiscount(Discount(code = "KM20", discountPercent = 20.0, minOrderAmount = 100000.0))
        }
    }
}
