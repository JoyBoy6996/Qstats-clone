package dev.pulsekit.app

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLog {
    private const val FILE = "last_crash.txt"

    fun install(app: PulseKitApplication) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { write(app, t, e) } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }

    fun write(context: Context, t: Thread, e: Throwable) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        File(context.filesDir, FILE).writeText(
            "$ts thread=${t.name}\n${e.javaClass.name}: ${e.message}\n" +
                e.stackTrace.joinToString("\n") { "  at $it" } +
                (e.cause?.let { c -> "\ncause: ${c.javaClass.name}: ${c.message}\n" +
                    c.stackTrace.take(15).joinToString("\n") { "  at $it" } } ?: "")
        )
    }

    fun read(context: Context): String? =
        File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.take(4000)

    fun clear(context: Context) { File(context.filesDir, FILE).delete() }
}
