# Embedded course and place map

This iteration extends the existing in-app Compose map. It does not introduce a proprietary map SDK, request a new API key, subscribe to a service, or enable device-location tracking.

## Source and coordinate boundaries

- Existing course line and road anchors come from the bundled OSRM snapshots documented in `REAL_ROADS_V04.md` and `app/src/main/assets/route-sources.json`. Road anchors are not verified parking or safe stopping points.
- Rest candidates use `RestFacilities` and the official sources recorded in `REST_ROUTING_SOURCES.md`. Coordinates are representative public POI points, not independently verified vehicle entrances. Opening, access and availability need checking before a visit.
- The Gangneung Tourism Development Corporation's [Sandglass Park guide](https://opentour.gtdc.or.kr/dzdocument.php?xch=intro&xid=opentour_map), checked 2026-11-11, identifies an open seaside photo area. The [park overview](https://gtdc.or.kr/opentour/dzdocument.php?xch=guide&xid=opentour_guide) describes scenery and commemorative photography. These support a photo-walk suggestion, not a precise camera position or an independently measured popularity claim.
- If this suggestion is displayed at the sourced Sandglass Park parking marker, the label must state it is the nearby parking representative point. It must not label that coordinate as the exact photo zone.
- Other scenic road anchors can be shown as course scenery references only, with no encouragement to stop on the road. No unverified place is presented as a famous or officially designated photo spot.

## Map and vehicle boundaries

The colored road line remains the base-course snapshot. A selected rest marker does not mean the displayed line has been rerouted through it. Edited rest stops participate in the separate itinerary and round-trip calculation according to `FAMILY_PLANNING_V10.md`; production routing and native-provider waypoint limits remain unchanged.

Map/place exploration remains subject to the application's parked-only, fail-closed safety gate. No physical vehicle test is available. No template certification, turn-by-turn navigation or safe stopping/photography authorization is claimed.

The [Google Map + Content design guidance](https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/map-content-template), checked 2026-11-11, informs corresponding list/marker selection and readable controls. This native Compose implementation is not a claim of implementing or certifying that Android for Cars template.

## OSM tile policy

[OSMF tile policy](https://operations.osmfoundation.org/policies/tiles/) checked 2026-11-11 requires HTTPS, app-identifying User-Agent, visible attribution and honoring HTTP cache headers. The application must fetch only the current visible viewport, with no region prefetch, offline download or tile archive. Existing 32 MiB HTTP caching is retained. Automated QA must not scan or pan/zoom large areas to collect tiles.

The public tile service is best-effort without SLA and is not a production hosting commitment. A wider release needs an appropriate hosting/provider plan and terms review; that choice is not made by this change. No additional map/photo imagery is copied from tourism pages.

## Verification

Implemented: course/rest/photo-walk filters, accessible selectable markers, matching filtered lists, sourced place details, and existing-family-plan rest selection badges. Nearby touch targets are separated while leader lines and small points preserve their true geographic anchors. Selecting a marker does not edit the family itinerary.

Final local aggregate: 139 unit tests, zero failures/errors/skips; lint zero errors and 32 warnings; debug and unsigned release builds pass. Debug APK SHA-256: `8e590d087ebc338a170888e573d9d6a86d59f9935f976e25c8aa350bc8b35851`.

Actual PLEOS emulator checks: home and detail maps, all five coast markers discoverable, filter/list/detail correspondence, photo source explanation, Close and Android Back, unverified-photo empty state, simulated offline and recovery, and route-switch state reset. Final captures are kept locally and excluded from the repository. The parent review inspected actual home/detail/photo-dialog/offline screenshots.

Physical driving, dynamic vehicle-state transition in this iteration, installed Kakao app, and production routing were not tested. The existing upper-level parked/unknown safety gate remains unchanged. Source research is not evidence of live place availability or real-car validation.
