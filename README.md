# DRIVE ATLAS · Android 알파

풍경 좋은 길을 나만의 도감으로 모읍니다. Kotlin + Jetpack Compose, 크림·숲녹색·세이지 색상과 차내 가로 화면을 중심으로 구현했습니다.

**현재 결과는 시뮬레이터 알파이며, 실차 연동 앱이나 제출 가능한 완성 release가 아닙니다.** 세 코스·카페·지점은 모두 가상 데이터이고 그림은 실제 사진이 아닌 직접 그린 개념 풍경입니다. 계정·서버·플레이리스트·음성·실시간 지도·내비게이션·차량 제어는 구현하지 않았습니다.

- [기획개발명세](docs/DRIVE_ATLAS_기획개발명세_v0.1.md)
- [구현 및 검증 상태](docs/IMPLEMENTATION_STATUS.md)

## 빌드

JDK 17, Android SDK 35(최소 API 28), Gradle wrapper 8.10.2, AGP 8.8.2, Kotlin 2.0.0을 사용합니다. `JAVA_HOME`, `ANDROID_HOME` 또는 추적하지 않는 `local.properties`를 설정하세요. 필요하면 `GRADLE_USER_HOME`으로 기존 캐시를 지정합니다.

```
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
sh gradlew :app:assembleRelease
```

`tools/build-local.sh`는 선택적으로 `DRIVE_ATLAS_ENV` 환경파일을 읽고 offline 빌드를 실행합니다. 소스에는 개인 절대 경로·SDK·인증정보를 포함하지 않습니다.

- Debug: `app/build/outputs/apk/debug/app-debug.apk`, `com.driveatlas.alpha.simulator`. 일반 개발용 debug 서명이며 시뮬레이터 조작 패널이 있습니다.
- Release 컴파일 산출물: `app/build/outputs/apk/release/app-release-unsigned.apk`, `com.driveatlas.alpha`. **미서명이라 그대로 설치·제출할 수 없고 실차 어댑터도 없습니다.** 개인 서명키를 생성하지 않았습니다. Debug 시뮬레이터를 실차 release로 제출하면 안 됩니다.

## 시연 순서

1. 시작 시 UNKNOWN 잠금입니다. 에뮬레이터 계기판의 P/0km/h는 앱에 연결되지 않으므로 잠금 해제 근거가 아닙니다.
2. Debug 전용 `가상 P / 위치 정상`을 누릅니다. 실제 차량 상태를 변경하지 않습니다. Release에는 이 버튼이 없습니다.
3. 가상 코스를 고르고 여정을 시작합니다. 순서대로 세 지점의 SIMULATED 위치 근거를 입력합니다.
4. 도착 근거가 15초보다 오래되면 `SIMULATED: 도착 위치 다시 확인`으로 갱신합니다. 최신 도착 근거와 주차 확인이 있어야 완료됩니다.
5. 도감에 SIMULATED 카드가 생깁니다. PNG를 만들면 앱 안에서 미리보기로 표시합니다. 닫기/Back으로 미리보기를 닫을 수 있습니다. 파일은 앱 내부에만 저장되고 외부 공유·갤러리 저장은 연결하지 않았습니다.
6. 가상 주행/상태 미확인은 탐색·편집·카드 생성을 잠급니다. 위치 끊김은 위치 근거와 완료를 거절합니다. 앱 background/restart 후 안전 상태는 UNKNOWN, 위치는 LOST로 돌아갑니다. 여정과 도감은 보존됩니다. 상세의 Back은 코스 목록으로 돌아갑니다.

## 근거 판정과 안전 경계

순서대로 필수 지점을 통과했는지 실제 도메인 로직으로 판정합니다. 유효한 좌표·정확도, 100m 반경, 정확도 40m 이하, 15초 이내 근거, 단조 증가 timestamp, 미래/재사용 근거 거절, simulator/vehicle 출처 일치를 확인합니다. 완료는 여정을 지우고 도감 1개를 더하는 원자적 저장 전환이며 체크박스 인증이 아닙니다. 미완료 여정은 카드를 만들지 못합니다. 시뮬레이터 출처가 재시작 후에도 보존되며 실차 인증으로 바뀌지 않습니다. Debug/Release는 application ID가 달라 저장공간도 분리됩니다.

이 숫자는 **검증 전 알파 설정값**이지 확정된 실제 완주 기준이 아닙니다. 경로 전 구간 연속 검증·위치 위변조 방지·서버 인증은 없습니다. 현재 millisecond wall-clock을 쓰므로 실제 SDK timestamp 단위/기준과 시계 변경 처리 검증이 필요합니다. SharedPreferences JSON은 작은 알파용 원자적 저장소입니다. 손상된 데이터는 빈 상태로 읽어 잘못된 보상이나 잠금 해제를 막지만 복구 안내 UI는 미구현입니다.

안전 상태는 저장하지 않습니다. 속도 0/신호 대기는 주차 근거가 아닙니다. 실제 어댑터는 오래된 안전 신호를 만료시키고 authoritative 주차·주행규제 이벤트를 반영해야 합니다. 현재 실차 어댑터가 없어 release는 계속 잠깁니다.

## 실차 연동 전 필요한 확인 (현재 비활성)

Manifest에 권한을 요청하지 않습니다. Pleos SDK 호출·키 생성·내비 권한 재시도도 없습니다. 실제 연동 전에 정확한 권한 승인과 아래 검증이 필요합니다.

- FusedLocation 1.1.3: `pleos.car.permission.FUSED_LOCATION`은 dangerous 권한입니다. isSupported → initialize → registerFusedLocationCallback, null/onError → LOST, 종료 시 unregister → release 계약을 구현해야 합니다. **SDK confidence를 미터 단위 accuracy로 가정하지 않습니다.** 품질값 매핑과 timestamp를 검증해야 합니다. Android fine/coarse와 같은 동작도 보장하지 않습니다.
- CarUxRestrictionsManager: registerListener/ActiveRestrictions 변경과 authoritative 주차·기어 상태를 연동해야 합니다. 필요한 정확한 권한은 별도 확인 대상입니다. 신호 누락·지연·충돌 시 잠그며 임의 권한을 추가하지 않습니다.
- NAVI_ROUTE/NAVI_ROUTE_SEARCH는 미승인 상태이며 추가·재시도하지 않습니다. 내비게이션은 알파 범위 밖입니다.
- 실제 코스/라이선스 검증, 운전자 주의 분산 시험, 실차 테스트, 개인정보 보관정책, 승인된 서명 release, 제출 검수가 남아 있습니다.

공식 참고:
https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro
https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
