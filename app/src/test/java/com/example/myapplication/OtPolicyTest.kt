package com.example.myapplication

import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.model.OtPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class OtPolicyTest {
    @Test fun checkoutMustBeStrictlyMoreThanFifteenMinutesLate() {
        assertEquals("NONE", OtPolicy.status(0, 15 * 60000L))
        assertEquals("PENDING", OtPolicy.status(0, 15 * 60000L + 1))
        assertEquals("NONE", OtPolicy.status(10000, 0))
    }
    @Test fun minutesAreWholeActualMinutesAfterScheduledEnd() {
        assertEquals(30L, OtPolicy.actualMinutes(0, 30 * 60000L + 59000))
        assertEquals(0L, OtPolicy.actualMinutes(100000, 0))
    }
    @Test fun overnightEndBelongsToNextDay() {
        val shift = ShiftEntity(shiftName = "Đêm", workDate = "2026-10-29",
            expectedStartTime = "20:00", expectedEndTime = "04:00", isOvernight = true)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        assertEquals(format.parse("2026-10-30 04:00")!!.time, OtPolicy.expectedEnd(shift, "2026-10-29"))
    }
}
