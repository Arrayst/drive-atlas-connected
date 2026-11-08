# DRIVE ATLAS v0.5 디자인 프리뷰

2026-11-08. 기존 v0.4(05ac5fa)에서 분리한 UI 작업입니다. 차량 연동 작업과 충돌하지 않도록 `/Users/USER/projects/drive-atlas-design`, `design/atlas-v05`에서 개발했습니다. 프리뷰 앱 ID는 `com.driveatlas.design.simulator`이며 v0.4와 연결판 데이터를 덮어쓰지 않습니다.

## 문제와 수정

기존 홈은 추천길·계절·테마라는 분류만 크게 보여주고, 실제 코스를 보려면 한 단계 더 들어가야 했습니다. 우표 테두리, 픽셀 아이콘, 영문 장식 라벨이 탐색 화면 전체에 반복되어 차량 앱보다 전시용 포스터처럼 보였습니다.

- 홈에서 실제 강릉·담양·춘천 코스를 바로 고릅니다. 추천/계절/테마는 상단 탭으로 유지합니다.
- 카테고리 대표 이미지 대신 코스 사진, 거리, 주행시간, 지역을 비교합니다. 수치는 기존 OSRM 경로 파일에서 읽습니다. 교통·정차를 제외한 추정임을 표시합니다.
- 208 dp의 짙은 사이드바를 136 dp 밝은 내비게이션 레일로 바꿨습니다. 주 메뉴는 104 dp 높이의 넓은 터치 영역입니다.
- 중성 바탕, 짙은 본문, 녹색 액션 하나로 위계를 정리했습니다. 새 아이콘은 직접 그린 선형 벡터입니다.
- 상세 화면은 실제 지도 62%, 코스 기준점·액션 38%로 나누었습니다. 지도와 사진을 혼동하지 않으며 기준점을 검증된 주차장으로 표현하지 않습니다.
- 도감에서만 우표 테두리와 기존의 직접 만든 픽셀 일러스트를 사용합니다. 모의 완주·미수집 상태와 PNG 보관 범위를 유지했습니다.
- 다녀온 길은 실제 저장 기록이 없으면 빈 상태를 보여줍니다. 방문한 적 없는 코스를 여행 기록처럼 보이는 지도에 표시하지 않습니다.
- 상세에서 돌아와도 홈의 계절/테마 선택이 유지됩니다. 기존 GuideFilters를 상위 화면에서 보관합니다.

## 실제 서비스 참고와 적용 범위

공식 페이지와 공개 이미지의 픽셀을 직접 확인했습니다. 아래는 공식 홍보 화면이며, 해당 앱의 최신 설치 버전을 실행한 검증은 아닙니다. 상표·로고·사진·지도 스크린샷·고유 아이콘을 앱에 복제하지 않았습니다.

1. **Porsche ROADS**
   - 공식 페이지: https://roads.porsche.com/en/porsche-roads-scenic-route-planner
   - 확인 화면: https://roads.porsche.com/wp-content/uploads/2023/03/app-teaser-5-screens-1.jpg
   - 참고: 경로와 주행시간을 먼저 보여주는 정보 순서, 실제 풍경 사진, 지도와 하나의 명확한 액션, 큰 탐색 분류.
   - 적용: 직접 코스 진입, 거리/시간 묶음, 상세 지도와 액션. Porsche의 빨간색 브랜드나 동일 화면 구성은 가져오지 않았습니다.
2. **AllTrails**
   - 공식 앱스토어: https://apps.apple.com/us/app/alltrails-hike-bike-run/id405075943
   - 공식 도움말: https://support.alltrails.com/hc/en-us/articles/360041612232-How-do-I-use-list-view-to-explore-trails
   - 확인한 공식 스토어 이미지: https://is1-ssl.mzstatic.com/image/thumb/PurpleSource221/v4/c1/25/16/c12516aa-6136-333c-410d-25dc8efde4b2/AT-2024-Organic-EN-US-s2.png/460x998bb-60.jpg
   - 참고: 사진 아래 코스명·지역·거리·시간을 짧게 배치하고 탐색 조건을 상단에 둔 패턴.
   - 적용: 계절/테마 필터와 정보가 있는 코스 카드. 평점·인기순·저장 기능처럼 없는 데이터를 꾸며내지 않았습니다.
