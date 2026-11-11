package com.driveatlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
 Text("오늘 우리 가족에게 맞는 길",fontSize=32.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("왕복 시간과 확인이 필요한 조건을 나란히 살펴보세요.",color=Muted,fontSize=18.sp,modifier=Modifier.padding(vertical=12.dp))
 Card(Modifier.fillMaxWidth().padding(bottom=20.dp),shape=RoundedCornerShape(3.dp),colors=CardDefaults.cardColors(containerColor=White)) {
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(28.dp),verticalAlignment=Alignment.CenterVertically) {
    Text("${p.carName}의 오늘 조건",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text("전체 여유 ${p.availableMinutes}분 · 걷기 ${p.walkMinutes}분 이내\n${if(p.restroomRequired) "화장실 필수" else "화장실 조건 없음"} · ${if(p.strollerRequired) "유모차 이동" else "유모차 조건 없음"}",fontSize=17.sp,lineHeight=26.sp,color=Ink,modifier=Modifier.weight(1f))
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
 if(ranked.none{it.matches}) Text("확인된 조건을 모두 만족하는 코스가 없어요. 부족한 정보와 시간 차이를 확인해 주세요.",color=Color(0xFF865300),fontSize=16.sp,modifier=Modifier.padding(bottom=16.dp))
 Column(Modifier.fillMaxWidth()) {
  ranked.forEachIndexed { index,m -> key(m.route.id) {
   FamilyComparisonCard(m,index,expandedRoute==m.route.id,{expandedRoute=if(expandedRoute==m.route.id)null else m.route.id},{onRoute(m.route)},Modifier.fillMaxWidth())
   Spacer(Modifier.height(12.dp))
  } }
 }
 Text("정렬 기준: 조건 불일치·미확인 수 → 직접 남긴 코스 평점 → 코스 총 시간. 자동 학습이나 실시간 혼잡 예측이 아닙니다.",color=Muted,fontSize=14.sp,modifier=Modifier.padding(vertical=16.dp))
 ranked.firstOrNull{it.route.id==expandedRoute}?.let { m ->
  FamilyPlanningPanel(m,onPlan,{expandedRoute=null})
 }
 Text("시간 단위: 분. 출발→코스·코스→복귀 시간까지 입력하면 전체 일정을 비교합니다. 시간을 직접 입력하거나 자동 왕복 시간에서 도로 예상값을 적용할 수 있습니다. 실시간 교통 ETA는 아닙니다. 미입력 시간을 0분으로 가정하지 않습니다.",color=Muted,fontSize=15.sp,modifier=Modifier.padding(vertical=16.dp))
 SecondaryButton("가족 차 기억 · 피드백",onMemory,Modifier.fillMaxWidth().padding(top=8.dp))
}

@Composable private fun FamilyComparisonCard(m:FamilyMatch,index:Int,expanded:Boolean,onExpand:()->Unit,onRoute:()->Unit,modifier:Modifier=Modifier) {
 val statusColor=when { m.conflicts.isNotEmpty()->Color(0xFFAC382C); m.unknowns.isNotEmpty()->Color(0xFF865300); else->Color(0xFF176B4D) }
 val status=when { m.conflicts.isNotEmpty()->"조건 조정 필요"; m.unknowns.isNotEmpty()->"추가 확인 필요"; else->"입력 조건 충족" }
 Surface(modifier,color=White,shape=RoundedCornerShape(3.dp),border=BorderStroke(if(expanded)2.dp else 1.dp,if(expanded)Ink else Line)) {
  Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.Top) {
   Column(Modifier.weight(.25f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(m.route.title,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text(m.route.subtitle,fontSize=16.sp,color=Muted)
    Text(m.previousRating?.let{"우리 가족의 이전 평가 ${String.format(Locale.KOREA,"%.1f",it)}/5 반영"}?:"아직 실제 경험으로 직접 남긴 평가가 없어요.",fontSize=14.sp,color=Muted)
   }
   Column(Modifier.weight(.22f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(if(m.plan.transferKnown) "${m.totalMinutes}분" else "왕복 미완성",fontSize=if(m.plan.transferKnown)34.sp else 26.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text(if(m.plan.transferKnown) "왕복 포함 총 계획 시간" else "확인된 구간 합계 ${m.totalMinutes}분",fontSize=15.sp,color=Muted)
    Text("출발→코스 ${m.plan.outboundMinutes?.let{ "${it}분" }?:"미입력"} / 복귀 ${m.plan.returnMinutes?.let{ "${it}분" }?:"미입력"}",fontSize=15.sp,color=Ink)
    Text("주행 약 ${m.plan.courseDriveMinutes?:m.route.minutes}분 / 휴식 ${m.plan.restMinutes}분 / 걷기 ${m.plan.walkMinutes}분",fontSize=14.sp,color=Muted)
    Text(if(m.plan.etaCalculatedAt!=null) "OSRM 도로 예상 · 실시간 교통 미반영" else "수동 계획 · 실시간 교통 미반영",fontSize=13.sp,color=Muted)
   }
   Column(Modifier.weight(.31f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text(status,fontSize=18.sp,fontWeight=FontWeight.Bold,color=statusColor)
    Text((m.conflicts+m.unknowns).joinToString("\n").ifBlank{"입력 시간·선택 휴식지 공개정보 기준 충족 · 당일 운영은 확인 필요"},fontSize=16.sp,lineHeight=25.sp,color=statusColor)
   }
   Column(Modifier.weight(.22f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    SecondaryButton(if(expanded) "시간·휴식 계획 접기" else "시간·휴식 계획 조정",onExpand,Modifier.fillMaxWidth())
    PrimaryButton("코스 상세 · 내비 연결",onRoute,Modifier.fillMaxWidth())
   }
  }
 }
}

@Composable private fun FamilyPlanningPanel(m:FamilyMatch,onPlan:(String,FamilyPlan)->Unit,onClose:()->Unit) {
 val context=LocalContext.current
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(3.dp),colors=CardDefaults.cardColors(containerColor=White)) {
  Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Eyebrow("PLAN / REST & RETURN")
   Text("${m.route.title} · 시간과 휴식",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Ink)
   Text("현재 적용 시간 · 출발→코스 ${m.plan.outboundMinutes?.let{"${it}분"}?:"미입력"} · 코스 주행 ${m.plan.courseDriveMinutes?:m.route.minutes}분 · 복귀 ${m.plan.returnMinutes?.let{"${it}분"}?:"미입력"}",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Ink)
   Text("아래 시간 버튼을 선택하면 해당 구간을 수동 계획으로 변경합니다.",fontSize=15.sp,color=Muted)
    m.plan.etaCalculatedAt?.let{at->Text("${if(m.plan.etaWasCached)"저장된 " else ""}OSRM 도로 예상 · ${m.plan.etaOriginLabel} · ${SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(at))} · 실시간 교통 미반영",color=Muted,fontSize=13.sp)}
    ChoiceRow("코스까지",listOf("미입력","0","30","60","120","180","240"),m.plan.outboundMinutes?.toString()?:"미입력"){onPlan(m.route.id,manualTransfers(m.plan,outbound=it.toIntOrNull()))}
    ChoiceRow("복귀 시간",listOf("미입력","0","30","60","120","180","240"),m.plan.returnMinutes?.toString()?:"미입력"){onPlan(m.route.id,manualTransfers(m.plan,back=it.toIntOrNull()))}
    ChoiceRow("휴식 계획",listOf("0","30","60","90"),m.plan.restMinutes.toString()){onPlan(m.route.id,m.plan.copy(restMinutes=it.toInt()))}
    ChoiceRow("걷기 계획",listOf("0","10","20","40"),m.plan.walkMinutes.toString()){onPlan(m.route.id,m.plan.copy(walkMinutes=it.toInt()))}
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
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(3.dp),colors=CardDefaults.cardColors(containerColor=White)) {
 Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text(if(saved) "저장한 기억" else "코스와 만족도",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
 if(!saved) {
  Text("기억할 길",color=Muted,fontSize=16.sp)
  Catalog.routes.forEach { route ->
   FilterChip(selected=routeId==route.id,onClick={routeId=route.id},label={Text(route.title,fontSize=18.sp)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(3.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Ink,selectedLabelColor=White))
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
  Card(Modifier.fillMaxWidth().padding(bottom=16.dp),shape=RoundedCornerShape(3.dp),colors=CardDefaults.cardColors(containerColor=White)) {
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(Catalog.routes.first{it.id==m.routeId}.title,fontWeight=FontWeight.Bold,fontSize=21.sp)
    Text("${m.carName} · 만족도 ${m.rating}/5 · ${SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(m.createdMs))}")
    Text(if(m.simulated)"시연 기록 · 추천 평점 제외" else "직접 남긴 경험 · 방문 미인증",color=Muted)
    if(m.note.isNotBlank())Text(m.note)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     OutlinedButton(onClick={editId=m.id;editNote=m.note;editRating=m.rating},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(3.dp),border=BorderStroke(1.dp,Line)){Text("기억 수정",fontSize=17.sp)}
     OutlinedButton(onClick={deleteId=m.id},modifier=Modifier.heightIn(min=56.dp),shape=RoundedCornerShape(3.dp),border=BorderStroke(1.dp,Line)){Text("기억 삭제",fontSize=17.sp)}
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
