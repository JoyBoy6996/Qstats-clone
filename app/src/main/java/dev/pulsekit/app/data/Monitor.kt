package dev.pulsekit.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import dev.pulsekit.app.shizuku.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val device: String = "",
    val android: String = "",
    val soc: String = "",
    val batteryPct: Int = -1,
    val batteryTempC: Float? = null,
    val charging: Boolean = false,
    val cpuTotalPct: Float = 0f,
    val hottest: ThermalZone? = null
)

data class ThermalState(
    val zones: List<ThermalZone> = emptyList(),
    val hottestHistory: List<Float> = emptyList(),
    val batteryHistory: List<Float> = emptyList()
)

data class CpuState(
    val coreLoad: List<Float> = emptyList(),
    val freqs: List<CoreFreq> = emptyList(),
    val totalHistory: List<Float> = emptyList(),
    val totalPct: Float = 0f
)

data class FpsState(
    val mode: String = "",
    val target: String = "",
    val fps: Float? = null,
    val jank: Int = 0,
    val frameMs: Float? = null,
    val fpsHistory: List<Float> = emptyList(),
    val jankHistory: List<Float> = emptyList(),
    val frameHistory: List<Float> = emptyList(),
    val recording: Boolean = false,
    val recordSeconds: Long = 0,
    val note: String = ""
)

/**
 * One sampler for the whole app. Ticks every second and publishes StateFlows the screens collect.
 * Charts use a fixed history window.
 */
object Monitor {
    private const val HISTORY = 120
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var appContext: Context? = null

    private val _home = MutableStateFlow(HomeState())
    private val _thermal = MutableStateFlow(ThermalState())
    private val _cpu = MutableStateFlow(CpuState())
    private val _fps = MutableStateFlow(FpsState())
    val home: StateFlow<HomeState> = _home.asStateFlow()
    val thermal: StateFlow<ThermalState> = _thermal.asStateFlow()
    val cpu: StateFlow<CpuState> = _cpu.asStateFlow()
    val fps: StateFlow<FpsState> = _fps.asStateFlow()

    private val hottestHist = ArrayDeque<Float>()
    private val batteryHist = ArrayDeque<Float>()
    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val jankHist = ArrayDeque<Float>()
    private val frameHist = ArrayDeque<Float>()

    private var prevTicks: List<Pair<Long, Long>>? = null
    private var prevGfx: GfxCounters? = null
    private var lastTickAt = 0L
    private var tickCount = 0
    private var target = ""          // empty means this app (FrameMetrics)
    private var recordSeconds = 0L
    private var pendingNote = ""

    fun start(context: Context) {
        if (job?.isActive == true) return
        appContext = context.applicationContext
        _home.value = HomeState(
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            soc = if (Build.VERSION.SDK_INT >= 31 && Build.SOC_MODEL.isNotBlank() && Build.SOC_MODEL != "unknown")
                Build.SOC_MODEL else "hardware: ${Build.HARDWARE}"
        )
        prevTicks = SysReader.procStat()
        lastTickAt = SystemClock.elapsedRealtime()
        job = scope.launch {
            while (true) {
                delay(1000)
                try { tick() } catch (e: Throwable) { pendingNote = "sampler error: ${e.javaClass.simpleName}" }
            }
        }
    }

    /** Sets the fps target. Blank means this app. Any other package uses Shizuku dumpsys gfxinfo. */
    fun setTarget(pkg: String) {
        target = pkg.trim()
        prevGfx = null
        fpsHist.clear(); jankHist.clear(); frameHist.clear()
    }

    fun startRecording() {
        val own = appContext?.packageName ?: ""
        FpsRecorder.start(target.ifEmpty { own }, _fps.value.mode)
        recordSeconds = 0
        pendingNote = ""
    }

    fun stopRecording() {
        val ctx = appContext ?: return
        scope.launch {
            val id = FpsRecorder.finish(ctx, "manual")
            pendingNote = if (id != null) "Saved session #$id" else "Recording too short to save (under 10 s)."
        }
    }

