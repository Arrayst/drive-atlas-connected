package com.driveatlas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
internal data class Guide(val seasons:List<String>,val moods:List<String>,val reason:String,val km:Int,val near:Int)
internal fun guide(r:Route):Guide=when(r.id){
 "coast"->Guide(listOf("봄","여름","가을"),listOf("탁 트인"),"탁 트인 수평선과 짧은 정차 지점. 가족·연인과 같은 풍경을 바라보며 대화하는 길이에요.",32,12)
 "forest"->Guide(listOf("봄","가을"),listOf("잔잔한","초록 충전"),"짧은 코스와 나무가 이어지는 풍경. 함께 조용한 오후를 보내고 싶을 때 고르는 길이에요.",21,8)
 else->Guide(listOf("여름","가을","겨울"),listOf("잔잔한","탁 트인"),"호수와 멀리 열린 산의 풍경. 가족·연인과 서두르지 않는 한 바퀴를 떠나요.",46,19)
}
internal fun filteredRoutes(mode:String,season:String,mood:String,region:String,near:Boolean):List<Route>{
 if(region!="물빛권역")return emptyList()
 val routes=Catalog.routes.filter{r->(mode!="season"||season in guide(r).seasons)&&(mood=="전체"||mood in guide(r).moods)}
 return if(near)routes.sortedBy{guide(it).near} else routes
}
@Composable internal fun GuideHome(snapshot:Snapshot,onCategory:(String)->Unit,onTrip:()->Unit,onAtlas:()->Unit){
 PageHeading("CURATED ROADS / VOL. 01","오늘은, 어떤 길로 갈까요?","가족·연인과 함께할 좋은 길을 고르고, 여행을 한 장의 우표로 남겨요."){Tag("시연 전용 · 가상 정차",Sage,Ink)}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){
  listOf(Triple("recommended","추천길","가볍게 고른, 오늘의 좋은 길"),Triple("season","계절 드라이브","지금 계절에 어울리는 풍경"),Triple("theme","테마 드라이브","오늘의 기분을 따라가는 길")).forEachIndexed{i,(id,title,desc)->
   Surface(onClick={onCategory(id)},modifier=Modifier.weight(1f),color=White,shape=StampEdge,border=BorderStroke(1.dp,Line)){
    Column(Modifier.padding(16.dp)){
     Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("FIELD GUIDE / 0${i+1}");PixelIcon(if(i==0)"compass" else if(i==1)"spark" else "route",Coral,Modifier.size(22.dp))}
     Spacer(Modifier.height(14.dp));PixelLandscape(i,Modifier.fillMaxWidth().height(280.dp))
     Column(Modifier.padding(16.dp)){
      Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text(desc,fontSize=18.sp,color=Muted,modifier=Modifier.padding(top=12.dp));Spacer(Modifier.height(26.dp))
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("길 살펴보기",color=Ink,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("→",color=Ink,fontSize=26.sp)}
     }
    }
   }
  }
 }
 Spacer(Modifier.height(30.dp));StatusStrip(snapshot,onTrip,onAtlas);Spacer(Modifier.height(26.dp))
 NoteBlock("우리의 첫 번째 여행 우표집","물빛권역의 바다·숲·호수 3개 가상 코스로 시연합니다. 지도는 직접 그린 도식이며 실제 도로·GPS·내비 안내가 아닙니다.")
}
@Composable internal fun ChoiceRow(label:String,values:List<String>,selected:String,onSelect:(String)->Unit){
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Text(label,Modifier.width(78.dp),fontSize=16.sp,color=Muted)
  values.forEach{v->FilterChip(selected=v==selected,onClick={onSelect(v)},label={Text(v,fontSize=17.sp)},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(7.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Ink,selectedLabelColor=White))}
 }
}
internal class GuideFilters {
 val season = mutableStateOf("가을")
 val mood = mutableStateOf("전체")
 val region = mutableStateOf("물빛권역")
 val near = mutableStateOf(false)
 val options = mutableStateOf(false)
}
@Composable internal fun CurationPage(mode:String,filters:GuideFilters,awards:List<Award>,onRoute:(Route)->Unit,onBack:()->Unit){
 var season by filters.season;var mood by filters.mood
 var region by filters.region;var near by filters.near;var options by filters.options
 val title=when(mode){"season"->"계절 드라이브";"theme"->"테마 드라이브";else->"추천길"}
 TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Text("←  길 고르기",fontSize=17.sp)}
 PageHeading("FIND YOUR NEXT ROAD",title,when(mode){"season"->"계절을 고르면 그 계절의 분위기가 담긴 길을 보여드려요.";"theme"->"설명은 짧게, 오늘의 기분은 선명하게.";else->"함께하기 좋은 길을, 고른 이유와 함께 담았어요."}){SecondaryButton(if(options)"조건 접기" else "지역·분위기",{options=!options})}
 if(mode=="season")ChoiceRow("계절",listOf("봄","여름","가을","겨울"),season){season=it}
 if(mode=="theme"||options)ChoiceRow("분위기",listOf("전체","탁 트인","잔잔한","초록 충전"),mood){mood=it}
 if(options){
  ChoiceRow("지역",listOf("물빛권역","다른 지역"),region){region=it;near=false}
  ChoiceRow("정렬",listOf("추천순","근처 체험"),if(near)"근처 체험" else "추천순"){near=it=="근처 체험"}
  Text(if(near)"실제 위치를 요청하지 않아요. 샘플 출발점과 가상 거리로 정렬합니다." else "물빛권역만 시연합니다. 전국·세계 코스는 아직 제공하지 않아요.",fontSize=15.sp,color=Muted,modifier=Modifier.padding(vertical=8.dp))
 }
 Spacer(Modifier.height(20.dp));val routes=filteredRoutes(mode,season,mood,region,near)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("$region · ${routes.size}개의 길",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink);Tag("모든 장소·시간·거리 가상",Sage,Ink)}
 Spacer(Modifier.height(18.dp))
 if(routes.isEmpty()){
  NoteBlock("이 조건에는 준비한 길이 없어요","다른 계절·분위기를 골라보세요. 현재 시연 권역은 물빛권역입니다.");Spacer(Modifier.height(18.dp));SecondaryButton("조건 초기화",{region="물빛권역";mood="전체";season="가을";near=false})
 }else routes.forEach{r->val g=guide(r)
  Surface(onClick={onRoute(r)},color=White,shape=Corner,border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().padding(bottom=18.dp)){
   Row(Modifier.heightIn(min=208.dp)){
    PixelLandscape(Catalog.routes.indexOf(r),Modifier.width(260.dp).height(220.dp))
    Column(Modifier.weight(1f).padding(24.dp)){
     Eyebrow("${routeKind(r)} / ${g.seasons.joinToString(" · ")}")
     Text(r.title,fontSize=27.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=8.dp))
     Text(g.reason,fontSize=17.sp,lineHeight=27.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
     Text("${r.minutes}분 · ${g.km} km · ${g.moods.joinToString(" / ")}${if(near)" · 샘플 출발점 ${g.near} km" else ""}",fontSize=16.sp,color=Ink,modifier=Modifier.padding(top=13.dp))
    }
    Column(Modifier.width(150.dp).padding(20.dp).align(Alignment.CenterVertically),horizontalAlignment=Alignment.End){
     if(awards.any{it.routeId==r.id})Tag("수집한 우표",Amber,Ink)
     Text("자세히  →",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=18.dp))
    }
   }
  }
 }
}
@Composable internal fun DemoNavigationDialog(route:Route,onDismiss:()->Unit,onStart:()->Unit){
 var failed by remember{mutableStateOf(false)}
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.widthIn(max=860.dp).padding(24.dp),color=Paper,shape=Corner){
   Column(Modifier.verticalScroll(rememberScrollState()).padding(34.dp)){
    Eyebrow("NAVIGATION HANDOFF / SIMULATION")
    Text(if(failed)"이 환경에서는 연결할 수 없어요" else "기본 내비로 길을 이어가요",fontSize=31.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=12.dp))
    Text(route.title,fontSize=22.sp,color=Muted,modifier=Modifier.padding(top=16.dp));Spacer(Modifier.height(24.dp))
    NoteBlock(if(failed)"미지원 상황 체험" else "실제 내비 연결이 아닌 시뮬레이션",if(failed)"내비 권한·연결을 확인할 수 없을 때의 예시입니다. 연결 성공이나 여정 시작으로 처리하지 않았어요." else "SDK 지원·권한·경로 일치는 아직 검증하지 않았어요. 아래 버튼은 가상 여정을 시작하며 실제 차량 내비를 실행하지 않습니다.")
    Spacer(Modifier.height(24.dp));Text(route.stops.joinToString("  →  "){it.name},fontSize=19.sp,color=Ink);Spacer(Modifier.height(28.dp))
    if(failed)PrimaryButton("연결 체험으로 돌아가기",{failed=false},Modifier.fillMaxWidth())
    else{PrimaryButton("연결 성공 체험 · 가상 여정 시작",onStart,Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));SecondaryButton("미지원 상황 보기",{failed=true},Modifier.fillMaxWidth())}
    TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End).heightIn(min=56.dp)){Text("취소",fontSize=18.sp)}
   }
  }
 }
}
@Composable internal fun ArchiveDialog(onDismiss:()->Unit,onArchive:(VisitStatus)->Unit){
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.widthIn(max=730.dp).padding(24.dp),color=Paper,shape=Corner){
   Column(Modifier.padding(32.dp)){
    Eyebrow("KEEP THE MEMORY");Text("기억은 남기고, 우표는 기다려요",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(vertical=18.dp))
    Text("완주 근거가 부족하면 여행 기록만 남깁니다. 빠진 구간을 채우러 되돌아갈 필요는 없어요. 안전이 먼저입니다.",fontSize=18.sp,lineHeight=28.sp,color=Muted);Spacer(Modifier.height(24.dp))
    PrimaryButton("미확인 기록 남기기",{onArchive(VisitStatus.UNCONFIRMED)},Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));SecondaryButton("중단 기록 남기기",{onArchive(VisitStatus.INTERRUPTED)},Modifier.fillMaxWidth())
    TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End).heightIn(min=56.dp)){Text("계속 체험하기",fontSize=18.sp)}
   }
  }
 }
}