3. **Roadtrippers**
   - 공식 기능 안내: https://roadtrippers.com/about/features/
   - 경유지 화면: https://roadtrippers.com/wp-content/uploads/2023/06/CleanShot-2023-06-13-at-16.20.42@2x-720x623.png
   - 경로 지도 화면: https://roadtrippers.com/wp-content/uploads/2019/12/Features-page-search-along-your-route.jpg
   - 참고: 번호가 있는 경유지 목록과 경로 지도를 분리하고 거리·시간을 실제 장소와 연결하는 패턴.
   - 적용: 상세의 1–3번 도로 기준점과 지도 번호 대응. 여행 예약·실시간 안내를 지원하는 것처럼 표현하지 않았습니다.

참고 이미지는 `evidence/design-v05/references/`에만 보관하며 APK 자산에 포함하지 않습니다.

## 사진과 지도

v0.4의 JPEG 원본과 경로 JSON을 그대로 사용합니다. 사진 저작자·라이선스는 홈/카드에 표시하고 상세한 원본·라이선스 링크는 출처 대화상자에서 볼 수 있습니다. 사진은 화면 비율에 맞춰 크롭·축소됩니다. 코스 인근 풍경 및 도보 구간 표시를 유지합니다.

- 정동진 해변: Loewelad, CC BY-SA 3.0.
- 담양 메타세쿼이아길: dalgial, CC BY-SA 3.0. 사진 속 나무길을 차량 주행 구간으로 표시하지 않습니다.
- 의암호: YellowTurtle9, CC BY 4.0.
- 지도: OpenStreetMap contributors, ODbL. 실제 도로선: 기존 OSRM driving 스냅샷.

원본 URL·라이선스·타일 정책·실연동 경계는 `REAL_ROADS_V04.md`와 `app/src/main/assets/route-sources.json`을 참조합니다. RealMap.kt의 타일 로딩·캐시·저작권 표시는 바꾸지 않았습니다.

## 통합 안내

`design-ui.patch`는 UI 소스와 이 문서만 포함합니다. 프리뷰 전용 applicationId/버전/앱 이름은 별도 변경이며 연결판에 적용하지 않습니다.

- 추가: `AtlasDesign.kt`. 화면 전용 컴포넌트와 원본 아이콘.
- MainActivity.kt: 색상/Material 색 토큰, Sidebar·AtlasPage 위임, GuideHome에 onRoute/filters 전달, 홈 category callback에서 불필요한 필터 초기화 제거, footer 정리.
- DemoScreens.kt: GuideHome/CurationPage/RecordsPage를 새 UI에 위임, GuideFilters.homeMode 추가. 필터 매칭 함수·ArchiveDialog는 그대로입니다.
- RealContent.kt: RealDetailPage를 새 UI에 위임. 사진 디코딩·출처·외부 URL·내비 대화상자는 그대로입니다.
- Atlas.kt, LocalStore.kt, RestoreValidation.kt, RealMap.kt, PublicWebActivity.kt, 위치/차량 SDK, 안전 판정·생명주기·주행 증거 로직은 변경하지 않았습니다.

연결판은 기존 안전 판정과 최신 라이프사이클/권한 수정을 그대로 유지해야 합니다. MainActivity 전체를 덮어쓰지 말고 UI hunk만 병합하세요. 연결판에서 UI의 `주차 · SIM`, `모의 방문`, `차량 내비 미연동` 문구는 실제 연결 상태에 맞게 별도 검토해야 합니다. 디자인 프리뷰가 실차 연동 검증을 대신하지 않습니다.

## 검증

- 최종 Debug APK, unsigned Release APK 빌드 성공.
- 단위테스트 35개 통과: AtlasEngine 13, DemoFlow 7, RealMap 7, RestoreValidation 8.
- lint: 오류 0, 기존 경고 6(ExifInterface, target API, 의존성 안내 3, Android 12+ 백업 규칙).
- `git diff --check` 통과.
- PLEOS 앱 영역 1682×1210에서 홈·실제 지도 상세·계절·테마·출처·도감·기록 화면을 직접 시각 검수했습니다. 핵심 버튼은 앱 영역 안에 있습니다.
- UI 검증 세부 결과와 캡처: `evidence/design-v05/`. 다른 화면 크기, 실제 주행, 차량 SDK, 실제 내비, 장기 성능·접근성 인증은 이 디자인 작업에서 검증하지 않았습니다.

### 재빌드

JDK 17, Android SDK API 35, 기존 검증 Gradle 캐시를 사용했습니다.

```sh
JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-17-aarch64-os_x.2/jdk-17.0.20.1+1/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
GRADLE_USER_HOME="$HOME/Documents/Codex/2026-11-02/task-2/.gradle-user-home" \
sh gradlew --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```
