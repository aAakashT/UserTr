# PrivateCall Browser

Android 14 privacy browser project for Android 10+ devices.

## Implementation status

All planned implementation phases are represented in the project:

1. Android/Compose skeleton
2. Browser navigation and tab lifecycle
3. Origin-scoped permission handling
4. Privacy dashboard
5. Browser-owned data cleanup
6. SAF-based download flow
7. WebRTC and audio routing controls
8. Unit tests and GitHub Actions

This repository should be treated as an initial native Android implementation. WebView,
Android permission behavior, and device audio behavior require physical or emulator
testing for final certification.

## Build locally

Requirements:

- JDK 17
- Android SDK 35
- Gradle wrapper

```bash
./gradlew assembleDebug
./gradlew test
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Implemented privacy and browser architecture

- Dark-first Material 3 interface with bottom navigation and responsive Compose surfaces.
- Address/search bar, HTTPS indicator, HTTP warning, back, forward, reload, stop, tabs,
  normal tabs, private tabs, and a private-session tab switcher.
- URL normalization and checks for insecure schemes and private/local network targets.
- WebView file access, universal file access, and content access are disabled.
- Geolocation prompts are denied. Camera and microphone prompts are limited to HTTPS
  origins and supported WebRTC resource types.
- Runtime Android microphone/camera prompts are separate from WebView permission prompts.
- Remembered site permissions are origin-scoped and can be revoked in Settings.
- Third-party cookies are blocked by default. Private tabs reject cookie acceptance.
- JavaScript, history, downloads, and dark mode are user-controlled settings.
- Privacy dashboard reports Android permission status and app-owned browser storage.
- `CLEAR ALL BROWSER DATA` requires confirmation and reports completed, failed,
  unsupported, and Android/system-setting-dependent operations individually.
- Downloads require user approval. The Storage Access Framework destination picker is used
  and executable/APK downloads receive an explicit warning.
- WebRTC audio focus, speaker routing, and Bluetooth SCO controls are provided.
- No advertising, analytics, tracking, telemetry, synchronization, password storage,
  autofill storage, automatic clipboard access, or covert recording.

## Security and privacy behavior

- Browsing history is disabled by default.
- Private tabs use no-cookie/no-cache settings.
- Closing private tabs and running cleanup remove private session cookies and active
  app-managed sessions.
- Download URLs and browser navigation reject unsupported schemes and private/local targets.
- There is no general external application-launch path implemented for WebView navigation.
- Invalid TLS certificates are never accepted and certificate verification is not bypassed.
- Release builds do not enable WebView debugging.

## Testing

Unit tests currently cover URL normalization, private-network rejection, HTTPS detection,
download source validation, executable-file detection, and private tab lifecycle.

```bash
./gradlew testDebugUnitTest
```

GitHub Actions validates the Gradle wrapper, runs debug unit tests, builds the debug APK,
and uploads it as `PrivateCall-Browser-debug-apk`.

## Android limitations

This app cannot clear Android system caches owned by Android or other apps, or logs
retained by websites, ISPs, VPN providers, or remote services. Service worker storage
is reported as unsupported where Android provides no application-wide clear operation.
