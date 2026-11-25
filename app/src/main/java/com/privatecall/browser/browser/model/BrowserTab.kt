package com.privatecall.browser.browser.model

data class BrowserTab(
    val id: Long,
    val title: String = "New tab",
    val url: String = "about:blank",
    val private: Boolean = false,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)
