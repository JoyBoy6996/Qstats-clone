package dev.pulsekit.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pulsekit.app.shizuku.ShizukuState
import androidx.compose.foundation.clickable

@Composable
fun ScreenTitle(text: String, sub: String? = null) {
    Text(text, style = MaterialTheme.typography.headlineSmall)
    sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun Panel(title: String, content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(k, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v)
    }
}

fun fmt(v: Float?, unit: String = "", digits: Int = 1): String =
    if (v == null) "n/a" else "%.${digits}f%s".format(v, unit)

/** Shows which mode is live: Shizuku privileged or basic. Used on every screen. */
@Composable
fun ModeBadge(status: ShizukuState.Status, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp)) {
            Text(status.mode.label, style = MaterialTheme.typography.labelLarge)
            Text(
                if (status.mode == ShizukuState.Mode.PRIVILEGED) "tap to manage Shizuku"
                else "tap to connect Shizuku for cross-app and vendor nodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShizukuActions(status: ShizukuState.Status, onRequest: () -> Unit) {
    if (status.mode == ShizukuState.Mode.NO_PERMISSION) {
        Button(onClick = onRequest) { Text("Grant Shizuku permission") }
    }
}
