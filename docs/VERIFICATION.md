# Foundation verification

Date: 2026-11-18

## Completed

- Gradle wrapper generated with Gradle 8.13 and official distribution SHA-256 pinned.
- Android SDK platform 36 revision 2 and build-tools 35.0.0 installed after matching official repository checksums.
- App/domain modules compile with JDK 17, Kotlin 2.0.21 and AGP 8.11.1.
- Ten Compose screen routes, five-destination dock, reduced-motion toggle and sample search flow implemented. Placeholder routes explicitly describe pending functionality.
- Local git repository initialized; no remote configured or upload performed.

Final command:

```powershell
.\gradlew.bat :app:assembleDebug :domain:test :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

Result: BUILD SUCCESSFUL, 2m 53s, 76 tasks (74 executed, 2 up-to-date).

- JVM: 8 tests, zero failures/errors. Final invocation reused the previously passing results.
- Android UI: 3 tests passed on Realme RMX3171, Android 10 / API 29.
- Lint: zero errors, nine dependency update warnings. These warnings remain visible; no lint baseline or suppression was added.
- APK manifest inspected with aapt: minSdk 29, targetSdk 36, compileSdk 36. No Internet or dangerous user permission requested; AndroidX contributes an app-scoped signature permission.
- Debug APK: 23,374,431 bytes. Installed and launched on the Android 10 phone; Home and Browser placeholder rendering visually inspected.
- CI workflow is written, not run remotely.

## Issues found and fixed

1. Unicode normalization initially discarded combining marks in Bengali. The regression test failed and the implementation now preserves Unicode marks.
2. Lint rejected an unescaped Windows SDK drive separator. Fixed local.properties rather than suppressing lint.
3. Added launcher icon and explicit Android 12+ backup/transfer exclusions after lint feedback.
4. Replaced the extended icon dependency with core icons to reduce the debug APK footprint.

## Remaining verification

Android 11, 12/12L, 13, 14, 15 and 16 are configured support targets, not device-certified yet. Large text, TalkBack, reduced system animation scale, tablet layouts, frame time, WebView memory and Android 15/16 native-library page-size compatibility need dedicated validation.

No claim of completed product: Room persistence, subtitle/genre/sort filters, language aliases, source-preserving deduplication, live connectors, browser, threat/ad filtering and downloads remain future phases. The initial domain helpers are not a complete browser security implementation.

Preferences storage disagreement and dock contour choice remain open. The current dock is a raised outline prototype, not a finalized reproduction of the supplied motion reference.
