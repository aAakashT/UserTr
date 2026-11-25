package com.privatecall.browser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.privatecall.browser.browser.BrowserViewModel

private data class MainDestination(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun PrivateCallBrowserApp(viewModel: BrowserViewModel) {
    val destinations = listOf(
        MainDestination("Browser", Icons.Default.Public),
        MainDestination("Private", Icons.Default.Lock),
        MainDestination("Privacy", Icons.Default.Security),
        MainDestination("Downloads", Icons.Default.Download),
        MainDestination("Settings", Icons.Default.Settings),
    )
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = {
                            selected = index
                            if (index == 1) viewModel.tabs.setPrivateSessionEnabled(true)
                            if (index == 0) viewModel.tabs.setPrivateSessionEnabled(false)
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selected) {
                0 -> BrowserScreen(viewModel, privateSession = false)
                1 -> BrowserScreen(viewModel, privateSession = true)
                2 -> PrivacyDashboardScreen(viewModel)
                3 -> DownloadsScreen()
                4 -> SettingsScreen()
            }
        }
    }
}
