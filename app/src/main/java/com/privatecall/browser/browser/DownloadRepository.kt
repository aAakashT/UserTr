package com.privatecall.browser.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DownloadItem(
    val name: String,
    val url: String,
)

class DownloadRepository(context: Context) {
    private val preferences = context.getSharedPreferences("download_history", Context.MODE_PRIVATE)

    fun items(): List<DownloadItem> {
        val json = preferences.getString("items", "[]") ?: "[]"
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(DownloadItem(item.getString("name"), item.getString("url")))
            }
        }
    }

    fun addCompleted(name: String, url: String) {
        val updated = listOf(DownloadItem(name, url)) + items()
        val array = JSONArray()
        updated.take(100).forEach { item ->
            array.put(
                JSONObject()
                    .put("name", item.name)
                    .put("url", item.url),
            )
        }
        preferences.edit().putString("items", array.toString()).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
