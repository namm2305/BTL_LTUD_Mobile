package com.example.myapplication.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.myapplication.data.entity.AttendanceEntity
import com.example.myapplication.data.entity.ShiftEntity
import com.example.myapplication.data.entity.User

data class AttendanceWithDetails(
    @Embedded
    val attendance: AttendanceEntity,

    @Relation(
        parentColumn = "staffId",
        entityColumn = "id"
    )
    val staff: User?,

    @Relation(
        parentColumn = "shiftId",
        entityColumn = "id"
    )
    val shift: ShiftEntity?
)
