# Trip console redesign

## Purpose

The earlier family ledger and route-time configuration form hid the actual decision. This revision puts the chosen trip, understandable hours/minutes, confidence of the input and one next action first. Blue action color, white panels, consistent spacing and system Korean type support that hierarchy; color alone is not the redesign.

- Home pairs the selected real catalog route with its map and one route-detail action. Equal navigation cells and shared gutters align the shell. The action label is centered independently of its trailing arrow; route cards reserve the same status-icon slot.
- Round-trip time uses a departure/route selector beside the result. All route options share a leading icon and text axis. The result separates outbound, course and return time; cached/public-origin and non-traffic limitations remain visible. Unknown results are not shown as zero.
- Family comparison shows three alternatives beside the selected plan, separating whole-outing duration from driving segments, rest and walking. Manual inputs are identified beside the total. Unknown conditions remain unknown. Settings open in a dismissible dialog with a fixed close header and aligned minute-option cells.
- Main actions and map zoom controls use 76dp minimum targets. Secondary text contrast uses the shared opaque muted token; no animation was added. A narrow-width stacked layout is implemented, but is not yet verified on a portrait device.

## Reference scope

Official reference images and guidance were read, not copied as app assets. No third-party design scripts were installed or executed. Toss component documentation is a reference for hierarchy, not evidence that the app reproduces a production Toss screen.

- [PLEOS design introduction](https://document.pleos.ai/en/docs/connect/guide/docs-design/Introduction/intro)
- [PLEOS driver distraction guidance](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)
- [PLEOS screen model](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)
- [Google automotive sizing](https://developers.google.com/cars/design/automotive-os/design-system/sizing): source of the 76dp target guidance, not claimed as a PLEOS certification requirement.
- [Google map and content](https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/map-content-template)
- [Toss TDS Top](https://tossmini-docs.toss.im/tds-mobile/components/top/) and [ListRow](https://tossmini-docs.toss.im/tds-mobile/components/ListRow/list-row-overview/)
- [Tesla route context](https://www.tesla.com/ownersmanual/model3/en_us/GUID-01F1A582-99D1-4933-B5FB-B2F0203FFE6F.html)

## Preserved boundaries

Presentation changes do not authorize location transmission. Request-specific routing consent, public versus private origins, public-result cache policy, safety gating, ranking and persistence remain in force. No synthetic visits, ratings, facility verifications or route shapes were introduced. New raw emulator captures remain local and ignored by git.

The public routing endpoint remains debug-only; release needs an appropriate production provider. Native multi-waypoint coordinate verification remains provider-limited. There is no physical-vehicle validation, production deployment, signing or automotive certification claim. Refer to ROUNDTRIP_V09.md and VEHICLE_RELEASE_READINESS.md for those boundaries.

## Validation

Final alignment revision: `testDebugUnitTest`, `lintDebug`, `assembleDebug` and `assembleRelease` passed. Unit tests: 105, zero failures/errors/skips. Lint: zero errors and 32 warnings (dependency updates 24, Android Gradle plugin 3, version-catalog suggestions 2, ExifInterface 1, target API 1, backup extraction rules 1). These warnings remain, not silently waived.

On the installed landscape PLEOS emulator, verified route-time consent cancellation without a new routing request, loading and applying an existing public cached ETA, repeated family-editor Close/Back dismissal, keyboard traversal and visible primary-action focus. Actual home, ETA/result, family/editor and focus screenshots were visually inspected. Cache labels and manual-input confidence appear beside the decision; selected/unselected options have a common text axis. No private GPS submission or new routing request was used for this UI pass.

Portrait/narrow rendering and physical-vehicle operation were not tested. Existing safety logic and unit tests remain, but this UI pass is not a new physical driving-safety validation. A successful build does not establish certification or release readiness.
