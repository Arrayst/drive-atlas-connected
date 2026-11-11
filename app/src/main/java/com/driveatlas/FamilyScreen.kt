package com.driveatlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable internal fun FamilyPage(snapshot: FamilySnapshot, onPreferences:(FamilyPreferences)->Boolean,
 onPlan:(String,FamilyPlan)->Boolean,onRoute:(Route)->Unit,onMemory:()->Unit) {
 val p=snapshot.preferences
 val ranked=rankFamilyRoutes(snapshot)
 var settingsOpen by rememberSaveable { mutableStateOf(false) }
 var expandedRoute by rememberSaveable { mutableStateOf<String?>(null) }
 var selectedId by rememberSaveable { mutableStateOf(ranked.first().route.id) }
 var explain by remember { mutableStateOf(false) }
 TripHeading("오늘, 함께 가기 좋은 길","시간은 한눈에, 확인할 조건은 빠짐없이 살펴보세요.")
 Card(Modifier.fillMaxWidth().padding(bottom=20.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=White)) {
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(28.dp),verticalAlignment=Alignment.CenterVertically) {
    Text("${p.carName}의 오늘 조건",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text("사용 가능 ${p.availableMinutes}분 · 걷기 ${p.walkMinutes}분 이내\n연속 주행 ${p.maxContinuousDrivingMinutes?.let{"${it}분 이내"}?:"미설정"} · ${if(p.restroomRequired) "화장실 필수" else "화장실 조건 없음"} · ${if(p.strollerRequired) "유모차 이동" else "유모차 조건 없음"}",fontSize=20.sp,lineHeight=30.sp,color=Ink,modifier=Modifier.weight(1f))
    SecondaryButton(if(settingsOpen) "가족 조건 접기" else "가족 조건 변경",{settingsOpen=!settingsOpen})
   }
   if(settingsOpen) FamilyConditionsEditor(p,onPreferences,{settingsOpen=false})
  }
 }
 val selected=ranked.firstOrNull{it.route.id==selectedId}?:ranked.first()
 TripWorkspace(firstWeight=.35f,first={ pane ->
  Column(pane,verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Text("같은 조건으로 비교",fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Muted,modifier=Modifier.padding(bottom=4.dp))
   ranked.forEach { m ->
    val active=m.route.id==selected.route.id
    Surface(onClick={selectedId=m.route.id;expandedRoute=null},shape=RoundedCornerShape(20.dp),color=if(active)Sage else White,border=if(active)BorderStroke(2.dp,Coral) else null,modifier=Modifier.fillMaxWidth()) {
     Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
       Text(m.route.title,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
       Box(Modifier.size(28.dp),contentAlignment=Alignment.Center){if(active)TripSelectionMark(true)}
      }
      Text(if(m.plan.transferKnown)tripDuration(m.totalMinutes) else "왕복 시간 미입력",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text(when{m.conflicts.isNotEmpty()->"조건 조정 ${m.conflicts.size}개";m.unknowns.isNotEmpty()->"추가 확인 ${m.unknowns.size}개";else->"입력 조건 충족"},fontSize=18.sp,color=if(m.conflicts.isEmpty()&&m.unknowns.isEmpty())Coral else Muted)
     }
    }
   }
   Text("왕복 주행 + 휴식 + 걷기\n미입력 시간은 0분으로 계산하지 않아요.",fontSize=17.sp,lineHeight=27.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
  }
 },second={ pane ->
  Column(pane,verticalArrangement=Arrangement.spacedBy(20.dp)) {
   FamilyComparisonCard(selected,expandedRoute==selected.route.id,{expandedRoute=if(expandedRoute==selected.route.id)null else selected.route.id},{onRoute(selected.route)},p.availableMinutes)
   if(expandedRoute==selected.route.id)Dialog(onDismissRequest={expandedRoute=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
    Surface(Modifier.widthIn(max=1200.dp).fillMaxWidth(.9f).heightIn(max=880.dp),shape=RoundedCornerShape(24.dp),color=White) {
     Column {
      Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
       Text("시간·휴식 계획",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
       TextButton(onClick={expandedRoute=null},modifier=Modifier.sizeIn(minWidth=90.dp,minHeight=76.dp)){Text("닫기",fontSize=20.sp)}
      }
      HorizontalDivider(color=Line)
      FamilyPlanningPanel(selected,onPlan,{expandedRoute=null},Modifier.weight(1f,false))
     }
    }
   }
  }
 })
 Row(Modifier.fillMaxWidth().padding(top=18.dp),verticalAlignment=Alignment.CenterVertically) {
  Text("실시간 교통·혼잡 예측이 아닌 입력 기반 비교입니다.",color=Muted,fontSize=18.sp,modifier=Modifier.weight(1f))
  TextButton(onClick={explain=!explain},modifier=Modifier.heightIn(min=76.dp)){Text(if(explain)"비교 기준 접기" else "비교 기준",fontSize=18.sp)}
  TextButton(onClick=onMemory,modifier=Modifier.heightIn(min=76.dp)){Text("우리 차 기억",fontSize=18.sp)}
 }
 if(explain)TripPanel {
  Text("조건 불일치·미확인 수 → 직접 선택한 개선 조건 → 직접 남긴 코스 평점 → 코스 총 시간 순으로 정렬합니다. 자동 학습이나 실시간 혼잡 예측이 아닙니다.",color=Muted,fontSize=18.sp)
  Text("출발→코스·코스→복귀 시간까지 입력하면 전체 일정을 비교합니다. 시간을 직접 입력하거나 왕복 시간에서 도로 예상값을 적용할 수 있습니다.",color=Muted,fontSize=18.sp)
 }
}

@Composable private fun FamilyComparisonCard(m:FamilyMatch,expanded:Boolean,onExpand:()->Unit,onRoute:()->Unit,available:Int) {
 val statusColor=when { m.conflicts.isNotEmpty()->Color(0xFFB64036); m.unknowns.isNotEmpty()->Color(0xFF8A5900); else->Coral }
 val status=when { m.conflicts.isNotEmpty()->"계획을 조금 조정해 주세요"; m.unknowns.isNotEmpty()->"출발 전에 확인이 필요해요"; else->"입력한 조건에 맞는 길이에요" }
 TripPanel(Modifier.fillMaxWidth()) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp),verticalAlignment=Alignment.CenterVertically) {
   RoutePhoto(m.route,Modifier.size(120.dp).clip(RoundedCornerShape(18.dp)))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text(m.route.title,fontSize=29.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text(m.route.subtitle,fontSize=18.sp,color=Muted)
   }
  }
  Text("왕복 포함 전체 계획 · "+if(m.plan.etaCalculatedAt!=null)"도로 예상 적용" else "수동 입력 기준",fontSize=19.sp,color=Muted,modifier=Modifier.padding(top=12.dp))
  Text(if(m.plan.transferKnown)tripDuration(m.totalMinutes) else "시간을 더 알려주세요",fontSize=48.sp,fontWeight=FontWeight.Bold,color=Ink,letterSpacing=(-1).sp)
  Text(if(m.plan.transferKnown) {val delta=available-m.totalMinutes;if(delta>=0)"여유 시간보다 ${tripDuration(delta)} 짧아요" else "여유 시간보다 ${tripDuration(-delta)} 길어요"} else "확인된 구간만 ${tripDuration(m.totalMinutes)} · 전체 시간은 아직 미정",fontSize=20.sp,color=statusColor)
  HorizontalDivider(color=Line,modifier=Modifier.padding(vertical=6.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   listOf("코스까지 가는 길" to (m.plan.outboundMinutes?.let{tripDuration(it)}?:"미입력"),"코스 즐기기 · 주행" to tripDuration(m.plan.courseDriveMinutes?:m.route.minutes),"돌아오는 길" to (m.plan.returnMinutes?.let{tripDuration(it)}?:"미입력")).forEach{(label,value)->
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){Text(label,fontSize=18.sp,color=Muted);Text(value,fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink)}
   }
  }
  Text("휴식·걷기·여유 시간  |  휴식 ${m.plan.effectiveRestMinutes}분  ·  걷기 ${m.plan.walkMinutes}분  ·  여유 시간 ${m.plan.bufferMinutes}분",fontSize=19.sp,color=Muted)
  if(m.plan.departureMinuteOfDay!=null)Text("${clockLabel(m.plan.departureMinuteOfDay)} 출발 → ${if(m.plan.returnNextDay)"다음 날 " else ""}${clockLabel(m.plan.returnDeadlineMinuteOfDay)} 복귀 마감",fontSize=19.sp,color=Ink)
  Text("휴식 장소 ${m.plan.effectiveRestFacilityIds.size}곳",fontSize=18.sp,color=Muted)
  Text(if(m.plan.etaCalculatedAt!=null) "OSRM 도로 예상 적용 · 실시간 교통 미반영" else "수동 입력 계획 · 실시간 교통 미반영",fontSize=17.sp,color=Muted)
  Surface(color=statusColor.copy(alpha=.07f),shape=RoundedCornerShape(16.dp)) {
   Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text(status,fontSize=22.sp,fontWeight=FontWeight.Bold,color=statusColor)
    (m.conflicts+m.unknowns).forEach{Text("• $it",fontSize=19.sp,lineHeight=28.sp,color=statusColor)}
    if(m.conflicts.isEmpty()&&m.unknowns.isEmpty())Text("입력 시간·선택 휴식지 공개정보 기준\n당일 운영과 현장 접근은 확인해 주세요.",fontSize=19.sp,lineHeight=28.sp,color=statusColor)
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   SecondaryButton(if(expanded)"계획 접기" else "시간·휴식 조정",onExpand,Modifier.weight(.45f))
   PrimaryButton("코스 상세 · 내비 연결",onRoute,Modifier.weight(.55f))
  }
  m.feedbackReasons.forEach { Text(it,fontSize=18.sp,color=Coral) }
  Text(m.previousRating?.let{"직접 남긴 이전 평가 ${String.format(Locale.KOREA,"%.1f",it)}/5 반영"}?:"아직 직접 남긴 코스 평가가 없어요.",fontSize=16.sp,color=Muted)
 }
}

