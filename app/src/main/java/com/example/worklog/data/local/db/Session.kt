package com.example.worklog.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Instant,
    val endTime: Instant? = null,
    val note: String? = null
)