@Composable internal fun RecordsPage(snapshot: Snapshot, onRoute: (Route) -> Unit) {
 var selected by remember { mutableStateOf(Catalog.routes.first()) }
 var tab by remember { mutableStateOf("지도") }
 val count = snapshot.awards.map { it.routeId }.distinct().size
 PageHeading("OUR ROAD MEMORIES", "다녀온 길", "재방문은 여행 기록에 쌓이고, 우표는 코스당 한 장으로 남아요.") { Tag("$count / 3 코스 · 모의 완주", Sage, Ink) }
 ChoiceRow("보기", listOf("지도", "목록"), tab) { tab = it }
 Spacer(Modifier.height(20.dp))
 if (tab == "지도") {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
   Column(Modifier.weight(.64f)) {
    Box(Modifier.fillMaxWidth().height(450.dp)) {
     PixelRouteMap(Catalog.routes.indexOf(selected), Modifier.fillMaxSize(), if (snapshot.awards.any { it.routeId == selected.id }) 3 else 0)
     Tag("가상 코스 도식 · 실제 이동 궤적 아님", White, Ink, Modifier.padding(18.dp))
    }
    Text(selected.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 18.dp))
    Text(if (snapshot.awards.any { it.routeId == selected.id }) "모의 완주 ${snapshot.awards.count { it.routeId == selected.id }}회 · 실제 방문 인증 아님" else "아직 모의 완주하지 않은 길", fontSize = 18.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
   }
   Column(Modifier.weight(.36f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Catalog.routes.forEach { r ->
     Surface(onClick = { selected = r }, color = if (selected.id == r.id) Ink else White, shape = Corner, border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(24.dp)) {
       Text(r.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = if (selected.id == r.id) White else Ink)
       Text(if (snapshot.awards.any { it.routeId == r.id }) "모의 완주 · ${snapshot.awards.count { it.routeId == r.id }}회" else "미방문", fontSize = 17.sp, color = if (selected.id == r.id) Amber else Muted, modifier = Modifier.padding(top = 13.dp))
      }
     }
    }
    SecondaryButton("선택한 코스 다시 보기", { onRoute(selected) }, Modifier.fillMaxWidth())
   }
  }
  Spacer(Modifier.height(28.dp))
 }
 Text("여행 기록 · ${snapshot.awards.size + snapshot.visits.size}회", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
 Spacer(Modifier.height(15.dp))
 val rows = (snapshot.awards.map { RecordRow(it.routeId, it.timeMs, "모의 완주", 3) } + snapshot.visits.map { RecordRow(it.routeId, it.timeMs, if (it.status == VisitStatus.UNCONFIRMED) "미확인" else "중단", it.progress) }).sortedByDescending { it.time }
 if (rows.isEmpty()) NoteBlock("첫 번째 길을 기다려요", "가상 여정을 마치면 이곳에 기록이 생겨요. 실제 위치와 이동 궤적은 수집하지 않습니다.")
 rows.forEach { v ->
  val r = Catalog.routes.first { it.id == v.id }
  Surface(onClick = { onRoute(r) }, color = White, shape = Corner, border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
   Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(formatDate(v.time), Modifier.width(150.dp), fontFamily = Mono, fontSize = 17.sp, color = Muted)
    Text(r.title, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
    Tag("${v.status} · ${v.progress}/3 구간", if (v.status == "모의 완주") Sage else Paper, Ink)
   }
  }
 }
 Spacer(Modifier.height(18.dp))
 Text("이 앱에만 저장합니다. 계정 동기화·외부 공유는 없으며 앱 삭제 시 기록이 사라집니다.", fontSize = 15.sp, color = Muted)
}
private data class RecordRow(val id: String, val time: Long, val status: String, val progress: Int)
