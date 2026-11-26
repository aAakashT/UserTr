package com.privatecall.browser.browser

import androidx.lifecycle.ViewModel
import com.privatecall.browser.security.UrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel : ViewModel() {
    val tabs = TabRepository()
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()
    private val _httpWarning = MutableStateFlow(false)
    val httpWarning: StateFlow<Boolean> = _httpWarning.asStateFlow()
    private val _secure = MutableStateFlow(false)
    val secure: StateFlow<Boolean> = _secure.asStateFlow()

    fun setAddress(value: String) {
        _address.value = value
    }

    fun syncActiveTab() {
        val url = tabs.activeTab?.url.orEmpty()
        _address.value = url.takeUnless { it == "about:blank" }.orEmpty()
        _secure.value = UrlValidator.isHttps(url)
        _httpWarning.value = url.startsWith("http://")
    }

    fun navigate(value: String) {
        val normalized = UrlValidator.normalize(value)
        tabs.updateActive { it.copy(url = normalized, title = normalized, loading = true) }
        _address.value = normalized
        _secure.value = UrlValidator.isHttps(normalized)
        _httpWarning.value = normalized.startsWith("http://")
    }

    fun navigateActive(url: String) {
        tabs.updateActive { it.copy(url = url, title = url, loading = true) }
        _address.value = url
        _secure.value = UrlValidator.isHttps(url)
        _httpWarning.value = url.startsWith("http://")
    }

    fun pageStarted(tabId: Long, url: String) {
        tabs.updateTab(tabId) { it.copy(url = url, title = url, loading = true) }
        if (tabs.activeTabId.value == tabId) {
            _address.value = url.takeUnless { it == "about:blank" }.orEmpty()
            _secure.value = UrlValidator.isHttps(url)
            _httpWarning.value = url.startsWith("http://")
        }
    }

    fun pageFinished(tabId: Long, title: String?, url: String?) {
        tabs.updateTab(tabId) {
            it.copy(
                title = title?.takeIf { name -> name.isNotBlank() } ?: it.title,
                url = url ?: it.url,
                loading = false,
            )
        }
    }

    fun updateHistoryState(tabId: Long, canBack: Boolean, canForward: Boolean) {
        tabs.updateTab(tabId) { it.copy(canGoBack = canBack, canGoForward = canForward) }
    }
}