@Composable private fun FamilyConditionsEditor(p:FamilyPreferences,onSave:(FamilyPreferences)->Boolean,onCancel:()->Unit) {
 var draft by remember(p){mutableStateOf(p)}
 var error by remember { mutableStateOf(false) }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("변경한 조건은 저장한 뒤 비교에 반영됩니다.",color=Muted,fontSize=18.sp)
  OutlinedTextField(value=draft.carName,onValueChange={draft=draft.copy(carName=it.take(24))},label={Text("차량 별명 · 24자 이내")},singleLine=true)
  ChoiceRow("사용 가능",listOf("60","120","180","240","360","480","720"),draft.availableMinutes.toString()){draft=draft.copy(availableMinutes=it.toInt())}
  ChoiceRow("연속 주행",listOf("미설정","30","60","90","120","180","240","360"),draft.maxContinuousDrivingMinutes?.toString()?:"미설정"){draft=draft.copy(maxContinuousDrivingMinutes=it.toIntOrNull())}
  ChoiceRow("걷기 한도",listOf("0","10","20","40"),draft.walkMinutes.toString()){draft=draft.copy(walkMinutes=it.toInt())}
  Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   FilterChip(selected=draft.restroomRequired,onClick={draft=draft.copy(restroomRequired=!draft.restroomRequired)},label={Text("화장실 필수")},modifier=Modifier.heightIn(min=76.dp))
   FilterChip(selected=draft.strollerRequired,onClick={draft=draft.copy(strollerRequired=!draft.strollerRequired)},label={Text("유모차 이동")},modifier=Modifier.heightIn(min=76.dp))
  }
  Text("다음 추천에서 바꾸고 싶은 점",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
  Text("직접 선택한 조건만 비교에 반영합니다. 메모를 분석하거나 자동 학습하지 않습니다.",fontSize=17.sp,color=Muted)
  FeedbackChoices(draft.feedback){draft=draft.copy(feedback=it)}
  if(error)Text("저장하지 못했어요. 주차 상태와 입력을 확인하세요.",color=Coral)
  Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   PrimaryButton("가족 조건 저장",{if(onSave(draft.copy(carName=draft.carName.trim())))onCancel() else error=true},enabled=draft.carName.isNotBlank())
   SecondaryButton("변경 취소",onCancel)
  }
 }
}

