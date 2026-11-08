# DRIVE ATLAS · Real Roads v0.4

Native Kotlin/Compose PLEOS demo with actual Korean course photos, an OpenStreetMap basemap and road-following OSRM route snapshots. The pixel stamp-book style remains; real navigation and simulated stamp collection are kept distinct.

## Run in the existing PLEOS emulator

```
~/Developer/pleos-emulator/existing/start-emulator.sh
~/Developer/pleos-emulator/existing/android-sdk/platform-tools/adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
~/Developer/pleos-emulator/existing/android-sdk/platform-tools/adb -s emulator-5580 shell am start -n com.driveatlas.real.simulator/com.driveatlas.MainActivity
```

The v0.3 package `com.driveatlas.demo.simulator` and original v0.2 apps are not overwritten. v0.4 data are isolated. App display tested at 1682×1210 within the PLEOS 2560×1440 screen.

1. Open **시뮬레이터 제어 + → 가상 주차 · 위치 정상**. This changes only demo state, not the car/emulator gear.
2. **추천길 → 강릉 헌화로** shows the real road snapshot and credits. An internet connection is needed for uncached map tiles; photos and route geometry are bundled.
3. **지도 연결 옵션** offers an external Kakao route URL, OSM destination view, or separately labelled simulated stamp collection.
4. **우표 수집 흐름 체험 · SIM** preserves the demo's evidence/visit/stamp workflow. No actual GPS or visit proof is claimed.
5. Leaving and returning to the app resets the safety lock intentionally. Reopen the simulator control to continue.

Build, provenance, photo licenses, map-service policies and limitations: [docs/REAL_ROADS_V04.md](docs/REAL_ROADS_V04.md).

No remote publication or real vehicle SDK permission configuration was performed.
