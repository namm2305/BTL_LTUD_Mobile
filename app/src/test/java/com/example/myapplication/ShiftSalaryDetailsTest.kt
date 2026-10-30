package com.example.myapplication

import com.example.myapplication.data.model.ShiftSalaryDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftSalaryDetailsTest {
    @Test fun penaltyCanExceedEarnings() {
        val result = ShiftSalaryDetails.calculate(0.5, 8.0, 30000.0, 1.5, 50000.0)
        assertEquals(15000.0, result.baseSalary, 0.001)
        assertEquals(-35000.0, result.totalSalary, 0.001)
    }

    @Test fun overtimeUsesMultiplierAfterStandardHours() {
        val result = ShiftSalaryDetails.calculate(10.0, 8.0, 30000.0, 1.5)
        assertEquals(240000.0, result.baseSalary, 0.001)
        assertEquals(90000.0, result.overtimeSalary, 0.001)
        assertEquals(330000.0, result.totalSalary, 0.001)
    }

    @Test fun noWorkWithPenaltyProducesNegativeSalary() {
        assertEquals(-50000.0,
            ShiftSalaryDetails.calculate(0.0, 8.0, 30000.0, 1.5, 50000.0).totalSalary, 0.001)
    }

    @Test fun cumulativeSalaryIncludesNegativeShifts() {
        val shifts = listOf(ShiftSalaryDetails(15000.0, fineAmount = 50000.0),
            ShiftSalaryDetails(240000.0))
        assertEquals(205000.0, shifts.sumOf { it.totalSalary }, 0.001)
    }

    @Test fun fineCanExactlyCancelEarnings() {
        assertEquals(0.0, ShiftSalaryDetails(30000.0, 20000.0, 50000.0).totalSalary, 0.001)
    }
}
