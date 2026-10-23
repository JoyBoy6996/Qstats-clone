package dev.pulsekit.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dev.pulsekit.app.data.FrameStatsHub
import dev.pulsekit.app.ui.PulseRoot
import dev.pulsekit.app.ui.theme.PulseTheme
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    private fun trace(t: Throwable): String {
        val sw = StringWriter()
        sw.write("UI CRASH\n")
        t.printStackTrace(java.io.PrintWriter(sw))
        return sw.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FrameStatsHub.attach(this) } catch (t: Throwable) { CrashLog.write(this, Thread.currentThread(), t) }

        // STAGE 1: plain android view. if this does not show, the crash is
        // below all our code (app class, theme, dex, device).
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        val hello = TextView(this).apply {
            text = "PULSEKIT STAGE 1 OK\napp process is alive.\n\nbuild: diag2\n\nnext: tap the button to load the real UI"
            textSize = 16f
        }
        val btn = Button(this).apply { text = "load real UI" }
        box.addView(hello)
        box.addView(btn)
        val status = TextView(this).apply { textSize = 12f; setPadding(0, 24, 0, 0) }
        box.addView(status)
        btn.setOnClickListener {
            status.text = "loading compose UI..."
            // STAGE 2: real compose UI. a crash here lands in the uncaught
            // handler; reopen the app to read it from the stage-1 screen? no:
            // we also print it into status first chance we get.
            try {
                setContent { PulseTheme { Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { PulseRoot() } } }
                status.text = ""
            } catch (t: Throwable) {
                status.text = trace(t).take(3000)
            }
        }
        setContentView(box, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onDestroy() {
        try { FrameStatsHub.detach(this) } catch (_: Throwable) {}
        super.onDestroy()
    }
}
