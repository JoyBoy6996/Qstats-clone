package dev.pulsekit.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pulsekit.app.data.Monitor
import dev.pulsekit.app.shizuku.ShizukuState
import dev.pulsekit.app.ui.LineChart

@Composable
fun CpuScreen(modifier: Modifier) {
    val c by Monitor.cpu.collectAsState()
    val status by ShizukuState.status.collectAsState()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenTitle("CPU", "per-core load from /proc/stat deltas, cpufreq from sysfs")
        ModeBadge(status) {}
        Panel("Total load history (%)") {
            LineChart(c.totalHistory, yMax = 100f, height = 120)
            KV("Now", fmt(c.totalPct, "%"))
        }
        Panel("Cores") {
            if (c.coreLoad.isEmpty()) Text("Waiting for first sample...")
            c.coreLoad.forEachIndexed { i, load ->
                val f = c.freqs.getOrNull(i)
                val freq = when {
                    f == null -> "n/a"
                    !f.online -> "offline"
                    else -> "${f.curMhz ?: "n/a"} MHz  (min ${f.minMhz ?: "n/a"}, max ${f.maxMhz ?: "n/a"})"
                }
                KV("Core $i  ${fmt(load, "%", 0)}", freq)
            }
        }
        Text("Governor and frequency writes are not included: this build is read-only.", modifier = Modifier.padding(top = 8.dp))
    }
}
