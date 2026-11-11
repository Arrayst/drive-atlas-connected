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

@Composable internal fun FamilyPage(snapshot: FamilySnapshot, onPreferences:(FamilyPreferences)->Unit,
 onPlan:(String,FamilyPlan)->Unit,onRoute:(Route)->Unit,onMemory:()->Unit) {
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
    Text("전체 여유 ${p.availableMinutes}분 · 걷기 ${p.walkMinutes}분 이내\n${if(p.restroomRequired) "화장실 필수" else "화장실 조건 없음"} · ${if(p.strollerRequired) "유모차 이동" else "유모차 조건 없음"}",fontSize=20.sp,lineHeight=30.sp,color=Ink,modifier=Modifier.weight(1f))
    SecondaryButton(if(settingsOpen) "가족 조건 접기" else "가족 조건 변경",{settingsOpen=!settingsOpen})
   }
   if(settingsOpen) {
 var car by remember(p.carName){mutableStateOf(p.carName)}
 OutlinedTextField(value=car,onValueChange={car=it.take(24)},label={Text("함께하는 차의 별명 (24자 이내)")},singleLine=true)
 TextButton(onClick={onPreferences(p.copy(carName=car.trim()))},enabled=car.isNotBlank()&&car!=p.carName){Text("차량 별명 저장")}
 ChoiceRow("전체 여유",listOf("60","120","180","240","360","480","720"),p.availableMinutes.toString()){onPreferences(p.copy(availableMinutes=it.toInt()))}
 ChoiceRow("걷기 한도",listOf("0","10","20","40"),p.walkMinutes.toString()){onPreferences(p.copy(walkMinutes=it.toInt()))}
 Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  FilterChip(selected=p.restroomRequired,onClick={onPreferences(p.copy(restroomRequired=!p.restroomRequired))},label={Text("화장실 필수")},modifier=Modifier.heightIn(min=56.dp))
  FilterChip(selected=p.strollerRequired,onClick={onPreferences(p.copy(strollerRequired=!p.strollerRequired))},label={Text("유모차 이동")},modifier=Modifier.heightIn(min=56.dp))
 }
   }
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
      Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState())) { FamilyPlanningPanel(selected,onPlan,{expandedRoute=null}) }
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
  Text("조건 불일치·미확인 수 → 직접 남긴 코스 평점 → 코스 총 시간 순으로 정렬합니다. 자동 학습이나 실시간 혼잡 예측이 아닙니다.",color=Muted,fontSize=18.sp)
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
   listOf("가는 길" to (m.plan.outboundMinutes?.let{tripDuration(it)}?:"미입력"),"코스 주행" to tripDuration(m.plan.courseDriveMinutes?:m.route.minutes),"오는 길" to (m.plan.returnMinutes?.let{tripDuration(it)}?:"미입력")).forEach{(label,value)->
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){Text(label,fontSize=18.sp,color=Muted);Text(value,fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink)}
   }
  }
  Text("휴식 ${m.plan.restMinutes}분  ·  걷기 ${m.plan.walkMinutes}분",fontSize=19.sp,color=Muted)
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
  Text(m.previousRating?.let{"직접 남긴 이전 평가 ${String.format(Locale.KOREA,"%.1f",it)}/5 반영"}?:"아직 직접 남긴 코스 평가가 없어요.",fontSize=16.sp,color=Muted)
 }
}

@Composable private fun FamilyPlanningPanel(m:FamilyMatch,onPlan:(String,FamilyPlan)->Unit,onClose:()->Unit) {
 val context=LocalContext.current
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=White)) {
  Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("시간 선택 · 분 단위",fontSize=18.sp,color=Muted)
   Text("${m.route.title} · 시간과 휴식",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Ink)
   Text("현재 적용 시간 · 출발→코스 ${m.plan.outboundMinutes?.let{"${it}분"}?:"미입력"} · 코스 주행 ${m.plan.courseDriveMinutes?:m.route.minutes}분 · 복귀 ${m.plan.returnMinutes?.let{"${it}분"}?:"미입력"}",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Ink)
   Text("아래 시간 버튼을 선택하면 해당 구간을 수동 계획으로 변경합니다.",fontSize=15.sp,color=Muted)
    m.plan.etaCalculatedAt?.let{at->Text("${if(m.plan.etaWasCached)"저장된 " else ""}OSRM 도로 예상 · ${m.plan.etaOriginLabel} · ${SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(at))} · 실시간 교통 미반영",color=Muted,fontSize=13.sp)}
    TripTimeChoices("코스까지",listOf("미입력","0","30","60","120","180","240"),m.plan.outboundMinutes?.toString()?:"미입력"){onPlan(m.route.id,manualTransfers(m.plan,outbound=it.toIntOrNull()))}
    TripTimeChoices("복귀 시간",listOf("미입력","0","30","60","120","180","240"),m.plan.returnMinutes?.toString()?:"미입력"){onPlan(m.route.id,manualTransfers(m.plan,back=it.toIntOrNull()))}
    TripTimeChoices("휴식 계획",listOf("0","30","60","90"),m.plan.restMinutes.toString()){onPlan(m.route.id,m.plan.copy(restMinutes=it.toInt()))}
    TripTimeChoices("걷기 계획",listOf("0","10","20","40"),m.plan.walkMinutes.toString()){onPlan(m.route.id,m.plan.copy(walkMinutes=it.toInt()))}
    val facilities=RestFacilities.forRoute(m.route.id)
    ChoiceRow("휴식 장소",listOf("선택 안 함")+facilities.map{it.name},RestFacilities.find(m.plan.restFacilityId,m.route.id)?.name?:"선택 안 함"){name->onPlan(m.route.id,m.plan.copy(restFacilityId=facilities.firstOrNull{it.name==name}?.id))}
    RestFacilities.find(m.plan.restFacilityId,m.route.id)?.let{f->
     Text("${f.parking}\n${f.access}\n${f.hours}",color=Muted,fontSize=15.sp)
     Text("공개자료 확인 ${f.checkedAt} · 해당 휴식 장소에 한정 · 실시간 운영/빈자리 미확인",color=Muted,fontSize=13.sp)
     TextButton(onClick={openWeb(context,f.source)}){Text("${f.sourceLabel} 시설 안내 ↗")}
     TextButton(onClick={openWeb(context,"https://map.kakao.com/link/search/"+java.net.URLEncoder.encode(f.name+" "+f.address,"UTF-8"))}){Text("휴식 장소 지도 검색 ↗")}
    }
   Text("${m.route.stops.joinToString(" → "){it.name}}\n내비 경유 좌표는 도로 지점입니다. 선택 휴식지의 정확한 진입은 별도 지도 검색·현장 안내로 확인하세요. 도로변 임의 정차는 금지합니다.",color=Muted,fontSize=16.sp)
   SecondaryButton("계획 접기",onClose)
  }
 }
}

