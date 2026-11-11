# Native navigation investigation

## Reproduced provider behavior

The final Damyang full-course request contained named latitude/longitude arguments, two explicitly indexed waypoints, and the public final destination. The SDK encodes named JSON fields. The provider generated guidance and emitted completion; final destination coordinates matched. Both waypoint names matched the requested order, but both returned coordinate pairs were `(0,0)`. Display coordinates were missing. Swapping axes and applying common integer scales did not recover valid course coordinates.

The temporary debug probe recorded only match indexes, validity/zero flags and name indexes, not raw user location. It was removed from the final source. The behavior is missing waypoint metadata, not a demonstrated app axis-order bug. Zero coordinates must not be replaced by the expected coordinates to manufacture a pass.

## SDK contract review

Official NaviHelper 3.0.5 depends on navi-data 3.0.4; that is intentional, not itself a mismatch. `ResponseHandler` decodes `List<WaypointInfo>` directly and forwards it without reordering. Source documentation defines routing latitude/longitude separately from optional display coordinates. Official release notes mention a constructor-order change, but the app uses named Kotlin arguments and the observed failure is zeros, not swapped axes.

- https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-release-note
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints

The installed native navigation app reported version `3.3.2-rc5-hotfix.3` during this emulator test. No provider update, security change, external support contact, fabricated POI identifier or permission expansion was performed.

## Functional alternative

The app now offers an explicit native destination request for each course point. This uses `requestRoute(RouteInfo)` rather than relying on broken multi-waypoint metadata. It still requires a fresh completion callback, a matching destination response and an empty waypoint list. Each request is separately verified; later legs require an explicit action while parked. This does not pretend to be automatic whole-course guidance or a completed trip.

The experimental full-course command remains available with its truthful unconfirmed status. A future provider fix can restore full-course confirmation without weakening the validator. Native-provider full-waypoint metadata and physical-vehicle testing remain external integration requirements.

## Verification

Final native per-leg emulator results are recorded below after execution.

### Final results

All three Damyang course legs passed on the installed PLEOS emulator. For each explicit destination request, the provider emitted a fresh completion callback, returned the matching selected destination coordinate and returned zero remaining waypoints. The app displayed “선택 구간 내비 확인 · 목적지 좌표 일치 · 전체 코스 연속 안내 아님”. The run ended at the original DRIVE ATLAS home.

Final aggregate: 81 unit tests passed (0 failures/errors/skips), lint 0 errors/32 warnings, Debug and unsigned Release builds successful. `git diff --check` passed. The three native-leg screenshots and local result list are retained locally under `evidence/native-legs-v08/` and intentionally excluded from the private source repository.

This verifies a working per-leg alternative on the emulator, not an upstream fix for the missing full-course waypoint coordinates, automatic leg switching or physical-vehicle certification. No user action is required to use the new per-leg UI on the updated emulator. Full-course automatic confirmation would need the provider to return actual waypoint coordinates, or a future independently verifiable SDK capability.
