package com.driveatlas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

/** Full app-window SIM surface. Never changes PLEOS system windows or driving restrictions. */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable internal fun PrototypeNavigationLayout(
 route:Route,entry:CoursePreviewEntry,plan:FamilyPlan,departureId:String,
 result:NavPrototypeRoute?,replay:NavReplayState,simulatedOffset:Point?,
 busy:Boolean,active:Boolean,canRun:Boolean,validRequest:Boolean,
 autoSpeech:Boolean,canContinuous:Boolean,error:String,speechStatus:String,hasTrip:Boolean,
 onBack:()->Unit,onPage:(String)->Unit,onDebug:()->Unit,
 onDeparture:(String)->Unit,onCourse:(String)->Unit,onCalculate:()->Unit,
 onToggleReplay:()->Unit,onToggleSpeech:()->Unit,onOneShot:()->Unit,
 onReset:()->Unit,onOffRoute:()->Unit,onClear:()->Unit
) {
 var settingsOpen by remember{mutableStateOf(false)}
 var toolsOpen by remember{mutableStateOf(false)}
 val drawerScroll=rememberScrollState()
 val settingsAnchor=remember{BringIntoViewRequester()}
 val drawer=rememberDrawerState(DrawerValue.Closed)
 val scope=rememberCoroutineScope()
 val canBrowse=canBrowseNavigation(canRun,active,replay.running)
 val latestCanBrowse by rememberUpdatedState(canBrowse)
 fun closeDrawer(){scope.launch{drawer.close()}}
 fun openDrawer(){if(latestCanBrowse)scope.launch{if(latestCanBrowse){drawerScroll.scrollTo(0);drawer.open()}}}
 fun openSettings(){if(latestCanBrowse){settingsOpen=true;scope.launch{if(latestCanBrowse){drawer.open();settingsAnchor.bringIntoView()}}}}
 fun parkedAction(action:()->Unit){if(latestCanBrowse)action()}
 LaunchedEffect(canBrowse){if(!canBrowse)drawer.close()}
 LaunchedEffect(result){if(result!=null)drawer.close()}
 ModalNavigationDrawer(
  drawerState=drawer,gesturesEnabled=false,
  drawerContent={
   ModalDrawerSheet(Modifier.widthIn(max=440.dp).fillMaxHeight()) {
    Column(Modifier.fillMaxSize().verticalScroll(drawerScroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      Text("Drive Atlas",Modifier.weight(1f),fontSize=26.sp,fontWeight=FontWeight.Bold,color=Ink)
      TextButton(shape=RoundedCornerShape(16.dp),onClick=::closeDrawer,modifier=Modifier.heightIn(min=64.dp)){Text("닫기",fontSize=18.sp)}
     }
     Text("현재 코스 · ${route.title}",fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
     Text("SIM · 주차·에뮬레이터 전용",color=AtlasGreen,fontSize=17.sp,fontWeight=FontWeight.Bold)
     TextButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onBack)},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text(entry.backLabel,fontSize=19.sp)}
     HorizontalDivider()
     Text("메뉴",fontSize=24.sp,fontWeight=FontWeight.Bold)
     listOf("discover" to "길 고르기","family" to "가족 비교","auto-eta" to "왕복 시간","family-memory" to "우리 차 기억","records" to "다녀온 길","atlas" to "우표 도감").forEach{(id,label)->
      TextButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction{onPage(id)}},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text(label,fontSize=20.sp)}
     }
     if(hasTrip)TextButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction{onPage("trip")}},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("여정 계속",fontSize=20.sp)}
     TextButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction{closeDrawer();onDebug()}},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("연결 상태 보기",fontSize=20.sp)}
     HorizontalDivider()
     TextButton(shape=RoundedCornerShape(16.dp),onClick={settingsOpen=!settingsOpen},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).bringIntoViewRequester(settingsAnchor)){Text(if(settingsOpen)"경로 설정 접기" else "경로 설정 펼치기",fontSize=22.sp,fontWeight=FontWeight.Bold)}
     if(settingsOpen) {
     Text("경로 설정",fontSize=24.sp,fontWeight=FontWeight.Bold)
     Text(route.title,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
     Text("공개 기준점 → 코스와 선택 쉼터 → 같은 기준점. 실제 차량 길안내가 아닌 위치 재생 시제품입니다.",fontSize=16.sp,color=Muted)
     Text("출발·귀가 기준점",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
     DepartureReferences.all.forEach { d ->
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction{onDeparture(d.id)}},enabled=canBrowse&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {
       Text((if(departureId==d.id)"✓ " else "")+d.label,fontSize=18.sp)
      }
     }
     if(!entry.courseLocked)Catalog.routes.forEach { course ->
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction{onCourse(course.id)}},enabled=canBrowse&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {
       Text((if(route.id==course.id)"✓ " else "")+course.title,fontSize=18.sp)
      }
     }
     Text("선택 쉼터 ${plan.effectiveRestFacilityIds.size}곳 · 가족 비교에서 저장한 순서 반영 · 집 주소/GPS는 사용하지 않음",fontSize=16.sp,color=Muted)
     Button(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onCalculate)},enabled=canBrowse&&!busy&&validRequest,modifier=Modifier.fillMaxWidth().heightIn(min=72.dp)) {
      Text(if(busy)"전체 경로를 계산하는 중…" else "전체 왕복 경로 계산",fontSize=20.sp)
     }
     if(!validRequest)Text("저장된 휴식 계획 좌표·순서가 유효하지 않습니다. 가족 비교에서 확인하세요.",fontSize=17.sp,color=Ink)
     if(error.isNotEmpty())Text(error,fontSize=16.sp,color=Ink)
     }
     if(result!=null) {
      val view=result.snapshot(replay)
      HorizontalDivider()
      Text("현재 시연 안내",fontSize=22.sp,fontWeight=FontWeight.Bold)
      Text(view.nextInstruction,fontSize=18.sp,color=Ink)
      TextButton(shape=RoundedCornerShape(16.dp),onClick={toolsOpen=!toolsOpen},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text(if(toolsOpen)"시연 도구 접기" else "시연 도구 펼치기",fontSize=22.sp,fontWeight=FontWeight.Bold)}
      if(toolsOpen) {
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onReset)},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("처음부터",fontSize=18.sp)}
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onOffRoute)},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("이탈 상태 체험",fontSize=18.sp)}
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onClear)},enabled=canBrowse,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("경로 지우기",fontSize=18.sp)}
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={parkedAction(onOneShot)},enabled=canBrowse&&!replay.offRoute&&!view.finished,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("현재 시연 문장 미리듣기 · 1회",fontSize=18.sp)}
     }
     }
     HorizontalDivider()
     Text("시연·출처 안내",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("주차·에뮬레이터 전용 / SIM 연속 음성은 직접 켜기 · 실시간 교통·자동 이탈 재탐색 미지원",fontSize=16.sp,color=Muted)
     Text("가상 위치 재생을 시작한 뒤 음성을 켜세요. 정지·화면 이탈 시 꺼집니다. 음성 ON은 10m/s, OFF는 100m/s 모의 재생입니다. 촘촘한 지시는 생략될 수 있으며 실제 운전에 사용할 수 없습니다.",fontSize=16.sp,color=Muted)
     Text("지점 좌표는 차량 진입구로 검증되지 않았습니다. 개발용 OSRM 공개 서버와 OSM 지도: 연결 필요·가동 보장 없음. 공개 기준점 시연이며 실제 운전에 사용하지 마세요.",fontSize=16.sp,color=Muted)
     Text("실제 장소·지도 · 방문/주행은 모의 체험 · 운전 중 조작 금지",fontSize=16.sp,color=Muted)
    }
   }
  }
 ) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
   val wide=maxWidth>=700.dp
   val cardWidth=navigationInstructionWidth(maxWidth.value).dp
   val view=result?.snapshot(replay)
   if(result==null)BaseCourseNavigationMap(route,Modifier.fillMaxSize())
   else PrototypeRouteMap(result,simulatedOffset?:view!!.position,Modifier.fillMaxSize(),
    replayDistance=if(replay.offRoute)null else replay.distanceMeters,showStatusLabel=false,controlsEnabled=canBrowse)
   Surface(Modifier.align(Alignment.TopStart).padding(16.dp),color=White,shape=RoundedCornerShape(16.dp),shadowElevation=4.dp) {
    TextButton(shape=RoundedCornerShape(16.dp),onClick=::openDrawer,enabled=canBrowse,modifier=Modifier.size(72.dp)){Text("메뉴",fontSize=20.sp,fontWeight=FontWeight.Bold)}
   }
   Surface(Modifier.align(Alignment.TopStart).padding(start=104.dp,top=16.dp).width(cardWidth),
    color=White,shape=RoundedCornerShape(16.dp),shadowElevation=4.dp) {
    Column(Modifier.padding(if(wide)20.dp else 14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text("SIM · ${if(replay.running)"위치 재생 중" else if(view?.finished==true)"재생 완료" else if(replay.offRoute)"이탈 시연 · 정지" else "재생 전·일시정지"}",fontSize=16.sp,color=AtlasGreen,fontWeight=FontWeight.Bold)
     if(view!=null) {
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       NavigationManeuverSymbol(view.maneuver,Modifier.size(if(wide)56.dp else 40.dp))
       if(!replay.offRoute&&!view.finished)Text("${view.metersToInstruction.toInt()} m",fontSize=if(wide)44.sp else 34.sp,fontWeight=FontWeight.Bold,color=Ink)
      }
      Text(view.nextInstruction,fontSize=if(wide)25.sp else 20.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=3,overflow=TextOverflow.Ellipsis)
      Text(String.format(Locale.KOREA,"남은 도로 %.1f km · 전체 %.1f km",view.remainingMeters/1000,result.meters/1000),fontSize=16.sp,color=Muted)
     } else {
      Text(route.title,fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text("기본 코스 지도 · 메뉴에서 왕복 경로를 계산하세요.",fontSize=16.sp,color=Muted)
     }
    }
   }
   Surface(Modifier.align(Alignment.BottomStart).padding(start=16.dp,end=16.dp,bottom=64.dp).widthIn(max=680.dp),
    color=White,shape=RoundedCornerShape(16.dp),shadowElevation=4.dp) {
    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     if(view==null) {
      Button(shape=RoundedCornerShape(16.dp),onClick=::openSettings,enabled=canBrowse,modifier=Modifier.heightIn(min=64.dp)){Text(if(busy)"경로 계산 중…" else "경로 설정",fontSize=20.sp)}
     } else FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Button(shape=RoundedCornerShape(16.dp),onClick={if(active&&canRun)onToggleReplay()},enabled=active&&canRun&&!replay.offRoute&&!view.finished,modifier=Modifier.heightIn(min=64.dp)){Text(if(replay.running)"일시정지" else "위치 재생",fontSize=20.sp)}
      OutlinedButton(shape=RoundedCornerShape(16.dp),onClick={if(active&&canRun)onToggleSpeech()},enabled=active&&canRun&&replay.running&&!replay.offRoute&&canContinuous,modifier=Modifier.heightIn(min=64.dp)){Text(if(autoSpeech)"음성 끄기" else "음성 켜기",fontSize=20.sp)}
     }
     Text(if(view==null)"SIM · 실제 차량 길안내 아님" else speechStatus,fontSize=15.sp,color=Muted,maxLines=2,overflow=TextOverflow.Ellipsis)
     if(view!=null)Text("SIM · 실제 길안내 아님 · ${if(autoSpeech)10 else 100}m/s 모의 재생",fontSize=14.sp,color=Muted)
     if(error.isNotEmpty()&&(result==null||replay.offRoute))Text(error,fontSize=15.sp,color=Ink,maxLines=3,overflow=TextOverflow.Ellipsis)
    }
   }
  }
 }
 // Registered after the drawer: system Back closes it without altering the route.
 BackHandler {
  if(shouldDismissDrawerOnBack(drawer.isOpen))closeDrawer()
  else if(replay.running){if(active&&canRun)onToggleReplay()}
  else if(canBrowse)onBack()
 }
}
