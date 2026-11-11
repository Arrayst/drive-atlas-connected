# DRIVE ATLAS v0.8 implementation and acceptance

The native connected app now includes the full local family planning and memory loop. It is not a claim of production deployment or physical-vehicle certification.

## Changes

- Plans include separately entered outbound and return travel time. Missing values remain unknown, never silently zero. Course driving, planned rest/walking, and both transfers contribute to the budget. Transfer values are user-confirmed planning estimates, not a live traffic ETA and not transmitted anywhere.
- Each course has a selectable, source-reviewed rest venue. Toilets/accessibility only satisfy a preference for that selected venue, never the whole road. The app shows its source, review date, hours, restrictions and unknowns, and offers explicit public-source/map-search actions. Route coordinates are not represented as parking entrances.
- Memories can be edited with recalculated recommendations; deletion requires an explicit confirmation dialog. Model-level writes continue to require authoritative parked state. Storage failures preserve old state; old preferences and memories remain compatible.
- NaviHelper verification only accepts coordinate evidence after routing completion, resets on repeated completion, invalidates destination/waypoint changes and handles response-query exceptions. Missing and mismatching responses are distinct. Strict ordered-waypoint checks remain; no fabricated success or reordered response workaround.

## Reviewed facility sources (2026-11-11)

- Gangneung Tourism Development Corporation, Simgok parking and entrance toilets: https://searoad.gtdc.or.kr/buchae_convenient.html . Access/parking: https://searoad.gtdc.or.kr/buchae_location.html . Seasonal walking-path hours: https://searoad.gtdc.or.kr/buchae_intro.html . Restrictions: https://searoad.gtdc.or.kr/buchae_info.html . Stroller access remains unknown. The walking path itself has no toilet.
- Korea Tourism Organization, Gwanbangjerim: https://access.visitkorea.or.kr/ms/detail.do?cotId=74d46d61-8d90-4d78-bba1-2fc52e385450 . Entrance public toilet, stroller movement and step-free entrance documented. No parking capacity invented.
- Korea Tourism Organization, Chuncheon Animation Museum: https://access.visitkorea.or.kr/ms/detail.do?cotId=4116afa4-b861-4106-984e-62507fb0e810 . Toilet, ramp, lift, parking and stroller rental information. Rental alone is not treated as full stroller accessibility. Seasonal/holiday closures and external facilities on closed days are not assumed available.

Public fact checks are not live opening, availability or safe-stopping certifications. The app does not direct a driver to stop on the road. Actual facility entry is checked through venue/map directions while parked.

## Deliberate limits

- Actual start location is not sent to a routing server. Automatic live round-trip ETAs need a supported route source and a clearly authorized location-sharing flow. Current implementation completes budget planning with explicit user estimates.
- Native provider still has no response request IDs. Full-course destination/ordered waypoint response has not been proven on physical hardware; unconfirmed responses remain unconfirmed. Native basemap issues belong to the installed provider and are not falsely reported fixed.
- No cloud accounts, persistent external credentials, payment, partner contracts, location permission expansion, production signatures, publication or store release were performed.
- Testing does not inject D/R state or bypass P/UX restrictions. Simulated memories remain marked and excluded from normal recommendation ratings.

## Validation

Aggregate Gradle unit tests/lint/debug/release builds and native emulator UI checks are required. See final results below and `evidence/family-v08/`.

## Additional safety and retention fixes

The in-app public-source/map viewer now owns an independent vehicle-only P/UX connection. Its WebView starts hidden and does not load until parked is verified, hides/pauses when safety closes, and disables reload. It does not connect GPS or native navigation. Existing permitted public-source HTTPS hosts are explicit.

Local memory storage no longer silently drops the oldest record at a count threshold. It preserves all valid existing rows, visibly rejects additional saves at 500, and requires the user's own confirmed deletion to free capacity. A capacity regression test restores 501 records without truncation and rejects a new record.

Read-only independent code review found those two issues; both were fixed and re-reviewed with no further actionable findings. That review was static, not proof of physical-vehicle behavior.

### Final aggregate and family UI results

- Final JDK 17 Gradle run: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --offline` passed. 79 tests, 0 failures/errors/skips. Lint 0 errors, 32 warnings (no net new warnings). Debug APK and unsigned Release APK generated.
- Existing emulator was initially stopped; restarted with its unchanged launch script. Existing package updated with data preserved, no new permission grants or vehicle-state changes.
- Native emulator family UI passed 7 checks: parked entry/data compatibility; outbound+return total; source-reviewed selected venue conditions; native-course detail/Back; simulated memory rating edit; deletion dialog Cancel; cold-restart plan/venue restoration. Screenshots inspected with no text overlap.
- The initial automated facility assertion stopped because its text was below the viewport; a subsequent check-state locator also used clickable instead of the checkbox parent. Those test-script issues were corrected. The final complete run passed; the app did not fabricate facility matches to satisfy the test.
- Physical D/R transitions, phone-size/TalkBack layouts and live web-source availability are not covered by this UI run. UNKNOWN/non-P safety policy is covered by tests and static review; the new web gate was code-reviewed, not represented as physical-driving certified.

### Actual native-provider test

The final APK submitted the existing public Damyang course to the installed emulator provider. The provider generated native guidance, foregrounded itself, and emitted start/completion callbacks. Destination response matched the requested final coordinate; two waypoint responses did NOT match the two ordered course coordinates within the existing 100m threshold. The app correctly retained “목적지 일치 / 경유지 대조 불일치 · 전체 코스 확인 보류”. Logs: `evidence/family-v08/native-callbacks.log`; native-screen screenshot: `06-native-result.png`.

The initial test script expected the assistant app to remain foregrounded, so its final home assertion failed after the provider opened its own guidance screen. The app was brought back explicitly, and its connection status was inspected rather than treating the empty UI query as success or as an app crash. The replay script now explicitly returns after the route request. No second route request was needed to inspect the result.

This is a verified external provider compatibility limit, not a completed full-course confirmation. Native guidance is not physical movement and not proof of visiting the course. The test changed only the emulator navigation route and did not transmit the user's physical location or grant any permissions.

### Native navigation follow-through

A deeper diagnostic established that the provider returns both waypoint coordinate pairs as `(0,0)` with correct names/order and no display coordinates. The final app therefore adds explicit single-destination navigation per course leg instead of weakening full-course checks. All three Damyang leg requests were confirmed on the emulator with fresh completion, correct destination and no remaining waypoints. See `NATIVE_NAVIGATION_V08.md` for evidence and scope. The final aggregate count is **81 passing tests**, lint **0 errors / 32 warnings**, both builds successful.
