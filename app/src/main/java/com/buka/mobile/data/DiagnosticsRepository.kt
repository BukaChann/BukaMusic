package com.buka.mobile.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val directory = File(appContext.filesDir, "diagnostics").apply { mkdirs() }

    fun install() {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash("jvm", thread.name, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
        startAnrWatchdog()
    }

    fun consumeSafeMode(): String? {
        if (!preferences.getBoolean(KEY_PENDING, false)) return null
        preferences.edit().putBoolean(KEY_PENDING, false).apply()
        return listLogs().firstOrNull { it.name.startsWith("crash-") || it.name.startsWith("anr-") }
            ?.let(::read)
            ?: "检测到上次异常退出，但诊断记录为空。"
    }

    fun listLogs(): List<File> =
        directory.listFiles()?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    fun read(file: File): String =
        runCatching { file.readText() }.getOrElse { "无法读取 ${file.name}: ${it.message}" }

    fun exportText(): String = buildString {
        appendLine("BukaMusic Mobile Diagnostics")
        appendLine("Generated: ${timestamp()}")
        listLogs().forEach { file ->
            appendLine()
            appendLine("=".repeat(72))
            appendLine(file.name)
            appendLine("=".repeat(72))
            appendLine(read(file))
        }
    }

    fun recordEvent(tag: String, message: String) {
        if (!preferences.getBoolean(KEY_PERSISTENT_LOGS, false)) return
        runCatching {
            File(directory, "events-${day()}.log").appendText(
                "${timestamp()} [$tag] $message\n",
            )
        }
    }

    fun setPersistentLogs(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PERSISTENT_LOGS, enabled).apply()
    }

    fun clearLogs() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun writeCrash(kind: String, threadName: String, throwable: Throwable) {
        directory.mkdirs()
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        val file = File(directory, "crash-${timestampForFile()}.txt")
        file.writeText(
            buildString {
                appendLine("type=$kind")
                appendLine("time=${timestamp()}")
                appendLine("thread=$threadName")
                appendLine()
                append(writer.toString())
            },
        )
        preferences.edit().putBoolean(KEY_PENDING, true).apply()
    }

    private fun writeAnr() {
        directory.mkdirs()
        val file = File(directory, "anr-${timestampForFile()}.txt")
        file.writeText(
            buildString {
                appendLine("type=anr")
                appendLine("time=${timestamp()}")
                appendLine("Main thread did not respond for more than 8 seconds.")
                appendLine("Last heartbeat age: ${SystemClock.elapsedRealtime() - heartbeat} ms")
            },
        )
        preferences.edit().putBoolean(KEY_PENDING, true).apply()
    }

    @Volatile
    private var heartbeat = SystemClock.elapsedRealtime()
    @Volatile
    private var lastAnrReport = 0L

    private fun startAnrWatchdog() {
        val main = Handler(Looper.getMainLooper())
        Thread({
            while (true) {
                main.post { heartbeat = SystemClock.elapsedRealtime() }
                try {
                    Thread.sleep(2_000L)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                val age = SystemClock.elapsedRealtime() - heartbeat
                if (age > 8_000L &&
                    SystemClock.elapsedRealtime() - lastAnrReport > 60_000L
                ) {
                    lastAnrReport = SystemClock.elapsedRealtime()
                    runCatching { writeAnr() }
                }
            }
        }, "buka-mobile-anr-watchdog").apply {
            isDaemon = true
            start()
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())

    private fun day(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

    private fun timestampForFile(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date())

    companion object {
        private const val PREFS = "buka_mobile_diagnostics"
        private const val KEY_PENDING = "safe_mode_pending"
        private const val KEY_PERSISTENT_LOGS = "persistent_logs"
        @Volatile private var installed = false
    }
}
