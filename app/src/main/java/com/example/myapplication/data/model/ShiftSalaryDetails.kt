package com.example.myapplication.data.model

data class ShiftSalaryDetails(
    val baseSalary: Double,
    val overtimeSalary: Double = 0.0,
    val fineAmount: Double = 0.0
) {
    val totalSalary: Double get() = baseSalary + overtimeSalary - fineAmount

    companion object {
        fun calculate(
            workedHours: Double,
            expectedHours: Double,
            hourlyRate: Double,
            overtimeMultiplier: Double,
            fineAmount: Double = 0.0
        ): ShiftSalaryDetails {
            require(workedHours.isFinite() && workedHours >= 0)
            require(expectedHours.isFinite() && expectedHours > 0)
            require(hourlyRate.isFinite() && hourlyRate >= 0)
            require(overtimeMultiplier.isFinite() && overtimeMultiplier >= 0)
            require(fineAmount.isFinite() && fineAmount >= 0)
            return ShiftSalaryDetails(
                baseSalary = minOf(workedHours, expectedHours) * hourlyRate,
                overtimeSalary = maxOf(0.0, workedHours - expectedHours) * hourlyRate * overtimeMultiplier,
                fineAmount = fineAmount
            )
        }
    }
}
