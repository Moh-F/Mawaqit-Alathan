// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import com.mohdev.alathan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val version: String, val build: Int, val minBuild: Int, val mandatory: Boolean,
    val notes: Map<String, String>, val url: String, val sha256: String,
) {
    fun forced(currentBuild: Int) = mandatory || currentBuild < minBuild
}

/**
 * Updates via GitHub Releases. The APK is signed with the SAME key as the installed one,
 * so Android installs it over the old version and keeps all data.
 */
object Updater {
    private val manifestUrl get() =
        "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest/download/latest.json"

    /** Pure and unit-tested. Returns null if there is nothing newer for [platform]. */
    fun parse(json: String, currentBuild: Int, platform: String = "android"): UpdateInfo? = runCatching {
        val j = JSONObject(json)
        val build = j.getInt("build")
        if (build <= currentBuild) return null
        val asset = j.getJSONObject("assets").optJSONObject(platform) ?: return null
        val notes = j.optJSONObject("notes")?.let { n -> n.keys().asSequence().associateWith { n.getString(it) } } ?: emptyMap()
        UpdateInfo(
            j.getString("version"), build, j.optInt("minBuild", 0), j.optBoolean("mandatory", false),
            notes, asset.getString("url"), asset.getString("sha256").lowercase(),
        )
    }.getOrNull()

    suspend fun check(ctx: Context, ignoreSnooze: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(manifestUrl).openConnection() as HttpURLConnection
            c.connectTimeout = 5000; c.readTimeout = 5000
            c.setRequestProperty("User-Agent", "MawaqitAlathan/${BuildConfig.VERSION_NAME}")
            if (c.responseCode != 200) return@runCatching null
            val info = parse(c.inputStream.bufferedReader().use { it.readText() }, BuildConfig.VERSION_CODE) ?: return@runCatching null
            if (!ignoreSnooze && !info.forced(BuildConfig.VERSION_CODE) && isSnoozed(ctx, info.build)) null else info
        }.onFailure { AppLog.w("update", "check failed: ${it.message}") }.getOrNull()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("updater", Context.MODE_PRIVATE)
    fun snooze(ctx: Context, i: UpdateInfo) = prefs(ctx).edit()
        .putInt("snooze_build", i.build).putLong("snooze_until", System.currentTimeMillis() + 24 * 3600_000L).apply()
    fun skip(ctx: Context, i: UpdateInfo) = prefs(ctx).edit().putInt("skip_build", i.build).apply()
    private fun isSnoozed(ctx: Context, build: Int): Boolean {
        val p = prefs(ctx)
        if (p.getInt("skip_build", -1) == build) return true
        return p.getInt("snooze_build", -1) == build && System.currentTimeMillis() < p.getLong("snooze_until", 0)
    }

    /** Downloads and verifies SHA-256. [onProgress] receives 0f..1f. */
    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        require(info.url.startsWith("https://")) { "update url must be https" }
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "update-${info.build}.apk")
        val c = URL(info.url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 20_000
        val total = c.contentLengthLong
        val md = MessageDigest.getInstance("SHA-256")
        c.inputStream.use { ins ->
            out.outputStream().use { os ->
                val buf = ByteArray(32 * 1024)
                var got = 0L
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    os.write(buf, 0, n); md.update(buf, 0, n); got += n
                    if (total > 0) onProgress(got.toFloat() / total)
                }
            }
        }
        val hex = md.digest().joinToString("") { "%02x".format(it) }
        if (hex != info.sha256) {
            out.delete()
            throw IllegalStateException("checksum mismatch")
        }
        out
    }

    /** Opens the system installer. Same signature + higher versionCode = update in place, data kept. */
    fun install(ctx: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
