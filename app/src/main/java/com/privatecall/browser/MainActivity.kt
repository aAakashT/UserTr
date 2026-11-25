package com.privatecall.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.content.Intent
import android.webkit.PermissionRequest
import com.privatecall.browser.privacy.SitePermissionRepository
import com.privatecall.browser.browser.DownloadRepository
import com.privatecall.browser.audio.AudioRouter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatecall.browser.browser.BrowserViewModel
import com.privatecall.browser.ui.PrivateCallBrowserApp
import com.privatecall.browser.ui.theme.PrivateCallBrowserTheme

class MainActivity : ComponentActivity() {
    private lateinit var mediaPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingMediaRequest: PermissionRequest? = null
    private var pendingPermissionRememberOrigin: String? = null
    private var pendingPermissionResources: Array<String> = emptyArray()
    private lateinit var sitePermissions: SitePermissionRepository
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var downloadDestinationLauncher: ActivityResultLauncher<String>
    private var pendingDownloadUrl: String? = null
    private var pendingDownloadName: String = "download"
    private var pendingMediaResult: ((Array<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sitePermissions = SitePermissionRepository(this)
        downloadRepository = DownloadRepository(this)
        downloadDestinationLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("*/*"),
        ) { uri ->
            val url = pendingDownloadUrl
            val name = pendingDownloadName
            if (uri != null && url != null) {
                saveDownloadToUri(url, uri)
                downloadRepository.addCompleted(name, url)
            }
            pendingDownloadUrl = null
        }
        mediaPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val request = pendingMediaRequest
            val grantedResources = pendingPermissionResources.filter { resource ->
                val androidPermission = when (resource) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                        android.Manifest.permission.RECORD_AUDIO
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                        android.Manifest.permission.CAMERA
                    else -> null
                }
                androidPermission != null && results[androidPermission] == true
            }.toTypedArray()

            if (grantedResources.isNotEmpty()) {
                val origin = pendingPermissionRememberOrigin
                val remember = origin != null
                if (remember) {
                    grantedResources.forEach { sitePermissions.grant(origin, it) }
                }
                request?.grant(grantedResources)
                if (grantedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    AudioRouter(this).requestCallAudioFocus()
                }
            } else {
                request?.deny()
            }
            pendingMediaResult?.invoke(grantedResources)
            pendingMediaResult = null
            pendingMediaRequest = null
            pendingPermissionResources = emptyArray()
            pendingPermissionRememberOrigin = null
        }
        enableEdgeToEdge()
        setContent {
            PrivateCallBrowserTheme {
                PrivateCallBrowserEntry()
            }
        }
    }

    fun requestMediaPermissions(
        request: PermissionRequest,
        origin: String?,
        remember: Boolean,
        onResult: (Array<String>) -> Unit = {},
    ) {
        val resources = request.resources.filter {
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }.toTypedArray()
        val androidPermissions = resources.mapNotNull { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    android.Manifest.permission.RECORD_AUDIO
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    android.Manifest.permission.CAMERA
                else -> null
            }
        }.distinct().toTypedArray()

        val allGranted = androidPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            if (remember && origin != null) {
                resources.forEach { sitePermissions.grant(origin, it) }
            }
            request.grant(resources)
            if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                AudioRouter(this).requestCallAudioFocus()
            }
            onResult(resources)
            return
        }

        pendingMediaRequest = request
        pendingPermissionResources = resources
        pendingPermissionRememberOrigin = if (remember) origin else null
        pendingMediaResult = onResult
        mediaPermissionLauncher.launch(androidPermissions)
    }

    fun sitePermissionRepository(): SitePermissionRepository = sitePermissions

    fun startUserApprovedDownload(url: String, suggestedName: String) {
        val safeName = suggestedName.takeIf { it.isNotBlank() && it.length <= 180 } ?: "download"
        pendingDownloadUrl = url
        pendingDownloadName = safeName
        downloadDestinationLauncher.launch(safeName)
    }

    fun downloadRepository(): DownloadRepository = downloadRepository

    private fun saveDownloadToUri(url: String, destination: Uri) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.getInputStream().use { input ->
                    contentResolver.openOutputStream(destination, "w")?.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Throwable) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this,
                        "Download failed",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
}

@Composable
private fun PrivateCallBrowserEntry(browserViewModel: BrowserViewModel = viewModel()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        PrivateCallBrowserApp(browserViewModel)
    }
}
