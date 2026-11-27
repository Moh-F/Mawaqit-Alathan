// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import org.json.JSONObject
import java.util.Locale

/** Schema: ONE definition drives the TV settings panel AND the phone/browser control page. */
sealed class Field(val key: String, val group: Tri, val label: Tri)
class FInt(key: String, group: Tri, label: Tri, val def: Int, val min: Int, val max: Int, val step: Int = 1, val unit: String = "") : Field(key, group, label)
class FBool(key: String, group: Tri, label: Tri, val def: Boolean) : Field(key, group, label)
class FColor(key: String, group: Tri, label: Tri, val def: String) : Field(key, group, label)
class FChoice(key: String, group: Tri, label: Tri, val def: String, val options: List<Pair<String, Tri>>) : Field(key, group, label)
class FText(key: String, group: Tri, label: Tri, val def: String) : Field(key, group, label)

object Schema {
    private val gContent = Tri("المحتوى", "Content", "Inhalt")
    private val gLayout = Tri("التخطيط والمسافات", "Layout & spacing", "Layout & Abstände")
    private val gSizes = Tri("أحجام الخطوط", "Font sizes", "Schriftgrößen")
    private val gColors = Tri("الألوان", "Colors", "Farben")
    private val gSystem = Tri("النظام", "System", "System")

    val presets = listOf(
        "emerald" to Tri("زمردي", "Emerald", "Smaragd"),
        "night" to Tri("ليلي", "Night", "Nacht"),
        "gold" to Tri("ذهبي", "Black & gold", "Schwarz-Gold"),
        "light" to Tri("فاتح", "Light", "Hell"),
    )