@Composable private fun FeedbackChoices(value:FamilyFeedback,onChange:(FamilyFeedback)->Unit) {
 Column {
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   FilterChip(selected=value.lessDriving,onClick={onChange(value.copy(lessDriving=!value.lessDriving))},label={Text("주행을 짧게")},modifier=Modifier.heightIn(min=76.dp))
   FilterChip(selected=value.moreRest,onClick={onChange(value.copy(moreRest=!value.moreRest))},label={Text("휴식을 넉넉히")},modifier=Modifier.heightIn(min=76.dp))
   FilterChip(selected=value.lessWalking,onClick={onChange(value.copy(lessWalking=!value.lessWalking))},label={Text("걷기를 적게")},modifier=Modifier.heightIn(min=76.dp))
  }
  TextButton(onClick={onChange(FamilyFeedback())},modifier=Modifier.heightIn(min=76.dp)){Text("개선 선택 모두 해제",fontSize=18.sp)}
 }
}

@Composable private fun FamilyPlanningPanel(m:FamilyMatch,onPlan:(String,FamilyPlan)->Boolean,onClose:()->Unit,modifier:Modifier=Modifier) {
 var draft by remember(m.route.id){mutableStateOf(m.plan)}
 var section by remember { mutableStateOf("이동 시간") }
 var error by remember { mutableStateOf(false) }
 Column(modifier) {
 Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Text("${m.route.title} · 저장 전까지 기존 계획을 유지합니다.",fontSize=20.sp,color=Ink)
  ChoiceRow("계획 항목",listOf("이동 시간","출발·복귀","휴식 장소"),section){section=it}
  when(section) {
   "이동 시간" -> {
    Text("분 단위 수동 입력 · 도로 예상 시간을 바꾸면 기존 계산 표시를 해제합니다.",fontSize=17.sp,color=Muted)
    TripTimeChoices("코스까지",listOf("미입력","0","30","60","120","180","240"),draft.outboundMinutes?.toString()?:"미입력"){draft=manualTransfers(draft,outbound=it.toIntOrNull())}
    TripTimeChoices("복귀 시간",listOf("미입력","0","30","60","120","180","240"),draft.returnMinutes?.toString()?:"미입력"){draft=manualTransfers(draft,back=it.toIntOrNull())}
    Text("휴식 합계만 입력하면 장소별 쉼 여부·연속 주행을 확인할 수 없어요. 휴식 장소에서 각각 설정하세요.",fontSize=16.sp,color=Muted)
    TripTimeChoices("휴식 합계",listOf("0","30","60","90"),draft.restMinutes.toString()){draft=draft.copy(restMinutes=it.toInt(),restMinutesByFacility=emptyMap())}
    TripTimeChoices("걷기 합계",listOf("0","10","20","40"),draft.walkMinutes.toString()){draft=draft.copy(walkMinutes=it.toInt())}
   }
   "출발·복귀" -> {
    Text("출발과 복귀 마감은 둘 다 설정하거나 둘 다 비워 주세요. 추가 여유는 전체 계획에 포함합니다.",fontSize=18.sp,color=Muted)
    ClockEditor("출발 시각",draft.departureMinuteOfDay){draft=draft.copy(departureMinuteOfDay=it)}
    ClockEditor("복귀 마감",draft.returnDeadlineMinuteOfDay){draft=draft.copy(returnDeadlineMinuteOfDay=it)}
    FilterChip(selected=draft.returnNextDay,onClick={draft=draft.copy(returnNextDay=!draft.returnNextDay)},label={Text("다음 날 복귀")},modifier=Modifier.heightIn(min=76.dp))
    TripTimeChoices("추가 여유",listOf("0","15","30","60"),draft.bufferMinutes.toString()){draft=draft.copy(bufferMinutes=it.toInt())}
    TextButton(onClick={draft=draft.copy(departureMinuteOfDay=null,returnDeadlineMinuteOfDay=null,returnNextDay=false)},modifier=Modifier.heightIn(min=76.dp)){Text("시각 설정 모두 지우기",fontSize=18.sp)}
   }
   else -> RestPlanEditor(m.route,draft){draft=it}
  }
  if(!draft.valid())Text("시각은 두 개 모두 입력하고, 복귀가 출발 이후 24시간 이내가 되도록 설정하세요.",color=Coral,fontSize=18.sp)
  if(error)Text("저장하지 못했어요. 주차 상태와 입력을 확인한 뒤 다시 시도하세요.",color=Coral)
  }
  HorizontalDivider(color=Line)
  Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   PrimaryButton("계획 저장",{if(onPlan(m.route.id,draft))onClose() else error=true},enabled=draft.valid())
   SecondaryButton("변경 취소",onClose)
  }
 }
}

