# DRIVE ATLAS submission verification · 2026-11-14

## Submission artifact

- Team: SELTO SPOT; app: DRIVE ATLAS.
- APK: `apk/SELTO SPOT_DRIVE ATLAS_release.apk`.
- Size: 99,983,288 bytes (below GitHub's 100 MiB per-file limit; Git can still issue its 50 MiB advisory).
- SHA-256: `f23adf9035c5c84272b9db50ee89b6bc3a8fde75ab515d757e86cb6647dba910`.
- Package/version: `com.driveatlas.connected`, `0.9.0-roundtrip` / `9`.
- Release configuration: `DEBUG=false`, `SIMULATOR=false`; min/target SDK 28/35.
- Dedicated RSA-3072 submission certificate; APK Signature Scheme v3 validation passed. This supports the package's min SDK 28. `zipalign -c -P 16 4` passed.
- Certificate SHA-256: `b101e063524dd69eef74bd741bbd0fe8edff4880af9b61030cd6d909eca47827`.
- Signing key is outside the repository; password is stored in the owner's macOS Keychain. Neither is embedded in the APK or committed. This is competition signing, not Pleos store approval or real-vehicle certification.

## Re-run build checks

The final source was tested on 2026-11-14 using JDK 17.0.20.1, Gradle 8.10.2 and Android SDK 35. All tasks were forced to run (`--rerun-tasks`), with cached dependencies (`--offline`).

```
bash ./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --rerun-tasks
```

- Debug unit tests: **293**, failures/errors/skipped **0**, 34 suites.
- Release unit tests: the same **293** test cases also passed independently, failures/errors/skipped **0**, 34 suites.
- Debug lint: **0 errors, 18 warnings, 2 informational issues**. Warnings were not represented as resolved.
- Release lint: **0 errors, 16 warnings, 2 informational issues**. The extra release checks ran with `:app:testReleaseUnitTest :app:lintRelease --rerun-tasks` (32 executed tasks).
- Debug and release assembly: **passed**; 101 executed tasks.
- Signing and alignment verification: **passed**, on the actual `apk/` file.
- Instrumented tests and physical-vehicle driving: **not run**.

## Runtime validation

Verified the exact signed APK on **Pleos Connect 2.1.0** (`release_connect_v2.1.0`), **Android 14 / API 34**, **2560 × 1440 at 160 dpi**. The production package was absent before installation, so no existing debug/review data was replaced. The installed `base.apk` SHA-256 exactly matched the submission APK.

- Installation and first cold launch passed (observed launch time 348 ms). A deliberate force-stop and cold reopen also passed (1172 ms). These are individual observations, not performance guarantees.
- Actual vehicle service reported `P`, distraction restriction `false`, UX restrictions `0`; no park override or forced permission grant was used.
- Public course map rendered terrain, roads, coastline and the course line after approved OpenFreeMap connection consent. Consent was retained across reopening.
- All three course selections, detail screens and Back; family editor Close; new-memory Cancel and repeated Android Back passed. Cancelling left the empty journal at zero records.
- External navigation options could be opened/closed without transmitting a destination. Release correctly disabled the debug-only SIM preview.
- Location remained ungranted and unread. No GPS collection, online ETA request, native guidance request, new saved memory or vehicle-state change was performed.
- No `FATAL EXCEPTION` was found in the inspected app-process log, and no app exits were recorded before the deliberate reopen. This is a bounded smoke test, not long-running crash-free certification.

Existing-memory edit/delete, new-save-to-restore flows, actual routing guidance, live traffic, other displays and real-vehicle driving were not part of this runtime smoke. Raw screenshots, XML, fixtures and device logs remain local and are excluded from the published source.

## Source and submission reference

The APK was built from the source included with this submission, incorporating the 2026-11-13 UI refinements and planning-time tests. Historical documents retain their original dated test results. The 112 app-source/build-input files had aggregate SHA-256 `8cdf4043b42f1a9bfc49f57842005c43e927278306c7f84d62285cd256aad161` at verification (sorted relative path plus each file SHA-256). The final commit hash is obtained after the submission commit and supplied in the email, rather than a self-referential hash in this file:

```
git rev-parse HEAD
(cd apk && shasum -a 256 -c SHA256SUMS)
```

The repository remains private during preparation. [Official submission guidance](https://appdevchallenge.auto-hmg.io/guide/submission) permits private submission, requires the final commit reference, and requires public visibility by November 16 through November 23 for development-artifact bonus review. Alternatively, the source/APK archive download route is described there. Changing repository visibility and sending the submission email are separate actions; this record does not claim they occurred.

## Limits

Release has no debug-only SIM navigation replay and no prototype public OSRM road-time service. Manual plan times are estimates, not live traffic. Map connection, location and navigation each retain their own permission/consent checks. Full-course native navigation callback validation, actual visits, facilities on site and real-vehicle behavior are not certified. The screenshots in the proposal captured the separate QA build; they are not presented as a release driving test.
