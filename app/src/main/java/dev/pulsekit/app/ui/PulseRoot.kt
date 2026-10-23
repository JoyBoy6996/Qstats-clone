package dev.pulsekit.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.pulsekit.app.ui.screens.CpuScreen
import dev.pulsekit.app.ui.screens.FpsScreen
import dev.pulsekit.app.ui.screens.HomeScreen
import dev.pulsekit.app.ui.screens.SessionsScreen
import dev.pulsekit.app.ui.screens.ThermalScreen

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    THERMAL("Thermal", Icons.Filled.Thermostat),
    CPU("CPU", Icons.Filled.Memory),
    FPS("FPS", Icons.Filled.Speed),
    SESSIONS("Sessions", Icons.Filled.Analytics)
}

@Composable
fun PulseRoot() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            Tab.HOME -> HomeScreen(m)
            Tab.THERMAL -> ThermalScreen(m)
            Tab.CPU -> CpuScreen(m)
            Tab.FPS -> FpsScreen(m)
            Tab.SESSIONS -> SessionsScreen(m)
        }
    }
}
