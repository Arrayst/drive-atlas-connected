# 구현·검증 상태 · 2026-11-07

이 파일은 정적 기획서의 ‘최종 UI 검증 중’ 이후 결과를 기록합니다. 기획서의 전체 MVP 인수 목표와 현재 구현을 혼동하지 마세요.

## 구현한 범위

- Kotlin Compose Android 알파: 가상 코스 3개, 코스 상세, 여정 진행, 도감, 주차 상태의 개념 풍경 PNG 생성 및 앱 내부 미리보기.
- 순서·좌표·정확도·시간·출처를 검증하는 위치근거 도메인 로직. 도착 근거 만료 후 마지막 지점 재확인 가능.
- UNKNOWN/MOVING 상태에서 탐색·편집·카드 생성 잠금. 앱 중지/재시작 후 UNKNOWN+LOST. 신호 대기나 속도 0을 주차로 사용하지 않음.
- SharedPreferences 원자적 JSON 저장. 중복 완료 방지, 미완료 여정 카드 생성 거절, 출처 보존 및 simulator→vehicle 승격 거절.
- Debug 전용 simulator 패널과 별도 `.simulator` application ID. Release에는 가상 주차 해제 버튼이 없으며 실제 어댑터 미연결 상태로 잠김.
- 상세→Back→코스 목록, PNG 미리보기→Back/닫기→도감.

## 실제 실행한 검증 (부분 수동 에뮬레이터 UI 검증)

- `:app:testDebugUnitTest`: **13개 통과, 실패 0, skip 0**.
- `:app:lintDebug`: **오류 0, 경고 6**. SDK target 최신성 1, 사용하지 않는 캐시 호환 version catalog의 kotlin-reflect 업데이트 안내 3, Android 12+ 백업 정책 명시 보강 1, 앱 아이콘 미설정 1. 제출 전 정리 필요.
- `:app:assembleDebug`: 성공. 개발용 debug 서명 시뮬레이터 APK.
- `:app:assembleRelease`: 성공. **미서명** vehicle-mode APK 생성만 확인, 설치·실차 검증·제출 가능 상태는 아님. 서명키 미생성.
- 기존 Pleos_CARRY ARM64 Android14 에뮬레이터를 실제로 실행하고 새 앱만 설치. 기존 진단 sample/CARGO는 변경하지 않음.
- 최종 앱에서 UNKNOWN 잠금 → 가상 주차 → 코스 목록 → 상세 → Back 목록 → 시작 → 위치 끊김 시 근거 거절 → 위치 복구 → 세 지점 → 완료 → 도감 1개 → PNG 실제 이미지 미리보기 → Back 닫기를 확인.
- 완료 버튼 연속 탭 후 도감 1개 유지. 앱 프로세스 재시작 후 잠금 확인, 주차 모의 입력 후 저장된 도감 1개 복구 확인.
- 가상 주행 잠금, background/resume 후 잠금 확인.
- 화면 이미지를 실제 픽셀로 검토하여 Canvas가 제목을 가리는 문제를 수정(`clipToBounds`)하고 재검증. 큰 Pleos 화면에서 텍스트가 정상적으로 보임.

## 회귀 테스트 범위

UNKNOWN/MOVING 잠금, 순서 밖 위치 거절, 잘못된 정확도/NaN·오래된/미래/중복 근거, denied/lost location, 미완료 완료 거절, 한 번만 보상/재시작 복구, 진행 중 재시작 잠금, 오래된 도착/주행 중 완료 거절, 도착 재확인, simulator 근거를 vehicle에서 거절, 저장된 simulator 여정을 vehicle에서 완료 거절, 포기 시 무보상/반복 시작 거절, 저장 실패 시 메모리 상태 불변.

## 확인하지 않은 범위 / 남은 일

- 실제 차량 또는 실제 GPS·Pleos SDK·CarUxRestrictions 연동, 위치/주행 권한 승인, 어댑터 신호 만료·품질값 매핑, 연속 경로 통과 및 위변조 방지.
- 좁은 가로 폭·세로 휴대폰·접근성 글자 크기·TalkBack 테스트. 개발 패널은 가로 스크롤로 방어했으나 작은 화면의 전체 UI는 미검증.
- 현재 timestamp 및 100m/40m/15s 임계값의 실차 검증. 주행 중 시뮬레이터 패널 자체는 개발 제어라 노출되지만 release에는 없음.
- 실제 사진 선택/촬영/공유, 갤러리 저장, 클라우드·로그인·음성·지도/내비 연결, 실제 운영 데이터.
- 저장소 손상 복구 UX, 기기이전 백업 차단 정책 강화, 일관된 앱 아이콘, 정식 서명, 제출 전 QA.
- GitHub 브랜치 작업은 요청받았으며 대상 저장소 미정으로 원격 생성·push 대기.

`evidence/`의 PNG는 에뮬레이터에서 검토한 현재 알파 화면입니다. 실제 차량 주행·완주 인증 증거가 아닙니다.
