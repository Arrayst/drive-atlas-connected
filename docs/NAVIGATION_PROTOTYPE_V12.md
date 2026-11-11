# In-app navigation replay prototype

## Scope

This is a parked, emulator-only development prototype, not certified vehicle navigation. It uses explicitly selected public departure references, the chosen course, ordered selected rest places, and return to the original reference. It does not read or transmit a home address or current device GPS for this feature.

The production gate must remain closed: no new Distraction Optimized or APP_MAPS declarations, no vehicle security or permission changes, and no claim of owning an active navigation route. Existing native navigation remains separate.

## Data and provider limits

Public route calculation uses an explicit user action and disclosure. Coordinates can be retained by the routing provider in server logs. The prototype must not issue requests on each replay tick, retry indefinitely or log/cache private coordinates.

OSRM geometry and maneuver steps are actual routing results, not a straight-line fallback. No traffic, road closure, lane-level or on-road safety guarantee is offered. The public demo is not an operating navigation service or SLA.

- OSRM route API: https://project-osrm.org/docs/v5.24.0/api/
- OSRM demo policy: https://github.com/Project-OSRM/osrm-backend/wiki/Demo-server
- Map tiles: https://operations.osmfoundation.org/policies/tiles/

Only currently visible OSM tiles are requested with app identification, HTTP caching and visible attribution. A fixed route overview avoids auto-following replay through new tile areas. No bulk prefetch or offline-region download is offered.

## Safety and audio

The prototype is visibly marked as simulation and is removed when the existing parked-only safety gate closes. Pause, Stop, leaving the screen and app backgrounding must stop replay and any speech. Location replay must not be treated as evidence of real driving or arrival.

Audio is an explicit one-shot instruction preview while replay is paused, not automatic continuous guidance. On PLEOS it uses official TextToSpeech SDK 3.3.1 with explicit `Mode.ON_DEVICE`, Korean locale, and the existing local SpeechService. No app registration, credentials, HYBRID/SERVER mode, microphone permission or external TTS installation is used. The normal TTS service permission and narrowly scoped package visibility are declared.

Each request has a cancellable generation token, temporary audio focus and a 30-second timeout. Completion, error, focus loss, leaving/backgrounding and Stop release the service and focus; late callbacks cannot restart playback or complete a replacement request. Repeat clicks cannot overlap PLEOS speech. The PLEOS SDK exposes no documented PCM/silence insertion API, so its 200 ms focus-settling delay is not claimed as data-level preroll. Without the PLEOS service, Android TTS is restricted to a successfully selected, installed offline Korean voice and requests a 200 ms silent utterance.

Actual digital audio production and speaker listening are separate checks. SDK callbacks alone do not prove audible playback, pronunciation quality or the absence of first-syllable clipping.

- Official local-only mode: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro

- PLEOS Maps/Navigation rules: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide
- PLEOS driving restrictions: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- PLEOS voice preroll guidance: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide

An eventual real navigation product requires route ownership/focus, appropriate category and permission declarations, driving-optimized UX, reliable location and road matching, interruption handling, suitable providers and physical-vehicle validation. Enabling a manifest flag alone is not that validation.

## Verification

Implementation includes full ordered route/step validation, monotonic-distance replay that cannot jump between overlapping outbound/return roads, bounded HTTP with cancellation, and a shared 1.1-second request budget with the existing ETA feature. An explicit synthetic offset demonstrates off-route detection and pauses replay. Automatic rerouting is not implemented; requesting the original complete itinerary again is not described as rerouting from an off-route position. Rest points are preserved, but replay does not wait the family-planned rest durations.

Actual public-request validation returned the Seoul reference → coast course → two selected rest places → Seoul road route (482.2 km), including an initial right-turn instruction for Sejong-daero at about 104 m. The emulator had no available offline Korean TTS voice, so text-only fallback was verified. Audible output, the first syllable and preroll audio quality were not verified. The existing emulator launch configuration also disables audio.

The earlier replay verification below remains valid; the later voice integration and its final verification are recorded separately.

### Final build checks

- 158 tests passed, zero failures/errors/skips.
- Lint: zero errors, 32 existing warnings.
- Debug and unsigned release builds succeeded; `git diff --check` passed.
- Final debug APK SHA-256: `ca537ac9e4922e6abcb727582c46d75a6111220d52b8ec83efab634d85eef13c`.
- Read-only review findings on offline voice selection verification and the synthetic off-route map marker were corrected before this aggregate run.

Runtime checks confirmed replay progress, pause holding distance, and restart returning to the original public-reference origin. Moving Settings to the foreground and then returning reset the prototype to its setup screen without automatic replay resumption. The PLEOS Home button alone left the app visible in its container, so that action was not treated as proof of backgrounding.

At that earlier checkpoint, physical vehicle operation, live GPS tracking, address search, automatic rerouting, live traffic, automatic continuous speech and verified audio output were outside the completed prototype. The on-device voice follow-up below supersedes only the digital audio-output limitation. The public route response and unit-test geometry are not real-driving evidence.

Final emulator replay checks also demonstrated a public-derived synthetic offset about 2603 m from the local route window, stopped replay, and moved the displayed simulation point. No rerouting request occurred.

Capture/runtime distinction: final route/voice-unavailable/off-route captures and replay checks use APK `688ff1e407140a882b4584cfb46edd1b84ece7c62990728549ac9b52f5e0424f`. The later final build hides stale turn distance while off-route, labels remaining distance as the last valid route value, and clears an old deviation message on reset/stop. Those last presentation changes were source-reviewed and included in the final aggregate, without another public route request. The Stop button itself was not separately exercised at runtime; restart and leaving/backgrounding were exercised.


## On-device voice follow-up

The emulator was restarted with audio output enabled, preserving app data and without enabling host microphone input. The installed PLEOS service, rather than an absent Google/offline Android voice, is the available synthesis provider. No new voice package, default-TTS setting or account was installed/configured.

Final integration checks: 162 tests, zero failures/errors/skips; lint zero errors and 33 warnings (one additional version-catalog style advisory for the new SDK dependency). Debug and unsigned release builds succeeded. Debug APK SHA-256: `9759987a1b8dbcb3e6a2711078827ea6eb36f6f403986bd226a2285fb6b9e491`.

Actual local-mode synthesis produced `ready`, `start` and `done` events, with a nonzero peak RMS of 6611.72 over about 3.1 seconds. The request used the public-reference route and no private location. AudioFlinger output frames increased from 237312 to 472320 during the check, providing digital output-path evidence beyond callbacks. Two rapid preview taps issued only one speech request. Restart during speech stopped it without a stale completion update; moving Android Settings into the foreground and returning reset the prototype to setup without autoplay. There was no self-focus collision. Speaker listening, Korean pronunciation quality, first-syllable clipping, PCM-level preroll and competing third-party audio focus were not verified. This is still a paused, one-shot simulation preview, not continuous voice navigation or automatic rerouting.

Raw screenshots, device logs and local QA manifests are deliberately excluded from source control. Some setup/route captures were refreshed for the voice build; older voice-unavailable captures describe the previous Android-TTS-only implementation, not the final PLEOS integration.