private fun clockLabel(minute:Int?)=minute?.let{String.format(Locale.KOREA,"%02d:%02d",it/60,it%60)}?:"미설정"
@Composable private fun ClockEditor(label:String,value:Int?,onChange:(Int?)->Unit) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
  Text(label,Modifier.width(100.dp),fontSize=18.sp,color=Muted)
  Text(clockLabel(value),Modifier.width(110.dp),fontSize=24.sp,fontWeight=FontWeight.Bold,color=Ink)
  SecondaryButton("− 30분",{onChange(((value?:540)-30+1440)%1440)})
  SecondaryButton("+ 30분",{onChange(((value?:510)+30)%1440)})
  TextButton(onClick={onChange(null)},modifier=Modifier.heightIn(min=76.dp)){Text("지우기")}
 }
}

@Composable private fun RestPlanEditor(route:Route,plan:FamilyPlan,onChange:(FamilyPlan)->Unit) {
 val context=LocalContext.current
 val ids=plan.effectiveRestFacilityIds
 fun update(value:List<String>){onChange(manualTransfers(plan,outbound=null,back=null).copy(restFacilityId=null,restFacilityIds=value,restMinutesByFacility=plan.restMinutesByFacility.filterKeys{it in value},restAfterStopIndex=plan.restAfterStopIndex.filterKeys{it in value}))}
 Text("휴식할 코스 구간을 직접 정합니다. 같은 구간에 놓은 휴식지는 아래 순서대로 경유합니다. 장소를 바꾸면 기존 도로 예상 적용을 해제합니다.",fontSize=18.sp,color=Muted)
 if(ids.isEmpty())Text("선택한 휴식 장소가 없어요.",fontSize=20.sp,color=Ink)
 ids.forEachIndexed { index,id -> RestFacilities.find(id,route.id)?.let { f ->
  Surface(color=Sage,shape=RoundedCornerShape(16.dp)) {
   Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("${index+1}. ${f.name}",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text("${f.parking}\n${f.access}\n${f.hours}",fontSize=17.sp,color=Muted)
    val slotLabels=listOf("코스 진입 전")+route.stops.map{"${it.name} 후"}
    ChoiceRow("휴식 구간",slotLabels,slotLabels[(plan.restAfterStopIndex[id]?:route.stops.lastIndex)+1]){label->onChange(manualTransfers(plan,outbound=null,back=null).copy(restAfterStopIndex=plan.restAfterStopIndex+(id to (slotLabels.indexOf(label)-1))))}
    TripTimeChoices("이곳 휴식",listOf("미입력","0","15","30","60","90"),plan.restMinutesByFacility[id]?.toString()?:"미입력"){value->onChange(plan.copy(restMinutesByFacility=if(value=="미입력")plan.restMinutesByFacility-id else plan.restMinutesByFacility+(id to value.toInt())))}
    Text(if(f.point==null)"좌표 미확인 · 이 장소를 포함한 자동 계산·내비 요청 불가" else "공개 시설 위치 · 차량 진입점 미검증 · ${f.coordinateNote}",fontSize=17.sp,color=Coral)
    Text("공개자료 ${f.checkedAt} · 당일 운영·진입은 별도 확인",fontSize=16.sp,color=Muted)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     if(index>0)SecondaryButton("위로",{update(ids.toMutableList().apply{add(index-1,removeAt(index))})})
     if(index<ids.lastIndex)SecondaryButton("아래로",{update(ids.toMutableList().apply{add(index+1,removeAt(index))})})
     SecondaryButton("제외",{update(ids-id)})
     TextButton(onClick={openWeb(context,f.source)},modifier=Modifier.heightIn(min=76.dp)){Text("시설 안내 ↗")}
    }
   }
  }
 }}
 RestFacilities.forRoute(route.id).filter{it.id !in ids}.forEach { f ->
  SecondaryButton("+ ${f.name}",{update(ids+f.id)},Modifier.fillMaxWidth())
 }
 if(ids.isNotEmpty())Text("실제 순서: "+(RestItinerary.route(route.id,ids,plan.restAfterStopIndex)?.stops?.joinToString(" → "){it.name}?:"좌표 확인 필요"),fontSize=18.sp,color=Ink)
 if(ids.isNotEmpty())TextButton(onClick={update(emptyList())},modifier=Modifier.heightIn(min=76.dp)){Text("휴식 장소 모두 제외",fontSize=18.sp)}
 Text("좌표 없는 장소는 자동 계산·내비 요청을 막습니다. 공개 시설 좌표도 차량 진입점이 아닙니다. 지도 검색과 현장 안내로 확인하고 도로변 임의 정차를 하지 마세요.",fontSize=17.sp,color=Muted)
}

