package dev.pulsekit.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "fps_sessions")
data class FpsSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val target: String,
    val mode: String,
    val avgFps: Double,
    val minFps: Double,
    val p5Fps: Double,
    val jankTotal: Int,
    val sampleCount: Int
)

@Entity(
    tableName = "fps_samples",
    foreignKeys = [ForeignKey(
        entity = FpsSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class FpsSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val tMs: Long,
    val fps: Float,
    val jank: Int,
    val frameMs: Float?
)
