package dev.pulsekit.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pulsekit.app.data.Monitor
import dev.pulsekit.app.shizuku.ShizukuState
import dev.pulsekit.app.ui.LineChart

@Composable
fun FpsScreen(modifier: Modifier) {
    val s by Monitor.fps.collectAsState()
    val status by ShizukuState.status.collectAsState()
    var pkg by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenTitle("FPS", "own app via FrameMetrics; other apps via Shizuku dumpsys gfxinfo")
        ModeBadge(status) {}
        Panel("Target") {
            Text("Now measuring: ${s.target.ifBlank { "this app" }}")
            Text("Mode: ${s.mode.ifBlank { "starting" }}")
            Spacer(Modifier.padding(4.dp))
            OutlinedTextField(
                value = pkg, onValueChange = { pkg = it }, singleLine = true,
                label = { Text("Package name (blank = this app)") }, modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(top = 6.dp)) {
                Button(onClick = { Monitor.setTarget(pkg) }) { Text("Set target") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { Monitor.setTarget("") }) { Text("This app") }
            }
        }
        Panel("Now") {
            KV("FPS", fmt(s.fps, "", 1))
            KV("Jank frames in last second", s.jank.toString())
            KV("Avg frame time", fmt(s.frameMs, " ms", 2))
        }
        Panel("FPS (last 2 min)") { LineChart(s.fpsHistory, yMax = 120f, height = 120) }
        Panel("Jank per second") { LineChart(s.jankHistory, yMax = 30f, height = 80) }
        Panel("Frame time (ms)") { LineChart(s.frameHistory, yMax = 50f, height = 80) }
        Panel("Recording") {
            if (s.recording) {
                KV("Elapsed", "${s.recordSeconds} s")
                Button(onClick = { Monitor.stopRecording() }) { Text("Stop and save") }
            } else {
                Button(onClick = { Monitor.startRecording() }) { Text("Start recording") }
            }
            Text("Stops on screen off, manual stop, or after 30 minutes. Sessions under 10 s are not saved.", modifier = Modifier.padding(top = 6.dp))
            if (s.note.isNotBlank()) Text(s.note, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
