package dev.pulsekit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.pulsekit.app.data.FrameStatsHub
import dev.pulsekit.app.ui.PulseRoot
import dev.pulsekit.app.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FrameStatsHub.attach(this)   // FrameMetrics for this app's own frames
        setContent { PulseTheme { PulseRoot() } }
    }

    override fun onDestroy() {
        FrameStatsHub.detach(this)
        super.onDestroy()
    }
}
