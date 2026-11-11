# Family itinerary, explicit feedback and travel memories

This iteration closes specific gaps between the proposal and the earlier prototype. It is not a production-provider, physical-vehicle or certification claim.

## Planning behavior

- Optional maximum continuous driving; departure and return-deadline times, explicit next-day return, and separate reserved extra time.
- Ordered rest choices with insertion positions before the first course point or after a chosen course point. Base course-point order stays unchanged. Per-stop rest minutes are explicit; zero minutes do not count as a driving break. Missing break durations remain unknown.
- Selected facilities become real routing coordinates, not only comparison labels. Coordinates are sourced public map markers/POI representatives, not independently verified vehicle gates or safe stopping permissions. Source and uncertainty are shown before transmission.
- Actual returned road legs support continuous-driving groups; road shaping points do not become rest breaks. No equal-time division or invented road durations.
- Road ETA identity includes stop coordinates/order/placement. Plan changes invalidate stale results; applying a result uses current break durations. Legacy data is migrated without manufacturing travel dates or silently splitting rest durations.
- Family controls use draft/save/cancel. Changing a plan and discarding it must not persist the draft.

## Feedback and memories

Explicit choices to drive less, rest more or walk less affect deterministic ranking and show their reasons. They can be changed or cleared. Notes are not mined for preferences. This is not trained AI or live traffic prediction.

Memories add optional actual travel date and feeling tags, with edit/clear support. Old memories keep their recording timestamp; an absent travel date is displayed as unspecified. Simulation records stay marked and excluded from normal ratings. No visit certification is issued by entering a date or feeling.

## Navigation and service boundaries

Selected rest coordinates are included in the edited itinerary. Native full-route requests are limited by the documented SDK capacity; oversized itineraries must not be silently truncated or reported as confirmed. Individual-stop guidance provides a manual alternative, selected only when safely parked. Kakao route capacity is also checked separately.

Public OSRM remains debug-only with request-specific consent. Release still requires an appropriate operating provider. No keys, commercial contracts, account creation, private GPS transmission or deployment were added by this work. Facility operational hours, entrance suitability and real-time parking availability remain unverified beyond their named public sources.

## Validation

Final integrated unit suite: 131 passed, zero failures/errors/skips. Debug lint has zero errors and 32 existing dependency/platform warnings. Debug and unsigned release builds succeeded.

Actual emulator checks confirmed draft/cancel preservation, explicit overnight schedule save, schedule clear/cancel versus clear/save, feedback save/clear, two public rest selections with order/placement and individual durations, canceled rest removal, routing-consent cancellation, real public two-rest ETA calculation/application, simulated memory save, preserved old memories, canceled metadata clearing and travel-date editing.

The public Seoul City Hall → coastal course with mid-course Simgok and post-course Hourglass parking marker → Seoul test returned 482.2 km and 406 road minutes (197 outbound, 11 course, 198 return). Adding 45 minutes of explicitly planned rests, 20 minutes walking and 30 minutes extra produced 501 minutes. The calculated longest continuous-driving block was 200 minutes. The actual UI showed a conflict at a 60-minute limit and removed that specific conflict at 240 minutes; the final media uses the explicitly chosen 240-minute demo condition, not the proposal's 60-minute example. These are a public-reference road-model test and user-entered plan, not a family journey, traffic estimate or verified arrival.

The app's own map line remains the base-course snapshot. Selected rest coordinates affect routing calculations and external navigation requests; the snapshot is not redrawn as live detour geometry. Native full-route capacity remains three total points; longer itineraries have explicit individual-stop alternatives, not a false full-course success.

Raw captures remain local and ignored. Physical vehicle, installed-Kakao app branch and production-provider operation are not validated by unit tests or the emulator. No photos/videos are published in source.