@Composable internal fun FamilyMemoriesPage(snapshot:FamilySnapshot,
 onSave:(String,String,Int,String,Boolean)->Boolean,onCompare:()->Unit,
 onEdit:(String,Int,String)->Boolean,onDelete:(String)->Boolean) {
 Text("함께한 시간을 다음 길로",fontSize=32.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("계정·클라우드 없이 이 앱 안에 저장됩니다. 직접 남긴 기록은 GPS 방문 인증이나 완주 배지가 아닙니다. 개인 신상이나 건강정보는 메모에 넣지 마세요.",color=Muted,modifier=Modifier.padding(vertical=16.dp))
 var routeId by rememberSaveable{mutableStateOf(Catalog.routes.first().id)}
 var rating by rememberSaveable{mutableIntStateOf(4)}
 var note by rememberSaveable{mutableStateOf("")}
 var simulated by rememberSaveable{mutableStateOf(false)}
 var id by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())}
 var editId by rememberSaveable{mutableStateOf<String?>(null)}
 var editNote by rememberSaveable{mutableStateOf("")}
 var editRating by rememberSaveable{mutableIntStateOf(4)}
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
  OutlinedTextField(value=note,onValueChange={note=it.take(280)},label={Text("기억하고 싶은 점 (선택, 280자)")},modifier=Modifier.fillMaxWidth(),minLines=2)
  FilterChip(selected=simulated,onClick={simulated=!simulated},label={Text("실제 여행이 아닌 시연 기록 (추천 평점에서 제외)")},modifier=Modifier.heightIn(min=56.dp))
  if(snapshot.memories.size>=500)Text("이 기기의 기억 500개 한도에 도달했어요. 기존 기록은 자동 삭제하지 않습니다. 직접 불필요한 기억을 삭제한 뒤 새 기억을 저장하세요.",color=Coral)
  PrimaryButton("${snapshot.preferences.carName}의 기억 저장",{onSave(id,routeId,rating,note,simulated)},Modifier.fillMaxWidth(),enabled=snapshot.memories.size<500)
 } else {
  Text("기억과 평가가 저장됐어요. 같은 기록은 중복 저장하지 않습니다.",color=Coral)
  SecondaryButton("새로운 기억 남기기",{id=UUID.randomUUID().toString();note=""},Modifier.fillMaxWidth())
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
    Text("${m.carName} · 만족도 ${m.rating}/5 · ${SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(m.createdMs))}")
    Text(if(m.simulated)"시연 기록 · 추천 평점 제외" else "직접 남긴 경험 · 방문 미인증",color=Muted)
    if(m.note.isNotBlank())Text(m.note)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     OutlinedButton(onClick={editId=m.id;editNote=m.note;editRating=m.rating},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Line)){Text("기억 수정",fontSize=17.sp)}
     OutlinedButton(onClick={deleteId=m.id},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Line)){Text("기억 삭제",fontSize=17.sp)}
    }
   }
  }
 }
 }
 }
 if(editId!=null) AlertDialog(onDismissRequest={editId=null},title={Text("기억과 평가 수정")},text={Column{
  ChoiceRow("만족도",listOf("1","2","3","4","5"),editRating.toString()){editRating=it.toInt()}
  OutlinedTextField(value=editNote,onValueChange={editNote=it.take(280)},label={Text("기억 메모")})
 }},confirmButton={TextButton(onClick={if(onEdit(editId!!,editRating,editNote))editId=null}){Text("수정 저장")}},dismissButton={TextButton(onClick={editId=null}){Text("취소")}})
 if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text("이 기억을 삭제할까요?")},text={Text("이 앱에 저장한 메모와 평점을 삭제합니다. 삭제한 기억은 복구할 수 없습니다. 다른 여행 기록과 우표는 유지됩니다.")},confirmButton={TextButton(onClick={if(onDelete(deleteId!!))deleteId=null}){Text("기억 삭제 확인")}},dismissButton={TextButton(onClick={deleteId=null}){Text("취소")}})

}
