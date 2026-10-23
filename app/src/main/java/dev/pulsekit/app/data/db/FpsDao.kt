package dev.pulsekit.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FpsDao {
    @Insert
    suspend fun insertSession(session: FpsSessionEntity): Long

    @Insert
    suspend fun insertSamples(samples: List<FpsSampleEntity>)

    @Query("SELECT * FROM fps_sessions ORDER BY startedAt DESC")
    fun sessions(): Flow<List<FpsSessionEntity>>

    @Query("SELECT * FROM fps_sessions WHERE id = :id")
    suspend fun session(id: Long): FpsSessionEntity?

    @Query("SELECT * FROM fps_samples WHERE sessionId = :id ORDER BY tMs")
    suspend fun samples(id: Long): List<FpsSampleEntity>

    @Query("DELETE FROM fps_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
