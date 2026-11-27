// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.data

import java.util.Calendar

data class NextInfo(val index: Int, val targetMs: Long, val tomorrow: Boolean)

object NextPrayer {
    fun atTime(now: Long, hhmm: String, dayOffset: Int): Long {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
    }

    /** First prayer after [now]; after the last one it is tomorrow's first (approximated with today's time). */
    fun compute(list: List<Prayer>, now: Long): NextInfo? {
        for ((i, p) in list.withIndex()) {
            val t = atTime(now, p.time, 0)
            if (t > now) return NextInfo(i, t, false)
        }
        return list.firstOrNull()?.let { NextInfo(0, atTime(now, it.time, 1), true) }
    }
}