@Composable internal fun FamilyMemoriesPage(snapshot:FamilySnapshot,
 onSave:(String,String,Int,String,Boolean,String?,List<String>)->Boolean,onCompare:()->Unit,
 onEdit:(String,Int,String,String?,List<String>)->Boolean,onDelete:(String)->Boolean) {
 Text("함께한 시간을 다음 길로",fontSize=32.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("계정·클라우드 없이 이 앱 안에 저장됩니다. 직접 남긴 기록은 GPS 방문 인증이나 완주 배지가 아닙니다. 개인 신상이나 건강정보는 메모에 넣지 마세요.",color=Muted,modifier=Modifier.padding(vertical=16.dp))
 var routeId by rememberSaveable{mutableStateOf(Catalog.routes.first().id)}
 var rating by rememberSaveable{mutableIntStateOf(4)}
 var note by rememberSaveable{mutableStateOf("")}
 var simulated by rememberSaveable{mutableStateOf(false)}
 var tripDate by rememberSaveable{mutableStateOf(java.time.LocalDate.now().toString())}
 var feelings by rememberSaveable{mutableStateOf(emptyList<String>())}
 var saveError by remember{mutableStateOf(false)}
 var id by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())}
 var editId by rememberSaveable{mutableStateOf<String?>(null)}
 var editNote by rememberSaveable{mutableStateOf("")}
 var editRating by rememberSaveable{mutableIntStateOf(4)}
 var editDate by rememberSaveable{mutableStateOf("")}
 var editFeelings by rememberSaveable{mutableStateOf(emptyList<String>())}
 var deleteId by rememberSaveable{mutableStateOf<String?>(null)}
 val saved=snapshot.memories.any{it.id==id}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(32.dp),verticalAlignment=Alignment.Top) {
 Column(Modifier.weight(.48f)) {
 Text(snapshot.preferences.carName,fontSize=18.sp,color=Muted)
 Text("새로운 기억 남기기",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=6.dp,bottom=20.dp))
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=White)) {
 Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text(if(saved) "저장한 기억" else "코스와 만족도",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
 if(!saved) {
  Text("기억할 길",color=Muted,fontSize=16.sp)
  Catalog.routes.forEach { route ->
   FilterChip(selected=routeId==route.id,onClick={routeId=route.id},label={Text(route.title,fontSize=18.sp)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Ink,selectedLabelColor=White))
  }
  ChoiceRow("만족도",listOf("1","2","3","4","5"),rating.toString()){rating=it.toInt()}
  MemoryDetailsEditor(tripDate,feelings,{tripDate=it},{feelings=it})
  OutlinedTextField(value=note,onValueChange={note=it.take(280)},label={Text("기억하고 싶은 점 (선택, 280자)")},modifier=Modifier.fillMaxWidth(),minLines=2)
  FilterChip(selected=simulated,onClick={simulated=!simulated},label={Text("실제 여행이 아닌 시연 기록 (추천 평점에서 제외)")},modifier=Modifier.heightIn(min=56.dp))
  if(snapshot.memories.size>=500)Text("이 기기의 기억 500개 한도에 도달했어요. 기존 기록은 자동 삭제하지 않습니다. 직접 불필요한 기억을 삭제한 뒤 새 기억을 저장하세요.",color=Coral)
  if(saveError)Text("저장하지 못했어요. 입력과 주차 상태를 확인하세요.",color=Coral)
  PrimaryButton("${snapshot.preferences.carName}의 기억 저장",{saveError=!onSave(id,routeId,rating,note,simulated,tripDate.ifBlank{null},feelings)},Modifier.fillMaxWidth(),enabled=snapshot.memories.size<500&&validMemoryDate(tripDate))
 } else {
  Text("기억과 평가가 저장됐어요. 같은 기록은 중복 저장하지 않습니다.",color=Coral)
  SecondaryButton("새로운 기억 남기기",{id=UUID.randomUUID().toString();note="";feelings=emptyList();tripDate=java.time.LocalDate.now().toString();saveError=false},Modifier.fillMaxWidth())
 }
 }
 }
 SecondaryButton("평가를 반영한 다음 길 보기",onCompare,Modifier.fillMaxWidth().padding(vertical=16.dp))
 }
 Column(Modifier.weight(.52f)) {
 Text("우리 차의 기록 ${snapshot.memories.size}개",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("직접 남긴 경험 ${snapshot.memories.count{!it.simulated}}개 / 시연 ${snapshot.memories.count{it.simulated}}개",fontSize=16.sp,color=Muted,modifier=Modifier.padding(top=8.dp,bottom=24.dp))
 if(snapshot.memories.isEmpty())Text("아직 기억이 없어요. 첫 가족 여행을 남겨보세요.",color=Muted)
 snapshot.memories.asReversed().forEach {m ->
  Card(Modifier.fillMaxWidth().padding(bottom=16.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=White)) {
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(Catalog.routes.first{it.id==m.routeId}.title,fontWeight=FontWeight.Bold,fontSize=21.sp)
    Text("${m.carName} · 만족도 ${m.rating}/5")
    Text("여행 날짜 ${m.tripDate?:"미입력"} · 저장 ${SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(m.createdMs))}",color=Muted)
    Text(if(m.feelingTags.isEmpty())"느낌 미선택" else m.feelingTags.joinToString(" · "),color=Coral)
    Text(if(m.simulated)"시연 기록 · 추천 평점 제외" else "직접 남긴 경험 · 방문 미인증",color=Muted)
    if(m.note.isNotBlank())Text(m.note)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     OutlinedButton(onClick={editId=m.id;editNote=m.note;editRating=m.rating;editDate=m.tripDate?:"";editFeelings=m.feelingTags;saveError=false},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Line)){Text("기억 수정",fontSize=17.sp)}
     OutlinedButton(onClick={deleteId=m.id},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Line)){Text("기억 삭제",fontSize=17.sp)}
    }
   }
  }
 }
 }
 }
 if(editId!=null) Dialog(onDismissRequest={editId=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.widthIn(max=1000.dp).fillMaxWidth(.9f).heightIn(max=850.dp),shape=RoundedCornerShape(24.dp),color=White) {
   Column {
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically){
     Text("기억과 평가 수정",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
     TextButton(onClick={editId=null},modifier=Modifier.heightIn(min=76.dp)){Text("닫기",fontSize=20.sp)}
    }
    HorizontalDivider(color=Line)
    Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
     ChoiceRow("만족도",listOf("1","2","3","4","5"),editRating.toString()){editRating=it.toInt()}
     MemoryDetailsEditor(editDate,editFeelings,{editDate=it},{editFeelings=it})
     OutlinedTextField(value=editNote,onValueChange={editNote=it.take(280)},label={Text("기억 메모 · 280자 이내")},modifier=Modifier.fillMaxWidth())
     if(saveError)Text("수정하지 못했어요. 입력과 주차 상태를 확인하세요.",color=Coral)
     Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
      PrimaryButton("수정 저장",{if(onEdit(editId!!,editRating,editNote,editDate.ifBlank{null},editFeelings))editId=null else saveError=true},enabled=validMemoryDate(editDate))
      SecondaryButton("변경 취소",{editId=null})
     }
    }
   }
  }
 }
 if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text("이 기억을 삭제할까요?")},text={Text("이 앱에 저장한 메모와 평점을 삭제합니다. 삭제한 기억은 복구할 수 없습니다. 다른 여행 기록과 우표는 유지됩니다.")},confirmButton={TextButton(onClick={if(onDelete(deleteId!!))deleteId=null}){Text("기억 삭제 확인")}},dismissButton={TextButton(onClick={deleteId=null}){Text("취소")}})

}

