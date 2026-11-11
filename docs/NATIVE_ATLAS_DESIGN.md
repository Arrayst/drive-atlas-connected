> Historical workbench iteration. The current trip-console layout and validation scope are documented in [TRIP_CONSOLE_DESIGN.md](TRIP_CONSOLE_DESIGN.md).

# Native atlas workbench

## Intent

The previous home presented three identical category cards, while the useful objects were actual roads. The redesign puts a selectable route index beside the actual OSM/OSRM map. Family comparison uses horizontal rows; the local journal puts entry and existing records side by side. One horizontal task bar replaces a sidebar plus duplicate shortcut pills.

Use neutral mineral surfaces, graphite navigation, restrained rust selection/action, system Korean typography, straight rules and shallow corners. No animated decoration, invented statistics, stock substitution, fake visits or new route claims.

## Guidance actually read

- Hallmark v2.0.0: https://github.com/Nutlope/hallmark/blob/main/skills/hallmark/SKILL.md
- Hallmark app-screen branch: https://github.com/Nutlope/hallmark/blob/main/skills/hallmark/references/flow.md
- Hallmark redesign: https://github.com/Nutlope/hallmark/blob/main/skills/hallmark/references/redesign.md
- Taste Skill v2.5.1: https://github.com/Leonxlnx/taste-skill/blob/main/skills/taste-skill/SKILL.md

These were read as public design text, not installed or executed. This is a Kotlin Compose automotive app, not a website: no CSS scaffold, random web-theme dealer, web gate scripts, hover choreography, shader or marketing hero was run or claimed as passed. Hallmark's app-screen branch specifically favors familiar task structure, density, comparisons and real content over marketing composition. Taste's real-material and consistent-edge principles informed the layout. Its marketing page/animation rules are deliberately not ported to a parked-vehicle interface. Existing safety/consent/source disclaimers are retained even where they use older punctuation. Touch targets remain at least 56dp for main actions, with instant focus feedback and no reveal animation.

## File and handler inventory

- `AtlasDesign.kt`: horizontal shell; route selector + map home; category links still open recommended, season and theme filters. All three catalog routes remain selectable. Selected-map details, source dialog, trip continuation, atlas and connection status remain reachable. Existing detail/curation and atlas handlers are untouched.
- `FamilyScreen.kt`: family preferences and nickname editing remain; ranking is unchanged. Each comparison retains total/partial times, conflicts, unknowns, rating provenance, planning and navigation-detail actions. Incomplete transfers are now headed “왕복 미완성”, with known minutes secondary. All planning controls and facility-specific sources remain.
- `FamilyScreen.kt` journal: car identity and actual stored counts, route/rating/note/simulated entry, save and duplicate prevention, compare, edit/cancel and confirmed delete/cancel remain. Empty and 500-record states remain. No new memories are fabricated for the layout.
- `MainActivity.kt`: presentation tokens, horizontal shell and duplicate-navigation removal only. Page dispatch, all safety checks, native navigation verification, consent, family persistence and actions are unchanged.

The three screenshots from the preceding version were inspected before implementation. Final native captures and checks are recorded separately; web-linter or full skill compliance is not claimed.

## Copy and navigation changes

- Home “어떤 길로 떠날까요?” and the instruction to choose a large card became “오늘의 길을 고르세요” and an instruction to inspect roads and stopping plans.
- Duplicate top shortcuts “가족 조건 · 코스 비교”, “가족 차 기억 · 피드백”, “자동 왕복 시간” are represented by the task-bar destinations “가족 비교”, “우리 차 기억”, “왕복 시간”. Their destination handlers are unchanged.
- The season/theme category entry labels are shortened to “계절별” and “테마별”; their destination headings and filters remain.
- Family description now tells the user to compare round-trip time and unresolved conditions. A partial total is no longer the headline: “왕복 미완성” leads, with the known subtotal immediately below.
- Decorative English section labels were removed from family and journal; the app wordmark/footer uses “Drive Atlas”. Existing safety, consent, service and license statements remain.

## Verification

Final source passes 102 unit tests with zero failures/errors/skips, `lintDebug`, `assembleDebug` and `assembleRelease`. Lint retains 32 existing warnings; a warning-free or production-certified result is not claimed. No tests were weakened or deleted.

Native PLEOS emulator screenshots were reviewed before and after. The first header measurement defect was caught and fixed before completion. Further review led to a compact family summary, distinct green success/amber unknown/red conflict states, and visible 56dp journal action buttons with 17sp labels. The tab row scrolls within its available width. Keyboard traversal is confirmed by a focused native node and a visible focus outline on the final APK; this is not a complete accessibility audit.

The presentation regression script checks three route detail/back paths, three category filters, offline-map retry, family settings/planning disclosure, navigation-dialog dismissal, journal edit/delete cancellation, ETA access, records/atlas and actual keyboard focus. The affected family/journal/navigation/focus flows are replayed on the final installed APK. ETA consent is opened and cancelled without submitting a calculation. The review does not create memories, delete existing records, alter vehicle state or send native navigation requests. Raw captures and UI hierarchy files remain in ignored local evidence, not published source.
