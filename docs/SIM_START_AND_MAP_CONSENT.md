# Explicit SIM start and saved public-map consent

## Scope

The parked debug/emulator prototype now exposes **길찾기 시작 · SIM** directly on the map. The selected public departure, course and existing rest plan remain visible and editable before starting. No real GPS, home address, family content, driving permission, native-provider route ownership or cloud voice is added.

## Start sequence

1. One explicit start action opens the public-map disclosure if it has not been accepted before. Accepting only the map connection does not send routing coordinates.
2. The existing separate OSRM disclosure shows the selected public departure, course, rest names and exact coordinate sequence for this request. It explains that confirmation starts SIM after the route and road map are ready. The on-device voice option is visible and can be unchecked; unsupported environments explicitly use text only.
3. After confirmation, one generation-scoped start ticket waits for the OSRM result and the new route's actual rendered road features. Neither a previous course viewport nor an earlier request's callback can satisfy it. No recomposition, screen entry or lifecycle resume creates a start ticket.
4. The ticket is consumed once to start SIM, with the explicitly selected on-device voice choice. Request/viewport loading exposes **시작 취소**. Failure, cancellation, Back, leaving the screen, backgrounding, withdrawal or loss of P/UX eligibility invalidates pending work.
5. Partial tile loading holds simulation advancement; a confirmed empty viewport, provider failure or continuous 18-second loading timeout stops replay and speech. Recovery never restarts a cancelled session. A manual **SIM 재개** after pause leaves voice off; voice requires its own button again.

OSRM consent is not persisted and new network route requests still require their own disclosure. Existing routes can be manually resumed without another OSRM request. The replay remains a simulation, not a real trip or visit record.

## Saved map consent

- The first explicit **동의하고 연결 기억** saves only the provider/resource/terms/privacy identity, public-course/reference-map purpose and disclosure version to a separate private atomic file. No coordinates, family data, GPS, route consent or voice preference is saved there.
- The scope explicitly covers public course exploration and public-reference SIM maps, so navigating between those screens or restarting the app does not repeat the same question. Release public-course maps remain available; release/non-simulator replay stays blocked regardless of consent.
- Any provider, resource endpoint, purpose or disclosure-version mismatch is treated as unaccepted. Old session-only consent is not migrated into stored acceptance.
- **지도 연결 동의 관리** is available through the public map's attribution/information dialog and the parked SIM menu. Withdrawal immediately removes the accepted map composition, invoking native MapView pause/stop/destroy, and deletes the saved token. The next connection needs explicit consent again. A failed write never grants connection; failed deletion disconnects the current process, reports the durability problem and offers a deletion retry.
- Map connection and routing dialogs are sequential, not stacked. All controls remain gated by foreground P/UX eligibility. App data, family memories and existing rest plans are retained.

## Voice bound

The continuous on-device speech scheduler previously cancelled a still-current utterance at six seconds after playback start. Its bounded playback allowance is now twelve seconds, separate from the fifteen-second initialization allowance and the controller's thirty-second absolute request timeout. Step changes, pause, background, focus loss and errors still cancel immediately. No new voice provider, installation, microphone permission or network mode is used. Unit tests cover both completion beyond six seconds and the retained upper bound. Audible output and pronunciation require runtime verification.

## Verification contract

Unit coverage includes first acceptance, process recreation, shared public-map scope, release restrictions, provider/purpose/version changes, malformed saved values, withdrawal and persistence failures; it also covers one-shot starts, missing route/map readiness, generation/request mismatch, cancellation/withdrawal, background, P/UX closure, recovery without restart and partial-versus-failed map loading. Full unit/lint/debug/release checks and physical UI/audio checks must be reported separately. No runtime pass is inferred from compilation.

## Recorded emulator QA (2026-11-12)

Final debug APK SHA-256: `02a9c883779a77dfe9df8831da8497fbebad51b27fbdb533059b7494486d4dc1`.

- 269 unit tests passed (zero failures, errors or skipped); lint had zero errors, 18 warnings and 2 informational items; debug and unsigned release builds passed.
- The emulator was restarted with its existing AVD data preserved. The final app opened through the normal PLEOS Home container, with DrivingView and multi-window bounds maintained.
- Explicit start approval proceeded through actual map readiness to 10 m/s SIM and on-device voice without a separate replay-button tap. The first three speech requests reported start and completion (2.234 s, 2.271 s and 5.437 s). Human listening and speaker quality were not verified.
- Shared map consent survived course/SIM navigation and an actual app-process restart through the normal app launcher. Withdrawal required acceptance again. Cancelling route approval, Back and background/return did not automatically resume replay or speech.
- Satisfaction form QA was non-saving: a new response required an explicit course, actual/SIM type and labelled rating; Save remained disabled until all three were selected. Optional details expanded, cancelling and reopening reset all choices, and an existing record's edit was cancelled. No existing memories were changed or deleted. Runtime persistence of a newly submitted response was not exercised to avoid creating unwanted records.
- A 20.0116-second silent screen recording shows public Seoul SIM movement, a turn and decreasing maneuver distance. Frames sampled once per second across the entire clip showed only public demo and vehicle UI. It is not actual driving footage or a speaker recording.
- Final state: map visible, replay stopped, voice off. Physical vehicle/GPS operation and narrow app-window layouts remain unverified.
