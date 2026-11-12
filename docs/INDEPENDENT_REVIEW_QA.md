# Independent review and runtime regression checks

## Scope

The review covered the parked PLEOS emulator prototype, public-reference SIM navigation, saved public-map consent and local trip-memory forms. It does not establish real-vehicle navigation, live GPS guidance or production routing readiness.

Antigravity completed a read-only review of commit `33d47a8`. Its subsequent emulator inspection stopped at the service quota limit before a runtime result was available. The runtime checks below were performed separately; they are not attributed to Antigravity.

## Corrections

- Both vector-map screens now key their AndroidView host by the actual MapView instance. A retry replaces the mounted native view instead of retaining a destroyed predecessor. The existing lifecycle owner remains solely responsible for destruction.
- SIM advances at a fixed 10 m/s independently of voice state. Muting, speech failure and manual resume no longer select the former 100 m/s fast-demo pace.
- Completed speech releases its active request while retaining duplicate prevention and cooldown. Idle speech no longer produces a misleading cancellation status.
- Map withdrawal distinguishes immediate connection blocking from durable consent-file deletion. A failed deletion remains visibly pending rather than reporting success.
- Unsaved trip-memory inputs survive temporary safety-lock/background removal of the page. The editor is still entirely absent while locked, and restoring a draft never saves it automatically.
- Editing a draft clears stale save-error feedback; validation messages take precedence.

## Verification on 2026-11-12

- 287 JVM unit tests passed, with no failures, errors or skipped tests.
- Debug lint passed with zero errors, 18 warnings and two informational items. Debug, unsigned release and instrumentation APK builds passed offline.
- Three new Compose identity checks initially failed on the JVM because Android Trace was unavailable. They were moved to Android instrumentation rather than masking Android calls with default-return mocks.
- All six Android instrumentation checks passed in 11.419 seconds: three Compose host-identity tests and three real MapLibre MapView tests. The latter rendered local background-only styles, checked snapshot pixels and attached dimensions, performed two retry replacements, verified predecessor detachment/destruction, retained the same view through ordinary recomposition and created a new view after reentry. No external map request, GPS or stored user record was used by this harness.
- Public Seoul SIM started through the normal app flow. Voice ON and OFF both displayed 10 m/s. With voice OFF, the same next-turn distance changed from 183 m to 83 m across approximately 9.1 seconds between UI samples, with capture overhead; the former tenfold jump was absent.
- Manual resume retained 10 m/s and voice OFF. Background/return did not automatically restart playback or speech.
- A new, unsaved memory form retained the selected course, SIM kind and rating across Home/another-app transitions. It was cancelled without saving or changing existing records.
- One on-device utterance reported start and completion after 7.178 seconds, with nonzero RMS output. Human listening, speaker quality and pronunciation were not verified. A later utterance was deliberately stopped by the mute check and is not counted as completed.

Final debug APK SHA-256: `6a5b20e810b44c0aa9ea531b502feb82bb4e4d64647080f98e6ff098a8499b13`.
Instrumentation APK SHA-256: `5d1ba4acd813946ff2959a7b96690b3c1a70c15f472903b4b912076ff6588373`.

The local-style harness proves native view replacement and rendering, not every provider/network-failure recovery scenario. Physical-vehicle behavior, live GPS, production routing availability and narrow-window layouts remain separate verification work. No provider account, billing, security permission, real location or existing family record was changed by this review.
