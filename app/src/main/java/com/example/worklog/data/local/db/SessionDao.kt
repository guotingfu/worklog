package com.example.worklog.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsList(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): Session?
    
    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSession(): Flow<Session?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<Session>)

    @Update
    suspend fun update(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM sessions WHERE startTime < :threshold")
    suspend fun deleteSessionsBefore(threshold: Instant)
}
