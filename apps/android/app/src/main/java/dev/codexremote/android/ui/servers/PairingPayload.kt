package dev.codexremote.android.ui.servers

import android.net.Uri

internal data class ParsedPairingInput(
    val pairingCode: String,
    val pairingMethod: String? = null,
    val trustLabel: String? = null,
    val baseUrl: String? = null,
)

internal fun parsePairingInput(raw: String): ParsedPairingInput? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null

    val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
    if (uri != null && uri.scheme?.startsWith("codex") == true) {
        val code = uri.getQueryParameter("code")
            ?: uri.getQueryParameter("pairingCode")
            ?: uri.getQueryParameter("token")
            ?: uri.lastPathSegment
            ?: trimmed
        val method = uri.getQueryParameter("method") ?: if (uri.queryParameterNames.isNotEmpty()) {
            "qr"
        } else {
            "manual-code"
        }
        return ParsedPairingInput(
            pairingCode = code,
            pairingMethod = method,
            trustLabel = uri.getQueryParameter("label") ?: uri.getQueryParameter("trustLabel"),
            baseUrl = uri.getQueryParameter("baseUrl") ?: uri.getQueryParameter("api"),
        )
    }

    return ParsedPairingInput(
        pairingCode = trimmed,
        pairingMethod = if (trimmed.contains("://")) "qr" else "manual-code",
    )
}
