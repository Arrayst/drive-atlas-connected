# DRIVE ATLAS v0.4 · Real roads

## What changed

- The v0.3 app/source are preserved. v0.4 is a separate package, `com.driveatlas.real.simulator`, labelled **DRIVE ATLAS Roads**.
- The home retains recommendation / season / theme and the stamp-book visual language, but uses actual location photographs with credits.
- Real Korean sample courses replace fictional locations: Gangneung Heonhwa-ro, roads near Damyang's Metasequoia walking attraction, and Chuncheon's west Uiam Lake road.
- Native Compose renders an OpenStreetMap basemap and the full road-following GeoJSON returned by OSRM. This is not a hand-drawn straight line, satellite imagery, live navigation, or a recorded vehicle trace.
- Kakao Maps official keyless URL passes the public course's start, one intermediate point, and destination to an external browser when supported. This PLEOS emulator has no default HTTPS handler, so a restricted HTTPS-only in-app web viewer is used. On Kakao’s landing screen choose **설치없이 지도보기**; no map-app installation or login is performed. It does not transmit the user's current location. An OSM destination link is also available.
- Existing simulated journey, evidence checks, stamps, visit archive, PNG preview and fail-closed safety state remain.

## Course provenance

`app/src/main/assets/route-sources.json` records the source URLs, geographic anchors, OSM ways, raw routing requests, photographs, licenses and limitations. The three `*-route.json` assets preserve the original OSRM driving responses, generated 2026-11-08 UTC.

| Course | Snapshot distance | Modelled driving time | Important boundary |
|---|---:|---:|---|
| 강릉 헌화로 | 6.2546 km | 580.9 s | 금진항–심곡항–정동진 vicinity. 헌화로 way is a two-lane tertiary road, not a bicycle-only path. |
| 담양 메타길 나들이 | 2.9641 km | 322.5 s | Surrounding public roads only. The photographed tree avenue and 관방제림 are walking attractions, not car access. |
| 춘천 의암호 서쪽길 | 18.784 km | 1122.3 s | 의암댐 west road–박물관 앞 박사로–춘천댐 vicinity. Uses the actual computed distance rather than a conflicting tourism article estimate. |

All routing anchors snap to the OSRM road graph at 0 m. This is graph consistency, **not field verification**. The app marks them as road reference points, not verified parking/stop areas. No traffic, closure, weather, access, opening-hours or parking validation is claimed. Photos depict the named area and may be from different dates/seasons or a nearby viewpoint; they are not the user's photos or evidence of visiting.

## Map service policy and data

The demo uses `https://tile.openstreetmap.org/{z}/{x}/{y}.png` only for the currently visible route viewport. No prefetch, bulk region scan, tile archive or offline map download is implemented or packaged. The web fallback has no JavaScript bridge, file/content access or geolocation access, blocks non-HTTPS/unsupported top-level links, and resets the main app’s safety state on return.

Native HTTP uses a stable `DriveAtlasDemo/0.4 (Android; local proof-of-concept)` User-Agent and Android `HttpResponseCache`, respecting HTTP cache/conditional-request behaviour. A 32 MiB local cache avoids unnecessary repeated downloads. Attribution is always visible on the map and links to https://www.openstreetmap.org/copyright .

This public best-effort tile service has no SLA and is **not a production hosting plan**. Before wider distribution choose an appropriate map service or hosted tile infrastructure, confirm its terms, and provide operator contact information. OSRM's public demo is used only to generate three fixed sample routes; the installed app does not send runtime routing requests to it. Snapshots are OSM-derived data under ODbL 1.0, https://opendatacommons.org/licenses/odbl/1-0/ .

- Policy: https://operations.osmfoundation.org/policies/tiles/
- Routing API: https://project-osrm.org/docs/v5.24.0/api/
- Kakao official route URL format: https://apis.map.kakao.com/web/guide/ (`/link/by/car/` with up to five intermediate stops)

## Photo notices

Original JPEG files are bundled. The image display decodes EXIF orientation and crops/scales to the UI frame. The original files are not rewritten.

- `coast.jpg`: **Jeongdongjin Beach**, © Loewelad, CC BY-SA 3.0. https://commons.wikimedia.org/wiki/File:Jeongdongjin_Beach.jpg ; https://creativecommons.org/licenses/by-sa/3.0/
- `forest.jpg`: **Metasequoia glyptostroboides Avenue**, © dalgial, CC BY-SA 3.0. https://commons.wikimedia.org/wiki/File:Metasequoia_glyptostroboides_Avenue.JPG ; https://creativecommons.org/licenses/by-sa/3.0/
- `lake.jpg`: **20231226 공지천유원지에서 바라본 의암호**, © YellowTurtle9, CC BY 4.0. https://commons.wikimedia.org/wiki/File:20231226_공지천유원지에서_바라본_의암호.jpg ; https://creativecommons.org/licenses/by/4.0/

No Pinterest photo, proprietary map screenshot, paid imagery, or API secret is included. Pixel stamp illustrations from the previous app remain original artwork.

## Actual vehicle integration boundaries

The app has INTERNET permission only (in addition to its AndroidX-generated signature receiver permission). It does not request location, vehicle or PLEOS navigation permissions. Unknown and moving states block app exploration; the debug controls change only local simulated state. Leaving the app resets safety/location to unknown/lost. Real `CarUxRestrictionsManager` and vehicle adapters remain unimplemented.

PLEOS NaviHelper v3.0.5 is a client API for the installed navigation service, **not an independent basemap renderer**. Route search and waypoint forwarding need `pleos.car.permission.NAVI_ROUTE_SEARCH`, information/trip APIs need `NAVI_ROUTE`, navigation-screen requests need `NAVI_CUSTOM_MAP`. The SDK, granted permissions, service support and callbacks must all be verified before reporting actual integration. v0.4 does not claim to fix the emulator's pre-existing `connect_empty` native navigation map problem.

- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions

## Build

Run the repository's Gradle wrapper with JDK 17 and an Android SDK containing API 35:

```
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

The debug APK is installable. The release build is unsigned and locked without real adapters. This is a review/demo build, not a vehicle-certified release.
