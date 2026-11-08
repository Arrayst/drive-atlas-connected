# v0.4 verification · 2026-11-08

## Final code checks

- `testDebugUnitTest`: 35 tests, 0 failures/errors/skips.
- `lintDebug`: 0 errors, 6 warnings. Existing target/dependency/backup warnings plus recommendation to use AndroidX ExifInterface instead of the platform implementation. The current three photo orientations were visually checked.
- `assembleDebug` and `assembleRelease`: successful. Release is unsigned, and no real vehicle adapter is enabled.
- Debug APK v2 signature verified; package `com.driveatlas.real.simulator`, version `0.4.0-real-roads` (code 4), min API 28, target API 35.
- Only INTERNET and AndroidX's self-signature receiver permission are declared. No location or PLEOS vehicle/navigation grant was requested.
- `git diff --check`: passed.

## Actual PLEOS emulator checks

AVD Pleos_CARRY, serial emulator-5580. App window `[853,90]–[2535,1300]` (1682×1210) in a 2560×1440 full display.

14 automated visible-UI flows are recorded in `evidence/real-v04/ui-checks.json`: home photos/categories, real coast map, offline simulation/retry, interactive zoom/fit, attribution dialog and close, connection cancel, safety lock after web return, lake winter filter/Back, Damyang map, simulated completion/stamp/PNG, restart persistence, partial archive/cancel, moving-state lock, and final real-map screen. An initial test harness wait was too short after cold-start; it was changed to bounded polling and the remaining flow rerun successfully. This was not an app crash.

Visual review caught and fixed map tile overflow outside the map region and JPEG EXIF rotation. The final lake photo crop was adjusted to show the horizon. Actual map screenshots confirm the road line, numbered anchors and visible OSM attribution. HTTP cache logs show repeated map views served from cache, e.g. 15 visible tiles / 15 loaded / 45 cumulative cache hits in the initial map test. The offline check is an explicitly labelled **app simulation**, not a device network-setting change.

The emulator's installed WebView shell lacks a default HTTPS intent handler. The app's restricted web fallback was therefore used. The official Kakao URL opened its no-install landing page; selecting **설치없이 지도보기** showed the 금진항→정동진 car route, **6.4 km / 14 min** at test time. This is external recalculation, not the bundled OSRM 6.3 km / 10 min estimate. The URL includes all three public course points; the mobile page visibly confirms the start/end and route, but independent retention of the intermediate point was not exhaustively verified. No app installation, sign-in, location permission or actual turn-by-turn guidance was started. Screenshot evidence of this external page is internal QA only.

No `com.driveatlas.real.simulator` crash was observed in the crash buffer during the checks. The device buffer contains an older, unrelated PLEOS profile-service crash. Broad soak/performance/accessibility testing and other vehicle-model layouts were not run.

## Delivered state

The separate v0.4 debug app is installed. The original Android repo remains at c180fd1, and the v0.3 worktree remains at 53ba838. No original package/data was cleared or overwritten. The new demo has one simulated Damyang stamp and one unconfirmed partial coast visit, with no active trip. It is left on the real coast map after testing.

APK: `dist/DRIVE_ATLAS_v0.4_real_roads.apk`, 30,693,169 bytes.
SHA-256: `ddc0300436c221d74d0993a8adf3906f7d55bfb0afcbca73df65742ab0a90b81`.

PLEOS native navigation's previous empty-map issue is not fixed or bypassed. Real GPS/vehicle status, permissions, navigation callbacks and live visits remain unimplemented. Do not present this as a car-certified or production-ready app.
