package com.example.myapplication

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.entity.Order
import com.example.myapplication.data.entity.OrderItem
import com.example.myapplication.data.entity.RestaurantTable
import com.example.myapplication.data.model.OrderItemChange
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderEditingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private suspend fun fixture(db: AppDatabase): Pair<Long, Long> {
        val table = db.tableDao().insertTable(RestaurantTable(tableName = "Bàn 1", qrCodeData = "test"))
        val order = db.orderDao().createWithItems(
            Order(tableId = table, tableName = "Bàn 1", totalAmount = 0.0, finalAmount = 0.0),
            listOf(OrderItem(orderId = 0, menuItemId = 1, menuItemName = "Phở", price = 30000.0, quantity = 2),
                OrderItem(orderId = 0, menuItemId = 2, menuItemName = "Trà", price = 10000.0, quantity = 1)))
        return table to order
    }
    private suspend fun expectBlocked(block: suspend () -> Unit) {
        var blocked = false
        try { block() } catch (_: IllegalStateException) { blocked = true }
        catch (_: IllegalArgumentException) { blocked = true }
        assertTrue("Mutation should have been blocked", blocked)
    }

    @Test fun customerBatchUpdatesQuantityAndCancelsWithReactiveTotals() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val (table, id) = fixture(db)
            val dao = db.orderDao()
            val original = dao.getOrderWithItemsByIdSync(id)!!
            assertEquals(70000.0, original.order.totalAmount, 0.001)
            coroutineScope {
                val observed = async {
                    withTimeout(5000) { dao.getOrdersByTable(table).first { it.first().order.revision == 1L } }
                }
                dao.editItems(id, table, false, 0, listOf(
                    OrderItemChange(original.items[0].id, 3), OrderItemChange(original.items[1].id, 0)))
                val changed = observed.await().single()
                assertEquals(90000.0, changed.order.totalAmount, 0.001)
                assertEquals(90000.0, changed.order.finalAmount, 0.001)
                assertEquals("CANCELLED", changed.items[1].status)
                assertEquals(2, changed.items.size) // Preserve cancellation history.
            }
        } finally { db.close() }
    }

    @Test fun confirmationLocksCustomerButStaffMayEditAndCancelWithReason() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val (table, id) = fixture(db)
            val dao = db.orderDao()
            val original = dao.getOrderWithItemsByIdSync(id)!!
            assertTrue(dao.transitionStatus(id, 0, "CONFIRMED"))
            expectBlocked { dao.editItems(id, table, false, 1,
                listOf(OrderItemChange(original.items[0].id, 3))) }
            dao.editItems(id, table, true, 1,
                listOf(OrderItemChange(original.items[0].id, 0, "Khách đổi ý")))
            val updated = dao.getOrderWithItemsByIdSync(id)!!
            assertEquals(10000.0, updated.order.totalAmount, 0.001)
            assertEquals("Khách đổi ý", updated.items[0].cancellationReason)
            assertEquals("CONFIRMED", updated.items[1].status)
            assertTrue(dao.payOrder(id, "CASH", null, 0.0, 10000.0))
            expectBlocked { dao.editItems(id, table, true, 3,
                listOf(OrderItemChange(original.items[1].id, 2))) }
            assertEquals("COMPLETED", dao.getOrderById(id)!!.status)
        } finally { db.close() }
    }

    @Test fun invalidBatchRollsBackAndStaleRevisionOrWrongTableCannotEdit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val (table, id) = fixture(db)
            val dao = db.orderDao()
            val item = dao.getOrderWithItemsByIdSync(id)!!.items[0]
            expectBlocked { dao.editItems(id, table, false, 0,
                listOf(OrderItemChange(item.id, 4), OrderItemChange(999999, 1))) }
            assertEquals(70000.0, dao.getOrderById(id)!!.totalAmount, 0.001)
            expectBlocked { dao.editItems(id, table + 1, false, 0, listOf(OrderItemChange(item.id, 3))) }
            dao.editItems(id, table, false, 0, listOf(OrderItemChange(item.id, 3)))
            expectBlocked { dao.editItems(id, table, false, 0, listOf(OrderItemChange(item.id, 4))) }
            assertFalse(dao.payOrder(id, "CASH", null, 0.0, 70000.0))
            assertEquals(100000.0, dao.getOrderById(id)!!.totalAmount, 0.001)
        } finally { db.close() }
    }

    @Test fun cancellingLastItemsFreesTableOnlyWhenNoOtherActiveOrdersRemain() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val (table, firstId) = fixture(db)
            val dao = db.orderDao()
            val secondId = dao.createWithItems(
                Order(tableId = table, tableName = "Bàn 1", totalAmount = 10000.0, finalAmount = 10000.0),
                listOf(OrderItem(orderId = 0, menuItemId = 2, menuItemName = "Trà", price = 10000.0, quantity = 1)))
            val first = dao.getOrderWithItemsByIdSync(firstId)!!
            dao.editItems(firstId, table, false, 0, first.items.map { OrderItemChange(it.id, 0) })
            assertEquals("CANCELLED", dao.getOrderById(firstId)!!.status)
            assertEquals(0.0, dao.getOrderById(firstId)!!.totalAmount, 0.001)
            assertEquals("OCCUPIED", db.tableDao().getTableById(table)!!.status)
            assertTrue(dao.payOrder(secondId, "CASH", null, 0.0, 10000.0))
            assertEquals("EMPTY", db.tableDao().getTableById(table)!!.status)
        } finally { db.close() }
    }

    @Test fun migrationPreservesOrdersAndInfersItemStatus() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY, status TEXT NOT NULL, totalAmount REAL)")
                    db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY, orderId INTEGER, quantity INTEGER)")
                    db.execSQL("INSERT INTO orders VALUES (1, 'PENDING', 30000.0), (2, 'COMPLETED', 60000.0), (3, 'CANCELLED', 0.0)")
                    db.execSQL("INSERT INTO order_items VALUES (1, 1, 1), (2, 2, 2), (3, 3, 1)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            AppDatabase.MIGRATION_10_11.migrate(helper.writableDatabase)
            helper.writableDatabase.query("SELECT status FROM order_items ORDER BY id").use {
                it.moveToNext(); assertEquals("PENDING", it.getString(0))
                it.moveToNext(); assertEquals("CONFIRMED", it.getString(0))
                it.moveToNext(); assertEquals("CANCELLED", it.getString(0))
            }
            helper.writableDatabase.query("SELECT SUM(totalAmount), SUM(revision) FROM orders").use {
                it.moveToFirst()
                assertEquals(90000.0, it.getDouble(0), 0.001)
                assertEquals(0, it.getInt(1))
            }
        }
    }
}
