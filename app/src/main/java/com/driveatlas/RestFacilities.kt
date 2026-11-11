package com.driveatlas

/** Published venue facts, not live availability or whole-road accessibility. */
data class RestFacility(val id:String,val routeId:String,val name:String,val address:String,
 val restroom:Boolean?,val stroller:Boolean?,val parking:String,val access:String,val hours:String,
 val source:String,val sourceLabel:String,val checkedAt:String="2026-11-11",
 val point:Point?=null,val coordinateSource:String?=null,
 val coordinateNote:String="공식 지도 대표점 · 차량 진입구·당일 주차 가능 여부 미확인")
object RestFacilities {
 val all=listOf(
  RestFacility("simgok","coast","심곡 제1주차장 · 바다부채길 인근","강릉시 강동면 심곡리 157-1",
   true,null,"심곡 제1·2주차장 안내 · 제1주차장 심곡리 157-1","출입구 앞 화장실. 유모차 접근성 미확인. 탐방로 내부에는 화장실 없음.",
   "선택 도보 탐방: 11~3월 09:00~16:30, 매표 15:30 마감. 기상·군사작전·안전상 통제 가능. 주차장·외부 화장실의 당일 개방은 별도 확인.",
   "https://searoad.gtdc.or.kr/buchae_convenient.html","강릉관광개발공사",
   point=Point(37.66521090561137,129.05301840220107),coordinateSource="https://searoad.gtdc.or.kr/buchae_location.html"),
  RestFacility("hourglass","coast","모래시계공원 주차장","강릉시 강동면 정동진리 567",
   null,null,"공식 안내 승용 200대 · 당일 혼잡·이용 가능 여부 미확인","화장실·유모차 접근성 별도 확인. 바다부채길 정동매표소는 주차 후 도보 이동 필요.",
   "주차장 운영시간·당일 통제 여부 별도 확인",
   "https://searoad.gtdc.or.kr/buchae_location.html","강릉관광개발공사",
   point=Point(37.68649091946655,129.0369137380555),coordinateSource="https://searoad.gtdc.or.kr/buchae_location.html"),
  RestFacility("gwanbang","forest","담양 관방제림","담양 관방제림",
   true,true,"주차 위치·혼잡은 출발 전 확인","입구 공중화장실 · 유모차 이동 가능 · 출입구 무단차 안내. 다른 코스 구간의 접근성을 보장하지 않음.",
   "상시 개방·연중무휴 안내. 현장 통제 및 시설 이용 가능 여부는 출발 전 확인.",
   "https://access.visitkorea.or.kr/ms/detail.do?cotId=74d46d61-8d90-4d78-bba1-2fc52e385450","한국관광공사 열린관광",
   point=Point(35.3239,126.9858),coordinateSource="https://access.visitkorea.or.kr/ms/detail.do?cotId=74d46d61-8d90-4d78-bba1-2fc52e385450"),
  RestFacility("animation","lake","춘천 애니메이션박물관","춘천시 서면 박사로 854",
   true,null,"좌측 주차장 장애인 주차구역 안내","화장실·경사로·자동문·엘리베이터. 유모차 대여 안내는 있지만 전체 구간 유모차 접근성은 별도 확인.",
   "10:00~18:00, 입장 17:00 마감. 월요일 휴관(공휴일이면 다음 날), 1월 1일·설·추석 당일 휴관. 휴관 중 외부 화장실/주차 개방 미확인.",
   "https://access.visitkorea.or.kr/ms/detail.do?cotId=4116afa4-b861-4106-984e-62507fb0e810","한국관광공사 열린관광",
   point=Point(37.8930313558032,127.69186731892),coordinateSource="https://access.visitkorea.or.kr/ms/detail.do?cotId=4116afa4-b861-4106-984e-62507fb0e810")
 )
 fun forRoute(id:String)=all.filter{it.routeId==id}
 fun find(id:String?,routeId:String)=all.firstOrNull{it.id==id&&it.routeId==routeId}
}