private fun validMemoryDate(value:String):Boolean = value.isBlank() || try { java.time.LocalDate.parse(value).toString()==value } catch(_:Exception){false}
@Composable private fun MemoryDetailsEditor(date:String,feelings:List<String>,onDate:(String)->Unit,onFeelings:(List<String>)->Unit) {
 OutlinedTextField(value=date,onValueChange={onDate(it.take(10))},label={Text("여행 날짜 · YYYY-MM-DD (선택)")},isError=!validMemoryDate(date),singleLine=true,modifier=Modifier.fillMaxWidth())
 if(!validMemoryDate(date))Text("예: 2026-11-11 · 실제 달력 날짜로 입력하세요.",color=Coral)
 Text("그날의 느낌 · 직접 선택, 추천 점수에는 미반영",fontSize=17.sp,color=Muted)
 FamilyFeelings.all.chunked(2).forEach { row ->
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { feeling ->
   FilterChip(selected=feeling in feelings,onClick={onFeelings(if(feeling in feelings)feelings-feeling else feelings+feeling)},label={Text(feeling,fontSize=18.sp)},modifier=Modifier.heightIn(min=76.dp))
  }}
 }
 TextButton(onClick={onDate("");onFeelings(emptyList())},modifier=Modifier.heightIn(min=76.dp)){Text("날짜·느낌 지우기",fontSize=18.sp)}
}
