package com.privatecall.browser.privacy

import android.content.Context
import android.webkit.PermissionRequest

class SitePermissionRepository(context: Context) {
    private val preferences = context.getSharedPreferences("site_permissions", Context.MODE_PRIVATE)

    fun isGranted(origin: String, resource: String): Boolean =
        preferences.getStringSet(origin, emptySet())?.contains(resource) == true

    fun grant(origin: String, resource: String) {
        val values = preferences.getStringSet(origin, emptySet())?.toMutableSet() ?: mutableSetOf()
        values.add(resource)
        preferences.edit().putStringSet(origin, values).apply()
    }

    fun revoke(origin: String, resource: String? = null) {
        if (resource == null) {
            preferences.edit().remove(origin).apply()
        } else {
            val values = preferences.getStringSet(origin, emptySet())?.toMutableSet() ?: mutableSetOf()
            values.remove(resource)
            if (values.isEmpty()) {
                preferences.edit().remove(origin).apply()
            } else {
                preferences.edit().putStringSet(origin, values).apply()
            }
        }
    }

    fun origins(): Set<String> = preferences.all.keys

    fun resources(origin: String): Set<String> =
        preferences.getStringSet(origin, emptySet()) ?: emptySet()

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun resourceLabel(resource: String): String = when (resource) {
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "Microphone"
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "Camera"
        else -> "Unsupported"
    }
}
