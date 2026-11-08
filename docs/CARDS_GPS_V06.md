# DRIVE ATLAS v0.6: three-card home and approved local GPS

## Home navigation

The home now contains three large horizontal cards: 추천길, 계절 드라이브, 테마 드라이브. Selecting a card opens that category's course list. Filters live inside the list, and each category keeps its own filter state. Course detail returns to its list; list Back returns to the three-card home. The real map, native navigation options, records, stamp album, and P + platform UX safety gate remain.

## Location permission and use

On 2026-11-08 the user explicitly approved DRIVE ATLAS reading emulator GPS for in-app course-visit checking, with no current-location transmission to external map servers. Only the existing `pleos.car.permission.FUSED_LOCATION` runtime prompt was accepted. The active driver profile (user 10) reports `granted=true` and `USER_SET`. The separate system profile remains ungranted; no other profile was granted access.

The FusedLocation callback was actually received. This is the emulator's preset location, not a physical vehicle fix. The app compares it locally with the nine built-in public course points and shows the closest point/distance as reference only. The comparison has no network calls and writes no visit, award, or coordinate history. Current location is not passed to the OSM tile preview, OSRM, Kakao, native route request, or another web URL. Native requests continue to use only the chosen public course coordinates.

The SDK provides no accuracy in meters; its original timestamp semantics are not established. Therefore even an exact coordinate match is labelled as an unverified visit. No real visit certificate or automatic stamp is awarded. Delayed callbacks are rejected across session changes, permission is rechecked, and coordinates/comparison are cleared on loss, revocation, or stale receipt.

## Preserved limits

Physical-vehicle operation is not certified. The native app's base-map problem and complete ordered-waypoint response verification from v0.5 remain separate limitations. The simulator's native route-guidance mode is never used as proof that the physical vehicle is moving.

## Validation

Debug/release builds and 45 unit tests passed. Lint: 0 errors, 32 warnings. The local-distance tests include invalid coordinates, exact matches still labelled unverified, and no award writes. Emulator checks and screenshots are under `evidence/cards-gps-v06/`; `tools/qa-cards-gps.py` tests the three entries, detail and Back, per-category filters, live location response and permission, preserved records/atlas, and cold restart.

Package remains `com.driveatlas.connected.emulator`; version is `0.6.0-cards-gps`. Existing app data and the v0.5 APK are preserved. Branch: `feature/cards-gps-v06`.
