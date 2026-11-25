package com.privatecall.browser.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadValidatorTest {
    @Test
    fun allowsHttpsDownloads() {
        assertTrue(DownloadValidator.isAllowedUrl("https://example.com/file.pdf"))
    }

    @Test
    fun rejectsPrivateAndExecutableSources() {
        assertFalse(DownloadValidator.isAllowedUrl("http://localhost/file.pdf"))
        assertFalse(DownloadValidator.isAllowedUrl("http://127.0.0.1/file.pdf"))
        assertTrue(DownloadValidator.isExecutable("installer.apk"))
        assertTrue(DownloadValidator.isExecutable("payload.exe"))
        assertFalse(DownloadValidator.isExecutable("document.pdf"))
    }
}
