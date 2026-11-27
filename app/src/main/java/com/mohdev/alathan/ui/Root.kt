// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mohdev.alathan.BuildConfig
import com.mohdev.alathan.R
import com.mohdev.alathan.core.AppLog
import com.mohdev.alathan.core.Fmt
import com.mohdev.alathan.core.LanServer
import com.mohdev.alathan.core.Settings
import com.mohdev.alathan.core.Tri
import com.mohdev.alathan.core.UpdateInfo
import com.mohdev.alathan.core.Updater
import com.mohdev.alathan.data.PrayerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File

@Composable
fun AlathanRoot() {
    var ready by remember { mutableStateOf(false) }
    val dark = Settings.color("bgColor").luminance() < 0.5f
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        if (!ready) Splash { ready = true } else AppContent()
    }
}

private class Step(val name: Tri, val run: suspend () -> Unit)

/** Splash: app icon + progress bar + numeric percent. Progress is REAL (completed init steps). */
@Composable
fun Splash(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val lang = Settings.lang()
    var progress by remember { mutableFloatStateOf(0f) }
    var label by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        val steps = listOf(
            Step(Names.loadingStep) { AppState.day = PrayerRepository.loadCache(ctx) },
            Step(Names.updateStep) {
                if (Settings.bool("checkUpdates")) {
                    withTimeoutOrNull(3500) { Updater.check(ctx) }?.let { AppState.update = it }
                }
            },
            Step(Names.timesStep) {
                withTimeoutOrNull(12_000) { PrayerRepository.load(ctx) }?.let { AppState.day = it }
            },
        )
        steps.forEachIndexed { i, s ->
            label = s.name.get(lang)
            runCatching { s.run() }.onFailure { AppLog.e("init", "step failed: ${s.name.en}", it) }
            progress = (i + 1f) / steps.size
        }
        val left = 600 - (System.currentTimeMillis() - started)
        if (left > 0) delay(left)
        onDone()
    }
    val bg = Settings.color("bgColor")
    val fg = Settings.color("textColor")
    val pct = Fmt.digits("${(progress * 100).toInt()}%", Settings.str("digits") == "arabic")
    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Column(Modifier.width(360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp).clip(RoundedCornerShape(28.dp)).background(colorResource(R.color.ic_background))) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Settings.color("accentColor"),
                trackColor = fg.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(12.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(pct, color = fg, fontSize = 20.sp)
            }
            Text(label, color = Settings.color("mutedColor"), fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun AppContent() {
    val ctx = LocalContext.current
    val lan = Settings.bool("lanServer")
    LaunchedEffect(lan) {
        if (lan) LanServer.start(ctx) else if (LanServer.running) LanServer.stop()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val d = AppState.day
            if (d == null || d.date != PrayerRepository.today() || d.stale || System.currentTimeMillis() - d.fetchedAt > 6 * 3_600_000L) {
                AppState.refreshTimes(ctx)
            }
        }
    }
    BackHandler(AppState.settingsOpen) { AppState.settingsOpen = false }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize()) { MainScreen() }
            if (AppState.settingsOpen) SettingsPanel(Modifier.width(460.dp))
        }
        if (Settings.bool("showRibbon") || BuildConfig.DEBUG) {
            VersionRibbon("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Modifier.align(Alignment.BottomEnd))
        }
    }
    AppState.update?.let { UpdateDialog(it) }
    if (AppState.showLog) LogDialog()
}

@Composable
fun UpdateDialog(info: UpdateInfo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = Settings.lang()
    val forced = info.forced(BuildConfig.VERSION_CODE)
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var file by remember { mutableStateOf<File?>(null) }

    fun start() {
        scope.launch {
            try {
                progress = 0f
                val f = Updater.download(ctx, info) { progress = it }
                file = f
                Updater.install(ctx, f)
            } catch (e: Exception) {
                AppLog.e("update", "download failed", e)
                error = e.message
                progress = null
            }
        }
    }
    LaunchedEffect(Unit) { if (Settings.str("updateMode") == "auto" && !forced) start() }

    AlertDialog(
        onDismissRequest = { if (!forced) { Updater.snooze(ctx, info); AppState.update = null } },
        properties = DialogProperties(dismissOnBackPress = !forced, dismissOnClickOutside = false),
        title = { Text("v${info.version}") },
        text = {
            Column {
                Text(info.notes[lang] ?: info.notes["en"] ?: "")
                progress?.let {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { it }, Modifier.fillMaxWidth())
                }
                error?.let { Text(it, color = Color(0xFFEF4444)) }
            }
        },
        confirmButton = {
            Button(onClick = { file?.let { Updater.install(ctx, it) } ?: start() }) { Text(Names.update.get(lang)) }
        },
        dismissButton = {
            if (!forced) Row {
                TextButton(onClick = { Updater.skip(ctx, info); AppState.update = null }) { Text(Names.skip.get(lang)) }
                TextButton(onClick = { Updater.snooze(ctx, info); AppState.update = null }) { Text(Names.later.get(lang)) }
            }
        },
    )
}

@Composable
fun LogDialog() {
    val lang = Settings.lang()
    val lines = remember {
        AppLog.recent().takeLast(80).map {
            runCatching {
                val o = JSONObject(it)
                "${o.getString("t").substring(11, 19)} [${o.getString("l")}] ${o.getString("tag")}: ${o.getString("m")}"
            }.getOrDefault(it)
        }
    }
    AlertDialog(
        onDismissRequest = { AppState.showLog = false },
        title = { Text("Log") },
        text = {
            LazyColumn(Modifier.height(320.dp)) {
                items(lines) { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        },
        confirmButton = { TextButton(onClick = { AppState.showLog = false }) { Text(Names.close.get(lang)) } },
    )
}
