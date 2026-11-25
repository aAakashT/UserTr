package com.privatecall.browser.security

object DownloadValidator {
    fun isAllowedUrl(url: String): Boolean =
        UrlValidator.isSafeHttpUrl(url)

    fun isExecutable(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".apk") ||
            lower.endsWith(".exe") ||
            lower.endsWith(".msi") ||
            lower.endsWith(".sh") ||
            lower.endsWith(".bat")
    }
}
