# Genuine vector navigation map

The full-window course preview, public-coordinate SIM replay, and all public course/place explorers use MapLibre Native Android 12.3.1, with an original Drive Atlas style for OpenFreeMap's OpenMapTiles schema. No brand map assets or Liberty style layers are copied. The previous raster tile loader and saturation filter have been removed.

## Data and permissions

- The shared connection gate appears before MapLibre initialization or creation of a native MapView. It is separate from OSRM routing consent and is session-only. SIM replay remains debug/emulator-only. Public course/place exploration remains available in release after consent, preserving the existing public-map feature without enabling real navigation.
- Consent explains the public displayed region and IP transmission to OpenFreeMap/Cloudflare, terms, privacy, internet dependency, and lack of traffic, SLA, offline navigation or real-driving certification. It never sends device GPS, home addresses or family records.
- TileJSON: `https://tiles.openfreemap.org/planet/latest`. Its actual vector-layer metadata was checked on 2026-11-12. Runtime follows the stable TileJSON URL, not a pinned weekly tile path.
- Glyphs: `https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf`, with the published Noto Sans Regular stack. Android's local sans-serif fallback generates Korean/CJK glyphs. Device font appearance needs emulator verification.
- Only visible-map resources are requested. Native prefetch is disabled and prefetch zoom delta is zero. No offline regions, bulk downloads or corridor warming are created. The provider's four-request cap is in its offline-download section; normal online viewport rendering uses the SDK's dispatcher.
- The SDK's default HTTP User-Agent includes the actual application package name, version and version code, plus SDK and Android version. This was checked in the official 12.3.1 Maven source (`HttpRequestImpl` and `HttpIdentifier`), rather than assuming a generic SDK identifier.
- OpenFreeMap, © OpenMapTiles and © OpenStreetMap contributors remain visibly linked. [Terms](https://openfreemap.org/tos/) and [privacy](https://openfreemap.org/privacy/) are linked in the consent gate.

## Rendering and safety

- Light neutral background, real gray building footprints, subdued parks/water, independent local/arterial/motorway layers and Korean-first labels. No POI icon crowding, pedestrian dotted lines, invented building heights or synthetic roads.
- Heading-up follow uses 17.8 zoom, 38° pitch and a 75% vertical vehicle anchor. Overview resets tilt and bearing; pan is manual until follow is selected. All gestures, camera buttons, external links and consent activation respect the supplied safety-control state.
- Passed road, remaining road, direction arrows, selected reference stops and the simulated vehicle use the original ordered OSRM geometry. Return-road overlaps never use nearest-point matching. The animated cursor is a binary-search interpolation on the same cumulative distance contract.
- Long GeoJSON line sections update once per replay tick. Only that tick's short, real road-vertex links and vehicle position animate. This avoids rebuilding a 100,000-vertex route at frame rate.
- All route/stop/vehicle layers start hidden. A loaded style alone is insufficient: actual road features must render. A blank viewport clears previous road-evidence state. Loading errors hide overlays and expose retry rather than presenting a route on an empty map.
- MapView start/resume/pause/stop/destroy follow Compose lifecycle. No location component or device-heading subscription is enabled.
- Existing OSRM steps, TTS, replay controls and P/vehicle safety checks are unchanged.

## Dependency and license

`org.maplibre.gl:android-sdk:12.3.1` is published on Maven Central and is compatible with this project's Kotlin 2.0 compiler. Newer 13.7.1 was checked, but its Kotlin 2.2 dependency is unsuitable without a separate toolchain upgrade.

MapLibre's BSD-2-Clause notice is included in the APK at `assets/licenses/maplibre-native.txt`, copied from the official `android-v12.3.1` license. OpenFreeMap/OpenMapTiles/OSM attribution is separate from the SDK license. Font resources retain the provider's Noto font licensing; no glyph assets are redistributed in this repository.

## Public course/place explorer

- Home, course detail, history atlas and trip maps share `VectorCourseExplorer` through `RealRoadMap`. Source-backed course/rest/photo records, family-selected rest metadata, the existing descriptions and official-source links are unchanged.
- Header and filters are outside the native viewport so they do not cover the road. The compact route is 4.5dp with a restrained 7dp casing. Visual markers are 32dp (38dp groups) inside separate 64dp accessible touch targets.
- All public-map cards reserve 520dp at normal text size: approximately 64dp header + 328dp unobscured native map + 64dp filters + 64dp attribution/offline controls. At 2× text, the card grows to 680dp and compact badges defer to the accessible list. These are layout budgets; narrow text wrapping and actual device screenshots require runtime verification. Trip progress is above the card, not covering its map/header.
- Nearby touch targets form stable connected groups. A group badge stays at its first member's actual representative coordinate, never a displaced pin or invented centroid. Other group members keep small dots at their real coordinates. Tapping opens the group's existing list/detail flow; the full filtered list remains available independently of map/network state.
- The previous wide pin displacement and long leader lines have been removed. Source cautions still explain that course references are not safe stopping places and the photo venue marker is a parking representative point, not an exact camera position.
- All/course/rest/photo filters, zoom/fit, selected-rest descriptions, offline simulation, empty-filter messages and source attribution remain. Tiny viewports and large text fall back to the accessible place list instead of clipped badges. The app's existing P/vehicle gate removes the entire explorer when controls are unsafe.
- Offline simulation destroys the native map composition and shows no route or markers; it makes no replacement network requests. Returning online still requires the shared provider consent if it was never granted.
- Provider legal/source links now work in the PLEOS built-in browser fallback using exact HTTPS hosts. Tests reject HTTP, credentials, arbitrary subdomains and look-alike domains.

## Verification

The implementation adds tests for provider consent/debug gating, original vector source/layers, Korean label fallback, default-hidden route layers, follow anchor/zoom, and ordered cursor behavior including overlapping return roads. Unit tests, lint, debug and release builds must pass before publication. Emulator rendering, foreground/back cycles, gesture locks, Korean labels, real tile failures and actual screenshot scale must be recorded separately; a compile is not runtime validation.

Sources: [MapLibre Android quickstart](https://github.com/maplibre/maplibre-native/blob/main/platform/android/docs/getting-started.md), [MapLibre local glyph options](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map-options/index.html), [official 12.3.1 Maven artifacts](https://repo.maven.apache.org/maven2/org/maplibre/gl/android-sdk/12.3.1/), [OpenFreeMap](https://openfreemap.org/), [OpenFreeMap terms](https://openfreemap.org/tos/), [OpenMapTiles schema](https://openmaptiles.org/schema/), [OpenStreetMap copyright](https://www.openstreetmap.org/copyright).

## Recorded emulator verification (2026-11-12)

Final debug APK SHA-256: `f9ac33781dee6d8466dc5a1d63bd907ec917bd398ede3c80cf2890e238d9440f`.

- 234 unit tests passed with zero failures/errors; lint reported zero errors; debug and unsigned release builds passed.
- Installed without clearing existing app data and opened through the normal PLEOS Home container. DrivingView remained visible.
- Gangneung public course detail: real vector coastline/roads, compact grouped markers, group list and individual place information, rest filter, attribution and offline simulation were exercised. Offline simulation hid the route and pins instead of drawing them over an empty map.
- Seoul public-reference SIM: Korean road labels, real building footprints, route alignment, direction arrows, heading-up camera and lower-screen vehicle were observed. Replay movement/turning, overview/follow, menu and pan lock, drawer dismissal and Back-to-pause were checked. No live GPS was used.
- Final attribution text was fully visible in the normal wide app window without overlap with the replay panel. Final screenshots were checked directly. Narrow official app-window layouts were not runtime verified; no forced window bounds or system settings were used.
- The preceding runtime pass recorded one completed voice utterance; another stopped at the existing approximately six-second safety limit. Full continuous speech completion and human listening quality are not established by this map change. The final visual pass kept voice off.
- No app FATAL/ANR was detected in the inspected runs. No numerical FPS/performance guarantee is claimed. Physical-vehicle use, live GPS navigation, traffic, automatic rerouting and production route ownership remain outside this SIM validation.
- App was left paused with voice off. The OpenFreeMap terms apply to the approved public demo service use: https://openfreemap.org/tos/ .
