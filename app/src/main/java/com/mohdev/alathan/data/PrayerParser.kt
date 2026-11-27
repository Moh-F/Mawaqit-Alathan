// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.data

import org.json.JSONArray
import org.json.JSONObject

/** Pure parsing functions (unit tested, no Android dependencies). */
object PrayerParser {

    /** "5:3", "05:30", "5:30 (CEST)" -> "05:30"; null if not a time. */
    fun normTime(s: String?): String? {
        val m = Regex("""(\d{1,2}):(\d{2})""").find(s ?: return null) ?: return null
        val h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].toInt()
        if (h > 23 || mi > 59) return null
        return "%02d:%02d".format(h, mi)
    }

    private fun addMinutes(hhmm: String, plus: Int): String {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        val total = ((h * 60 + m + plus) % 1440 + 1440) % 1440
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Iqama value like "+10", "10" (minutes after the adhan) or "21:30" (fixed time). */
    fun iqamaFor(prayerTime: String, v: String?): String? {
        val t = v?.trim().orEmpty()
        if (t.isEmpty()) return null
        if (t.contains(":")) return normTime(t)
        val n = t.removePrefix("+").toIntOrNull() ?: return null
        return addMinutes(prayerTime, n)
    }

    /** Extracts the `confData = { ... }` object embedded in a mawaqit.net mosque page. */
    fun extractConfData(html: String): JSONObject? {
        val idx = html.indexOf("confData")
        if (idx < 0) return null
        val start = html.indexOf('{', idx)
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until html.length) {
            val c = html[i]
            if (inStr) {
                if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
            } else when (c) {
                '"' -> inStr = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return runCatching { JSONObject(html.substring(start, i + 1)) }.getOrNull()
                }
            }
        }
        return null
    }

    fun fromMawaqit(html: String, month: Int, day: Int, date: String, now: Long): DayTimes? {
        val conf = extractConfData(html) ?: return null
        val times = conf.optJSONArray("times") ?: return null
        if (times.length() < 5) return null
        val five = (0 until 5).map { normTime(times.optString(it)) ?: return null }
        val sunrise = normTime(conf.optString("shuruq", "")) ?: return null

        // Iqama (best effort; the structure differs between mosques)
        val iqRow: JSONArray? = runCatching {
            val cal = conf.optJSONArray("iqamaCalendar")
            cal?.optJSONObject(month - 1)?.optJSONArray(day.toString())
        }.getOrNull()

        fun iqAt(i: Int): String? = iqRow?.optString(i)?.let { iqamaFor(five[i], it) }

        val prayers = listOf(
            Prayer("fajr", five[0], iqAt(0)),
            Prayer("sunrise", sunrise, null),
            Prayer("dhuhr", five[1], iqAt(1)),
            Prayer("asr", five[2], iqAt(2)),
            Prayer("maghrib", five[3], iqAt(3)),
            Prayer("isha", five[4], iqAt(4)),
        )
        val name = conf.optString("name", "").ifBlank { conf.optString("label", "") }.ifBlank { null }
        val jumua = normTime(conf.optString("jumua", ""))
        return DayTimes(date, prayers, "mawaqit", name, jumua, now)
    }

    fun fromAladhan(json: String, date: String, now: Long): DayTimes? = runCatching {
        val t = JSONObject(json).getJSONObject("data").getJSONObject("timings")
        fun g(k: String) = normTime(t.getString(k))!!
        DayTimes(
            date,
            listOf(
                Prayer("fajr", g("Fajr"), null), Prayer("sunrise", g("Sunrise"), null),
                Prayer("dhuhr", g("Dhuhr"), null), Prayer("asr", g("Asr"), null),
                Prayer("maghrib", g("Maghrib"), null), Prayer("isha", g("Isha"), null),
            ),
            "aladhan", null, null, now,
        )
    }.getOrNull()
}
