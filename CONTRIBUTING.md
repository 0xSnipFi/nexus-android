# Contributing to NEXUS

A reproducible search failure, an accessibility observation or a small, tested patch is welcome.

1. Check [open issues](https://github.com/0xSnipFi/nexus-android/issues) for an existing report. For a large change, describe the problem in an issue before implementing it.
2. Fork this repository, create a branch from main and make one focused change. The [source map and build instructions](README.md#build-from-source) identify the relevant modules.
3. Run the JVM tests, assemble the APK and run lint using the README command. For a UI change, also test the actual flow on a phone or emulator. Run the WebView journeys and catalogue checks with ./gradlew :app:connectedDebugAndroidTest on a test device with internet access. Live source availability can affect the result.
4. Open a pull request against main. Include the user-visible behavior, tests run, and a cropped before/after screenshot for visual changes. Remove notifications, account details, device identifiers and clipboard contents from shared captures.

For search changes, include a matching title and a no-match case. Distinguish unavailable sources from zero results. A new connector should document its search method, permission to access the source, time limits and behavior when unavailable. Do not submit credentials, signing keys or downloadable media.

Not writing code? Testing a different screen size or Android version and documenting the result is a useful contribution.

