# Cross-screen visual refinement: build checkpoint

## Implemented scope

- Home, course detail, route selectors, atlas and records: stronger selected states, responsive rows/grids and empty states, improved wrapping and safety-caption readability.
- Family comparison and planning: clearer time breakdowns, wrapping conditions, scrollable condition dialog with a fixed close control, and preserved visibility of saved nonpreset durations.
- ETA: itinerary detail disclosure and clearer separation of calculation, consent and lower-priority simulation entry.
- Family memories: responsive editing/list layout, larger controls, wrapping feelings/actions and an explicit empty state. Existing private data and save/edit/delete semantics remain unchanged.
- Navigation drawer: destination menu first, current course visible, route settings and tools collapsed. Existing safety gates, Back behavior and source notices remain.
- Turn display: preserved OSRM maneuver modifiers drive semantic direction icons; unsupported or off-route states use a neutral icon, not guessed text parsing.
- Maps: tile-only desaturation, stronger cased blue remaining route, subdued passed geometry, larger directional pointer and source-backed departure/course/rest markers with clustering. Original tile bytes and attribution remain unchanged; only visible tiles are requested.

## Source and provider limits

Local tile rendering changes were assessed against the OSMF Legal FAQ, tile policy and attribution guidance. These do not explicitly approve a particular visual filter; the implementation does not extract or alter the database or change the server access policy. Existing OSM attribution remains readable.

- https://osmfoundation.org/wiki/Licence/Licence_and_Legal_FAQ
- https://operations.osmfoundation.org/policies/tiles/
- https://osmfoundation.org/wiki/Licence/Attribution_Guidelines

No new provider, key, SDK or invented photo location was introduced. Route-point markers are not assertions of safe parking or exact camera positions. Navigation remains a parked emulator simulation, not live-GPS or production driving guidance.

## Verification status

Aggregate unit tests, lint, debug build and unsigned release build passed. 214 tests passed with no failures/errors/skips; lint reported zero errors and 12 warnings. Debug APK SHA-256: `7ecb52cf5618862400d0e8d5867caa840f6d5609b381a6d8b4abcee4a97a8655`.

An independent review identified a narrow-map footer overlap. The map now measures overlay heights and reserves the actual pin-safe area; when no accessible pin fits, it explicitly directs users to the complete place list. Compact zoom controls avoid being hidden by a wrapped footer. Pure layout tests cover those cases.

The existing AVD was restarted with data preserved. An earlier unexplained exit was not attributed to the app; it later remained stable while the launch session was retained. The old installed APK identity was verified before capturing comparison screens, then the new APK was installed without clearing app data.

Actual checks completed: home and detail selection/CTA placement, family settings Close and Back without saving, ETA transmission-consent cancellation without a request, empty records and atlas, public-reference route calculation, full-map follow/overview, clustered markers and semantic turn icon. During SIM playback, menu taps and map drag remained blocked; Back paused playback, disabled speech and preserved the route. Two automatic ON_DEVICE utterances completed. DrivingView remained visible; no application FATAL or ANR appeared in the inspected log.

Public demonstration screenshots were reviewed separately from internal-only family/ETA screens. Private memory content was not exported or modified. Narrow-window platform switching, physical driving/vehicle restriction transitions, human listening quality and speech interruption latency remain unverified. Existing live-GPS, automatic-rerouting and production-provider limitations are unchanged.

The family-memory page received a privacy-preserving runtime check: button bounds and scrolling were inspected, and opening the edit form then cancelling with Back left data unchanged. No private content was printed, captured or uploaded. Its final pixel-level typography review remains unverified. The emulator was left at the public-route first instruction with replay stopped, voice off and follow mode selected.
