// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.core

/** A text in the three supported languages. */
data class Tri(val ar: String, val en: String, val de: String) {
    fun get(lang: String): String = when (lang) {
        "ar" -> ar
        "de" -> de
        else -> en
    }
}
