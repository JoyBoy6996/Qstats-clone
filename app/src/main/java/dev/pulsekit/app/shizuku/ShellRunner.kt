package dev.pulsekit.app.shizuku

import java.util.concurrent.TimeUnit

/**
 * Runs a shell command. With Shizuku granted it runs as the shell user through
 * Shizuku.newProcess (hidden API, reached by reflection). Otherwise it runs as the
 * app uid, which only reads world-readable nodes.
 */
object ShellRunner {

    private val newProcessMethod by lazy {
        try {
            rikka.shizuku.Shizuku::class.java
                .getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .apply { isAccessible = true }
        } catch (e: Throwable) { null }
    }

    fun run(command: String, timeoutMs: Long = 4000): String? = try {
        val process: Process = if (ShizukuState.isGranted() && newProcessMethod != null) {
            newProcessMethod!!.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        } else {
            ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        }
        val out = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        process.destroy()
        out
    } catch (e: Throwable) {
        null
    }
}
