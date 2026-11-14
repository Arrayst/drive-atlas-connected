# DRIVE ATLAS · Family + Round Trip 0.9

Native Kotlin/Compose Android app for the Hyundai PLEOS emulator: family conditions → course and rest planning → navigation handoff → family-car memories and feedback → next recommendation.

## Competition submission · 2026-11-14

- App: **DRIVE ATLAS**; team: **SELTO SPOT**.
- Competition release: [`apk/SELTO SPOT_DRIVE ATLAS_release.apk`](apk/SELTO%20SPOT_DRIVE%20ATLAS_release.apk). Spaces are intentional; quote the path in shell commands.
- Application ID: `com.driveatlas.connected`; version `0.9.0-roundtrip` (code `9`); min SDK `28`, target SDK `35`.
- This is the **release** build (`DEBUG=false`, `SIMULATOR=false`), signed with the team's dedicated submission key. It is not the debug/design-review app and does not enable debug-only SIM replay or the prototype public OSRM ETA provider.
- APK SHA-256: `f23adf9035c5c84272b9db50ee89b6bc3a8fde75ab515d757e86cb6647dba910`.
- Certificate SHA-256: `b101e063524dd69eef74bd741bbd0fe8edff4880af9b61030cd6d909eca47827` (public certificate fingerprint, not a private key).
- Verified emulator: **Pleos Connect 2.1.0** (`release_connect_v2.1.0`), **Android 14 / API 34**, **2560 × 1440 at 160 dpi**. Use an existing authorized Pleos emulator; its image is not redistributed.
- Validation: debug and release each passed 293 unit tests; both lint runs had zero errors. The signed APK installed, opened its real map, passed the bounded course/family/memory/Back smoke and reopened successfully. See the report for warnings and untested cases.
- Build and runtime validation, remaining limits and source-reference instructions: [submission verification](docs/SUBMISSION_20261114.md).

Install only on an authorized PLEOS test emulator/device with the required services:

```sh
adb devices -l
adb -s <selected-emulator-serial> install -r "apk/SELTO SPOT_DRIVE ATLAS_release.apk"
adb -s <selected-emulator-serial> shell am start -n com.driveatlas.connected/com.driveatlas.MainActivity
```

A conflicting existing signature must be resolved deliberately; do not uninstall or erase user data to bypass it. Actual parked `P` and platform UX authorization are required. Missing/unsafe vehicle signals keep the app locked. Do not force permissions or simulate vehicle state to defeat this gate.

The course map is optional and starts behind explicit connection consent. Opening it sends public course-region requests and the device's IP address to OpenFreeMap/Cloudflare, and remembers consent on that installation. It does not require transmitting current GPS, a home address or family records. See [OpenFreeMap terms](https://openfreemap.org/tos/) and [privacy](https://openfreemap.org/privacy/). Location, external navigation and online road-time requests have separate checks. Release users can enter planning estimates manually; this package does not claim live traffic or a production ETA provider.

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
- Build output: `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned intermediate). The reviewed, signed competition package is under `apk/`; signing keys and passwords are never included. Rebuilding the source does not grant access to the submission key.
- Debug application ID: `com.driveatlas.connected.emulator`

Use an existing authorized PLEOS emulator; proprietary emulator images and SDK-source archives are not included in this repository. Do not bypass permissions or vehicle restrictions to run the app.

## Run and verify

Install the debug APK with `adb install -r` on the selected emulator. Open DRIVE ATLAS Connected. The app unlocks only after actual emulator P/UX checks succeed. The task bar opens the route-index/map workbench, family comparison, road ETA, local journal, prior visits and stamp atlas. Category filters remain below the route index.

The native navigation dialog exposes both full-course and individual-leg actions. Each action clearly warns that the emulator navigation route may change. Select a later leg only after safely parking. Leg selection is manual, not an automatic whole-course itinerary or a visit award.

Tests cover safety, location evidence, restore validation, ranking, scoped facilities, nullable transfer time, persistence failure, record capacity, and fresh navigation callback evidence. The current presentation smoke test is `tools/qa-design-workbench.py`; it opens retained flows and cancels edit/delete dialogs without saving records or requesting navigation. Older replay scripts use their version-specific labels. Emulator replay scripts live under `tools/qa-family-v08.py`, `tools/qa-native-v08.py` and `tools/qa-native-legs.py`. These scripts interact with a running test emulator and may add explicitly simulated records or change its navigation route; do not run them against a real vehicle.

## Important limits

- Outbound/return values may be manually entered or explicitly calculated from road routes. Neither includes real-time traffic. The public routing server is debug-only, small noncommercial prototype infrastructure; production needs a suitable provider.
- Public facility facts are not live availability or whole-road accessibility guarantees.
- The installed native provider returns `(0,0)` for full-course waypoint coordinates despite correct names and a matching final destination. The app deliberately does not certify that full request. Single-destination navigation is handled separately; see the latest verification report.
- Emulator GPS or navigation is not physical driving or proof of a visit.
- No cloud accounts, payments, partner contracts, production signing, store publication or physical-vehicle certification is claimed.

Latest visual refinement and validation: [2026-11-13 screen refinement](docs/DESIGN_REFINEMENT_20261113.md).

Design rationale and source adaptation: [Native atlas workbench](docs/NATIVE_ATLAS_DESIGN.md).

Details and test results: [Round trip 0.9](docs/ROUNDTRIP_V09.md), [vehicle/release readiness](docs/VEHICLE_RELEASE_READINESS.md), [Family 0.8](docs/FAMILY_V08.md), [native-provider investigation](docs/NATIVE_NAVIGATION_V08.md), [prior connected integration](docs/PLEOS_CONNECTED_V05.md).

## Data and licenses

Family data stays in private local app storage; Android backup is disabled. Public photos retain their individual attribution/license links in the app and `docs/REAL_ROADS_V04.md`. Map/source attribution and service limitations are documented there. Intermediate build outputs, signing keys, local configuration, SDK-source archives and raw verification captures are excluded from source publication. The explicitly reviewed, signed competition APK under `apk/` is the sole build-artifact exception.
