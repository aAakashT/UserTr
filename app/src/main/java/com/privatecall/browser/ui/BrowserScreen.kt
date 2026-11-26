package com.privatecall.browser.ui

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.privatecall.browser.browser.BrowserViewModel
import com.privatecall.browser.MainActivity
import com.privatecall.browser.security.UrlValidator
import com.privatecall.browser.security.DownloadValidator
import com.privatecall.browser.privacy.PrivacyPreferences
import com.privatecall.browser.audio.AudioRouter

private fun stopMediaCapture(webView: WebView?, reload: Boolean = true) {
    webView?.evaluateJavascript(
        """
        (function () {
          try {
            if (window.__privateCallStopMedia) {
              window.__privateCallStopMedia();
            }
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
              navigator.mediaDevices.enumerateDevices = navigator.mediaDevices.enumerateDevices;
            }
          } catch (e) {}
        })();
        """.trimIndent(),
        null,
    )
    if (reload) webView?.reload()
}

private fun voiceProcessingScript(preset: String, sampleBrightnessDb: Float): String {
    val safeBrightnessDb = sampleBrightnessDb.takeIf { it.isFinite() }?.coerceIn(-6f, 6f) ?: 0f
    return """
    (function () {
      if (!navigator.mediaDevices ||
        typeof navigator.mediaDevices.getUserMedia !== "function") return;
      window.__privateCallVoicePreset = "$preset";
      window.__privateCallVoiceBrightnessDb = $safeBrightnessDb;
      if (navigator.mediaDevices.__privateCallWrapped) {
        if (window.__privateCallSetVoiceToneProfile) {
          window.__privateCallSetVoiceToneProfile($safeBrightnessDb);
        }
        return;
      }
      navigator.mediaDevices.__privateCallWrapped = true;
      var streams = [];
      var tonePipelines = [];
      window.__privateCallSetVoiceToneProfile = function (brightnessDb) {
        var gain = Math.max(-6, Math.min(6, Number(brightnessDb) || 0));
        tonePipelines.forEach(function (pipeline) {
          try {
          pipeline.filter.gain.setTargetAtTime(gain, pipeline.context.currentTime, 0.04);
          } catch (e) {}
        });
      };
      window.__privateCallStopMedia = function () {
        streams.forEach(function (stream) {
          stream.getTracks().forEach(function (track) { try { track.stop(); } catch (e) {} });
        });
        streams = [];
        tonePipelines.forEach(function (pipeline) {
          try { pipeline.context.close(); } catch (e) {}
        });
        tonePipelines = [];
      };
      var originalGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
      navigator.mediaDevices.getUserMedia = function (constraints) {
        return originalGetUserMedia(constraints).then(function (stream) {
          streams.push(stream);
          var preset = window.__privateCallVoicePreset || "Off";
          var brightnessDb = Number(window.__privateCallVoiceBrightnessDb) || 0;
          if (!constraints || !constraints.audio ||
            (preset === "Off" && Math.abs(brightnessDb) < 0.05)) return stream;
          try {
            var context = new (window.AudioContext || window.webkitAudioContext)();
            var source = context.createMediaStreamSource(stream);
            var gainNode = context.createGain();
          var toneFilter = context.createBiquadFilter();
          toneFilter.type = "highshelf";
          toneFilter.frequency.value = 3200;
          toneFilter.gain.value = brightnessDb;
          tonePipelines.push({ context: context, filter: toneFilter });
          gainNode.gain.value =
            preset.indexOf("deep") >= 0 ? 0.95 :
            preset.indexOf("bright") >= 0 ? 1.12 :
            preset.indexOf("radio") >= 0 ? 0.85 :
            preset.indexOf("bass") >= 0 ? 1.08 :
            preset.indexOf("telephone") >= 0 ? 0.80 :
            1.0;
            var output = context.createMediaStreamDestination();
            function connectProcessed(node) {
              node.connect(toneFilter);
              toneFilter.connect(gainNode);
              gainNode.connect(output);
            }
          if (preset.indexOf("deep") >= 0) {
              var f = context.createBiquadFilter(); f.type = "lowpass"; f.frequency.value = 1150;
              source.connect(f); connectProcessed(f);
          } else if (preset.indexOf("bright") >= 0) {
              var f2 = context.createBiquadFilter(); f2.type = "highpass"; f2.frequency.value = 180;
              source.connect(f2); connectProcessed(f2);
          } else if (preset.indexOf("radio") >= 0) {
              var low = context.createBiquadFilter(); low.type = "highpass"; low.frequency.value = 300;
              var high = context.createBiquadFilter(); high.type = "lowpass"; high.frequency.value = 3400;
              source.connect(low); low.connect(high); connectProcessed(high);
          } else if (preset.indexOf("warm") >= 0) {
            var warm = context.createBiquadFilter(); warm.type = "lowpass"; warm.frequency.value = 2400;
            var warmHigh = context.createBiquadFilter(); warmHigh.type = "highshelf"; warmHigh.frequency.value = 300; warmHigh.gain.value = -2;
            source.connect(warm); warm.connect(warmHigh); connectProcessed(warmHigh);
          } else if (preset.indexOf("cinematic") >= 0) {
            var cineLow = context.createBiquadFilter(); cineLow.type = "highpass"; cineLow.frequency.value = 80;
            var cineHigh = context.createBiquadFilter(); cineHigh.type = "lowpass"; cineHigh.frequency.value = 7000;
            source.connect(cineLow); cineLow.connect(cineHigh); connectProcessed(cineHigh);
          } else if (preset.indexOf("podcast") >= 0) {
            var podLow = context.createBiquadFilter(); podLow.type = "highpass"; podLow.frequency.value = 100;
            var podPresence = context.createBiquadFilter(); podPresence.type = "peaking"; podPresence.frequency.value = 2200; podPresence.Q.value = 1; podPresence.gain.value = 2;
            source.connect(podLow); podLow.connect(podPresence); connectProcessed(podPresence);
          } else if (preset.indexOf("bass") >= 0) {
            var bass = context.createBiquadFilter(); bass.type = "lowshelf"; bass.frequency.value = 180; bass.gain.value = 5;
            var bassCut = context.createBiquadFilter(); bassCut.type = "lowpass"; bassCut.frequency.value = 5200;
            source.connect(bass); bass.connect(bassCut); connectProcessed(bassCut);
          } else if (preset.indexOf("crisp") >= 0) {
            var crisp = context.createBiquadFilter(); crisp.type = "peaking"; crisp.frequency.value = 3200; crisp.Q.value = 1.2; crisp.gain.value = 4;
            source.connect(crisp); connectProcessed(crisp);
          } else if (preset.indexOf("telephone") >= 0) {
            var telLow = context.createBiquadFilter(); telLow.type = "highpass"; telLow.frequency.value = 350;
            var telHigh = context.createBiquadFilter(); telHigh.type = "lowpass"; telHigh.frequency.value = 3200;
            source.connect(telLow); telLow.connect(telHigh); connectProcessed(telHigh);
          } else if (preset.indexOf("robot") >= 0) {
            var ringMod = context.createGain();
            ringMod.gain.value = 0;
            var modulator = context.createOscillator();
            modulator.frequency.value = 35;
            modulator.connect(ringMod.gain);
            modulator.start();
            source.connect(ringMod);
            connectProcessed(ringMod);
          } else if (preset.indexOf("distorted") >= 0) {
            var shaper = context.createWaveShaper();
            var curve = new Float32Array(4096);
            for (var i = 0; i < curve.length; i++) {
              var x = (i * 2) / (curve.length - 1) - 1;
              curve[i] = Math.tanh(x * 4) / Math.tanh(4);
            }
            shaper.curve = curve;
            shaper.oversample = "4x";
            source.connect(shaper);
            connectProcessed(shaper);
          } else if (preset.indexOf("echo") >= 0) {
            var dry = context.createGain(); dry.gain.value = 0.78;
            var delay = context.createDelay(1); delay.delayTime.value = 0.24;
            var feedback = context.createGain(); feedback.gain.value = 0.28;
            var wet = context.createGain(); wet.gain.value = 0.42;
            source.connect(dry); connectProcessed(dry);
            source.connect(delay); delay.connect(feedback); feedback.connect(delay);
            delay.connect(wet); connectProcessed(wet);
            } else {
              connectProcessed(source);
            }
            if (context.state === "suspended") context.resume();
            var processed = output.stream.getAudioTracks()[0];
            var tracks = stream.getVideoTracks().concat(processed ? [processed] : []);
            var combined = new MediaStream(tracks);
            streams.push(combined);
            return combined;
          } catch (e) {
            stream.getTracks().forEach(function (track) { try { track.stop(); } catch (_) {} });
            throw e;
          }
        });
      };
    })();
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(viewModel: BrowserViewModel, privateSession: Boolean) {
    val tabs by viewModel.tabs.tabs.collectAsState()
    val activeId by viewModel.tabs.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId }
    val address by viewModel.address.collectAsState()
    val secure by viewModel.secure.collectAsState()
    val httpWarning by viewModel.httpWarning.collectAsState()
    var showTabs by remember { mutableStateOf(false) }
    val webViews = remember { mutableStateMapOf<Long, WebView>() }
    val activeWebView = webViews[activeId]
    val context = LocalContext.current
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingPermissionOrigin by remember { mutableStateOf<String?>(null) }
    var rememberPermission by remember { mutableStateOf(false) }
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    var pendingDownloadName by remember { mutableStateOf("") }
    var microphoneActive by remember { mutableStateOf(false) }
    var cameraActive by remember { mutableStateOf(false) }
    val voicePreferences = remember { PrivacyPreferences(context) }
    var voiceModulationEnabled by remember {
        mutableStateOf(voicePreferences.voiceModulationEnabled)
    }
    val voicePreset = if (voiceModulationEnabled) {
        voicePreferences.voiceModulationPreset
    } else {
        "Off"
    }
    var voiceSampleName by remember { mutableStateOf(voicePreferences.voiceSampleName) }
    var voiceSampleBrightnessDb by remember {
        mutableStateOf(
            if (voiceModulationEnabled) voicePreferences.voiceSampleBrightnessDb else 0f,
        )
    }
    val currentVoiceProcessingScript by rememberUpdatedState(
        voiceProcessingScript(voicePreset, voiceSampleBrightnessDb),
    )
    val voiceScriptHandlers = remember { mutableMapOf<Long, ScriptHandler>() }
    val voiceScriptContents = remember { mutableMapOf<Long, String>() }
    fun updateVoiceDocumentStartScript(webView: WebView, tabId: Long, script: String) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) ||
            voiceScriptContents[tabId] == script
        ) {
            return
        }
        voiceScriptHandlers.remove(tabId)?.remove()
        voiceScriptHandlers[tabId] = WebViewCompat.addDocumentStartJavaScript(
            webView,
            script,
            setOf("https://*"),
        )
        voiceScriptContents[tabId] = script
    }
    var voiceSampleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeId) {
        viewModel.syncActiveTab()
    }

    val tabIds = tabs.map { it.id }
    LaunchedEffect(tabIds) {
        val removedIds = webViews.keys.filterNot { it in tabIds }
        removedIds.forEach { id ->
            voiceScriptHandlers.remove(id)?.remove()
            voiceScriptContents.remove(id)
            webViews.remove(id)?.let { webView ->
                stopMediaCapture(webView, reload = false)
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    DisposableEffect(webViews) {
        onDispose {
            voiceScriptHandlers.values.toList().forEach { it.remove() }
            voiceScriptHandlers.clear()
            voiceScriptContents.clear()
            webViews.values.toList().forEach { webView ->
                stopMediaCapture(webView, reload = false)
                webView.stopLoading()
                webView.destroy()
            }
            webViews.clear()
        }
    }

    pendingDownloadUrl?.let { downloadUrl ->
        val executable = DownloadValidator.isExecutable(pendingDownloadName)
        AlertDialog(
            onDismissRequest = { pendingDownloadUrl = null },
            title = { Text("Download file?") },
            text = {
                Column {
                    Text(pendingDownloadName.ifBlank { "Unnamed download" })
                    if (executable) {
                        Text(
                            "Warning: executable or APK downloads can run code. Install only if you trust the source.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (context is MainActivity) {
                        context.startUserApprovedDownload(downloadUrl, pendingDownloadName)
                    }
                    pendingDownloadUrl = null
                }) { Text("Choose destination") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownloadUrl = null }) { Text("Cancel") }
            },
        )
    }

    pendingPermissionRequest?.let { request ->
        val origin = pendingPermissionOrigin
        AlertDialog(
            onDismissRequest = {
                request.deny()
                pendingPermissionRequest = null
            },
            title = { Text("Allow site access?") },
            text = {
                Column {
                    Text("Requested origin:")
                    Text(origin ?: "Untrusted origin")
                    Text("Requested: ${request.resources.joinToString { resource ->
                        when (resource) {
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "microphone"
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "camera"
                            else -> "unsupported resource"
                        }
                    }}")
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(
                            checked = rememberPermission,
                            onCheckedChange = { rememberPermission = it },
                        )
                        Text(
                            text = "Remember for this origin",
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (origin != null && context is MainActivity) {
                        context.requestMediaPermissions(
                            request,
                            origin,
                            rememberPermission,
                        ) { granted ->
                            microphoneActive =
                                granted.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                            cameraActive =
                                granted.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                        }
                    } else {
                        request.deny()
                    }
                    pendingPermissionRequest = null
                    rememberPermission = false
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    request.deny()
                    pendingPermissionRequest = null
                    rememberPermission = false
                }) { Text("Deny") }
            },
        )
    }

    if (privateSession) {
        if (activeTab?.private != true) viewModel.tabs.setPrivateSessionEnabled(true)
    } else if (activeTab?.private == true) {
        viewModel.tabs.setPrivateSessionEnabled(false)
    }

    if (showTabs) {
        AlertDialog(
            onDismissRequest = { showTabs = false },
            title = { Text("${tabs.size} tabs") },
            text = {
                Column {
                    tabs.forEach { tab ->
                        TextButton(onClick = {
                            viewModel.tabs.selectTab(tab.id)
                            showTabs = false
                        }) {
                            Text(if (tab.title.isBlank()) tab.url else tab.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tabs.openTab(privateSession)
                    showTabs = false
                }) { Text("New tab") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = { activeWebView?.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = { activeWebView?.goForward() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
            }
            OutlinedTextField(
                value = address,
                onValueChange = viewModel::setAddress,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(if (secure) "Secure address" else "Address or search") },
                leadingIcon = {
                    Icon(
                        if (secure) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (secure) "HTTPS" else "Connection",
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { viewModel.navigate(address) }),
            )
            IconButton(onClick = { activeWebView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload")
            }
            IconButton(onClick = { activeWebView?.stopLoading() }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
        }

        if (httpWarning) {
            Text(
                text = "Warning: this page uses unencrypted HTTP",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = androidx.compose.ui.graphics.Color(0xFFFFB300),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            IconButton(onClick = { viewModel.tabs.openTab(privateSession) }) {
                Icon(Icons.Default.Add, contentDescription = "New tab")
            }
            IconButton(onClick = { showTabs = true }) {
                Icon(Icons.Default.Tab, contentDescription = "Tab switcher")
            }
            IconButton(onClick = {
                stopMediaCapture(activeWebView, reload = false)
                val audioRouter = AudioRouter(context)
                audioRouter.routeBluetooth(false)
                audioRouter.routeSpeaker(false)
                audioRouter.releaseCallAudioFocus()
                val wasPrivate = viewModel.tabs.closeActive()
                if (wasPrivate) {
                    CookieManager.getInstance().removeSessionCookies(null)
                    CookieManager.getInstance().flush()
                }
                microphoneActive = false
                cameraActive = false
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close active tab")
            }
            TextButton(onClick = {
                stopMediaCapture(activeWebView)
                val audioRouter = AudioRouter(context)
                audioRouter.routeBluetooth(false)
                audioRouter.routeSpeaker(false)
                audioRouter.releaseCallAudioFocus()
                microphoneActive = false
                cameraActive = false
            }) {
                Text(if (microphoneActive || cameraActive) "Stop mic/camera" else "Mic/camera idle")
            }
            TextButton(onClick = {
                val activity = context as? MainActivity
                if (activity == null) {
                    voiceSampleError = "Voice samples can only be selected in the browser."
                } else {
                    activity.chooseVoiceSample { error ->
                        voiceSampleError = error
                        if (error == null) {
                            voiceSampleName = voicePreferences.voiceSampleName
                            voiceModulationEnabled = true
                            voicePreferences.voiceModulationEnabled = true
                            voiceSampleBrightnessDb = if (voiceModulationEnabled) {
                                voicePreferences.voiceSampleBrightnessDb
                            } else {
                                0f
                            }
                        }
                    }
                }
            }) {
                Text(if (voiceSampleName.isBlank()) "Use voice sample" else "Sample active")
            }
            TextButton(onClick = {
                val enabled = !voiceModulationEnabled
                voiceModulationEnabled = enabled
                voicePreferences.voiceModulationEnabled = enabled
                voiceSampleBrightnessDb = if (enabled) {
                    voicePreferences.voiceSampleBrightnessDb
                } else {
                    0f
                }
                val preset = if (enabled) voicePreferences.voiceModulationPreset else "Off"
                val script = voiceProcessingScript(
                    preset,
                    if (enabled) voicePreferences.voiceSampleBrightnessDb else 0f,
                )
                activeWebView?.let { updateVoiceDocumentStartScript(it, activeId, script) }
                activeWebView?.evaluateJavascript(script, null)
                stopMediaCapture(activeWebView)
            }) {
                Text(if (voiceModulationEnabled) "Voice filter on" else "Voice filter off")
            }
            if (voiceSampleName.isNotBlank()) {
                TextButton(onClick = {
                    voiceSampleName = ""
                    voiceSampleBrightnessDb = 0f
                    voicePreferences.voiceSampleName = ""
                    voicePreferences.voiceSampleBrightnessDb = 0f
                }) {
                    Text("Clear sample")
                }
            }
        }
        voiceSampleError?.let { Text("Voice sample error: $it") }
        if (voiceSampleName.isNotBlank()) {
            Text(
                "Sample EQ profile: $voiceSampleName. Applies to new microphone captures in this browser; " +
                    "it shapes tone, not voice identity or pitch. Re-enable the mic after toggling.",
            )
        }

        if (activeTab?.loading == true) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            tabs.forEach { tab ->
                key(tab.id) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            WebView(viewContext).apply {
                                webViews[tab.id] = this
                                val privacyPreferences = PrivacyPreferences(viewContext)
                                settings.javaScriptEnabled = privacyPreferences.javascriptEnabled
                                settings.domStorageEnabled = !tab.private
                                settings.databaseEnabled = !tab.private
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.allowFileAccessFromFileURLs = false
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.cacheMode = if (tab.private) {
                                    android.webkit.WebSettings.LOAD_NO_CACHE
                                } else {
                                    android.webkit.WebSettings.LOAD_DEFAULT
                                }
                                setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                                    val userName = android.webkit.URLUtil.guessFileName(
                                        url,
                                        contentDisposition,
                                        mimeType,
                                    )
                                    if (DownloadValidator.isAllowedUrl(url)) {
                                        pendingDownloadUrl = url
                                        pendingDownloadName = userName
                                    }
                                }
                                isVerticalScrollBarEnabled = true
                                isHorizontalScrollBarEnabled = false
                                webChromeClient = object : WebChromeClient() {
                                    override fun onPermissionRequest(request: PermissionRequest) {
                                        val origin = request.origin?.toString()?.let(UrlValidator::origin)
                                        val supported = request.resources.all {
                                            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                        }
                                        if (origin == null || !origin.startsWith("https://") || !supported) {
                                            request.deny()
                                            return
                                        }
                                        val repository =
                                            (context as? MainActivity)?.sitePermissionRepository()
                                        val alreadyGranted = request.resources.all {
                                            repository?.isGranted(origin, it) == true
                                        }
                                        if (alreadyGranted && context is MainActivity) {
                                            context.requestMediaPermissions(request, origin, false)
                                        } else {
                                            pendingPermissionOrigin = origin
                                            pendingPermissionRequest = request
                                        }
                                    }

                                    override fun onPermissionRequestCanceled(
                                        request: PermissionRequest,
                                    ) {
                                        if (pendingPermissionRequest === request) {
                                            pendingPermissionRequest = null
                                            pendingPermissionOrigin = null
                                        }
                                    }

                                    override fun onGeolocationPermissionsShowPrompt(
                                        origin: String?,
                                        callback: android.webkit.GeolocationPermissions.Callback?,
                                    ) {
                                        callback?.invoke(origin, false, false)
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        url?.let { viewModel.pageStarted(tab.id, it) }
                                        view?.evaluateJavascript(
                                            currentVoiceProcessingScript,
                                            null,
                                        )
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        view?.evaluateJavascript(currentVoiceProcessingScript, null)
                                        viewModel.pageFinished(tab.id, view?.title, url)
                                        viewModel.updateHistoryState(
                                            tab.id,
                                            view?.canGoBack() == true,
                                            view?.canGoForward() == true,
                                        )
                                    }
                                }
                                updateVoiceDocumentStartScript(
                                    this,
                                    tab.id,
                                    currentVoiceProcessingScript,
                                )
                                loadUrl(tab.url)
                            }
                        },
                        update = { webView ->
                            webView.visibility = if (tab.id == activeId) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }
                            if (tab.id == activeId) {
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(!tab.private)
                                if (!tab.private) {
                                    cookieManager.setAcceptThirdPartyCookies(webView, false)
                                }
                                webView.evaluateJavascript(currentVoiceProcessingScript, null)
                                updateVoiceDocumentStartScript(
                                    webView,
                                    tab.id,
                                    currentVoiceProcessingScript,
                                )
                            }
                            if (webView.url != tab.url) {
                                webView.loadUrl(tab.url)
                            }
                        },
                    )
                }
            }
        }
    }
}