    val fields: List<Field> = listOf(
        // Content
        FText("mosqueUrl", gContent, Tri("رابط المسجد على mawaqit.net", "Mosque page on mawaqit.net", "Moschee-Seite auf mawaqit.net"), ""),
        FText("city", gContent, Tri("المدينة (احتياطي)", "City (fallback)", "Stadt (Fallback)"), "Saarbrücken"),
        FText("country", gContent, Tri("الدولة (احتياطي)", "Country (fallback)", "Land (Fallback)"), "Germany"),
        FInt("method", gContent, Tri("طريقة الحساب (احتياطي)", "Calculation method (fallback)", "Berechnungsmethode (Fallback)"), 3, 0, 23),
        FBool("showSeconds", gContent, Tri("إظهار الثواني", "Show seconds", "Sekunden anzeigen"), true),
        FBool("showHijri", gContent, Tri("إظهار التاريخ الهجري", "Show Hijri date", "Hidschri-Datum anzeigen"), true),
        FBool("showSunrise", gContent, Tri("إظهار الشروق", "Show sunrise", "Sonnenaufgang anzeigen"), true),
        FBool("showIqama", gContent, Tri("إظهار الإقامة", "Show iqama", "Iqama anzeigen"), true),
        FBool("showCountdown", gContent, Tri("إظهار العد التنازلي", "Show countdown", "Countdown anzeigen"), true),
        FBool("hour24", gContent, Tri("نظام 24 ساعة", "24-hour clock", "24-Stunden-Format"), true),
        FChoice("digits", gContent, Tri("شكل الأرقام", "Digits", "Ziffern"), "western",
            listOf("western" to Tri("0 1 2 3", "0 1 2 3", "0 1 2 3"), "arabic" to Tri("٠ ١ ٢ ٣", "٠ ١ ٢ ٣", "٠ ١ ٢ ٣"))),
        FChoice("fontWeight", gContent, Tri("سُمك الخط", "Font weight", "Schriftstärke"), "medium",
            listOf("normal" to Tri("عادي", "Normal", "Normal"), "medium" to Tri("متوسط", "Medium", "Mittel"), "bold" to Tri("عريض", "Bold", "Fett"))),

        // Layout
        FChoice("layout", gLayout, Tri("شكل العرض", "Prayer layout", "Gebetszeiten-Layout"), "row",
            listOf("row" to Tri("صف أفقي", "Row", "Reihe"), "column" to Tri("قائمة عمودية", "List", "Liste"), "grid" to Tri("شبكة", "Grid", "Raster"))),
        FChoice("headerAlign", gLayout, Tri("محاذاة الساعة", "Clock alignment", "Uhr-Ausrichtung"), "center",
            listOf("start" to Tri("بداية", "Start", "Anfang"), "center" to Tri("وسط", "Center", "Mitte"), "end" to Tri("نهاية", "End", "Ende"))),
        FInt("screenPad", gLayout, Tri("هامش الشاشة (Overscan)", "Screen margin (overscan)", "Bildschirmrand (Overscan)"), 48, 0, 200, 4, "dp"),
        FInt("headerGap", gLayout, Tri("مسافة بعد الساعة", "Space below clock", "Abstand unter der Uhr"), 24, 0, 160, 4, "dp"),
        FInt("cardGap", gLayout, Tri("المسافة بين البطاقات", "Gap between cards", "Abstand zwischen Karten"), 16, 0, 80, 2, "dp"),
        FInt("cardPad", gLayout, Tri("حشوة البطاقة", "Card padding", "Karten-Innenabstand"), 20, 0, 80, 2, "dp"),
        FInt("corner", gLayout, Tri("استدارة الزوايا", "Corner radius", "Eckenradius"), 24, 0, 80, 2, "dp"),
        FInt("cardHeight", gLayout, Tri("ارتفاع البطاقة (0 = تلقائي)", "Card height (0 = auto)", "Kartenhöhe (0 = auto)"), 0, 0, 500, 10, "dp"),

        // Sizes
        FInt("clockSize", gSizes, Tri("حجم الساعة", "Clock size", "Uhrgröße"), 120, 30, 320, 4, "sp"),
        FInt("dateSize", gSizes, Tri("حجم التاريخ", "Date size", "Datumsgröße"), 28, 12, 90, 2, "sp"),
        FInt("countdownSize", gSizes, Tri("حجم العد التنازلي", "Countdown size", "Countdown-Größe"), 36, 12, 140, 2, "sp"),
        FInt("nameSize", gSizes, Tri("حجم اسم الصلاة", "Prayer name size", "Gebetsname-Größe"), 34, 12, 120, 2, "sp"),
        FInt("timeSize", gSizes, Tri("حجم وقت الصلاة", "Prayer time size", "Gebetszeit-Größe"), 56, 14, 180, 2, "sp"),
        FInt("iqamaSize", gSizes, Tri("حجم الإقامة", "Iqama size", "Iqama-Größe"), 20, 10, 90, 2, "sp"),

        // Colors
        FChoice("preset", gColors, Tri("نمط جاهز (يغيّر الألوان)", "Preset (sets the colors)", "Vorlage (setzt Farben)"), "emerald", presets),
        FColor("bgColor", gColors, Tri("لون الخلفية 1", "Background 1", "Hintergrund 1"), "#06231B"),
        FColor("bg2Color", gColors, Tri("لون الخلفية 2", "Background 2", "Hintergrund 2"), "#0B3D2E"),
        FBool("bgGradient", gColors, Tri("تدرّج الخلفية", "Background gradient", "Hintergrundverlauf"), true),
        FColor("cardColor", gColors, Tri("لون البطاقة", "Card color", "Kartenfarbe"), "#FFFFFF"),
        FInt("cardOpacity", gColors, Tri("شفافية البطاقة", "Card opacity", "Karten-Deckkraft"), 12, 0, 100, 2, "%"),
        FColor("textColor", gColors, Tri("لون النص", "Text color", "Textfarbe"), "#FFFFFF"),
        FColor("mutedColor", gColors, Tri("لون النص الثانوي", "Secondary text", "Sekundärtext"), "#9FBFB3"),
        FColor("accentColor", gColors, Tri("لون الصلاة القادمة", "Next prayer color", "Farbe nächstes Gebet"), "#E8C766"),
        FColor("passedColor", gColors, Tri("لون الصلوات الماضية", "Passed prayers color", "Farbe vergangene Gebete"), "#6F8F83"),

        // System
        FChoice("language", gSystem, Tri("اللغة", "Language", "Sprache"), "system",
            listOf("system" to Tri("لغة النظام", "System", "System"), "ar" to Tri("العربية", "العربية", "العربية"), "en" to Tri("English", "English", "English"), "de" to Tri("Deutsch", "Deutsch", "Deutsch"))),
        FBool("showRibbon", gSystem, Tri("شريط رقم الإصدار", "Version ribbon", "Versionsband"), true),
        FBool("burnIn", gSystem, Tri("حماية الشاشة (تحريك طفيف)", "Burn-in protection (tiny shift)", "Einbrennschutz (kleine Verschiebung)"), true),
        FBool("checkUpdates", gSystem, Tri("فحص التحديث عند التشغيل", "Check for updates on start", "Beim Start nach Updates suchen"), true),
        FChoice("updateMode", gSystem, Tri("نمط التحديث", "Update mode", "Update-Modus"), "prompt",
            listOf("prompt" to Tri("اسألني", "Ask me", "Nachfragen"), "auto" to Tri("نزّل تلقائياً ثم اسأل", "Auto-download, then ask", "Auto-Download, dann fragen"))),
        FBool("lanServer", gSystem, Tri("التحكم من الهاتف وسجل الأخطاء (الشبكة المحلية)", "Phone control & log viewer (LAN)", "Handy-Steuerung & Log (LAN)"), false),
    )

