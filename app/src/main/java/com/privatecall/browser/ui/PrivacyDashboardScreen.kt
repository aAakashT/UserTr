package com.privatecall.browser.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.privatecall.browser.browser.BrowserViewModel
import com.privatecall.browser.privacy.CleanupRepository
import com.privatecall.browser.privacy.PrivacyPreferences
import java.util.Locale

@Composable
fun PrivacyDashboardScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val preferences = remember { PrivacyPreferences(context) }
    val cleanupRepository = remember { CleanupRepository(context) }
    val javascript = remember { mutableStateOf(preferences.javascriptEnabled) }
    val thirdParty = remember { mutableStateOf(preferences.blockThirdPartyCookies) }
    val saveHistory = remember { mutableStateOf(preferences.saveHistory) }
    val downloads = remember { mutableStateOf(preferences.downloadsEnabled) }
    val showClear = remember { mutableStateOf(false) }
    val clearPermissions = remember { mutableStateOf(false) }
    val report = remember { mutableStateOf<List<String>>(emptyList()) }

    if (showClear.value) {
        AlertDialog(
            onDismissRequest = { showClear.value = false },
            title = { Text("CLEAR ALL BROWSER DATA") },
            text = {
                Column {
                    Text(
                        "This clears browser-owned cache, cookies, storage, private sessions, " +
                            "tabs, app temporary files and app-managed history.",
                    )
                    Text(
                        "It cannot clear Android caches belonging to other apps or logs retained " +
                            "by websites, ISPs, VPN providers or remote services.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearPermissions.value,
                            onCheckedChange = { clearPermissions.value = it },
                        )
                        Text("Also clear saved site permissions")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = cleanupRepository.clearBrowserData(clearPermissions.value)
                    viewModel.tabs.clearAll()
                    report.value = result.operations.map { "${it.name}: ${it.status}" }
                    showClear.value = false
                }) { Text("Clear data") }
            },
            dismissButton = {
                TextButton(onClick = { showClear.value = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Privacy Dashboard", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow("Microphone", permissionStatus(context, Manifest.permission.RECORD_AUDIO))
                StatusRow("Camera", permissionStatus(context, Manifest.permission.CAMERA))
                StatusRow("Location", "Denied by app policy")
                StatusRow("Browser history", if (saveHistory.value) "Enabled" else "Disabled")
                StatusRow("Downloads", if (downloads.value) "User approval required" else "Disabled")
                StatusRow("Private session", if (viewModel.tabs.privateTabsCount() > 0) "Active" else "No private tabs")
                StatusRow("Browser storage", formatBytes(cleanupRepository.storageUsageBytes()))
            }
        }

        SettingSwitch("Block third-party cookies", thirdParty.value) {
            thirdParty.value = it
            preferences.blockThirdPartyCookies = it
        }
        SettingSwitch("JavaScript", javascript.value) {
            javascript.value = it
            preferences.javascriptEnabled = it
        }
        SettingSwitch("Save browsing history", saveHistory.value) {
            saveHistory.value = it
            preferences.saveHistory = it
        }
        SettingSwitch("Allow downloads", downloads.value) {
            downloads.value = it
            preferences.downloadsEnabled = it
        }

        FilledTonalButton(
            onClick = { showClear.value = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Text("CLEAR ALL BROWSER DATA")
        }

        report.value.forEach { item ->
            Text(item, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun permissionStatus(context: Context, permission: String): String =
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        "Granted"
    } else {
        "Not granted"
    }

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}
