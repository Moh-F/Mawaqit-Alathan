package com.mohdev.alathan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdev.alathan.core.Fmt
import com.mohdev.alathan.core.Settings
import com.mohdev.alathan.data.NextPrayer
import com.mohdev.alathan.data.Prayer
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CardState { PASSED, NEXT, UPCOMING }

@Composable
fun MainScreen() {
    val lang = Settings.lang()
    val arabic = Settings.str("digits") == "arabic"
    val h24 = Settings.bool("hour24")

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000 + 10)
        }
    }
    var hint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(20_000); hint = false }

    val textC = Settings.color("textColor")
    val mutedC = Settings.color("mutedColor")
    val weight = when (Settings.str("fontWeight")) {
        "bold" -> FontWeight.Bold
        "normal" -> FontWeight.Normal
        else -> FontWeight.Medium
    }
    val bg1 = Settings.color("bgColor")
    val brush: Brush = if (Settings.bool("bgGradient")) Brush.verticalGradient(listOf(bg1, Settings.color("bg2Color"))) else SolidColor(bg1)
    val shift = if (Settings.bool("burnIn")) (((now / 60_000) % 5).toInt() - 2) * 2 else 0

    val day = AppState.day
    val shown: List<Prayer> = day?.prayers?.filter { Settings.bool("showSunrise") || it.id != "sunrise" } ?: emptyList()
    val next = NextPrayer.compute(shown, now)

    Box(Modifier.fillMaxSize().background(brush)) {
        Column(
            Modifier.fillMaxSize().offset(shift.dp, shift.dp).padding(Settings.int("screenPad").dp),
        ) {
            Header(now, lang, arabic, h24, textC, mutedC, weight)
            Spacer(Modifier.height(Settings.int("headerGap").dp))

            if (day != null && next != null) {
                if (Settings.bool("showCountdown")) {
                    val name = Names.prayer[shown[next.index].id]?.get(lang) ?: ""
                    val secs = (next.targetMs - now) / 1000
                    val tm = if (next.tomorrow) " (${Names.tomorrow.get(lang)})" else ""
                    Text(
                        "${Names.next.get(lang)}: $name$tm  ·  ${Fmt.hms(secs, arabic)}",
                        color = Settings.color("accentColor"),
                        fontSize = Settings.int("countdownSize").sp,
                        fontWeight = weight,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Settings.int("headerGap").dp))
                }
                Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PrayerArea(shown, next.index, next.tomorrow, lang, arabic, h24, weight)
                }
                val sourceText = buildString {
                    append(day.mosque ?: if (day.source == "aladhan") "Aladhan" else "Mawaqit")
                    if (day.stale) append(" · ").append(Names.stale.get(lang))
                }
                Text(sourceText, color = mutedC.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        (if (AppState.loading) Names.loading else Names.noData).get(lang),
                        color = mutedC, fontSize = 28.sp,
                    )
                }
            }
            if (hint) {
                Text(Names.hint.get(lang), color = mutedC.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Header(now: Long, lang: String, arabic: Boolean, h24: Boolean, textC: Color, mutedC: Color, weight: FontWeight) {
    val align = when (Settings.str("headerAlign")) {
        "start" -> Alignment.Start
        "end" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)
    val sec = if (Settings.bool("showSeconds")) String.format(Locale.US, ":%02d", s) else ""
    val clock = if (h24) {
        Fmt.digits(String.format(Locale.US, "%02d:%02d%s", h, m, sec), arabic)
    } else {
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (h < 12) (if (lang == "ar") "ص" else "AM") else (if (lang == "ar") "م" else "PM")
        Fmt.digits(String.format(Locale.US, "%d:%02d%s %s", h12, m, sec, suffix), arabic)
    }
    val greg = Fmt.digits(SimpleDateFormat("EEEE d MMMM yyyy", Locale.forLanguageTag(lang)).format(Date(now)), arabic)
    val hijri = if (Settings.bool("showHijri")) runCatching {
        val f = android.icu.text.SimpleDateFormat("d MMMM y", android.icu.util.ULocale("$lang@calendar=islamic-umalqura"))
        Fmt.digits(f.format(Date(now)), arabic) + if (lang == "ar") " هـ" else " AH"
    }.getOrNull() else null

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(clock, color = textC, fontSize = Settings.int("clockSize").sp, fontWeight = weight, maxLines = 1, softWrap = false)
        }
        Text(greg, color = mutedC, fontSize = Settings.int("dateSize").sp)
        if (hijri != null) Text(hijri, color = mutedC, fontSize = Settings.int("dateSize").sp)
    }
}

