package com.privatecall.browser.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun normalizesDomainInput() {
        assertEquals("https://example.com", UrlValidator.normalize("example.com"))
    }

    @Test
    fun rejectsPrivateNetworkAddresses() {
        assertFalse(UrlValidator.isSafeHttpUrl("http://localhost:8080"))
        assertFalse(UrlValidator.isSafeHttpUrl("http://127.0.0.1"))
        assertFalse(UrlValidator.isSafeHttpUrl("http://192.168.1.1"))
    }

    @Test
    fun extractsHttpsOrigin() {
        assertEquals("https://example.com", UrlValidator.origin("https://example.com/path?q=1"))
        assertFalse(UrlValidator.isHttps("http://example.com"))
    }
}
