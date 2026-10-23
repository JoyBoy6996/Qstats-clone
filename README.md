# PulseKit

A Scene-style system monitor for Android, written from scratch. It covers the home dashboard, thermal zones, per-core CPU, fps stats, and fps recording. It supports optional Shizuku for privileged reads.

## Build steps

1. Install Android Studio (Ladybug or newer) with JDK 17.
2. Open the PulseKit folder. Let Gradle sync. Android Studio fetches the wrapper jar on first sync.
   If you build from a terminal instead, run `gradle wrapper --gradle-version 8.9` once in the project root to generate gradlew and the wrapper jar, then `./gradlew assembleDebug`.
3. Pinned versions: AGP 8.5.2, Kotlin 2.0.21 (with the Compose compiler plugin), KSP 2.0.21-1.0.27, Gradle 8.9, compileSdk 35, minSdk 26.
4. Debug APK: app/build/outputs/apk/debug/app-debug.apk.

## Signing

Debug builds use the Android debug key. For a release build, set these environment variables and run `./gradlew assembleRelease`:
PULSEKIT_KEYSTORE (path to your .jks), PULSEKIT_STORE_PASS, PULSEKIT_KEY_ALIAS, PULSEKIT_KEY_PASS.
Without PULSEKIT_KEYSTORE the release build falls back to the debug key. Never commit a keystore or passwords.

## Shizuku

1. Install the Shizuku app from its official GitHub releases or Play Store.
2. Start Shizuku. Use Wireless debugging (Android 11+) or connect to a PC with adb once.
3. Open PulseKit. The mode badge on each screen shows the current state. Tap it, or press Grant Shizuku permission on the home screen.
4. Approve the prompt in the Shizuku dialog. The badge changes to "Shizuku mode: privileged shell active".
5. If Shizuku stops (reboot, or the wireless debugging session ends), the app drops back to basic mode automatically. Start Shizuku again to restore privileged mode.

Shizuku is hidden-API based. The app reaches Shizuku.newProcess by reflection, so it runs on Shizuku 13.x. Update the Shizuku app if the badge says the version is too old.

## What works without Shizuku (basic mode)

- Home dashboard: device model, Android version, SoC string, battery level, battery temperature, charging state, total CPU load.
- Thermal: reads /sys/class/thermal/thermal_zone*/temp and type directly. Many devices expose these to apps. Zones that fail to read show n/a.
- CPU: per-core load from /proc/stat deltas, and cpufreq current, min, and max from sysfs. Offline cores are marked.
- FPS for this app's own screens: FrameMetrics, per frame. Gives fps, jank per second, and frame time.
- Recording and storage: start and stop recording, samples stored in Room, session list, CSV export via the Android share sheet.

## What needs Shizuku

- FPS for other apps: `dumpsys gfxinfo <package>` run through the Shizuku shell. The FPS tab accepts a package name and computes fps from the cumulative frame counters.
- Foreground app lookup: `dumpsys activity activities`.
- Thermal zones when sysfs is not readable to apps: a shell batch read.

## Recording rules

Recording stops on manual stop, on screen off (ACTION_SCREEN_OFF), or after 30 minutes. Sessions shorter than 10 seconds are not saved. Each session stores average, minimum, 5th percentile fps, total jank, and per-second samples.

## Known limitations

- No native daemon. The cross-app fps number is sampled from `dumpsys gfxinfo` once per second. The counters are cumulative, so fps is a delta over about 1 second and can lag or drift on busy apps. It is not a per-frame stream like a native daemon would give.
- Cross-app fps depends on the target app's gfxinfo counters being available. Some apps or Android versions report them poorly.
- The app's own FrameMetrics covers only PulseKit's own screen. It does not measure other apps.
- Vendor thermal nodes (ttj, dynamic_tj, MediaTek or Qualcomm specifics) are not read. Many are SELinux-restricted even with Shizuku.
- Read-only: this build does not change governors, frequencies, or thermal configs.
- No floating overlay in this version. The overlay needs SYSTEM_ALERT_WINDOW and is not implemented.
- Charts keep the last 120 samples in memory only. Sessions in Room keep full per-second data.
- Not tested on device by the author. Expect to fix small compile issues the first time Android Studio builds.

## Project layout

app/src/main/java/dev/pulsekit/app
- PulseKitApplication.kt: starts Shizuku listeners and the sampler. Screen-off stop rule.
- MainActivity.kt: single activity, Compose, FrameMetrics attach.
- shizuku/: ShizukuState (binder, permission, mode), ShellRunner (privileged or basic shell).
- data/: SysReader (sysfs/procfs), FpsSources (FrameMetrics, dumpsys gfxinfo), Monitor (1 Hz sampler), FpsRecorder, db/ (Room entities, DAO, database).
- ui/: PulseRoot (bottom nav), screens/ (Home, Thermal, CPU, FPS, Sessions), Charts (line chart), theme (Material 3 dark).

## Attribution

Original code. The approach comes from general Android APIs (FrameMetrics, procfs, sysfs, Room, Shizuku's public API). Scene is a commercial product and none of its code is reproduced here.
