// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.core

import android.content.Context
import android.os.Build
import android.util.Log
import com.mohdev.alathan.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Central logger: captures every crash/error, rotates files, feeds the LAN viewer. Redacts personal data. */
object AppLog {
    enum class Level { DEBUG, INFO, WARN, ERROR, FATAL }

    private const val MAX_FILE_BYTES = 1_000_000L
    private const val MAX_FILES = 5
    private var dir: File? = null
    private val ring = ArrayDeque<String>()
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "applog").apply { isDaemon = true } }
    var deviceInfo: String = ""
        private set

    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "logs").apply { mkdirs() }
        deviceInfo = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) " +
            "Android ${Build.VERSION.RELEASE}/API${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}"
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            write(Level.FATAL, "crash", "Uncaught in ${t.name}: $e", e, sync = true)
            previous?.uncaughtException(t, e)
        }
        i("log", "Session start | $deviceInfo")
    }

    fun d(tag: String, msg: String) = write(Level.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = write(Level.INFO, tag, msg)
    fun w(tag: String, msg: String) = write(Level.WARN, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = write(Level.ERROR, tag, msg, t)

    fun write(level: Level, tag: String, msg: String, t: Throwable? = null, sync: Boolean = false) {
        val o = JSONObject()
        o.put("t", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
        o.put("l", level.name.lowercase())
        o.put("tag", tag)
        o.put("m", Redact.text(msg))
        if (t != null) o.put("st", Redact.text(Log.getStackTraceString(t)))
        val line = o.toString()
        synchronized(ring) {
            ring.addLast(line)
            while (ring.size > 500) ring.removeFirst()
        }
        for (l in listeners) runCatching { l(line) }
        Log.println(if (level >= Level.ERROR) Log.ERROR else Log.INFO, "Alathan/$tag", msg)
        val job = Runnable { append(line) }
        if (sync) runCatching { io.submit(job).get(1500, TimeUnit.MILLISECONDS) } else io.execute(job)
    }

    private fun append(line: String) {
        val d = dir ?: return
        val f = File(d, "app.log")
        if (f.length() > MAX_FILE_BYTES) rotate(d)
        FileWriter(f, true).use { it.write(line + "\n") }
    }

    private fun rotate(d: File) {
        File(d, "app.${MAX_FILES - 1}.log").delete()
        for (i in MAX_FILES - 2 downTo 1) File(d, "app.$i.log").takeIf { it.exists() }?.renameTo(File(d, "app.${i + 1}.log"))
        File(d, "app.log").renameTo(File(d, "app.1.log"))
    }

    fun recent(): List<String> = synchronized(ring) { ring.toList() }
    fun addListener(l: (String) -> Unit) { listeners.add(l) }
    fun removeListener(l: (String) -> Unit) { listeners.remove(l) }

    fun readAll(): String {
        val d = dir ?: return ""
        val b = StringBuilder()
        for (i in MAX_FILES - 1 downTo 1) File(d, "app.$i.log").takeIf { it.exists() }?.let { b.append(it.readText()) }
        File(d, "app.log").takeIf { it.exists() }?.let { b.append(it.readText()) }
        return b.toString()
    }

    fun clear() {
        dir?.listFiles()?.forEach { it.delete() }
        synchronized(ring) { ring.clear() }
    }
}
