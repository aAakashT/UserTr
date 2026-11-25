package com.privatecall.browser.privacy

import android.content.Context

class PrivacyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("privacy_preferences", Context.MODE_PRIVATE)

    var javascriptEnabled: Boolean
        get() = preferences.getBoolean("javascript", true)
        set(value) = preferences.edit().putBoolean("javascript", value).apply()

    var blockThirdPartyCookies: Boolean
        get() = preferences.getBoolean("third_party_cookies", true)
        set(value) = preferences.edit().putBoolean("third_party_cookies", value).apply()

    var saveHistory: Boolean
        get() = preferences.getBoolean("history", false)
        set(value) = preferences.edit().putBoolean("history", value).apply()

    var darkMode: Boolean
        get() = preferences.getBoolean("dark_mode", true)
        set(value) = preferences.edit().putBoolean("dark_mode", value).apply()

    var searchEngineUrl: String
        get() = preferences.getString("search_engine", "https://duckduckgo.com/?q=")
            ?: "https://duckduckgo.com/?q="
        set(value) = preferences.edit().putString("search_engine", value).apply()

    var downloadsEnabled: Boolean
        get() = preferences.getBoolean("downloads", true)
        set(value) = preferences.edit().putBoolean("downloads", value).apply()
}