    private suspend fun tick() {
        val now = SystemClock.elapsedRealtime()
        val dtMs = (now - lastTickAt).coerceAtLeast(1)
        val dt = dtMs / 1000f
        lastTickAt = now
        tickCount++

        // CPU: /proc/stat deltas per core
        val ticks = SysReader.procStat()
        var coreLoad = _cpu.value.coreLoad
        var total = _cpu.value.totalPct
        val prev = prevTicks
        if (ticks != null && prev != null && prev.size == ticks.size) {
            val pct = ticks.indices.map { i ->
                val dTotal = ticks[i].second - prev[i].second
                val dIdle = ticks[i].first - prev[i].first
                if (dTotal <= 0) 0f else (100f * (dTotal - dIdle) / dTotal).coerceIn(0f, 100f)
            }
            total = pct[0]
            coreLoad = pct.drop(1)
        }
        if (ticks != null) prevTicks = ticks
        push(cpuHist, total)
        val freqs = coreLoad.indices.map { SysReader.coreFreq(it) }
        _cpu.value = CpuState(coreLoad, freqs, cpuHist.toList(), total)

        // Thermal: sysfs, or Shizuku shell fallback
        val zones = SysReader.thermalZones()
        val hottest = zones.filter { it.tempC != null }.maxByOrNull { it.tempC!! }
        hottest?.tempC?.let { push(hottestHist, it) }
        val battery = readBattery()
        battery.tempC?.let { push(batteryHist, it) }
        _thermal.value = ThermalState(zones, hottestHist.toList(), batteryHist.toList())
        _home.value = _home.value.copy(
            batteryPct = battery.pct,
            batteryTempC = battery.tempC,
            charging = battery.charging,
            cpuTotalPct = total,
            hottest = hottest
        )

        // FPS
        val ownPkg = appContext?.packageName ?: ""
        val crossApp = target.isNotEmpty() && target != ownPkg
        val batch = FrameStatsHub.drain()
        var fpsVal: Float? = null
        var jankVal = 0
        var frameMs: Float? = null
        val mode: String
        if (!crossApp) {
            mode = "own app (FrameMetrics)"
            if (batch.frames > 0) {
                fpsVal = batch.frames / dt
                jankVal = batch.jank
                frameMs = batch.avgFrameMs
            }
        } else if (ShizukuState.isGranted()) {
            mode = "cross-app (Shizuku, dumpsys gfxinfo)"
            val cur = GfxDumpsys.read(target)
            val last = prevGfx
            if (cur != null && last != null && last.pkg == cur.pkg && cur.total >= last.total) {
                val d = cur.total - last.total
                jankVal = (cur.janky - last.janky).coerceAtLeast(0).toInt()
                fpsVal = d / dt
                if (d > 0) frameMs = dt * 1000f / d
            }
            if (cur != null) prevGfx = cur
        } else {
            mode = "cross-app unavailable, needs Shizuku"
        }
        push(fpsHist, fpsVal ?: 0f)
        push(jankHist, jankVal.toFloat())
        frameMs?.let { push(frameHist, it) }

        // Recording
        var recording = FpsRecorder.isRecording
        if (recording) {
            recordSeconds += dtMs / 1000
            val stop = FpsRecorder.onTick(fpsVal, jankVal, frameMs, dtMs)
            if (stop != null) {
                appContext?.let { ctx ->
                    val id = FpsRecorder.finish(ctx, stop)
                    pendingNote = if (id != null) "Recording stopped ($stop). Saved session #$id"
                    else "Recording stopped ($stop). Too short to save."
                }
                recording = false
            }
        } else {
            recordSeconds = 0
        }

        val note = pendingNote
        _fps.value = FpsState(
            mode = mode,
            target = if (crossApp) target else ownPkg,
            fps = fpsVal,
            jank = jankVal,
            frameMs = frameMs,
            fpsHistory = fpsHist.toList(),
            jankHistory = jankHist.toList(),
            frameHistory = frameHist.toList(),
            recording = recording,
            recordSeconds = recordSeconds,
            note = note
        )
    }

    private data class Battery(val pct: Int, val tempC: Float?, val charging: Boolean)

    private fun readBattery(): Battery {
        val ctx = appContext ?: return Battery(-1, null, false)
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return Battery(-1, null, false)
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return Battery(
            pct = if (level < 0) -1 else level * 100 / scale,
            tempC = if (temp == Int.MIN_VALUE) null else temp / 10f,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        )
    }

    private fun push(q: ArrayDeque<Float>, v: Float) {
        q.addLast(v)
        while (q.size > HISTORY) q.removeFirst()
    }
}
