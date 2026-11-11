# DRIVE ATLAS Family v0.7

## Implemented in the native app

- Added parked-only entry points for family conditions/course comparison and family-car memories/feedback.
- Time and maximum walking preferences, restroom/stroller requirements, and a car nickname persist locally.
- Each real public course accepts a planned rest and walking duration. Comparison includes estimated in-course driving + planned rest + walking, and explains incompatible or unknown conditions. Travel from home to the course and return is not included.
- Restroom, parking and stroller accessibility have not been verified. Required unknown facilities prevent a confirmed match; no positive facility claims are invented. Public route points are not guaranteed safe stopping places.
- Course detail connects to the existing native Pleos navigation flow. No new navigation provider, location transmission or runtime permission is added.
- Users can save self-reported family-car memories with rating and optional note. These are separate from verified awards and from the existing simulated trip/stamp engine.
- The car nickname is snapshotted per memory so changing cars does not rewrite older records. Notes and ratings persist across restart. Repeated save for the same record ID is rejected.
- The next comparison is ranked by condition conflicts/unknowns, previous user-reported course ratings, then total planned course time. Simulated feedback is excluded. This is an explicit local rule, not a claim of trained AI or live crowd forecasting.
- All writes independently enforce the existing authoritative PARKED safety state. Failed persistence does not mutate in-memory state. Existing vehicle signal staleness, route verification and location evidence policies are unchanged.
- Data stays in separate private SharedPreferences, with existing Android backup disabled. No account, cloud synchronization, external analytics or child identity collection.

## Constraints

This is a native app implementation in the existing connected prototype, not a certified physical-vehicle release. The three routes retain their existing source data. Place-level facilities, live traffic, accurate approach/return travel time, full ordered-waypoint provider confirmation, native provider basemap repair, real GPS visit certification and production cloud accounts are not completed by this change. Existing recorded emulator success is not a new test result or evidence of physical-vehicle operation.

The family memories feature is self-reported, so it must not issue a verified badge. Simulator records are explicitly marked and never boost the normal recommendation rating. The UI asks users not to enter personal identities or health information into notes.

## Verification

New `FamilyEngineTest` covers 13 cases: parked gate, input validation, missing facilities, total duration, no-match state, duplicate saves, moving/unknown memory rejection, rating-driven ordering, simulation exclusion, car-name history, persistence failure, invalid restore rows/deduplication, and restart.

Use the existing JDK 17 rather than the Android Studio bundled JDK 25 with this Gradle version. `gradlew` does not have an executable bit in this checkout, so invoke it with `bash`. Final aggregate and emulator results will be recorded after execution.

### Executed validation (2026-11-11)

- Final code: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease` succeeded using JDK 17. 58 tests passed, zero failed/error/skipped; lint zero errors and 32 existing warnings. Release is unsigned, not a release-ready distribution.
- Existing PLEOS emulator was started with its existing launch script. Existing `com.driveatlas.connected.emulator` was updated using `adb install --no-streaming -r`; prior data and permission settings were preserved. No vehicle-state injection, new permission grant, location transmission or safety bypass.
- Actual P/UX parked signals opened the UI. Seven smoke checks passed: family comparison entry; unknown required facility/no-match; real course detail and Back; explicitly simulated memory save/no repeat; simulated rating excluded from ordering; cold restart persistence with renewed P/UX gate; original home return.
- Screenshots in `evidence/family-v07/` were inspected at native emulator resolution. The family and saved-memory screens are readable without text overlaps. Phone-sized/large-font/TalkBack validation is not claimed.
- A final wording correction makes simulation saves explicitly say their rating is excluded. Aggregate checks were repeated after that correction and final APK was reinstalled for a targeted UI check.
- UI testing creates clearly marked simulated local memory records, never real verified visits. The records remain visible as test data; pre-existing data was not cleared.
- No commit, push, external publication or store deployment performed.
