// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.data

import org.json.JSONArray
import org.json.JSONObject

/** [time] and [iqama] are "HH:mm" (24h). */
data class Prayer(val id: String, val time: String, val iqama: String?)

data class DayTimes(
    val date: String,              // yyyy-MM-dd
    val prayers: List<Prayer>,     // fajr, sunrise, dhuhr, asr, maghrib, isha
    val source: String,            // "mawaqit" | "aladhan"
    val mosque: String?,
    val jumua: String?,
    val fetchedAt: Long,
    val stale: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("date", date).put("source", source).put("mosque", mosque ?: JSONObject.NULL)
        .put("jumua", jumua ?: JSONObject.NULL).put("fetchedAt", fetchedAt)
        .put("prayers", JSONArray().also { a ->
            prayers.forEach { a.put(JSONObject().put("id", it.id).put("time", it.time).put("iqama", it.iqama ?: JSONObject.NULL)) }
        })

    companion object {
        fun fromJson(o: JSONObject): DayTimes {
            val arr = o.getJSONArray("prayers")
            val list = (0 until arr.length()).map {
                val p = arr.getJSONObject(it)
                Prayer(p.getString("id"), p.getString("time"), if (p.isNull("iqama")) null else p.getString("iqama"))
            }
            return DayTimes(
                o.getString("date"), list, o.getString("source"),
                if (o.isNull("mosque")) null else o.getString("mosque"),
                if (o.isNull("jumua")) null else o.getString("jumua"),
                o.optLong("fetchedAt", 0L),
            )
        }
    }
}

val PRAYER_IDS = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
