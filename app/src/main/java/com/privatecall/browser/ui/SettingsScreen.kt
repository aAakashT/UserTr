package com.privatecall.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privatecall.browser.MainActivity
import com.privatecall.browser.privacy.PrivacyPreferences
import com.privatecall.browser.privacy.SitePermissionRepository
import com.privatecall.browser.audio.AudioRouter

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val privacyPreferences = remember { PrivacyPreferences(context) }
    val sitePermissions = remember { SitePermissionRepository(context) }
    val darkMode = remember { mutableStateOf(privacyPreferences.darkMode) }
    val javascript = remember { mutableStateOf(privacyPreferences.javascriptEnabled) }
    val searchEngine = remember { mutableStateOf(privacyPreferences.searchEngineUrl) }
    val origins = remember { mutableStateOf(sitePermissions.origins().toList()) }
    val audioRouter = remember { AudioRouter(context) }
    val speaker = remember { mutableStateOf(false) }
    val bluetooth = remember { mutableStateOf(false) }
    val voicePreset = remember { mutableStateOf(privacyPreferences.voiceModulationPreset) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings")

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Dark mode")
            Switch(
                checked = darkMode.value,
                onCheckedChange = {
                    darkMode.value = it
                    privacyPreferences.darkMode = it
                },
            )
        }

        Text("Search engine")
        val engines = listOf(
            "DuckDuckGo" to "https://duckduckgo.com/?q=",
            "Google" to "https://www.google.com/search?q=",
            "Brave" to "https://search.brave.com/search?q=",
        )
        engines.forEach { (name, url) ->
            FilterChip(
                selected = searchEngine.value == url,
                onClick = {
                    searchEngine.value = url
                    privacyPreferences.searchEngineUrl = url
                },
                label = { Text(name) },
            )
        }

        Text("Legitimate VoIP audio")
        Text("Transparent voice processing (applies only to new getUserMedia captures)")
        val voicePresets = listOf("Off", "Deep", "Bright", "Radio")
        voicePresets.forEach { preset ->
            FilterChip(
                selected = voicePreset.value == preset,
                onClick = {
                    voicePreset.value = preset
                    privacyPreferences.voiceModulationPreset = preset
                },
                label = { Text(preset) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Speaker")
            Switch(
                checked = speaker.value,
                onCheckedChange = {
                    speaker.value = it
                    if (it) bluetooth.value = false
                    audioRouter.routeSpeaker(it)
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Bluetooth headset")
            Switch(
                checked = bluetooth.value,
                onCheckedChange = {
                    bluetooth.value = it
                    if (it) speaker.value = false
                    audioRouter.routeBluetooth(it)
                },
            )
        }

        Text("Saved site permissions")
        if (origins.value.isEmpty()) {
            Text("No remembered permissions")
        } else {
            origins.value.forEach { origin ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(origin)
                        sitePermissions.resources(origin).forEach { resource ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(sitePermissions.resourceLabel(resource))
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        sitePermissions.revoke(origin, resource)
                                        origins.value = sitePermissions.origins().toList()
                                    },
                                    label = { Text("Revoke") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
