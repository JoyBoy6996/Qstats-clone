package dev.pulsekit.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.pulsekit.app.data.FpsRecorder
import dev.pulsekit.app.data.Monitor
import dev.pulsekit.app.shizuku.ShizukuState

class PulseKitApplication : Application() {

    // Stop rule from the report: recording ends when the screen turns off.
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            FpsRecorder.requestStop("screen off")
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        try { ShizukuState.init() } catch (t: Throwable) { CrashLog.write(this, Thread.currentThread(), t) }
        try { ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED) } catch (t: Throwable) { CrashLog.write(this, Thread.currentThread(), t) }
        try { Monitor.start(this) } catch (t: Throwable) { CrashLog.write(this, Thread.currentThread(), t) }
    }
}