    private val byKey = fields.associateBy { it.key }
    fun get(key: String): Field? = byKey[key]

    fun default(f: Field): String = when (f) {
        is FInt -> f.def.toString()
        is FBool -> f.def.toString()
        is FColor -> f.def
        is FChoice -> f.def
        is FText -> f.def
    }

    /** Returns a valid value or null if the input must be rejected. */
    fun sanitize(f: Field, raw: String): String? = when (f) {
        is FInt -> raw.toIntOrNull()?.coerceIn(f.min, f.max)?.toString()
        is FBool -> if (raw == "true" || raw == "false") raw else null
        is FColor -> if (Regex("^#[0-9a-fA-F]{6}$").matches(raw)) raw.uppercase() else null
        is FChoice -> if (f.options.any { it.first == raw }) raw else null
        is FText -> raw.take(300)
    }

    /** Color palette the TV remote cycles through (any hex is possible from the phone page). */
    val palette = listOf(
        "#FFFFFF", "#000000", "#06231B", "#0B3D2E", "#0F172A", "#1E293B", "#E8C766", "#F59E0B",
        "#10B981", "#34D399", "#38BDF8", "#818CF8", "#F472B6", "#EF4444", "#9FBFB3", "#6F8F83",
    )

    val presetColors: Map<String, Map<String, String>> = mapOf(
        "emerald" to mapOf("bgColor" to "#06231B", "bg2Color" to "#0B3D2E", "cardColor" to "#FFFFFF", "textColor" to "#FFFFFF", "mutedColor" to "#9FBFB3", "accentColor" to "#E8C766", "passedColor" to "#6F8F83"),
        "night" to mapOf("bgColor" to "#0B1020", "bg2Color" to "#1B2440", "cardColor" to "#FFFFFF", "textColor" to "#F1F5F9", "mutedColor" to "#94A3B8", "accentColor" to "#38BDF8", "passedColor" to "#64748B"),
        "gold" to mapOf("bgColor" to "#000000", "bg2Color" to "#1A1405", "cardColor" to "#E8C766", "textColor" to "#F5E6B3", "mutedColor" to "#A8924A", "accentColor" to "#FFD54A", "passedColor" to "#7A6A33"),
        "light" to mapOf("bgColor" to "#F8FAFC", "bg2Color" to "#E2E8F0", "cardColor" to "#0F172A", "textColor" to "#0F172A", "mutedColor" to "#475569", "accentColor" to "#047857", "passedColor" to "#94A3B8"),
    )
}

