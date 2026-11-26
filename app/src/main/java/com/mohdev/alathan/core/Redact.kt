package com.mohdev.alathan.core

/** Removes personal data and secrets from log text (GDPR: data minimisation). */
object Redact {
    private val email = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
    private val secret = Regex(
        """(bearer|token|authorization|password|passwd|secret|apikey|api_key)(["\s:=]+)[^\s",}&]+""",
        RegexOption.IGNORE_CASE
    )
    private val ip = Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""")

    fun text(s: String): String = s
        .replace(email, "<email>")
        .replace(secret) { m -> "${m.groupValues[1]}${m.groupValues[2]}<redacted>" }
        .replace(ip, "<ip>")
}
