package com.example.worklog.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Instant,
    val endTime: Instant?,
    val note: String? = null,
    // [新增] 标记该次打卡是否为法定节假日（用于强制计算全天加班）
    val isHoliday: Boolean = false
)