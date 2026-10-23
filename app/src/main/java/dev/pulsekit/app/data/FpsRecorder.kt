package dev.pulsekit.app.data

import android.content.Context
import dev.pulsekit.app.data.db.FpsSampleEntity
import dev.pulsekit.app.data.db.FpsSessionEntity
import dev.pulsekit.app.data.db.PulseDatabase
import kotlin.math.roundToInt

data class FpsSample(val tMs: Long, val fps: Float, val jank: Int, val frameMs: Float?)

/**
 * Records fps samples. Monitor calls [onTick] once per second while recording.
 * Stops on: manual stop, screen off (requestStop from the receiver), or the 30 minute cap.
 * Sessions shorter than [MIN_SAVE_MS] are not saved.
 */
object FpsRecorder {
    const val MAX_MS = 30 * 60 * 1000L
    const val MIN_SAVE_MS = 10_000L

    @Volatile private var active = false
    @Volatile private var stopReason: String? = null
    private var startedAt = 0L
    private var elapsedMs = 0L
    private var target = ""
    private var mode = ""
    private val samples = mutableListOf<FpsSample>()

    val isRecording: Boolean get() = active

    @Synchronized
    fun start(target: String, mode: String) {
        if (active) return
        this.target = target
        this.mode = mode
        samples.clear()
        stopReason = null
        elapsedMs = 0
        startedAt = System.currentTimeMillis()
        active = true
    }

    fun requestStop(reason: String) {
        if (active) stopReason = reason
    }

    /** Adds one sample. Returns a stop reason when recording should end. */
    @Synchronized
    fun onTick(fps: Float?, jank: Int, frameMs: Float?, dtMs: Long): String? {
        if (!active) return null
        elapsedMs += dtMs
        if (fps != null) samples += FpsSample(elapsedMs, fps, jank, frameMs)
        return stopReason ?: if (elapsedMs >= MAX_MS) "30 minute limit" else null
    }

    /** Ends the session and saves it if it was long enough. Returns the session id, or null. */
    suspend fun finish(context: Context, reason: String): Long? {
        val snapshot: List<FpsSample>
        val duration: Long
        synchronized(this) {
            if (!active) return null
            active = false
            snapshot = samples.toList()
            duration = elapsedMs
            samples.clear()
        }
        if (duration < MIN_SAVE_MS) return null
        val measured = snapshot.map { it.fps.toDouble() }.sorted()
        if (measured.isEmpty()) return null
        val dao = PulseDatabase.get(context).fps()
        val id = dao.insertSession(
            FpsSessionEntity(
                startedAt = startedAt,
                endedAt = startedAt + duration,
                target = target,
                mode = "$mode, stopped: $reason",
                avgFps = measured.average(),
                minFps = measured.first(),
                p5Fps = measured[((measured.size - 1) * 0.05).roundToInt()],
                jankTotal = snapshot.sumOf { it.jank },
                sampleCount = snapshot.size
            )
        )
        dao.insertSamples(snapshot.map {
            FpsSampleEntity(sessionId = id, tMs = it.tMs, fps = it.fps, jank = it.jank, frameMs = it.frameMs)
        })
        return id
    }
}
