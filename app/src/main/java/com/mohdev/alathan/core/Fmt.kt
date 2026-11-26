package com.mohdev.alathan.core

import java.util.Locale

object Fmt {
    private const val AR = "٠١٢٣٤٥٦٧٨٩"

    /** Normalises any digits to western, then converts to Arabic-Indic if [arabic]. */
    fun digits(s: String, arabic: Boolean): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val idx = AR.indexOf(c)
            val d = if (idx >= 0) '0' + idx else c
            sb.append(if (arabic && d in '0'..'9') AR[d - '0'] else d)
        }
        return sb.toString()
    }

    /** "HH:mm" -> display time. */
    fun time(hhmm: String, h24: Boolean, lang: String, arabicDigits: Boolean): String {
        val p = hhmm.split(":")
        val h = p[0].toInt()
        val m = p[1].toInt()
        val s = if (h24) {
            String.format(Locale.US, "%02d:%02d", h, m)
        } else {
            val suffix = if (h < 12) (if (lang == "ar") "ص" else "AM") else (if (lang == "ar") "م" else "PM")
            val h12 = if (h % 12 == 0) 12 else h % 12
            String.format(Locale.US, "%d:%02d %s", h12, m, suffix)
        }
        return digits(s, arabicDigits)
    }

    fun hms(totalSeconds: Long, arabicDigits: Boolean): String {
        val t = totalSeconds.coerceAtLeast(0)
        val s = String.format(Locale.US, "%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)
        return digits(s, arabicDigits)
    }
}
