# NEXUS for Android

NEXUS 0.4.2 is a local-first media discovery browser. Search movie, anime and cartoon titles, view available artwork, open original source pages, copy one or all source links, and keep a watchlist on the device.

## Build and run

Install JDK 17, Android SDK Platform 36 and Build Tools 35.0.0. Set ANDROID_HOME or configure sdk.dir in a local.properties file that stays on your machine. From the repository root:

~~~sh
./gradlew :domain:test :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
~~~

On Windows use gradlew.bat instead of ./gradlew. The debug APK is for development; no signing credentials are included.

## Source map

| Path | Responsibility |
| --- | --- |
| [app](app/) | Android entry point, navigation, WebView, clipboard and watchlist integration |
| [Prototype.kt](app/src/main/java/app/nexus/discovery/Prototype.kt) | Connects the locally bundled interface to Android actions |
| [prototype.html](app/src/main/assets/prototype.html) | Responsive screens, search controls and animated navigation |
| [Catalogue.kt](app/src/main/java/app/nexus/discovery/Catalogue.kt) | Selects sources, filters and ranks hits, prepares source links |
| [domain](domain/) | Android-independent search engine, matching and JVM tests |
| [assets](app/src/main/assets/) | Offline catalogues, artwork, source review data and dataset notices |
| [docs](docs/) | Historical implementation notes, verification records and source compatibility reviews |
| [.github/workflows](.github/workflows/) | Build, JVM tests, lint and debug APK artifact |

## Coverage and limits

The app targets API 36 and installs from API 29. The responsive interface has been checked on Android 10 (720 × 1600) and Android 15 (1080 × 2400); other screen sizes and OS versions have not all been tested. The legacy Compose UI tests predate the current WebView search screen.

Results combine bundled title metadata and supported online listings. A match does not establish streaming, language or download availability. The source review list is not a list of active connectors. Downloads currently has an empty state.

Dataset notices are included for the [anime catalogue](app/src/main/assets/catalog-license.txt) and [movie catalogue](app/src/main/assets/movie-data-license.txt). Those notices do not grant rights to third-party cover art.
