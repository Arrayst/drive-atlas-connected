# DRIVE ATLAS v0.5 Connected prototype

This package is separate from the v0.4 real-roads demo and the design prototype. It does not migrate or modify their saved stamps.

## Signals and safety

- `CarPropertyManager` reads `GEAR_SELECTION` through normal `CAR_POWERTRAIN` permission.
- `CarUxRestrictionsManager` reads authoritative platform UX restrictions.
- Browsing opens only with P gear (4), distraction optimization false, and restriction flags 0. Missing, unavailable, error, background, or disconnected state remains UNKNOWN and closed.
- The app reads both values on connection, callbacks, and every 2 seconds while foreground. On return it reconnects instead of retaining the old UNKNOWN lock forever.
- UNKNOWN wording does not assert that the car is moving. Non-P / restricted state likewise does not claim positive speed.
- No speed-zero inference, privileged driving-state permission, manual safety override, permission grant through ADB, or changes to vehicle security settings.
- A live callback from the emulator is emulator data, not physical driving evidence.

## Native navigation

Official `NaviHelper:3.0.5` is used with normal NAVI_ROUTE, NAVI_ROUTE_SEARCH and NAVI_CUSTOM_MAP permissions. Initial connection and route-state callbacks are tracked separately from request submission.

- SearchPlace opens the installed navigation search for a public course destination. It does not claim a route has started.
- The experimental coordinate course request sends two public waypoints and a public destination. It leaves unavailable POI IDs blank rather than fabricating them. Service compatibility is not guaranteed.
- A route is called confirmed only after route-completed callback plus destination and both ordered waypoint coordinates matching within 100 meters. A 30-second timeout remains unconfirmed.
- Foreground requests open the installed native navigation app. NaviHelper is not a map renderer and cannot repair the native app's existing blank base-map problem.
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
