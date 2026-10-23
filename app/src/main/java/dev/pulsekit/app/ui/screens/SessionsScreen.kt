package dev.pulsekit.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.pulsekit.app.data.db.FpsSessionEntity
import dev.pulsekit.app.data.db.PulseDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val dao = remember { PulseDatabase.get(ctx).fps() }
    val sessions by dao.sessions().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FpsSessionEntity?>(null) }

    val chosen = selected
    if (chosen != null) {
        Column(modifier.fillMaxSize().padding(16.dp)) {
            ScreenTitle("Session #${chosen.id}", fmtTime(chosen.startedAt))
            Panel("Summary") {
                KV("Target", chosen.target)
                KV("Mode", chosen.mode)
                KV("Average fps", "%.1f".format(chosen.avgFps))
                KV("Minimum fps", "%.1f".format(chosen.minFps))
                KV("5th percentile fps", "%.1f".format(chosen.p5Fps))
                KV("Jank frames", chosen.jankTotal.toString())
                KV("Samples", chosen.sampleCount.toString())
            }
            Button(onClick = {
                scope.launch {
                    val samples = dao.samples(chosen.id)
                    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                    val file = File(dir, "pulsekit_session_${chosen.id}.csv")
                    file.bufferedWriter().use { w ->
                        w.write("t_ms,fps,jank,frame_ms\n")
                        samples.forEach { w.write("${it.tMs},${it.fps},${it.jank},${it.frameMs ?: ""}\n") }
                    }
                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(send, "Export CSV"))
                }
            }) { Text("Export CSV") }
            Button(onClick = { selected = null }, modifier = Modifier.padding(top = 8.dp)) { Text("Back") }
        }
        return
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        ScreenTitle("Sessions", "recorded fps sessions stored on this device")
        if (message.isNotBlank()) Text(message)
        if (sessions.isEmpty()) Text("No sessions yet. Start a recording on the FPS tab.")
        LazyColumn {
            items(sessions) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = s },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#${s.id}  ${fmtTime(s.startedAt)}", style = MaterialTheme.typography.titleSmall)
                        Text("avg %.1f fps, min %.1f, jank %d".format(s.avgFps, s.minFps, s.jankTotal))
                        Text(s.target, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun fmtTime(ms: Long): String =
    SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ms))
