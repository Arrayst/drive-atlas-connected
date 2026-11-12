# Course-to-in-app route preview

## Implemented

The course-detail primary action now opens DRIVE ATLAS's own map preview instead of an external navigation selection dialog. The selected course stays locked for this detail-origin preview. The saved family rest selection and order feed the same checked route request. Screen Back and Android Back return to the same course detail. The existing ETA entry still permits choosing a different course. External/native navigation options remain explicitly separate and secondary; entering the preview does not launch them.

The preview first displays the bundled base-course road map. This is clearly distinguished from the full public-origin → course/rests → return route, which requires an explicit disclosed OSRM request. No straight-line or unverified geometry is substituted. Detail and preview controls wrap at narrow widths, and the main header uses two rows below 700 dp.

## Not a production navigation release

This remains a debug, emulator, parked-only route replay. It does not acquire live GPS, introduce a personal address lookup, own a vehicle active route, enable in-motion operation, offer continuous automatic speech, or implement automatic rerouting. Existing on-device speech is an explicit paused one-shot preview. A production vehicle route provider, route focus, location matching, driving UX validation and physical-vehicle testing remain separate requirements.

## DrivingView investigation

Official PLEOS guidance says DrivingView is a fixed vehicle-information area which an app must not hide or terminate. Landscape uses the two app windows, not the vehicle-information region.

- https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model
- https://document.pleos.ai/en/docs/connect/guide/docs-design/getting-started/layout

Read-only inspection first found DRIVE ATLAS as a fullscreen task on the primary display, outside the visible normal home layout. The app has no fullscreen/immersive or DrivingView-hiding code. Later inspection, without our input, showed normal DrivingView and navigation windows launched by PLEOS Home. Both system processes were present. Direct ADB activity launch bypassing the Home container is a plausible cause, not yet a reproduced conclusion.

Future UI QA should launch DRIVE ATLAS through the visible PLEOS Home app list and verify DrivingView visibility and app window bounds before functional testing. Do not force window bounds, disable system apps, change security settings, or interpret the abnormal fullscreen capture as an approved platform feature.

## Verification status

- 166 unit tests passed; zero failures/errors/skips.
- Lint: zero errors and 33 warnings.
- Debug and unsigned release builds succeeded.
- `git diff --check` passed.
- Debug APK SHA-256: `b1c68239c49d3e368c2796aa25e1ae7e39fbc61583079af3620ffa196eec5faa`.
- New tests cover all catalog-course entries, detail/ETA back targets, rejection of invalid entries, and all four safety-gate conditions.

Latest integration APK installation and emulator visual/interaction testing are pending permission to interact with the currently used emulator. No claim of successful new screen navigation, narrow-window rendering, system-container restoration or final runtime regression testing is made at this checkpoint. Earlier v12 route/audio runtime results are recorded separately and do not replace verification of this new flow.

## Integrated follow-up verification

The course-detail entry was subsequently installed and exercised in the normal PLEOS app container with DrivingView visible. It preserves the selected course and opens the in-app simulation. The later follow-camera and continuous-speech verification, final APK identity and remaining limitations are recorded in DESIGN_REFINEMENT_V14.md.
