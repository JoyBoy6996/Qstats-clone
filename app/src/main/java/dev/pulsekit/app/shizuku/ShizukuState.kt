package dev.pulsekit.app.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Tracks the Shizuku binder and permission. Privileged paths check [isGranted];
 * the UI shows [status] so the user always knows which mode is active.
 */
object ShizukuState {

    enum class Mode(val label: String) {
        NOT_RUNNING("Basic mode: Shizuku is not running"),
        NO_PERMISSION("Basic mode: Shizuku running, PulseKit not granted"),
        PRIVILEGED("Shizuku mode: privileged shell active"),
        UNSUPPORTED("Basic mode: Shizuku version too old (needs 11+)")
    }

    data class Status(val mode: Mode = Mode.NOT_RUNNING, val uid: Int = -1)

    const val REQUEST_CODE = 4217
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()
    private var inited = false

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    fun init() {
        if (inited) return
        inited = true
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
    }

    fun refresh() {
        _status.value = try {
            when {
                !Shizuku.pingBinder() -> Status(Mode.NOT_RUNNING)
                Shizuku.isPreV11() -> Status(Mode.UNSUPPORTED)
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    Status(Mode.PRIVILEGED, Shizuku.getUid())
                else -> Status(Mode.NO_PERMISSION)
            }
        } catch (e: Throwable) {
            Status(Mode.NOT_RUNNING)
        }
    }

    fun isGranted(): Boolean = _status.value.mode == Mode.PRIVILEGED

    /** Returns a user-facing message, or null when the grant dialog was requested. */
    fun requestPermission(): String? = try {
        when {
            !Shizuku.pingBinder() -> "Shizuku is not running. Start it in the Shizuku app, then try again."
            Shizuku.isPreV11() -> "Your Shizuku is too old. Update the Shizuku app."
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> { refresh(); null }
            Shizuku.shouldShowRequestPermissionRationale() ->
                "Permission was denied before. Open Shizuku, find PulseKit, and grant it there."
            else -> { Shizuku.requestPermission(REQUEST_CODE); null }
        }
    } catch (e: Throwable) {
        "Could not reach Shizuku: ${e.javaClass.simpleName}"
    }
}
