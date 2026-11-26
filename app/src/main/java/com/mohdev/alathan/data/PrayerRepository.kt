package com.mohdev.alathan.data

import android.content.Context
import com.mohdev.alathan.core.AppLog
import com.mohdev.alathan.core.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerRepository {
    private const val UA = "Mozilla/5.0 (Linux; Android 9; AFTMM) AppleWebKit/537.36 Chrome/110 Safari/537.36 MawaqitAlathan"

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun loadCache(ctx: Context): DayTimes? = runCatching {
        val s = ctx.getSharedPreferences("prayer_cache", Context.MODE_PRIVATE).getString("day", null) ?: return null
        DayTimes.fromJson(JSONObject(s))
    }.getOrNull()

    private fun saveCache(ctx: Context, d: DayTimes) {
        ctx.getSharedPreferences("prayer_cache", Context.MODE_PRIVATE).edit().putString("day", d.toJson().toString()).apply()
    }

    /** Only mawaqit.net mosque pages are accepted (no arbitrary hosts). */
    fun validMosqueUrl(u: String): Boolean = runCatching {
        val url = URL(u.trim())
        url.protocol == "https" && (url.host == "mawaqit.net" || url.host == "www.mawaqit.net")
    }.getOrDefault(false)

    private fun httpGet(url: String, timeoutMs: Int = 10_000): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept-Language", "en,ar;q=0.8,de;q=0.6")
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            val buf = StringBuilder()
            c.inputStream.bufferedReader(Charsets.UTF_8).use { r ->
                val chunk = CharArray(8192)
                while (true) {
                    val n = r.read(chunk)
                    if (n < 0) break
                    buf.append(chunk, 0, n)
                    if (buf.length > 4_000_000) throw IllegalStateException("response too large")
                }
            }
            return buf.toString()
        } finally {
            c.disconnect()
        }
    }

    /** Fetches today's times: mawaqit.net page first, calculated times (aladhan) as fallback, then cache. */
    suspend fun load(ctx: Context): DayTimes? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val date = today()
        val mosque = Settings.str("mosqueUrl").trim()

        if (mosque.isNotEmpty()) {
            if (!validMosqueUrl(mosque)) {
                AppLog.w("times", "Mosque URL rejected (must be https://mawaqit.net/...)")
            } else runCatching {
                val html = httpGet(mosque)
                PrayerParser.fromMawaqit(html, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), date, now)
                    ?: throw IllegalStateException("confData not found in page")
            }.onSuccess {
                saveCache(ctx, it); AppLog.i("times", "Loaded from mawaqit.net"); return@withContext it
            }.onFailure { AppLog.e("times", "mawaqit.net failed: ${it.message}", it) }
        }

        runCatching {
            val city = URLEncoder.encode(Settings.str("city"), "UTF-8")
            val country = URLEncoder.encode(Settings.str("country"), "UTF-8")
            val method = Settings.int("method")
            val json = httpGet("https://api.aladhan.com/v1/timingsByCity?city=$city&country=$country&method=$method")
            PrayerParser.fromAladhan(json, date, now) ?: throw IllegalStateException("bad aladhan response")
        }.onSuccess {
            saveCache(ctx, it); AppLog.i("times", "Loaded calculated times (aladhan)"); return@withContext it
        }.onFailure { AppLog.e("times", "aladhan failed: ${it.message}", it) }

        loadCache(ctx)?.copy(stale = true)
    }
}
