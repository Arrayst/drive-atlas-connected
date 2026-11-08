# DRIVE ATLAS v0.3 검증 결과

검증일: 2026-11-08 KST. 대상: 설치된 `com.driveatlas.demo.simulator`, 버전 `0.3.0-demo`, versionCode 3.

## 결과

- 단위테스트 28개 통과: AtlasEngine 13, RestoreValidation 8, 신규 DemoFlow 7.
- Android lint: 오류 0, 경고 5. targetSdk 35 최신버전 경고 1, 기존 Kotlin 의존성 업데이트 안내 3, Android 12+ 백업 규칙 안내 1. 경고가 없는 상태로 설명하지 않는다.
- Debug APK 빌드와 서명 검증 통과. APK Signature Scheme v2, 개발용 서명.
- Release 컴파일 통과. 미서명이며 실차 어댑터가 없어 제출·실차용 완성본이 아니다.
- 실제 PLEOS_CARRY에서 새 패키지 설치·실행 성공. 기존 alpha/simulator 패키지와 별도 저장소 사용.
- 디스플레이 2560×1440 / 160dpi. 앱 영역 `[853,90]–[2535,1300]`, 1682×1210.
- 홈 3개 진입점, 상세 출발, 진행 버튼, 우표 PNG 버튼이 앱 창 안에 들어오는지 XML 좌표 확인. 주요 화면 PNG 직접 시각 검수.
- `git diff --check` 통과.
- 실행 중 crash buffer에서 이 패키지의 crash 기록 없음. exit-info에는 QA의 의도적인 force-stop만 기록됨. 장기 안정성 보장은 아니다.

## 실제 조작 검증 14개

1. Home has 3 entry points inside 1682x1210 app bounds
2. Winter filter and detail Back preserve conditions
3. Mood filter chooses the forest course
4. Empty region, reset and explicit sample-nearby sorting
5. Navigation failure/cancel does not start a journey
6. Lost location rejects simulated evidence; restored location accepts it
7. Restart preserves journey progress but resets safety/location
8. Complete journey, course stamp, PNG preview and Back
9. Visited map and list show completion
10. Repeat journey adds record without a second course stamp
11. Partial journey archived without a stamp
12. Archive Cancel keeps journey; interrupted record grants no stamp
13. Moving state blocks navigation and album editing
14. Return to home with persisted demo records


## 남긴 상태

홈 화면으로 복귀하여 실행 상태로 두었다. QA로 생성한 모의 완주 2회, 미확인 1회, 중단 1회가 남아 있으며 우표는 숲 코스 1종이다. 기존 v0.2 데이터는 수정·초기화하지 않았다. 데이터 보존 확인을 위해 새 앱을 한 번 강제종료·재실행했으며 안전 상태가 미확인으로 돌아가는 것을 확인했다.

## 미검증·범위 밖

실차 GPS·차량 신호·내비 SDK/권한, 실제 경로 일치, 여러 차종/화면 모드, 장기 주행·부하 시험, 정식 주의 분산·접근성 인증, 계정 동기화, 외부 공유, 앱 내 삭제·복구, 실제 코스 콘텐츠 검수는 완료되지 않았다. 화면의 지점·위치·거리·방문·내비 성공은 가상 시연이다.

에뮬레이터 시계는 실제 컴퓨터 날짜와 다를 수 있다. 시연 기록 날짜를 실제 여행 날짜로 해석하지 않는다.

## 근거

- `evidence/demo-v0.3/build-final.log`
- `evidence/demo-v0.3/ui-test.log`, `ui-checks.json`
- `evidence/demo-v0.3/01-start-locked.png`부터 `14-final-home.png`
- `app/build/test-results/testDebugUnitTest/`
- `app/build/reports/lint-results-debug.txt`
- `tools/qa-demo.py`
