package com.mohdev.alathan

import com.mohdev.alathan.core.Redact
import com.mohdev.alathan.core.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterRedactTest {
    private val manifest = """{"version":"1.2.0","build":12,"minBuild":10,"mandatory":false,
      "notes":{"ar":"أ","en":"e","de":"d"},
      "assets":{"android":{"url":"https://github.com/x/y/releases/download/v1.2.0/a.apk","sha256":"ABCDEF"}}}"""

    @Test fun newerBuildIsOffered() {
        val i = Updater.parse(manifest, 11)
        assertNotNull(i)
        assertEquals("1.2.0", i!!.version)
        assertEquals("abcdef", i.sha256)
        assertFalse(i.forced(11))
    }

    @Test fun sameOrOlderBuildIsIgnored() {
        assertNull(Updater.parse(manifest, 12))
        assertNull(Updater.parse(manifest, 99))
    }

    @Test fun belowMinBuildIsForced() {
        assertTrue(Updater.parse(manifest, 9)!!.forced(9))
    }

    @Test fun otherPlatformMissingAndGarbageAreNull() {
        assertNull(Updater.parse(manifest, 1, "windows"))
        assertNull(Updater.parse("not json", 1))
    }

    @Test fun redactsPersonalData() {
        val r = Redact.text("user a.b@mail.de token=abc123 ip 192.168.1.20 ok")
        assertFalse(r.contains("a.b@mail.de"))
        assertFalse(r.contains("abc123"))
        assertFalse(r.contains("192.168.1.20"))
        assertTrue(r.contains("ok"))
    }
}
