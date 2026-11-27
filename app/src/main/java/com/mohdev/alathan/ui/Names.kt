// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.ui

import com.mohdev.alathan.core.Tri

object Names {
    val prayer = mapOf(
        "fajr" to Tri("الفجر", "Fajr", "Fadschr"),
        "sunrise" to Tri("الشروق", "Sunrise", "Sonnenaufgang"),
        "dhuhr" to Tri("الظهر", "Dhuhr", "Dhuhr"),
        "asr" to Tri("العصر", "Asr", "Asr"),
        "maghrib" to Tri("المغرب", "Maghrib", "Maghrib"),
        "isha" to Tri("العشاء", "Isha", "Ischa"),
    )
    val next = Tri("القادمة", "Next", "Nächstes")
    val tomorrow = Tri("غداً", "tomorrow", "morgen")
    val iqama = Tri("الإقامة", "Iqama", "Iqama")
    val loading = Tri("جارٍ تحميل المواقيت…", "Loading prayer times…", "Gebetszeiten werden geladen…")
    val noData = Tri("لا توجد بيانات. تحقق من الاتصال بالإنترنت.", "No data. Check the internet connection.", "Keine Daten. Internetverbindung prüfen.")
    val hint = Tri("زر OK أو القائمة = الإعدادات", "Press OK or Menu = settings", "OK oder Menü = Einstellungen")
    val stale = Tri("بيانات قديمة", "stale data", "veraltete Daten")
    val loadingStep = Tri("تحميل الإعدادات", "Loading settings", "Einstellungen laden")
    val updateStep = Tri("فحص التحديثات", "Checking for updates", "Nach Updates suchen")
    val timesStep = Tri("جلب أوقات الصلاة", "Fetching prayer times", "Gebetszeiten abrufen")
    val update = Tri("تحديث", "Update", "Aktualisieren")
    val later = Tri("لاحقاً", "Later", "Später")
    val skip = Tri("تخطي هذا الإصدار", "Skip this version", "Version überspringen")
    val close = Tri("إغلاق", "Close", "Schließen")
    val settings = Tri("الإعدادات", "Settings", "Einstellungen")
    val reload = Tri("إعادة تحميل المواقيت", "Reload prayer times", "Gebetszeiten neu laden")
    val checkUpdate = Tri("فحص التحديث الآن", "Check for updates now", "Jetzt nach Updates suchen")
    val viewLog = Tri("عرض سجل الأخطاء", "View log", "Protokoll anzeigen")
    val clearLog = Tri("مسح السجل", "Clear log", "Protokoll löschen")
    val reset = Tri("إعادة الإعدادات الافتراضية", "Reset all settings", "Alle Einstellungen zurücksetzen")
    val upToDate = Tri("لا يوجد تحديث", "You are up to date", "Sie sind auf dem neuesten Stand")
    val done = Tri("تم", "Done", "Fertig")
    val lanOpen = Tri("افتح من متصفح الهاتف:", "Open on your phone:", "Auf dem Handy öffnen:")
    val privacy = Tri("لا يتم جمع أي بيانات. الإعدادات محفوظة على الجهاز فقط.", "No data is collected. Settings stay on this device.", "Es werden keine Daten erhoben. Einstellungen bleiben auf dem Gerät.")
}
