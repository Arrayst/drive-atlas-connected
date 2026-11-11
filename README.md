# DRIVE ATLAS · Family + Round Trip 0.9

Native Kotlin/Compose Android app for the Hyundai PLEOS emulator: family conditions → course and rest planning → navigation handoff → family-car memories and feedback → next recommendation.

## Implemented

- Consent-gated road-based round trips: departure → all course stops → departure, with independent outbound/course/return legs, explicit family-plan application and public-reference-only 24-hour cache.

- Three real Korean courses with licensed photos, bundled road geometry and OSM preview.
- Family time/walking preferences; separate outbound/return planning estimates; explicit unknown handling.
- Source-reviewed selectable rest venues with scoped restroom/accessibility facts, opening restrictions and source links.
- Native navigation search, experimental full-course request, and explicit single-destination course-leg requests. Request submission alone is never displayed as verified guidance.
- Private local memories tied to a car nickname, rating, notes, edit/delete confirmation and feedback-based ranking. Simulated memories are labelled and excluded from normal ratings. Old records are not silently removed when storage reaches its stated 500-record limit.
- Authoritative P gear and platform UX restrictions gate both the app and its internal public web viewer. Missing/stale signals fail closed. No manual park override.
- Local-only approved GPS proximity checks; no physical visit certification or automatic current-location transmission to external map servers.

## Build

Requires JDK 17 and an Android SDK containing compile SDK 35 with Android automotive libraries. Dependencies resolve from Google, Maven Central and the official PLEOS Maven repository defined in `settings.gradle.kts`.

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Offline mode works only after required dependencies have been cached. The Android Studio bundled JDK 25 is not compatible with this project's current Gradle 8.10.2 setup.

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned, not a store-ready release)
- Debug application ID: `com.driveatlas.connected.emulator`

Use an existing authorized PLEOS emulator; proprietary emulator images and SDK-source archives are not included in this repository. Do not bypass permissions or vehicle restrictions to run the app.

## Run and verify

Install the debug APK with `adb install -r` on the selected emulator. Open DRIVE ATLAS Connected. The app unlocks only after actual emulator P/UX checks succeed. Family planning and memories are available above the original three category cards.

The native navigation dialog exposes both full-course and individual-leg actions. Each action clearly warns that the emulator navigation route may change. Select a later leg only after safely parking. Leg selection is manual, not an automatic whole-course itinerary or a visit award.

Tests cover safety, location evidence, restore validation, ranking, scoped facilities, nullable transfer time, persistence failure, record capacity, and fresh navigation callback evidence. Emulator replay scripts live under `tools/qa-family-v08.py`, `tools/qa-native-v08.py` and `tools/qa-native-legs.py`. These scripts interact with a running test emulator and may add explicitly simulated records or change its navigation route; do not run them against a real vehicle.

## Important limits

- Outbound/return values may be manually entered or explicitly calculated from road routes. Neither includes real-time traffic. The public routing server is debug-only, small noncommercial prototype infrastructure; production needs a suitable provider.
- Public facility facts are not live availability or whole-road accessibility guarantees.
- The installed native provider returns `(0,0)` for full-course waypoint coordinates despite correct names and a matching final destination. The app deliberately does not certify that full request. Single-destination navigation is handled separately; see the latest verification report.
- Emulator GPS or navigation is not physical driving or proof of a visit.
- No cloud accounts, payments, partner contracts, production signing, store publication or physical-vehicle certification is claimed.

Details and test results: [Round trip 0.9](docs/ROUNDTRIP_V09.md), [vehicle/release readiness](docs/VEHICLE_RELEASE_READINESS.md), [Family 0.8](docs/FAMILY_V08.md), [native-provider investigation](docs/NATIVE_NAVIGATION_V08.md), [prior connected integration](docs/PLEOS_CONNECTED_V05.md).

## Data and licenses

Family data stays in private local app storage; Android backup is disabled. Public photos retain their individual attribution/license links in the app and `docs/REAL_ROADS_V04.md`. Map/source attribution and service limitations are documented there. Build outputs, signing keys, local configuration, SDK-source archives and new raw verification captures are excluded from source publication.
