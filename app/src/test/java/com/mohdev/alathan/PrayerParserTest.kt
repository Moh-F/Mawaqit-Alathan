// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan

import com.mohdev.alathan.data.NextPrayer
import com.mohdev.alathan.data.Prayer
import com.mohdev.alathan.data.PrayerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class PrayerParserTest {
    private val html = """
        <html><script>
        let confData = {"name":"Test \"Mosque\" {x}","times":["05:30","12:45","15:50","18:20","20:00"],"shuruq":"07:40","jumua":"13:15",
        "iqamaCalendar":[{"26":["+10","+10","15","+5","21:30"]},{"1":["+1","+1","+1","+1","+1"]}]};
        let other = 1;
        </script></html>
    """.trimIndent()

    @Test fun parsesTimesAndName() {
        val d = PrayerParser.fromMawaqit(html, 1, 26, "2026-01-26", 0L)
        assertNotNull(d)
        assertEquals(listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"), d!!.prayers.map { it.id })
        assertEquals("05:30", d.prayers[0].time)
        assertEquals("07:40", d.prayers[1].time)
        assertEquals("Test \"Mosque\" {x}", d.mosque)
        assertEquals("13:15", d.jumua)
    }

    @Test fun parsesIqama() {
        val d = PrayerParser.fromMawaqit(html, 1, 26, "2026-01-26", 0L)!!
        assertEquals("05:40", d.prayers[0].iqama)   // +10
        assertEquals("12:55", d.prayers[2].iqama)   // dhuhr +10
        assertEquals("16:05", d.prayers[3].iqama)   // asr: plain number = minutes after adhan
        assertEquals("21:30", d.prayers[5].iqama)   // fixed time
        assertNull(d.prayers[1].iqama)              // sunrise has none
    }

    @Test fun missingConfDataReturnsNull() {
        assertNull(PrayerParser.fromMawaqit("<html></html>", 1, 1, "x", 0L))
    }

    @Test fun normTime() {
        assertEquals("05:03", PrayerParser.normTime("5:03"))
        assertEquals("19:45", PrayerParser.normTime("19:45 (CET)"))
        assertNull(PrayerParser.normTime("25:00"))
        assertNull(PrayerParser.normTime("abc"))
    }

    @Test fun iqamaWrapsPastMidnight() {
        assertEquals("00:10", PrayerParser.iqamaFor("23:50", "+20"))
    }

    @Test fun aladhan() {
        val json = """{"data":{"timings":{"Fajr":"05:30 (CET)","Sunrise":"07:40 (CET)","Dhuhr":"12:45 (CET)","Asr":"15:50 (CET)","Maghrib":"18:20 (CET)","Isha":"20:00 (CET)"}}}"""
        val d = PrayerParser.fromAladhan(json, "2026-01-26", 0L)!!
        assertEquals("aladhan", d.source)
        assertEquals("20:00", d.prayers[5].time)
    }

    @Test fun nextPrayerBeforeAndAfterIsha() {
        val list = listOf(Prayer("fajr", "05:30", null), Prayer("dhuhr", "12:45", null), Prayer("isha", "20:00", null))
        fun at(h: Int, m: Int) = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }.timeInMillis
        assertEquals(1, NextPrayer.compute(list, at(10, 0))!!.index)
        val after = NextPrayer.compute(list, at(21, 0))!!
        assertEquals(0, after.index)
        assertEquals(true, after.tomorrow)
    }
}
