# Full app-window simulation navigation

## Scope

Navigation now uses the entire window assigned to DRIVE ATLAS by PLEOS. It does not request immersive/fullscreen system mode, alter window bounds or cover DrivingView or platform controls. Both the initial bundled-course map and calculated simulation route fill the app area.

The former global header, tabs and footer are omitted only from this navigation surface. All existing destinations, route setup, tools and source/simulation notices remain available in the top-left modal drawer. The map carries compact instruction/distance and playback/voice overlays, with visible SIM identification and OSM attribution.

## Interaction and safety

- A parked, foreground and non-replaying state is required to open the drawer.
- The drawer scrim blocks touches from reaching the map; Back closes the drawer without changing the current route. Back during SIM playback pauses it instead of opening another feature.
- SIM playback closes the drawer and disables map browsing, overview/follow switching, source links and map retry controls. Pause and voice-off remain available.
- This replay lock is separate from actual vehicle safety. Existing CarUxRestrictions and gear checks still require P, unrestricted UX and fresh connected signals. Restricted or unknown state replaces the app content with SafetyPage, disposing playback and speech. Connection dialogs and actions now also honor that gate.
- This does not enable real-driving navigation. There is no live-GPS guidance, automatic rerouting, production routing service or physical-vehicle verification.

## Build checkpoint

APK SHA-256: `baa8d3997293e4778d6bfc4bf3e6ad93dafdd8a9391540e6463b3a2ae8d80a0e`.
All 196 unit tests passed; lint: zero errors and 33 warnings. Debug and unsigned release builds succeeded. Runtime verification follows below.

## Emulator verification

The final APK was installed without clearing user data and launched through the platform app icon. The app occupied its assigned 1682×1210 multi-window area; DrivingView and system bars remained visible. The full map, top-left drawer and compact overlays were inspected before and after route calculation. Drawer Back preserved the course. During SIM playback, menu taps and map drag did not open browsing or disengage follow; Back paused playback, disabled speech and preserved the current route/instruction.

Two continuous ON_DEVICE utterances completed (2.676 and 3.926 seconds). No application FATAL or ANR was found in the inspected log. New screenshots contain only the public demonstration route. The silent screen recording is 13.974 seconds (approximately 14 seconds), not an audible speech recording or real-driving evidence. The app was left paused with voice off.

Narrow-window switching, actual vehicle P/UX transitions, physical driving, human listening quality and interruption latency during an utterance were not runtime-tested. They must not be presented as verified by the emulator checks above.
