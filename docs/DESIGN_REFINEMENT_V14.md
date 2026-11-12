# Map-first family drive design refinement

## Direction and scope

A family drive planner should make the chosen road, travel time, stops and next action easy to scan. This pass changes hierarchy and layout rather than adding decorative gradients, more generic cards, fabricated ratings or unrelated vehicle controls.

The supplied Keyflow article body was not accessible, so this work does not claim to reproduce its exact instructions or run its tool. General visual-refinement principles were considered from the public candidate reference https://github.com/heydamir/unslop without installing or executing it.

## Changes

- Home uses numbered route rows with actual bundled road distance/time and smaller photos. The map has more visual space; selected route, focus and action are distinct.
- Navigation uses an underline instead of six equal pill containers. Controls retain accessible touch areas and visible focus. Narrow windows stack the header and content.
- Course detail keeps its newly added in-app preview primary action, with a bounded button and external navigation clearly secondary. Selection, return target, family stops and simulation restrictions remain intact.
- Detail, curation and preview map/instruction panels use available-width breakpoints rather than forcing large minimum-width columns.
- ETA separates outbound, course and return road time, displays actual endpoint labels and itinerary order, and keeps live-traffic/stop-time limitations explicit.
- Family planning separates driving, resting, walking and buffer time, groups related conditions, shows minute units, and preserves warning/unknown states.
- Route selectors retain selection semantics and visible keyboard/rotary focus. No SDK, route geometry, permission, storage or driving-safety behavior was changed by this design pass.

The stamp collection and all historical-record layouts were not comprehensively redesigned. No new vehicle lock, charging or other control feature was added.

## Verification

- Full offline aggregate succeeded: unit tests, lint, debug APK and unsigned release APK.
- 166 unit tests; zero failures/errors/skips.
- Lint: zero errors, 33 warnings.
- Diff whitespace checks passed.
- Debug APK SHA-256: `dc92fc16562f288bb0856c6156bf8322c3a8e088dd317ad6211c32050bc0bd82`.

This APK includes the uncommitted v13 course-to-preview integration. Earlier APK hashes and screenshots are not evidence of the new visual output. Existing screenshots informed the audit, but fresh emulator visual inspection, normal PLEOS container bounds, narrow-window appearance and interaction regressions remain pending permission to install and interact with the emulator. No device installation/input, screenshot of the new build or repository publication was performed during this pass.

## Approved emulator check and continuous SIM follow-up

The first design APK (`dc92fc…bd82`) was subsequently installed with data preserved and launched through the PLEOS app list. The platform reported `ai.umos.applibrary` as launcher, DRIVE ATLAS in its app multi-window area, and DrivingView visible beside it. Home → course detail → the selected-course in-app preview worked. This does not establish the exact cause of the earlier abnormal fullscreen launch. Additional input was paused when an unexpected consent dialog suggested concurrent interaction.

A later request added optional continuous SIM speech and a compact result layout. After calculation, setup collapses behind a visible settings action; map, simulated position, next instruction, distance and playback/voice controls become primary. Speech is initially off and uses PLEOS ON_DEVICE only. Opting in changes replay to 10 m/s; speech off retains 100 m/s. Guidance uses two approach bands (800 m and 200 m), per-step deduplication and an eight-second minimum gap. Stale speech is cancelled when the step changes or after four seconds; dense instructions can still be skipped. Near-turn language avoids claiming a false 100 m distance. Pause, leaving/backgrounding, focus loss and errors turn continuous speech off without automatic resumption.

This is not live-GPS navigation. No route ownership, operating routing contract, automatic rerouting or physical-vehicle validation was added. The voice pipeline still uses a 200 ms focus-settling delay rather than verified PCM-level preroll.

Latest aggregate: 174 tests passed, no failures/errors/skips; lint zero errors and 33 warnings; debug and unsigned release builds succeeded. Latest debug APK SHA-256: `0fa28f456c1b5e58cf3945363f7d816fcb60a126c5207e974bc393b80b4818dd`. This later APK's installation, continuous audio runtime checks and new compact-result screenshot verification are pending safe resumption of emulator interaction. Earlier images do not show that final result layout.

## Continuous speech runtime correction

The first continuous-speech build was installed and its compact route view verified in the normal PLEOS app container with DrivingView present. Runtime testing exposed cancellation immediately after voice initialization. A paused one-shot utterance completed and audio output frames increased, so this was not treated as successful continuous guidance.

The observer now lives for the speech controller lifetime rather than restarting for each replay/opt-in state change. Initialization has a separate 15-second limit; the four-second stale-speech limit begins at the actual start callback. Turning continuous guidance off updates its status text, while SDK errors retain their explanation. Latest aggregate after this correction: 177 tests passed, debug/unsigned release builds and lint succeeded. Corrected APK SHA-256: `0cdd7522f19fad7cf746325aec37430339804d2d46303dd5b5236cc4842979d3`. Corrected-build runtime results will be recorded separately.

## Follow-camera simulation refinement

The corrected continuous-speech build completed three automatic cues in runtime checks (right turn, slight right turn and left turn). Pause turned speech off, and background/return did not resume speech or replay. One longer cue did not report completion; continuous phrases were subsequently shortened without extending the safety timeouts. Manual one-shot preview retains its simulation introduction. No human listening or first-syllable quality claim is made.

The follow-camera build uses the ordered replay distance for position interpolation and direction, avoiding ambiguity where outbound and return geometry overlap. The map follows the simulated marker, manual pan disengages follow, and a whole-route control restores the overview. Only visible tiles are requested; a bounded memory cache and cached route projection avoid repeatedly loading tiles or reprojecting the entire route on each frame. The next-turn distance is enlarged and low-priority testing controls are collapsed. This is still a north-up simulation map, not live-GPS or production turn-by-turn navigation.

Follow-camera aggregate: 189 tests passed, debug and unsigned release built, lint passed. APK SHA-256: `fe07fe244e602840d2a7a4042a81009083957d53aed5da650f23818e283edc37`. Final visual/runtime checks are recorded below when complete.

## Final verified build

Final APK SHA-256: `9edd530b9e27c3dc49edea2caf7c8de4dd8e3d8c67dbd405220d7535d3fe80ec`. All 190 unit tests passed; lint reported zero errors and 33 warnings; debug and unsigned release builds succeeded. Follow zoom is 16. The same-instruction speech limit is six seconds, while changed instructions are still cancelled promptly, initialization remains bounded to 15 seconds and the eight-second cue gap remains.

This build was installed without clearing user data and launched using the normal PLEOS system app icon. DrivingView remained visible beside the app. Detailed map follow, direction, large next-distance display and whole-route overview were visually checked. Manual pan disengagement and re-follow were also verified in the immediately preceding follow build; that behavior did not change. Three automatic utterances completed (4.433, 2.987 and 3.069 seconds), including slight-right instructions. Pause disabled continuous speech; background and return did not restart playback or voice. No app ANR or fatal exception was observed.

A 15-second silent emulator recording and three screenshots show only the public demonstration route. These are not recordings of real driving or audible voice quality. Human listening, first-syllable quality, interruption latency during speech, physical-vehicle operation, live GPS, automatic rerouting, production routing availability and platform navigation route ownership remain unverified or unimplemented. The app was left at the first instruction with replay stopped and voice off.
