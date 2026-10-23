package dev.pulsekit.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pulsekit.app.data.Monitor
import dev.pulsekit.app.shizuku.ShizukuState
import dev.pulsekit.app.ui.LineChart
import dev.pulsekit.app.CrashLog

@Composable
fun HomeScreen(modifier: Modifier) {
    val h by Monitor.home.collectAsState()
    val cpu by Monitor.cpu.collectAsState()
    val status by ShizukuState.status.collectAsState()
    val ctx = LocalContext.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenTitle("PulseKit", "live system summary")
        ModeBadge(status) { ShizukuState.requestPermission()?.let { android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_LONG).show() } }
        var crash by remember { mutableStateOf(CrashLog.read(ctx)) }
        if (crash != null) {
            Panel("Last crash (tap to clear)") {
                Text(crash ?: "", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text("tap here to clear", modifier = Modifier.clickable { CrashLog.clear(ctx); crash = null })
            }
        }
        Panel("Device") {
            KV("Model", h.device.ifBlank { "n/a" })
            KV("Android", h.android.ifBlank { "n/a" })
            KV("SoC", h.soc.ifBlank { "n/a" })
        }
        Panel("Battery") {
            KV("Level", if (h.batteryPct < 0) "n/a" else "${h.batteryPct}%")
            KV("Temperature", fmt(h.batteryTempC, " C"))
            KV("Charging", if (h.charging) "yes" else "no")
        }
        Panel("CPU and thermal") {
            KV("CPU total", fmt(cpu.totalPct, "%"))
            KV("Hottest zone", h.hottest?.let { "${it.name} ${fmt(it.tempC, " C")}" } ?: "n/a (no readable zone)")
            Column { LineChart(cpu.totalHistory, yMax = 100f, height = 80) }
        }
        Text("Monitors update once per second.", modifier = Modifier.padding(top = 8.dp))
    }
}
