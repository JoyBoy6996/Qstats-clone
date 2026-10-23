package dev.pulsekit.app.data

import dev.pulsekit.app.shizuku.ShellRunner
import dev.pulsekit.app.shizuku.ShizukuState
import java.io.File

data class ThermalZone(val name: String, val tempC: Float?, val source: String)
data class CoreFreq(val curMhz: Long?, val minMhz: Long?, val maxMhz: Long?, val online: Boolean)

/** Direct sysfs and procfs reads. Every read returns null on failure instead of throwing. */
object SysReader {

    private fun readOrNull(path: String): String? =
        try { File(path).readText().trim() } catch (e: Exception) { null }

    /** /sys/class/thermal/thermal_zone*: direct read, then a Shizuku shell batch read if nothing is readable. */
    fun thermalZones(): List<ThermalZone> {
        val dirs = File("/sys/class/thermal").listFiles()
            ?.filter { it.name.startsWith("thermal_zone") }?.sortedBy { it.name }.orEmpty()
        val direct = dirs.map { z ->
            val t = readOrNull("${z.path}/temp")?.toIntOrNull()
            ThermalZone(readOrNull("${z.path}/type") ?: z.name, t?.let { it / 1000f }, "sysfs")
        }
        if (direct.any { it.tempC != null } || !ShizukuState.isGranted()) return direct
        val out = ShellRunner.run(
            "for z in /sys/class/thermal/thermal_zone*; do " +
                "echo \"\$(basename \$z)|\$(cat \$z/type 2>/dev/null)|\$(cat \$z/temp 2>/dev/null)\"; done"
        ) ?: return direct
        return out.lines().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 3) return@mapNotNull null
            val t = p[2].trim().toIntOrNull()
            ThermalZone(p[1].trim().ifEmpty { p[0] }, t?.let { it / 1000f }, "shizuku shell")
        }
    }

    /** Index 0 is the aggregate "cpu" line, then one entry per core. Pairs are (idle, total) jiffies. */
    fun procStat(): List<Pair<Long, Long>>? {
        val lines = try { File("/proc/stat").readLines() } catch (e: Exception) { return null }
        return lines.filter { it.startsWith("cpu") }.map { line ->
            val p = line.trim().split(Regex("\\s+")).drop(1).map { it.toLongOrNull() ?: 0L }
            val idle = p.getOrElse(3) { 0L } + p.getOrElse(4) { 0L }
            idle to p.sum()
        }
    }

    fun coreFreq(index: Int): CoreFreq {
        val base = "/sys/devices/system/cpu/cpu$index"
        val online = readOrNull("$base/online")?.let { it == "1" } ?: true
        fun mhz(name: String): Long? = readOrNull("$base/cpufreq/$name")?.toLongOrNull()?.div(1000)
        return CoreFreq(mhz("scaling_cur_freq"), mhz("scaling_min_freq"), mhz("scaling_max_freq"), online)
    }

    fun coreCount(): Int = procStat()?.size?.minus(1) ?: Runtime.getRuntime().availableProcessors()
}
