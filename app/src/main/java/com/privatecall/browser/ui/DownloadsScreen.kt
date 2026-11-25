package com.privatecall.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privatecall.browser.MainActivity

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val repository = remember(context) {
        (context as? MainActivity)?.downloadRepository()
    }
    val items = remember { mutableStateOf(repository?.items().orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall)
        FilledTonalButton(
            onClick = {
                repository?.clear()
                items.value = emptyList()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Clear download history") }

        if (items.value.isEmpty()) {
            Text("No saved download history")
        } else {
            items.value.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(item.url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
