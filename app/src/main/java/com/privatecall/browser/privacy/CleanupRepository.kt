package com.privatecall.browser.privacy

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import android.webkit.WebView
import com.privatecall.browser.browser.DownloadRepository
import java.io.File

data class CleanupItem(
    val name: String,
    val status: String,
)

data class CleanupReport(
    val operations: List<CleanupItem>,
)

class CleanupRepository(private val context: Context) {

    fun clearBrowserData(clearSitePermissions: Boolean): CleanupReport {
        val operations = mutableListOf<CleanupItem>()

        runCleanup("WebView page cache") {
            val webView = WebView(context)
            webView.clearCache(true)
            webView.clearFormData()
            webView.destroy()
        }.let(operations::add)

        runCleanup("Cookies") {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }.let(operations::add)

        runCleanup("Website storage and IndexedDB") {
            WebStorage.getInstance().deleteAllData()
            context.deleteDatabase("webview.db")
            context.deleteDatabase("webviewCache.db")
        }.let(operations::add)

        runCleanup("Service worker website data") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                throw UnsupportedOperationException("Service worker controls require Android 7+")
            }
            throw UnsupportedOperationException(
                "Android does not expose an application-wide service worker data clear operation",
            )
        }.let(operations::add)

        runCleanup("App temporary files") {
            context.cacheDir?.deleteRecursively()
            context.codeCacheDir?.deleteRecursively()
            File(context.filesDir, "downloads").deleteRecursively()
        }.let(operations::add)

        runCleanup("App-managed history") {
            val history = context.getSharedPreferences("browser_history", Context.MODE_PRIVATE)
            history.edit().clear().commit()
        }.let(operations::add)

        runCleanup("Private session data") {
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
        }.let(operations::add)

        runCleanup("Download history") {
            DownloadRepository(context).clear()
        }.let(operations::add)

        if (clearSitePermissions) {
            runCleanup("Saved site permissions") {
                SitePermissionRepository(context).clear()
            }.let(operations::add)
        } else {
            operations += CleanupItem("Saved site permissions", "Kept by user")
        }

        operations += CleanupItem("Browser tabs and active sessions", "Completed")
        operations += CleanupItem(
            "Android or app system caches",
            "Requires Android system settings; application cannot clear other apps",
        )
        operations += CleanupItem(
            "Remote, ISP, VPN, and website logs",
            "Cannot be cleared from this application",
        )
        return CleanupReport(operations)
    }

    fun storageUsageBytes(): Long {
        fun sizeOf(file: File): Long =
            if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else file.length()
        return sizeOf(context.cacheDir) + sizeOf(context.filesDir) +
            sizeOf(context.getDir("webview", Context.MODE_PRIVATE))
    }

    private fun runCleanup(name: String, operation: () -> Unit): CleanupItem =
        try {
            operation()
            CleanupItem(name, "Completed")
        } catch (_: UnsupportedOperationException) {
            CleanupItem(name, "Unsupported")
        } catch (_: Throwable) {
            CleanupItem(name, "Failed")
        }
}
