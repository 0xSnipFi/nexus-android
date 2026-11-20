# NEXUS implementation baseline

## Scope and decisions

User confirmed Android 10 through Android 16 support on 2026-11-18. Configure minSdk 29, compileSdk 36 and targetSdk 36. No maxSdk restriction. Test Android API 29, 30, 31, 32, 33, 34, 35 and 36; configuration alone is not a compatibility certification.

Initial application ID: app.nexus.discovery (provisional until distribution). Constructor injection keeps the domain independent of Android. No remote providers, accounts, analytics or Internet permission in the UI foundation. All preview catalogue entries are explicitly sample metadata, without source or playback claims.

Open decisions: Room preferences versus TRD DataStore; exact curved dock contour (video cutout versus image raised contour); microphone/scanner/rating controls are not specified; original paper sketch missing; local backup/export policy; approved providers, licences and connector permissions. These do not block a local UI foundation. Do not resolve them silently during their implementation phase.

## Traceability

| Requirement | Module / screen | Implementation task | Acceptance test |
| --- | --- | --- | --- |
| PRD-01 / FR-01 | domain, Search, Results | Title and alias matching | Title-only and alternate-title corpus |
| PRD-02 / FR-02 | domain, Search | Independent audio/subtitle/type/year/genre filters | Apply/clear each filter independently |
| PRD-03 | domain | Normalize Unicode and fuzzy rank | Misspelling and multilingual corpus |
| PRD-04 / FR-03 | source-connectors, Results, Source Details | Typed connector contract and per-source evidence | Contract and source attribution tests |
| PRD-05 | domain, Results | Deduplicate while retaining source links | Same-title remakes and alternate-source tests |
| PRD-06 / FR-04 | source-connectors | Permitted external discovery | Permission, timeout, quotas and parser tests |
| PRD-07 / FR-05 | browser, Browser | Hardened WebView and bounded tabs | Schemes, back/forward, lifecycle tests |
| PRD-08 / FR-06 | protection, Browser | Pop-up and independent redirect checks | Unsolicited versus user navigation tests |
| PRD-09 / FR-08 | browser | Manual genuine CAPTCHA | Controlled manual verification journey |
| PRD-10 / FR-09 | core-database, Watchlist | Persist metadata and original source | Restart, offline, save/delete tests |
| PRD-11 / FR-10 | core-database, History | Persist query, filters and time | Individual delete, clear-all, restart |
| PRD-12 / FR-11 | downloads, Downloads | Rights gate, transport, storage validation | MIME, extension, size, restriction, resume tests |
| PRD-13 / FR-07 | protection, Security Warning | Threat reasons and safe return | Known threat, unknown and false-positive cases |
| PRD-14 / FR-12 | settings, Settings | Local privacy/theme/motion settings | Persistence, no-account and no-telemetry review |
| TRD 1–3 | app, domain, future data modules | Separate models, typed contracts, schema | Dependency boundaries and database migrations |
| TRD 4–5 | connectors, protection | Integration inventory, list updates/rollback | Licence/privacy review and failed update tests |
| TRD 6–7 | downloads, data | Authorized transfers and private storage | Restriction rejection, secret storage and deletion |
| TRD 8 / UI / Animation | app UI | Tokens, five destinations, motion, semantics | Font scaling, TalkBack, motion disabled, touch targets |
| TRD 9–10 | build / CI | Pinned versions, lint, tests, APK | Repeatable clean build and release signing checklist |
| PRD NFR / TRD 9 | all | Bounded images/WebViews, errors and offline states | Frame time, memory, cold start, network loss |

## Delivery sequence

0. Documentation, traceability and environment inspection.
1. Gradle app/domain foundation; minimal debug APK, JVM tests and Android lint.
2. Compose theme, navigation and all ten screen routes with clearly labelled sample/empty states; accessibility and UI tests.
3. Room local records and migrations, after resolving preferences storage; restart/offline tests.
4. Complete search filters, language aliases, ranking and source-preserving deduplication; relevance corpus.
5. Registry of 40 supplied URL strings, initially unverified; permission review before enabling any connector.
6. Hardened browser, safe tabs, redirect checks and manual CAPTCHA; controlled security fixtures.
7. Vetted ad engine and filter-list licensing, safe update/rollback and compatibility tests.
8. Authorized downloads and supported storage/notification behavior across API 29–36.
9. Device matrix, profiling, privacy disclosures, debug/release builds and controlled signing.

Roadmap phase numbers differ from master-prompt numbers; this sequence follows the master prompt and preserves all roadmap deliverables. No phase is complete merely because its screen exists.

## Verified reference pages

- https://developer.android.com/about/versions/16/setup-sdk
- https://developer.android.com/build/releases/agp-8-11-0-release-notes
- https://developer.android.com/jetpack/androidx/releases/compose-kotlin
- https://developer.android.com/jetpack/androidx/releases/activity
- https://developer.android.com/jetpack/androidx/releases/navigation

Dependencies are pinned to a conservative compatible baseline, not claimed to be the newest releases. Review updates before release.
