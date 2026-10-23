package dev.pulsekit.app.data

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import dev.pulsekit.app.shizuku.ShellRunner
import java.util.concurrent.ConcurrentLinkedQueue

data class FrameBatch(val frames: Int, val jank: Int, val avgFrameMs: Float?)
data class GfxCounters(val pkg: String, val total: Long, val janky: Long)

/** Own-app frames via FrameMetrics. Durations are queued on the main thread and drained each tick. */
object FrameStatsHub {
    private const val JANK_NS = 16_700_000L
    private val queue = ConcurrentLinkedQueue<Long>()

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        queue.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
    }

    fun attach(activity: Activity) =
        activity.window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper()))

    fun detach(activity: Activity) =
        activity.window.removeOnFrameMetricsAvailableListener(listener)

    fun drain(): FrameBatch {
        val durations = ArrayList<Long>()
        while (true) durations.add(queue.poll() ?: break)
        if (durations.isEmpty()) return FrameBatch(0, 0, null)
        val jank = durations.count { it > JANK_NS }
        return FrameBatch(durations.size, jank, durations.average().toFloat() / 1_000_000f)
    }
}

/**
 * Cross-app counters from `dumpsys gfxinfo <pkg>` run through Shizuku. Counters are cumulative,
 * so fps comes from deltas between ticks. This is dumpsys sampling, not a per-frame feed.
 */
object GfxDumpsys {
    private val PKG_OK = Regex("^[a-zA-Z0-9_.]+$")
    private val TOTAL = Regex("Total frames rendered:\\s*(\\d+)")
    private val JANKY = Regex("Janky frames:\\s*(\\d+)")
    private val RESUMED = Regex("([a-zA-Z][\\w]*(?:\\.[\\w]+)+)/")

    fun read(pkg: String): GfxCounters? {
        if (!PKG_OK.matches(pkg)) return null
        val out = ShellRunner.run("dumpsys gfxinfo $pkg") ?: return null
        val total = TOTAL.find(out)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val janky = JANKY.find(out)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return GfxCounters(pkg, total, janky)
    }

    fun foregroundPackage(): String? {
        val out = ShellRunner.run("dumpsys activity activities | grep mResumedActivity", 5000) ?: return null
        return RESUMED.find(out)?.groupValues?.get(1)
    }
}
