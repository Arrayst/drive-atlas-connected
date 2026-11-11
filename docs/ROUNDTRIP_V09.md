# Road-based round trip 0.9

## Behavior

The Android app computes an ordered loop: selected departure → every course stop → the same departure. Outbound and return use separate road legs rather than doubling a straight-line or one-way estimate. Results are rounded up independently for outbound, course driving and return, then added to the existing family rest/walking plan only after the user taps Apply. Manual transfer edits clear automatic provenance. Source, calculation time and cached status survive app restart.

Public departure references are Seoul City Hall, Daejeon Station and Busan City Hall, with source links in code. They are building-area references, not verified vehicle entrances. Optional SDK location is a recent local receipt, not certified physical position. No location is sent automatically: each request identifies FOSSGIS/OSRM, the destination service, exact coordinates and logging implications before consent. A materially changed or stale SDK point invalidates approval.

Only public-reference results are cached in private local app storage for 24 hours. Personal coordinates and personal-location results are not cached. Existing family memories and ratings are not transmitted. Network failures, timeout, rate limits, malformed or inconsistent responses and missing roads are explicit errors, never fabricated estimates. Cancelled requests cannot update results. Cached data requires an explicit separate action and remains labelled; no background retry.

The parser checks ordered leg/waypoint counts, road snap bounds, finite nonnegative durations and distances, and total/leg reconciliation. The app does not claim that a returned road route proves a stop has been visited or that native navigation accepted the whole itinerary.

## Provider and release boundary

Debug uses HTTPS router.project-osrm.org, identifying User-Agent, at most one request start per 1.1 seconds, bounded timeout/response size and no redirects. The FOSSGIS public service is for small noncommercial prototype use, logs requests, and offers no uptime guarantee or real-time traffic. Attribution, provider information and map corrections are linked in the app. Release builds deliberately refuse this public endpoint until an appropriate production provider is configured; no secret credential, contract, paid service or deployment was provisioned.

Sources: https://routing.openstreetmap.de/about.html , https://github.com/Project-OSRM/osrm-backend/wiki/Demo-server , https://project-osrm.org/docs/v26.6.1/http.html .

## Verification

Final pre-layout verification: 102 unit tests passed; debug lint reported 0 errors and 32 existing warnings; debug and unsigned release APKs built successfully. All five emulator replay checks passed: consent cancellation, actual HTTPS calculation, family-plan application, explicit public-cache load and cold-restart provenance restoration.

A real HTTPS response on the Pleos emulator for the public Seoul City Hall → Gangneung course → Seoul City Hall loop returned 480.8 km. The app showed 197 min outward, 10 min course driving and 195 min return: 402 min after section rounding. It is a road-model estimate, not traffic ETA. Consent cancellation, explicit application, public cache and persistence are exercised by tools/qa-eta-v09.py; it never chooses private SDK location. Raw local emulator captures are excluded from Git.

No physical test vehicle is available. Physical-vehicle validation is Not Run, and no user action to obtain a vehicle is required for current emulator development. See VEHICLE_RELEASE_READINESS.md for optional future production prerequisites. Network-loss handling is implemented and error-message mapping is unit tested; a real device-network outage is not claimed as tested.

## Family layout refresh

The main rail now prioritizes course discovery, family comparison and car memories, while retaining legacy records and stamps as secondary links. Family comparison uses three responsive cards, a shared conditions summary and one expandable full-width planning panel. Unknown transfer times are labelled incomplete, never represented as a full round trip. Conflict, unverified and fulfilled states use distinct text plus color. Memories have a car-level summary and separated editor/history panels. Safety gates, persistence callbacks, confirmation dialogs and ETA provenance are unchanged. The layout build again passed all 102 tests, debug lint and both APK builds; the three-card layout was visually checked on the Pleos emulator.

## Task-focused accessibility polish

Hallmark README and the Taste Skill discussion were used as design principles, not installed packages or executed scripts. The vehicle app keeps its existing actions, warnings and actual data rather than importing landing-page decoration. Sources: https://github.com/Nutlope/hallmark and https://best-onedevyjuns.tistory.com/147 .

Shared primary/secondary buttons and sidebar navigation expose visible keyboard focus rings. Primary navigation uses selected tab semantics. Family planning displays applied outbound/course/return values explicitly above manual presets. Family/sidebar secondary text is darkened to #626B64: contrast is 5.04:1 against Paper, 5.52:1 against white and 4.75:1 against Sage. Blue focus on white is 5.98:1; mint focus against dark primary buttons is 9.03:1. These measurements cover the reviewed surfaces, not a blanket whole-app accessibility certification. No new animated transitions or continuous decorative motion were added.

Final polish validation: all 102 unit tests passed again, lint reported 0 errors / 32 warnings, and debug plus unsigned release builds succeeded. On the Pleos emulator, actual TAB traversal focused secondary and primary actions without activating them; blue/mint focus rings were visually checked. Sidebar focus and selected destination remained distinguishable. The expanded plan showed current 197 / 10 / 195 minutes plus its original source/time without clipping. These checks did not alter saved records or preferences or make additional external requests.
