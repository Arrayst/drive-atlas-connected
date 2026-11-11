# Navigation dialog and Kakao handoff

## Reproduced issues

The app-owned navigation dialog used an existing default gray dim but presented an oversized scrolling list; Close was below the initial viewport. The revision strengthens background dim and shadow, keeps header/Close and the primary action visible, and puts alternative/experimental actions in disclosure. It does not claim that dim was entirely absent before.

Kakao's official route URL was valid. On the emulator, no Kakao app was installed. The mobile WebView reached an installation interstitial, then showed route text with a blank map. The web fallback now requests the public desktop route page. Actual emulator inspection showed map tiles, the blue driving route and the three public coastal course points (departure, intermediate waypoint, destination), with Kakao's 15-minute/6.4-km result at test time. This is an external web route, not PLEOS native driving guidance or the app's round-trip ETA.

## Changes and safety boundaries

- Explicit app-window dim, rounded/elevated dialog, fixed Close, consistent control alignment and one main action.
- Connector callback/requests/failure are never styled as verified success. Only coordinate-verified connector results use the confirmed state; essential raw status remains visible.
- Kakao is opened directly using its documented route scheme when its app is installed. Otherwise the public route opens in the existing parked-only web viewer. No account, API key, installation or personal GPS transmission was introduced.
- Main-frame network and HTTP errors are retained rather than overwritten by a subsequent page-finished callback.
- Vehicle P/UX gating, blocked file access/geolocation/mixed content, and the lack of a JavaScript bridge remain. The app does not claim control of the third-party map's layout or route calculation.

Official route contracts: https://apis.map.kakao.com/web/guide/#routeurl and https://apis.map.kakao.com/android_v2/docs/api-guide/urlscheme/.

## Validation scope

Final combined unit tests: 109 passed, zero failures/errors/skips. Debug and release builds and debug lint passed; lint retains 32 dependency/platform warnings and zero errors. Actual emulator checks cover the compact dialog, advanced-section scrolling with pinned header/footer, repeated Back/reopen/Close, outside-tap dismissal without activating underlying navigation, and keyboard focus. The three-point Kakao web route was inspected visually, not inferred from a page-loaded callback. Native viewer controls use 76dp targets; Kakao page controls remain third-party content. The desktop page leaves some unused lower viewport space; its content was not altered or injected into. The installed-Kakao app branch is not runtime-tested because the emulator has no Kakao app. No physical-vehicle validation or certification is implied. New captures remain local; they are not included in public source.