@Composable
private fun PrayerArea(list: List<Prayer>, nextIdx: Int, tomorrow: Boolean, lang: String, arabic: Boolean, h24: Boolean, weight: FontWeight) {
    val gap = Settings.int("cardGap").dp
    val ch = Settings.int("cardHeight")
    fun stateOf(i: Int) = when {
        tomorrow -> if (i == 0) CardState.NEXT else CardState.PASSED
        i < nextIdx -> CardState.PASSED
        i == nextIdx -> CardState.NEXT
        else -> CardState.UPCOMING
    }
    fun sized(m: Modifier) = if (ch > 0) m.height(ch.dp) else m

    when (Settings.str("layout")) {
        "column" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            list.forEachIndexed { i, p -> PrayerCard(p, stateOf(i), true, lang, arabic, h24, weight, sized(Modifier.fillMaxWidth())) }
        }
        "grid" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            list.withIndex().chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowItems.forEach { (i, p) -> PrayerCard(p, stateOf(i), false, lang, arabic, h24, weight, sized(Modifier.weight(1f))) }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            list.forEachIndexed { i, p -> PrayerCard(p, stateOf(i), false, lang, arabic, h24, weight, sized(Modifier.weight(1f))) }
        }
    }
}

@Composable
private fun PrayerCard(p: Prayer, state: CardState, wide: Boolean, lang: String, arabic: Boolean, h24: Boolean, weight: FontWeight, modifier: Modifier) {
    val shape = RoundedCornerShape(Settings.int("corner").dp)
    val accent = Settings.color("accentColor")
    val textC = Settings.color("textColor")
    val mutedC = Settings.color("mutedColor")
    val passed = Settings.color("passedColor")
    val nameC = if (state == CardState.PASSED) passed else if (state == CardState.NEXT) accent else mutedC
    val timeC = if (state == CardState.PASSED) passed else if (state == CardState.NEXT) accent else textC
    val bg = Settings.color("cardColor").copy(alpha = Settings.int("cardOpacity") / 100f)
    val base = modifier.clip(shape).background(bg)
    val framed = if (state == CardState.NEXT) base.border(3.dp, accent, shape) else base
    val name = Names.prayer[p.id]?.get(lang) ?: p.id
    val time = Fmt.time(p.time, h24, lang, arabic)
    val iqama = if (Settings.bool("showIqama") && p.iqama != null)
        "${Names.iqama.get(lang)} ${Fmt.time(p.iqama, h24, lang, arabic)}" else null
    val pad = Settings.int("cardPad").dp

    if (wide) {
        Row(framed.padding(pad), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = nameC, fontSize = Settings.int("nameSize").sp, fontWeight = weight)
            Column(horizontalAlignment = Alignment.End) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(time, color = timeC, fontSize = Settings.int("timeSize").sp, fontWeight = weight, maxLines = 1, softWrap = false)
                }
                if (iqama != null) Text(iqama, color = mutedC, fontSize = Settings.int("iqamaSize").sp)
            }
        }
    } else {
        Column(framed.padding(pad), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(name, color = nameC, fontSize = Settings.int("nameSize").sp, fontWeight = weight, maxLines = 1)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(time, color = timeC, fontSize = Settings.int("timeSize").sp, fontWeight = weight, maxLines = 1, softWrap = false)
            }
            if (iqama != null) Text(iqama, color = mutedC, fontSize = Settings.int("iqamaSize").sp, maxLines = 1)
        }
    }
}
