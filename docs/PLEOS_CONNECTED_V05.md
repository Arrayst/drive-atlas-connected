# DRIVE ATLAS v0.5 Connected prototype

This package is separate from the v0.4 real-roads demo and the design prototype. It does not migrate or modify their saved stamps.

## Signals and safety

- `CarPropertyManager` reads `GEAR_SELECTION` through normal `CAR_POWERTRAIN` permission.
- `CarUxRestrictionsManager` reads authoritative platform UX restrictions.
- Browsing opens only with P gear (4), distraction optimization false, and restriction flags 0. Missing, unavailable, error, background, or disconnected state remains UNKNOWN and closed.
- The app reads both values on connection, callbacks, and every 2 seconds while foreground. Reads run off the UI thread, new UX restrictions apply immediately, and reads older than 6 seconds fail closed. On return it reconnects instead of retaining the old UNKNOWN lock forever.
- UNKNOWN wording does not assert that the car is moving. Non-P / restricted state likewise does not claim positive speed.
- No speed-zero inference, privileged driving-state permission, manual safety override, permission grant through ADB, or changes to vehicle security settings.
- A live callback from the emulator is emulator data, not physical driving evidence.

## Native navigation

Official `NaviHelper:3.0.5` is used with normal NAVI_ROUTE, NAVI_ROUTE_SEARCH and NAVI_CUSTOM_MAP permissions. Initial connection and route-state callbacks are tracked separately from request submission. Both the installed provider’s legacy `onRouteEnded` event and the current `onRoutingCompleted` event are handled; neither request submission nor foreground opening alone is success.

- SearchPlace opens the installed navigation search for a public course destination. It does not claim a route has started.
- The experimental coordinate course request sends two public waypoints and a public destination. It leaves unavailable POI IDs blank rather than fabricating them. Service compatibility is not guaranteed.
- A route is called confirmed only after route-completed callback plus destination and both ordered waypoint coordinates matching within 100 meters. A 30-second timeout remains unconfirmed.
- The explicit open button launches the verified installed native navigation app (`ai.umos.maps.android.navigation.app` on this emulator). If unavailable, it only submits a Foreground request and labels that as unverified. NaviHelper is not a map renderer and cannot repair the native app's existing blank base-map problem.
- Existing native route selection may change only when the user invokes the explicit route action.

## Location

Official `FusedLocation:1.1.3` is declared but no permission is granted by this build. Only the runtime permission screen/user approval can allow access. The app does not invoke location calls before permission is granted.

If permitted, location is displayed locally in connection diagnostics while parked, with an emulator label. Current location is not passed to the external map preview, OSRM, Kakao, or OSM. No location coordinates are logged or saved by this adapter. The SDK supplies confidence but no accuracy in meters, and timestamp clock semantics are not specified. Values therefore never become a real visit certificate. A callback not received for 15 seconds is marked stale.

## Existing content

The v0.4 licensed photos, fixed OSRM road snapshots, OSM visible-tile viewer, map credits, and external map viewer remain. A separate, explicitly labelled SIM stamp exercise remains. It cannot generate a real visit award.

## Primary documentation

- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- https://source.android.com/docs/automotive/driver_distraction/consume
- https://developer.android.com/reference/android/car/VehiclePropertyIds#GEAR_SELECTION
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions
- Official public Maven repository: https://nexus-playground.pleos.ai/repository/maven-releases/

## Validation

See `evidence/live-v05/` and the verification section appended after emulator testing. A successful APK build alone is not evidence of navigation, GPS or physical-vehicle support.

## Emulator results (2026-11-08)

- Debug and release builds succeeded. 40 unit tests passed. Lint: 0 errors, 32 warnings (including existing EXIF and dependency-version notices).
- Installed separate debug package `com.driveatlas.connected.emulator`, version 0.5.0-connected. Existing original/v0.3/v0.4/design packages preserved.
- Actual emulator service returned P gear, distraction optimization false, UX restrictions 0. Home opens automatically; explicit reconnect and return from native navigation re-read vehicle state. No simulated park override was used.
- Native service 3.0.4 connected. After opening its app and allowing initialization, INITIALIZING/FREE_DRIVE and route-state callbacks were received.
- A real SDK request reached the provider with the public destination and two public waypoints. Native UI generated a route from the emulator's simulated current area to Gangneung (about 220 km) and showed active guidance. Route-start and legacy route-end callbacks were observed. This verifies native route generation, not successful full-course coordinate validation.
- Full destination + ordered waypoint response matching did not pass, so the app does not report the entire course as verified. Before native initialization, requests can time out without a completion callback. The app retains explicit pending/timeout/partial-result states.
- Native base-map road detail remains blank/incomplete. Opening or routing through NaviHelper did not repair it. The app's own OSM road preview did render correctly.
- FUSED_LOCATION remains `granted=false`. Its adapter is implemented but GPS reading was NOT enabled or runtime-tested because approval is pending. No real visit award can be generated from its confidence value.
- No D/R movement was injected. Shell car-service command help is restricted on this user-build emulator; no root, security-setting change or bypass was attempted. Non-P/missing/UX-restricted policy paths are covered by unit tests, not claimed as physical-driving validation.
- Screenshots in `evidence/live-v05`: integrated home, real map, connection diagnostics, native app, native active route, and return. The first native UI assertion was too early during its initialization; its actual visible screen was subsequently checked directly. An ADB streaming install failed with a package-service broken pipe; the confirmed failure was retried using non-streaming install successfully.

## Demo operation

Open **DRIVE ATLAS Connected**. With the emulator in P and platform UX restrictions clear, the home opens automatically. Choose a course, then **지도 연결 옵션**. For a cold native app, open **기본 내비 화면 보기** first and wait for its UI to initialize, then return before submitting a public-coordinate course. Treat the course action as experimental until its explicit verification succeeds. The simulator's native guidance mode is not physical vehicle movement and does not override the P/UX safety check.

Location permission must be approved separately through the standard runtime dialog. It is optional for browsing and native public-course handoff. This APK is an emulator demo, not a certified physical-vehicle release.
