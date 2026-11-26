package com.mohdev.alathan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdev.alathan.core.AppLog
import com.mohdev.alathan.core.FBool
import com.mohdev.alathan.core.FChoice
import com.mohdev.alathan.core.FColor
import com.mohdev.alathan.core.FInt
import com.mohdev.alathan.core.FText
import com.mohdev.alathan.core.Field
import com.mohdev.alathan.core.LanServer
import com.mohdev.alathan.core.Schema
import com.mohdev.alathan.core.Settings
import com.mohdev.alathan.core.Tri
import com.mohdev.alathan.core.Updater
import kotlinx.coroutines.launch

private sealed class Item {
    class Header(val text: String) : Item()
    class FieldItem(val f: Field, val first: Boolean) : Item()
    class Info(val text: String) : Item()
    class Action(val label: Tri, val run: () -> Unit) : Item()
}

private val PanelBg = Color(0xEE0E1317)
private val Fg = Color(0xFFF1F5F2)
private val Mut = Color(0xFF9FB0A8)
private val Acc = Color(0xFFE8C766)

/** Right-hand settings panel; changes apply live to the screen on the left. D-pad: up/down = move, left/right = change, OK = toggle. */
@Composable
fun SettingsPanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val lang = Settings.lang()
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    val rows = buildList<Item> {
        var g: Tri? = null
        var firstDone = false
        for (f in Schema.fields) {
            if (f.group != g) { g = f.group; add(Item.Header(f.group.get(lang))) }
            val isFirst = !firstDone && f !is FText
            add(Item.FieldItem(f, isFirst)); if (isFirst) firstDone = true
            if (f.key == "lanServer" && LanServer.url.isNotEmpty()) add(Item.Info("${Names.lanOpen.get(lang)}\n${LanServer.url}"))
        }
        add(Item.Header(Names.settings.get(lang)))
        add(Item.Action(Names.reload) { scope.launch { AppState.refreshTimes(ctx); status = Names.done.get(lang) } })
        add(Item.Action(Names.checkUpdate) {
            scope.launch {
                val u = Updater.check(ctx, ignoreSnooze = true)
                if (u != null) AppState.update = u else status = Names.upToDate.get(lang)
            }
        })
        add(Item.Action(Names.viewLog) { AppState.showLog = true })
        add(Item.Action(Names.clearLog) { AppLog.clear(); status = Names.done.get(lang) })
        add(Item.Action(Names.reset) { Settings.resetAll(); status = Names.done.get(lang) })
        add(Item.Action(Names.close) { AppState.settingsOpen = false })
        add(Item.Info(Names.privacy.get(lang)))
    }

    Column(modifier.fillMaxHeight().background(PanelBg).padding(16.dp)) {
        Text(Names.settings.get(lang), color = Fg, fontSize = 22.sp)
        if (status.isNotEmpty()) Text(status, color = Acc, fontSize = 14.sp)
        Spacer(Modifier.size(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows) { item ->
                when (item) {
                    is Item.Header -> Text(item.text, color = Acc, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
                    is Item.Info -> Text(item.text, color = Mut, fontSize = 13.sp)
                    is Item.Action -> ActionRow(item.label.get(lang), item.run)
                    is Item.FieldItem -> FieldRow(item.f, lang, if (item.first) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
    }
}

@Composable
private fun RowShell(modifier: Modifier, onKey: (Key) -> Boolean, content: @Composable RowScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus }
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyDown) onKey(e.key) else false }
            .focusable()
            .background(if (focused) Color(0x33FFFFFF) else Color(0x14FFFFFF), RoundedCornerShape(10.dp))
            .border(if (focused) 2.dp else 0.dp, if (focused) Acc else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) { content() }
}

private fun isActivate(k: Key) = k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter

@Composable
private fun ActionRow(label: String, run: () -> Unit) {
    RowShell(Modifier, { k -> if (isActivate(k)) { run(); true } else false }) {
        Text(label, color = Acc, fontSize = 16.sp)
    }
}

@Composable
private fun FieldRow(f: Field, lang: String, modifier: Modifier) {
    val label = f.label.get(lang)
    if (f is FText) {
        var focused by remember { mutableStateOf(false) }
        Column(modifier.fillMaxWidth().onFocusChanged { focused = it.hasFocus }) {
            Text(label, color = if (focused) Acc else Mut, fontSize = 13.sp)
            OutlinedTextField(
                value = Settings.str(f.key),
                onValueChange = { Settings.set(f.key, it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    val value = Settings.str(f.key)
    fun adjust(dir: Int): Boolean {
        when (f) {
            is FInt -> Settings.set(f.key, (value.toInt() + dir * f.step).coerceIn(f.min, f.max).toString())
            is FBool -> Settings.set(f.key, (value != "true").toString())
            is FChoice -> {
                val i = f.options.indexOfFirst { it.first == value }.coerceAtLeast(0)
                Settings.set(f.key, f.options[(i + dir + f.options.size) % f.options.size].first)
            }
            is FColor -> {
                val p = Schema.palette
                val i = p.indexOf(value.uppercase())
                val n = if (i < 0) 0 else (i + dir + p.size) % p.size
                Settings.set(f.key, p[n])
            }
            is FText -> return false
        }
        return true
    }
    RowShell(modifier, { k ->
        when {
            k == Key.DirectionRight -> adjust(1)
            k == Key.DirectionLeft -> adjust(-1)
            isActivate(k) && (f is FBool) -> adjust(1)
            isActivate(k) -> adjust(1)
            else -> false
        }
    }) {
        Text(label, color = Fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
        when (f) {
            is FColor -> {
                Box(Modifier.size(22.dp).background(Settings.color(f.key), RoundedCornerShape(6.dp)).border(1.dp, Mut, RoundedCornerShape(6.dp)))
                Spacer(Modifier.size(8.dp))
                Text(value, color = Mut, fontSize = 13.sp)
            }
            is FBool -> Text(if (value == "true") "● ON" else "○ OFF", color = if (value == "true") Acc else Mut, fontSize = 15.sp)
            is FChoice -> Text("‹ ${f.options.first { it.first == value }.second.get(lang)} ›", color = Acc, fontSize = 15.sp)
            is FInt -> Text("‹ $value${f.unit} ›", color = Acc, fontSize = 15.sp)
            is FText -> {}
        }
    }
}
