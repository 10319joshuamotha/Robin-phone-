# ROBIN — Android assistant (Stage 1)

A first buildable Kotlin + Jetpack Compose project for an Android voice-first assistant. This is a genuine starter app, not the complete master specification yet.

## Stage 1 currently included

- Animated, tappable orb with pulsing glow and orbiting highlights.
- Runtime microphone permission request.
- Android `SpeechRecognizer` integration for one-shot speech input. Recognition quality and offline availability depend on the speech service installed/configured on the phone; this app does not claim speech recognition is always offline.
- Android Text-to-Speech spoken responses when a TTS engine is available.
- Basic local command parsing: greeting, current time, help, open dialer, and `call <number>`.
- Calls use `ACTION_DIAL`, so ROBIN opens the normal dialer with the number filled in and does not place a call silently.
- App-private defaults and backup disabled.
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact.

## Not implemented yet

Wake-word listening, offline LLM/model download, audio recording and library, reminders/alarms, SMS/WhatsApp integration, explicit web search, video transcripts, full-body avatar customization, floating overlay, encrypted data vault, release signing workflow, and comprehensive automated/device tests. The UI does not present fake controls for these features.

## Build in GitHub Actions (phone-only workflow)

1. Create a GitHub repository and upload the contents of this folder (the files inside `robin-android`, not the folder nested inside another project).
2. Open the repository on your phone. Go to **Actions** and enable workflows if GitHub asks.
3. Select **Build ROBIN APK** → **Run workflow**.
4. Open the completed workflow run and download the `robin-debug-apk` artifact.
5. Extract the ZIP on your phone. Install the APK. Android may ask you to allow installs from the app you used to open it; enable that only for the trusted installer you chose.
6. Open ROBIN and grant microphone permission when prompted.

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk` inside the build workspace. GitHub Actions stores it in the downloadable artifact named `robin-debug-apk`.

## Build locally in an Android IDE

Open this folder as a Gradle Android project in an Android IDE that supports Kotlin, Android Gradle Plugin 8.13.2, Gradle 8.13, and Android SDK Platform 36. Run the `assembleDebug` Gradle task. This repository does not include the binary Gradle wrapper JAR; the workflow installs Gradle directly, and a compatible Android IDE can import the Gradle project. A machine with only Java installed is not enough to build an APK: Android SDK platform/build tools are required.

## Release signing

For a release APK, create a private Android keystore in a trusted Android IDE or build environment and keep the keystore and passwords out of source control. Configure Gradle signing with secrets/environment variables in your private build environment, then run `assembleRelease`. The current release build is minified with R8 but is **not yet configured with a release signing key**. Never commit the keystore, passwords, or API credentials to this repository. Minification raises the effort of casual inspection; it cannot make an APK impossible to reverse engineer.

## Privacy and platform notes

- Speech recognition is started only after the user taps the orb/button and grants microphone permission. There is no background listening or wake-word service in Stage 1.
- Android's speech recognition provider may send audio to its service depending on device settings/provider implementation. Check the provider's offline language packs and privacy terms if offline speech is required.
- TTS uses the phone's installed engine and language data.
- No cloud AI, web search, or external AI API is integrated in this stage.
- `targetSdk` is 36. Device-specific Android 16 behavior still needs testing on the POCO X6 5G.

## Troubleshooting

- If speech input fails, check Settings → Apps → ROBIN → Permissions → Microphone, ensure a speech recognition service is installed/enabled, and download offline language data in that provider if available.
- If speech output is silent, check system TTS settings, install language data, and raise media/accessibility volume as appropriate.
- If installation is blocked, use the Android install prompt only for the APK you built/downloaded from your own repository.
- If CI fails, open the workflow run's logs and inspect the Gradle/SDK error before treating the build as successful.
