package com.privatecall.browser.security

import java.net.URI

object UrlValidator {
    fun normalize(input: String, searchTemplate: String = "https://duckduckgo.com/?q="): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://duckduckgo.com"

        val direct = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return if (isSafeHttpUrl(direct)) direct else searchTemplate + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    fun isSafeHttpUrl(input: String): Boolean {
        val uri = runCatching { URI(input) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.trim()?.lowercase() ?: return false
        if (host == "localhost" || host.endsWith(".localhost")) return false
        if (host == "127.0.0.1" || host.startsWith("127.") || host == "::1") return false
        if (host.startsWith("169.254.") || host.startsWith("0.") || host.endsWith(".local")) return false
        if (host.startsWith("10.") || host.startsWith("192.168.") ||
            host.matches(Regex("""^172\.(1[6-9]|2\d|3[01])\."""))) return false
        return uri.rawUserInfo == null
    }

    fun isHttps(input: String): Boolean =
        runCatching { URI(input).scheme?.lowercase() == "https" }.getOrDefault(false)

    fun origin(input: String): String? {
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        val host = uri.host?.lowercase() ?: return null
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        return "$scheme://$host$port"
    }
}