object Settings {
    private lateinit var prefs: SharedPreferences
    val values = mutableStateMapOf<String, String>()

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        for (f in Schema.fields) {
            val stored = prefs.getString(f.key, null)
            values[f.key] = stored?.let { Schema.sanitize(f, it) } ?: Schema.default(f)
        }
    }

    fun str(key: String): String = values[key] ?: Schema.get(key)?.let { Schema.default(it) } ?: ""
    fun int(key: String): Int = str(key).toIntOrNull() ?: 0
    fun bool(key: String): Boolean = str(key) == "true"
    fun color(key: String): Color {
        val h = str(key).removePrefix("#")
        val v = h.toLongOrNull(16) ?: 0L
        return Color((0xFF000000L or v).toInt())
    }

    /** Effective UI language: ar / en / de. */
    fun lang(): String {
        val s = str("language")
        if (s == "ar" || s == "en" || s == "de") return s
        val l = Locale.getDefault().language
        return if (l == "ar" || l == "de") l else "en"
    }

    /** Validates and stores. Returns true if accepted. */
    fun set(key: String, raw: String, fromSystem: Boolean = false): Boolean {
        val f = Schema.get(key) ?: return false
        val v = Schema.sanitize(f, raw) ?: return false
        if (values[key] == v) return true
        values[key] = v
        prefs.edit().putString(key, v).apply()
        when (key) {
            "preset" -> Schema.presetColors[v]?.forEach { (k, c) -> set(k, c) }
            "language" -> if (!fromSystem) applyLocale()
        }
        return true
    }

    /** The system per-app language picker wins: mirror it into our setting without re-applying. */
    fun syncFromSystem() {
        val cur = AppCompatDelegate.getApplicationLocales()
        val l = if (cur.isEmpty) "system" else cur[0]?.language ?: "system"
        val v = if (l == "ar" || l == "en" || l == "de") l else "system"
        set("language", v, fromSystem = true)
    }

    fun applyLocale() {
        val tag = str("language")
        val list = if (tag == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(list)
    }

    fun resetAll() {
        for (f in Schema.fields) set(f.key, Schema.default(f))
    }

    fun toJson(): JSONObject = JSONObject().also { o -> Schema.fields.forEach { o.put(it.key, str(it.key)) } }

    /** Applies the keys present in [o]; unknown keys and invalid values are ignored. Returns the number applied. */
    fun applyJson(o: JSONObject): Int {
        var n = 0
        for (k in o.keys()) {
            if (set(k, o.opt(k)?.toString() ?: continue)) n++
        }
        return n
    }

    /** JSON description of the schema for the browser control page. */
    fun schemaJson(): String {
        val arr = org.json.JSONArray()
        for (f in Schema.fields) {
            val o = JSONObject()
            o.put("key", f.key)
            o.put("group", triJson(f.group))
            o.put("label", triJson(f.label))
            when (f) {
                is FInt -> { o.put("type", "int"); o.put("min", f.min); o.put("max", f.max); o.put("step", f.step); o.put("unit", f.unit) }
                is FBool -> o.put("type", "bool")
                is FColor -> o.put("type", "color")
                is FText -> o.put("type", "text")
                is FChoice -> {
                    o.put("type", "choice")
                    val opts = org.json.JSONArray()
                    f.options.forEach { (k, t) -> opts.put(JSONObject().put("value", k).put("label", triJson(t))) }
                    o.put("options", opts)
                }
            }
            arr.put(o)
        }
        return arr.toString()
    }

    private fun triJson(t: Tri) = JSONObject().put("ar", t.ar).put("en", t.en).put("de", t.de)
}
