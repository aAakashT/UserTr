# Troubleshooting

## Voice processing in browser recordings and calls

The browser can process microphone audio only when the website captures it through
the browser's microphone APIs. The built-in voice effects are applied inside this
browser; they do not affect other apps or browsers. Sample-based voice identity
conversion is not implemented.

### Quick check

1. In **Settings → Legitimate VoIP audio**, select a non-Off preset. Choose
   **Robotic**, **Distorted**, or **Echo** for an obvious effect.
2. Open the recording or calling site using HTTPS.
3. If prompted, allow microphone access. Android microphone permission and the
   website's browser permission must both be allowed.
4. Turn on **Voice filter** in the browser toolbar. This reloads the page and stops
   active media; wait for it to reload.
5. Start a new recording or call. If the site already had a call open, end it and
   join again so it requests a fresh microphone stream.
6. Record a short test, stop, and play it back. Compare with the filter disabled.
   Use headphones if monitoring live audio.

The `Voice test` screen is a local preview only; it does not inject audio into a
website call. Speaker and Bluetooth settings route playback and do not change the
microphone signal.

### How to tell what capture path a site uses

- A microphone permission prompt, or an active microphone indicator while recording,
  is a useful sign that the page is capturing live audio. A missing prompt is not
  conclusive: permission may already have been granted.
- For a site whose source you can inspect, search its code and loaded scripts for
  `navigator.mediaDevices.getUserMedia` or the legacy `getUserMedia` API. A WebRTC
  call usually sends a microphone track obtained this way.
- `MediaRecorder` records a `MediaStream`; it does not identify where that stream
  came from. Check how the stream was created (for example, from `getUserMedia` or
  from an existing audio element).
- A site recording an uploaded file or audio already playing on the page is not
  capturing live microphone input, so the microphone filter will not affect it.
- If a site uses a native app, a device-specific bridge, or another input route,
  this browser's web-page processing cannot intercept it.

On desktop Chromium, a developer can inspect an accessible page with DevTools and
search loaded Sources for `getUserMedia`. Android WebView inspection requires a
debug build configured to allow WebView debugging; release builds in this project
keep WebView debugging disabled. Do not enable release debugging for diagnosis.

### If the recording still sounds unchanged

1. Confirm a strong effect preset is selected, not **Off** or a subtle tone preset.
2. Check Android's microphone privacy control and the app's microphone permission.
3. Check that the browser has microphone permission for the site's HTTPS origin.
   Revisit the permission prompt or revoke and grant the origin permission again
   if necessary.
4. Turn the filter off and on, let the page reload, then start a brand-new capture.
5. Confirm the site is recording the live microphone rather than uploaded or
   page-playback audio.
6. If only one site fails, its capture implementation may be incompatible with the
   WebView wrapper. Compare with another HTTPS recorder that requests the mic.

Processing depends on Android System WebView support and the site's implementation.
The browser cannot guarantee that every site uses the processed stream.

## Gradle fails while parsing Java version `25.0.5.0.2`

The attached build log fails before tests run. Kotlin's Java-version parser rejects
the reported version string. This is a Java runtime/toolchain setup issue, not a
unit-test failure. The project requires JDK 17.

1. Check the runtime and available SDKMAN Java installations:

   ```bash
   java -version
   readlink -f "$(command -v java)"
   ls -1 /usr/local/sdkman/candidates/java
   ```

2. In this Codespace, Java is installed under `/usr/local/sdkman/candidates/java`.
   Prefer an installed JDK 17. If JDK 17 is not installed, the listed JDK 21 is
   compatible with the project's Gradle 8.9 wrapper and can be used instead. Do not
   use the JDK 25 installation that produced the parsing error.
3. Set `JAVA_HOME` to the exact installed JDK directory and put its `bin` first on
   `PATH`. For the JDK 21 shown in the report:

   ```bash
   export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.13.0+2-ms
   export PATH="$JAVA_HOME/bin:$PATH"
   ```

4. If choosing to install JDK 17 instead, initialize SDKMAN and inspect its available
   Java 17 identifiers:

   ```bash
   source /usr/local/sdkman/bin/sdkman-init.sh
   sdk list java
   ```

   Choose an available Java 17 identifier from the list and install/use it:

   ```bash
   sdk install java <Java-17-identifier>
   sdk use java <Java-17-identifier>
   ```

   Replace `<Java-17-identifier>` with the exact identifier shown by `sdk list java`.
5. Confirm `java -version` and `./gradlew --version` report the selected Java
   installation. In Android Studio, also set **Settings → Build, Execution,
   Deployment → Build Tools → Gradle → Gradle JDK** to the same JDK.
6. Restart the IDE terminal or Gradle daemon after changing the JDK, then retry:

   ```bash
   ./gradlew --stop
   ./gradlew testDebugUnitTest
   ```

If `java -version` reports JDK 17 but Gradle still reports `25.0.5.0.2`, compare the
Java home shown by `./gradlew --version` with the selected JDK and correct the IDE or
shell Gradle runtime.

## Gradle says the Android SDK location was not found

This workspace has an SDK in `.android-sdk` at the repository root. From the
repository directory, set the SDK environment variables before running Gradle:

```bash
export ANDROID_HOME="$PWD/.android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew testDebugUnitTest
```

Alternatively, create a machine-local `local.properties` file in the repository
root with this entry:

```properties
sdk.dir=/workspaces/UserTr/.android-sdk
```

`local.properties` is ignored by Git because SDK paths are specific to each
machine. If working from another checkout location, use that checkout's absolute
path instead.
