package com.privatecall.browser.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.privatecall.browser.audio.VoiceTestController
import com.privatecall.browser.audio.VoiceTestSettings
import com.privatecall.browser.privacy.PrivacyPreferences

@Composable
fun VoiceTestScreen() {
    val context = LocalContext.current
    val preferences = remember { PrivacyPreferences(context) }
    val controller = remember { VoiceTestController() }
    val testState by controller.state.collectAsState()
    var effectsEnabled by remember { mutableStateOf(preferences.voiceTestEffectsEnabled) }
    var noiseSuppression by remember {
        mutableStateOf(preferences.voiceTestNoiseSuppressionEnabled)
    }
    var echoCancellation by remember {
        mutableStateOf(preferences.voiceTestEchoCancellationEnabled)
    }
    var automaticGainControl by remember {
        mutableStateOf(preferences.voiceTestAutomaticGainControlEnabled)
    }
    var monitorEnabled by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(preferences.voiceTestPreset) }
    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    fun currentSettings() = VoiceTestSettings(
        effectsEnabled = effectsEnabled,
        noiseSuppressionEnabled = noiseSuppression,
        echoCancellationEnabled = echoCancellation,
        automaticGainControlEnabled = automaticGainControl,
        monitorEnabled = monitorEnabled,
        preset = preset,
    )

    fun startTest() {
        if (microphoneGranted) {
            controller.start(currentSettings())
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphoneGranted = granted
        if (granted) startTest()
    }

    DisposableEffect(controller) {
        onDispose { controller.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Voice test")
        Text("Test the microphone locally before using a browser call.")
        if (!microphoneGranted) {
            Text("Microphone permission is required to start the test.")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (testState.isRunning) "Input level" else "Microphone idle")
                Text(
                    if (testState.isRunning) {
                        "${testState.inputLevelDb.toInt()} dBFS"
                    } else {
                        "Start the test to monitor input."
                    },
                )
                LinearProgressIndicator(
                    progress = ((testState.inputLevelDb + 60f) / 60f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (testState.isRunning) {
                            controller.stop()
                        } else if (microphoneGranted) {
                            startTest()
                        } else {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (testState.isRunning) "Stop test" else "Start microphone test")
                }
            }
        }

        SwitchRow(
            label = "Voice effects",
            checked = effectsEnabled,
            onCheckedChange = {
                effectsEnabled = it
                preferences.voiceTestEffectsEnabled = it
                controller.updateSettings(currentSettings())
            },
        )
        Text(
            "Voice preset: ${
                if (preset == "Off") "Natural" else preferences.voicePresetName(preset)
            }",
        )
        PrivacyPreferences.voicePresetIds.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { presetId ->
                    Button(
                        onClick = {
                            preset = presetId
                            preferences.voiceTestPreset = presetId
                            controller.updateSettings(currentSettings())
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(preferences.voicePresetName(presetId))
                    }
                }
            }
        }
        Button(
            onClick = {
                preset = "Off"
                preferences.voiceTestPreset = "Off"
                controller.updateSettings(currentSettings())
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Natural / no preset")
        }

        SwitchRow(
            label = "Noise suppression",
            checked = noiseSuppression,
            supported = !testState.isRunning || testState.noiseSuppressionAvailable,
            onCheckedChange = {
                noiseSuppression = it
                preferences.voiceTestNoiseSuppressionEnabled = it
                controller.updateSettings(currentSettings())
            },
        )
        SwitchRow(
            label = "Echo cancellation",
            checked = echoCancellation,
            supported = !testState.isRunning || testState.echoCancellationAvailable,
            onCheckedChange = {
                echoCancellation = it
                preferences.voiceTestEchoCancellationEnabled = it
                controller.updateSettings(currentSettings())
            },
        )
        SwitchRow(
            label = "Automatic gain control",
            checked = automaticGainControl,
            supported = !testState.isRunning || testState.automaticGainControlAvailable,
            onCheckedChange = {
                automaticGainControl = it
                preferences.voiceTestAutomaticGainControlEnabled = it
                controller.updateSettings(currentSettings())
            },
        )
        SwitchRow(
            label = "Listen to processed audio",
            checked = monitorEnabled,
            onCheckedChange = {
                monitorEnabled = it
                controller.updateSettings(currentSettings())
            },
        )
        Text("Use headphones while monitoring to reduce feedback.")
        Text("Audio is processed on this device only. It is not recorded or uploaded.")
        Text(
            "The test does not alter audio in calls. Browser call processing depends on the site and device.",
        )
        testState.error?.let { Text("Audio error: $it") }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    supported: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(if (supported) label else "$label (not supported on this device)")
        Switch(
            checked = checked,
            enabled = supported,
            onCheckedChange = onCheckedChange,
        )
    }
}
