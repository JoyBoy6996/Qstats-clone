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
fun ThermalScreen(modifier: Modifier) {
    val t by Monitor.thermal.collectAsState()
    val status by ShizukuState.status.collectAsState()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenTitle("Thermal", "reads /sys/class/thermal/thermal_zone*, Shizuku shell as fallback")
        ModeBadge(status) {}
        Panel("Hottest zone history (C)") {
            LineChart(t.hottestHistory, yMin = 20f, yMax = 90f, height = 140)
        }
        Panel("Battery temperature history (C)") {
            LineChart(t.batteryHistory, yMin = 20f, yMax = 60f, height = 100)
        }
        Panel("Zones (${t.zones.size})") {
            if (t.zones.isEmpty()) Text("No thermal zones found on this device.")
            t.zones.forEach { z -> KV(z.name, "${fmt(z.tempC, " C")}  [${z.source}]") }
        }
        Text("Vendor nodes such as ttj or dynamic_tj are not read here: they are often SELinux-restricted.", modifier = Modifier.padding(top = 8.dp))
    }
}
