# DRIVE ATLAS · PLEOS 시연 앱 v0.3

가족·연인과 함께할 좋은 길을 고르고, 다녀온 코스를 픽셀 우표로 남기는 Android 데모입니다. **실차용 앱이나 실제 내비게이션이 아닙니다.** 코스·좌표·시간·거리·방문 기록은 모두 시연 데이터입니다.

## 이번 개선판

- 첫 화면: 추천길 / 계절 드라이브 / 테마 드라이브.
- 상위 메뉴: 길 고르기 / 다녀온 길 / 우표 도감.
- 계절·분위기·지역 필터, 샘플 위치를 이용한 근처 정렬, 빈 결과·초기화.
- 코스 상세, 내비 연결 성공·미지원 시뮬레이션, 순서가 있는 3개 구간의 가상 여정.
- 같은 코스의 우표는 한 장으로 표시하고 재방문 기록은 별도로 누적.
- 미완료 여정은 미확인 또는 중단 기록으로 남기며 완주 우표를 만들지 않음.
- 다녀온 길의 코스 도식·목록, 우표 PNG 앱 내부 미리보기.
- 상세에서 돌아오면 이전 메뉴와 추천 조건 복원.

기존 `/Users/USER/projects/drive-atlas` 및 v0.2 앱은 수정·삭제하지 않았습니다. 이 별도 작업 폴더는 `demo/pleos-pixel-guide` 브랜치이며 앱 ID와 저장소를 분리했습니다.

## 설치와 실행

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- 앱 이름: DRIVE ATLAS Demo
- 앱 ID: `com.driveatlas.demo.simulator`
- 버전: `0.3.0-demo` / versionCode 3
- Android 최소 API 28, target/compile API 35.

```sh
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 shell am start -n com.driveatlas.demo.simulator/com.driveatlas.MainActivity
```

기존 PLEOS_CARRY 실행 스크립트는 `~/Developer/pleos-emulator/existing/start-emulator.sh`입니다. 이미 실행 중이면 다시 띄우지 마세요.

## 2분 시연 순서

1. 앱 시작은 차량 상태 미확인 잠금입니다. 왼쪽 아래 **시뮬레이터 제어 → 가상 주차·위치 정상**을 선택합니다. 실제 차량 상태를 변경하지 않습니다.
2. 추천길·계절·테마 중 선택하고 코스 상세로 들어갑니다.
3. **이 길로 떠나기 → 연결 성공 체험·가상 여정 시작**. 실제 내비 앱은 실행하지 않습니다. 미지원 상황은 여정을 시작하지 않습니다.
4. **다음 지점 근거 입력·SIM**을 세 번 누르고 **주차 후 여정 완료**를 선택합니다. 도착 근거가 15초보다 오래됐다면 **도착 위치 다시 확인·SIM**을 먼저 누릅니다.
5. 우표 도감과 다녀온 길의 지도·목록을 확인합니다. 같은 코스를 반복해도 우표 종류 수는 늘지 않습니다.
6. 주행·미확인·위치 끊김은 시뮬레이터 패널에서 체험합니다. 앱을 닫거나 재시작하면 주차·위치는 다시 미확인/끊김이 되고, 여정·기록은 남습니다.

## 안전·데이터 범위

- 실제 차량 상태, GPS, 위치 권한, 계정, 서버, 실시간 지도, 내비 SDK와 연결하지 않습니다. 앱이 직접 요청하는 dangerous 권한과 외부 통신 기능은 없습니다.
- 앱의 가상 상태 조절 버튼은 debug 시연용입니다. 실차 주행 제한을 해제하는 수단이 아닙니다. 실제 운전 중 사용하지 마세요.
- 지도는 직접 그린 도식이며 지리적 정확도나 실제 경관 경로 일치를 보장하지 않습니다.
- 코스 우표는 모의 기록입니다. GPS 완주·방문 인증이라고 설명하면 안 됩니다.
- 기록은 기기 내 이 앱에만 저장합니다. 계정 동기화·갤러리 저장·외부 공유·앱 내 삭제/복구 UI는 없습니다. 앱 삭제 시 기록이 사라집니다.
- Release도 컴파일되지만 미서명이고 실차 어댑터가 없어 잠금 상태입니다. 제출용·실차용 완성본이 아닙니다.

## 빌드와 검증

JDK 17과 Android SDK 35를 사용합니다. `JAVA_HOME`, `ANDROID_HOME`을 지정하고 필요하면 기존 `GRADLE_USER_HOME` 캐시를 사용하세요.

```sh
sh gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

실제 UI 회귀 검증 스크립트는 `tools/qa-demo.py`, 화면과 로그는 `evidence/demo-v0.3/`에 있습니다. 최종 확인 범위는 `docs/DEMO_V03_QA.md`를 참고하세요.

## 디자인 참고 및 실제 연동 전 확인

외부 이미지를 다운로드하거나 작품을 복제하지 않고, 기존 프로젝트의 직접 그린 픽셀 풍경·우표 테두리를 사용했습니다. Pinterest 검색 화면은 접근되지 않았으며 공개 픽셀 표현 가이드와 우편·우표 트렌드만 참고했습니다.

- [Pinterest 픽셀 표현 가이드](https://guide.pinterest.com/diy-and-crafts/alpha-pattern-grid-pixel-art-ideas/)
- [Pinterest 2026 공개 트렌드](https://business.pinterest.com/en-ca/pdf/pinterest-predicts/2026-trend-report/)
- [Pleos NaviHelper 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [Pleos 내비 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [Pleos 주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)

공식 문서상 경유지 요청과 내비 화면 요청 API가 있지만, 이 앱에서 지원·권한·콜백을 검증한 것은 아닙니다. NAVI_ROUTE_SEARCH, NAVI_CUSTOM_MAP, FusedLocation 및 CarUxRestrictions의 승인·연동과 실제 주차 상태 확인을 별도로 검증해야 합니다. 기존 미승인 권한을 임의로 추가·재요청하지 않았습니다.
