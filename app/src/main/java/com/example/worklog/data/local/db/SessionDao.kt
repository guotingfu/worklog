package com.example.worklog.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<Session?>
    
    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE startTime >= :start AND endTime <= :end ORDER BY startTime DESC")
    fun getSessionsBetween(start: Instant, end: Instant): Flow<List<Session>>
    
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
