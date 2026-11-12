package com.driveatlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable internal fun AutoEtaPage(snapshot:FamilySnapshot,onCurrentPoint:()->Point?,onCanCalculate:()->Boolean,onApply:(EtaEstimate)->Boolean,onFamily:()->Unit,onPrototype:(()->Unit)?=null) {
 val context=LocalContext.current;val cache=remember{EtaCache(context)};val scope=rememberCoroutineScope()
 var routeId by remember{mutableStateOf(Catalog.routes.first().id)}
 var originId by remember{mutableStateOf<String?>(DepartureReferences.all.first().id)}
 var origin by remember{mutableStateOf(DepartureReferences.all.first().point)}
 var originLabel by remember{mutableStateOf(DepartureReferences.all.first().label)}
 var result by remember{mutableStateOf<EtaEstimate?>(null)}
 var message by remember{mutableStateOf("")};var consent by remember{mutableStateOf<EtaRequest?>(null)}
 var busy by remember{mutableStateOf(false)};var job by remember{mutableStateOf<Job?>(null)}
 var generation by remember{mutableIntStateOf(0)}
 val selectedRestIds=(snapshot.plans[routeId]?:FamilyPlan()).effectiveRestFacilityIds
 val restSlots=(snapshot.plans[routeId]?:FamilyPlan()).restAfterStopIndex
 val requestPreview=EtaRequest(routeId,origin,originLabel,originId,selectedRestIds,true,restAfterStopIndex=restSlots)
 val cached=cache.load(requestPreview,System.currentTimeMillis())
 LaunchedEffect(routeId,selectedRestIds,restSlots){generation++;job?.cancel();busy=false;result=null;consent=null}
 fun reset(){generation++;result=null;message=""}
 fun runCalculation(request:EtaRequest){
  if(!onCanCalculate()){message="주차 상태를 다시 확인한 뒤 계산하세요.";return}
  val token=++generation;busy=true;result=null;message="공개 경로 서버에서 도로 기준 시간을 계산하고 있어요."
  job=scope.launch {
   try{
    val value=EtaService.calculate(request)
    if(generation==token){result=value;message="계산 완료. 실시간 교통·신호 대기·주차 시간은 포함하지 않습니다."
     try{cache.save(request,value)}catch(_:Exception){message+=" 이 기기에 캐시를 저장하지 못했어요."}}
   }catch(e:CancellationException){throw e}catch(e:EtaUnavailable){if(generation==token)message=etaErrorMessage(e.kind)}
   catch(_:Exception){if(generation==token)message=etaErrorMessage("InvalidResponse")}
   finally{if(generation==token)busy=false}
  }
 }
 var editOrigin by remember { mutableStateOf(false) }
 var details by remember { mutableStateOf(false) }
 var itineraryOpen by remember { mutableStateOf(false) }
 val route=Catalog.routes.first{it.id==routeId}
 TripHeading("왕복 도로 시간","출발부터 복귀까지 계산합니다. 휴식과 걷기는 가족 계획에서 따로 더하세요.")
 TripWorkspace(firstWeight=.36f,first={ pane ->
  Column(pane,verticalArrangement=Arrangement.spacedBy(20.dp)) {
   TripPanel {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Text("출발 기준점",color=Muted,fontSize=18.sp,modifier=Modifier.weight(1f))
     TextButton(onClick={editOrigin=!editOrigin},enabled=!busy,modifier=Modifier.heightIn(min=76.dp)){Text(if(editOrigin)"접기" else "변경",fontSize=20.sp)}
    }
    Text(originLabel,color=Ink,fontSize=27.sp,fontWeight=FontWeight.Bold)
    Text(if(originId==null)"SDK 수신 위치 참고값 · 정확도 확인 필요" else "공개 위치 기준 · 현재 내 위치 아님",color=Muted,fontSize=18.sp)
    if(editOrigin&&!busy) {
     DepartureReferences.all.forEach{ref->
      SecondaryButton((if(originId==ref.id)"✓  " else "")+ref.label,{originId=ref.id;origin=ref.point;originLabel=ref.label;reset();editOrigin=false},Modifier.fillMaxWidth())
     }
     SecondaryButton("SDK 수신 위치 선택",{
      val p=onCurrentPoint()
      if(p==null)message="새 위치를 확인할 수 없어요. 공개 기준점을 사용하거나 연결 상태에서 위치 권한·수신 상태를 확인하세요."
      else{originId=null;origin=p;originLabel="SDK 수신 위치 참고값";reset();message="SDK는 미터 단위 정확도를 제공하지 않습니다. 출발 좌표를 직접 확인한 뒤 계산하세요.";editOrigin=false}
     },Modifier.fillMaxWidth())
    }
   }
   if(!busy)Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("다녀올 코스",color=Ink,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=12.dp))
    Catalog.routes.forEach { r ->
     val interaction=remember(r.id){MutableInteractionSource()}
     val focused by interaction.collectIsFocusedAsState()
     Surface(onClick={routeId=r.id;reset()},color=if(r.id==routeId)AtlasSoft else White,
      interactionSource=interaction,border=BorderStroke(if(focused)3.dp else 1.dp,if(focused||r.id==routeId)AtlasGreen else Line),
      shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth().heightIn(min=88.dp).semantics{selected=r.id==routeId;role=Role.RadioButton}) {
      Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
       TripSelectionMark(r.id==routeId)
       Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(r.title,fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Ink)
        Text("${r.stops.size}개 코스 기준점",fontSize=18.sp,color=Muted)
       }
      }
     }
    }
   }
  }
 },second={ pane ->
  TripPanel(pane) {
   Text(if(result!=null)"왕복 도로 예상" else "이번 왕복 여정",color=Muted,fontSize=20.sp)
   val v=result
   if(v!=null) {
    Text(tripDuration(v.roadMinutes),fontSize=44.sp,fontWeight=FontWeight.Bold,color=Ink,letterSpacing=(-2).sp)
    Text("${String.format(Locale.KOREA,"%.1f",v.meters/1000)} km · ${if(v.cached)"저장된 공개 결과" else "도로 기준 계산"}",fontSize=20.sp,color=Coral)
    Text("교통·신호·주차 제외 · 휴식과 걷기는 별도",fontSize=18.sp,color=Muted)
   } else {
    Text("출발 → 코스 → 복귀",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text("편도 시간을 두 배로 하지 않고\n돌아오는 도로까지 계산해요.",fontSize=21.sp,lineHeight=32.sp,color=Muted)
   }
   HorizontalDivider(color=Line,modifier=Modifier.padding(vertical=10.dp))
   Text("구간별 도로 시간",fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
   EtaSegment("가는 길",originLabel+" → "+route.stops.first().name,v?.outboundMinutes)
   EtaSegment("코스 주행",route.stops.first().name+" → "+route.stops.last().name,v?.courseMinutes)
   EtaSegment("돌아오는 길",route.stops.last().name+" → "+originLabel,v?.returnMinutes)
   Text("선택한 경유 순서 기준 · 휴식지 이동은 해당 도로 구간에 포함",fontSize=17.sp,color=Muted)
   HorizontalDivider(color=Line)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text(route.title,fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
     Text("코스 기준점 ${route.stops.size}곳 · 선택 휴식지 ${selectedRestIds.size}곳",fontSize=17.sp,color=Muted)
    }
    TextButton(onClick={itineraryOpen=!itineraryOpen},modifier=Modifier.heightIn(min=76.dp)) {
     Text(if(itineraryOpen)"순서 접기" else "경유 순서",fontSize=18.sp)
    }
   }
   if(itineraryOpen) {
   Text(RestItinerary.route(routeId,selectedRestIds,restSlots)?.stops?.joinToString(" → "){it.name}?:"좌표 확인 필요",fontSize=20.sp,lineHeight=30.sp,color=Ink)
   selectedRestIds.forEach { id -> RestFacilities.find(id,routeId)?.let { facility ->
    Text("${facility.name} · "+if(facility.point==null)"좌표 미확인 · 계산할 수 없음" else "공개 시설 좌표 · 차량 진입점 미검증",fontSize=18.sp,color=Muted)
   }}
   }
   if(selectedRestIds.isNotEmpty())Text("가족 계획에서 지정한 코스 구간에 휴식지를 삽입합니다. 휴식·걷기·여유 시간은 도로 예상 시간에 포함하지 않습니다.",fontSize=18.sp,color=Muted)
   if(!busy)SecondaryButton("휴식 장소·순서 변경",onFamily,Modifier.fillMaxWidth())
   Spacer(Modifier.height(8.dp))
   if(busy){
    LinearProgressIndicator(Modifier.fillMaxWidth())
    SecondaryButton("계산 취소",{generation++;job?.cancel();busy=false;message="계산을 취소했어요. 요청이 이미 전송됐다면 서버 기록은 취소되지 않습니다."},Modifier.fillMaxWidth())
   } else if(v!=null) {
    PrimaryButton("이 시간을 가족 계획에 적용",{if(onApply(v))message="자동 시간이 가족 계획에 저장됐어요." else message="지금은 적용할 수 없어요. 주차 상태와 계산 범위를 확인하세요."},Modifier.fillMaxWidth())
    SecondaryButton("가족 계획에서 비교",onFamily,Modifier.fillMaxWidth())
    TextButton(onClick={result=null},modifier=Modifier.heightIn(min=76.dp)){Text("다시 계산하기",fontSize=19.sp)}
    Text("계산 ${SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(v.calculatedAt))} · 분 단위 올림",color=Muted,fontSize=16.sp)
   } else {
    PrimaryButton("전송 정보 확인 후 계산",{
     val p=if(originId==null)onCurrentPoint() else origin
     if(p==null)message="위치 수신이 오래되어 계산을 시작하지 않았어요. 공개 기준점을 선택하세요."
     else if(RestItinerary.route(routeId,selectedRestIds,restSlots)==null)message="선택한 휴식 장소의 좌표를 확인할 수 없어 계산하지 않았어요. 가족 계획에서 해당 장소를 제외해 주세요."
     else{origin=p;consent=EtaRequest(routeId,p,originLabel,originId,selectedRestIds,restAfterStopIndex=restSlots)}
    },Modifier.fillMaxWidth())
    if(cached!=null)SecondaryButton("저장된 공개 결과 보기",{result=cached;message="저장된 공개 기준점 결과입니다. 현재 도로·교통 상태를 새로 확인한 값이 아닙니다."},Modifier.fillMaxWidth())
   }
   Text("좌표와 전송 대상을 확인한 뒤 요청마다 동의해요. 개인 위치는 자동 전송·저장하지 않아요.",color=Muted,fontSize=17.sp,lineHeight=26.sp)
  }
 })
 if(message.isNotBlank())Text(message,color=Coral,fontSize=20.sp,modifier=Modifier.padding(vertical=20.dp))
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Text("실시간 교통 ETA가 아닌 도로 예상 시간입니다.",color=Muted,fontSize=18.sp,modifier=Modifier.weight(1f))
  TextButton(onClick={details=!details},modifier=Modifier.heightIn(min=76.dp)){Text(if(details)"계산 안내 접기" else "데이터·개인정보 안내",fontSize=18.sp)}
 }
 if(BuildConfig.DEBUG&&BuildConfig.SIMULATOR&&onPrototype!=null)OutlinedButton(onClick=onPrototype,modifier=Modifier.heightIn(min=72.dp)){Text("앱 안에서 전체 경로 따라보기 · SIM",fontSize=20.sp)}
 if(details)TripPanel {
  Text("선택 좌표: ${String.format(Locale.US,"%.5f, %.5f",origin.lat,origin.lon)}",fontSize=18.sp,color=Ink)
  Text("공개 기준점은 건물·역 인근 위치이며 실제 차량 출입구가 아닙니다. 공개 기준점 결과만 24시간 캐시합니다.",color=Muted,fontSize=18.sp)
  Text("OSRM/FOSSGIS 공개 서버: 소규모 비상업 시험용 · 요청 기록됨 · 최대 초당 1회 · 서비스 보장 없음. 운영 배포판은 별도 경로 공급자 설정 전까지 이 서버를 호출하지 않습니다.",color=Muted,fontSize=18.sp)
  TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")},modifier=Modifier.heightIn(min=76.dp)){Text("© OpenStreetMap 기여자 · 데이터 저작권",fontSize=18.sp)}
  TextButton(onClick={openWeb(context,"https://project-osrm.org/")},modifier=Modifier.heightIn(min=76.dp)){Text("Routing: OSRM · 제공 정보",fontSize=18.sp)}
  TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/fixthemap")},modifier=Modifier.heightIn(min=76.dp)){Text("지도 오류 수정 안내",fontSize=18.sp)}
 }
 consent?.let{request->AlertDialog(onDismissRequest={consent=null},properties=DialogProperties(usePlatformDefaultWidth=false),modifier=Modifier.widthIn(max=900.dp).fillMaxWidth(.9f),title={Text("이번 경로 요청에 동의할까요?",fontSize=28.sp)},text={ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontSize=20.sp,lineHeight=31.sp)){Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("받는 곳: FOSSGIS가 운영하는 OSRM 공개 라우팅 서버 (router.project-osrm.org).")
  Text("보내는 데이터: 선택한 출발 좌표 ${String.format(Locale.US,"%.5f, %.5f",request.origin.lat,request.origin.lon)}와 ${Catalog.routes.first{it.id==request.routeId}.title}의 공개 코스 좌표 및 아래 선택 휴식지 좌표. 설정한 코스·휴식 순서대로 경유한 뒤 같은 출발점으로 복귀합니다.")
  Text("경유 순서: "+(RestItinerary.route(request.routeId,request.selectedRestIds,request.restAfterStopIndex)?.stops?.joinToString(" → "){it.name}?:"좌표 확인 필요"))
  request.selectedRestIds.forEach { id -> RestFacilities.find(id,request.routeId)?.let { f ->
   Text("${f.name}: ${f.point?.let{String.format(Locale.US,"%.5f, %.5f",it.lat,it.lon)}?:"좌표 없음"} · ${f.coordinateNote}")
  }}
  if(request.selectedRestIds.isNotEmpty())Text("공개 시설 위치는 차량 출입구로 검증되지 않았습니다. 도로에 맞춘 위치로 계산될 수 있어 실제 진입·회차 경로와 다를 수 있습니다. 이 한계를 확인하고 해당 좌표 전송에 동의합니다.")
  Text("서버에 요청이 기록됩니다. 이름·가족 메모·평가는 보내지 않습니다. ${if(request.publicReferenceId==null)"개인 위치 전송은 이동 정보를 노출할 수 있으며, 이 앱은 해당 좌표를 캐시하지 않습니다." else "선택한 공개 출발 기준점을 보냅니다."}")
 }}},confirmButton={TextButton(modifier=Modifier.heightIn(min=76.dp),onClick={consent=null
  val current=if(request.publicReferenceId==null)onCurrentPoint() else request.origin
  if(current==null||!nativePointMatches(request.origin,current)){message="위치가 변경되었거나 수신이 오래됐어요. 출발점을 다시 확인하세요."}
  else runCalculation(request.copy(allowApproximateRestPoints=true))}){Text("이번 요청에 동의하고 계산",fontSize=20.sp)}},dismissButton={TextButton(modifier=Modifier.sizeIn(minWidth=90.dp,minHeight=76.dp),onClick={consent=null}){Text("취소",fontSize=20.sp)}})}
}

@Composable private fun EtaSegment(label:String,route:String,minutes:Int?) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically) {
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Text(label,fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Ink)
   Text(route,fontSize=18.sp,lineHeight=26.sp,color=Muted)
  }
  Text(minutes?.let{tripDuration(it)}?:"계산 전",fontSize=22.sp,fontWeight=FontWeight.Bold,color=if(minutes==null)Muted else AtlasGreen)
 }
}
