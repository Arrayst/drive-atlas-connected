package com.driveatlas

import androidx.compose.foundation.layout.*
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
 val context=LocalContext.current
 val p=snapshot.preferences
 Eyebrow("FAMILY / PLAN TOGETHER")
 Text("오늘 우리 가족에게 맞는 길",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("가족 조건 → 코스·휴식 비교 → 기존 내비 연결 → 기억·평가 → 다음 추천",color=Muted,modifier=Modifier.padding(vertical=12.dp))
 var car by remember(p.carName){mutableStateOf(p.carName)}
 OutlinedTextField(value=car,onValueChange={car=it.take(24)},label={Text("함께하는 차의 별명 (24자 이내)")},singleLine=true)
 TextButton(onClick={onPreferences(p.copy(carName=car.trim()))},enabled=car.isNotBlank()&&car!=p.carName){Text("차량 별명 저장")}
 ChoiceRow("전체 여유",listOf("60","120","180","240","360","480","720"),p.availableMinutes.toString()){onPreferences(p.copy(availableMinutes=it.toInt()))}
 ChoiceRow("걷기 한도",listOf("0","10","20","40"),p.walkMinutes.toString()){onPreferences(p.copy(walkMinutes=it.toInt()))}
 Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  FilterChip(selected=p.restroomRequired,onClick={onPreferences(p.copy(restroomRequired=!p.restroomRequired))},label={Text("화장실 필수")},modifier=Modifier.heightIn(min=56.dp))
  FilterChip(selected=p.strollerRequired,onClick={onPreferences(p.copy(strollerRequired=!p.strollerRequired))},label={Text("유모차 이동")},modifier=Modifier.heightIn(min=56.dp))
 }
 Text("시간 단위: 분. 출발→코스·코스→복귀 시간까지 입력하면 전체 일정을 비교합니다. 왕복 시간은 직접 확인한 예상값이며 실시간 내비 ETA가 아닙니다. 미입력 시간을 0분으로 가정하지 않습니다.",color=Muted,fontSize=15.sp,modifier=Modifier.padding(vertical=16.dp))
 val ranked=rankFamilyRoutes(snapshot)
 if(ranked.none{it.matches}) Text("확인된 조건을 모두 만족하는 코스가 없어요. 부족한 정보와 시간 차이를 확인해 주세요.",color=Coral,modifier=Modifier.padding(bottom=16.dp))
 ranked.forEachIndexed { index,m ->
  Card(Modifier.fillMaxWidth().padding(bottom=18.dp),colors=CardDefaults.cardColors(containerColor=White)) {
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("${index+1}. ${m.route.title}",fontSize=23.sp,fontWeight=FontWeight.Bold)
    Text("${m.route.subtitle}\n코스 주행 약 ${m.route.minutes}분 + 휴식 ${m.plan.restMinutes}분 + 걷기 ${m.plan.walkMinutes}분\n출발→코스 ${m.plan.outboundMinutes?.toString()?:"미입력"}분 · 코스→복귀 ${m.plan.returnMinutes?.toString()?:"미입력"}분\n${if(m.plan.transferKnown)"왕복 포함 총" else "확인된 구간 합계"} ${m.totalMinutes}분",fontSize=17.sp)
    ChoiceRow("코스까지",listOf("미입력","0","30","60","120","180","240"),m.plan.outboundMinutes?.toString()?:"미입력"){onPlan(m.route.id,m.plan.copy(outboundMinutes=it.toIntOrNull()))}
    ChoiceRow("복귀 시간",listOf("미입력","0","30","60","120","180","240"),m.plan.returnMinutes?.toString()?:"미입력"){onPlan(m.route.id,m.plan.copy(returnMinutes=it.toIntOrNull()))}
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
    Text((m.conflicts+m.unknowns).joinToString(" · ").ifBlank{"입력 시간·선택 휴식지 공개정보 기준 충족 · 당일 운영은 확인 필요"},color=if(m.matches)Coral else Muted)
    Text(m.previousRating?.let{"우리 가족의 이전 평가 ${String.format(Locale.KOREA,"%.1f",it)}/5 반영"}?:"아직 실제 경험으로 직접 남긴 평가가 없어요.",color=Muted)
    Text("${m.route.stops.joinToString(" → "){it.name}}\n내비 경유 좌표는 도로 지점입니다. 선택 휴식지의 정확한 진입은 별도 지도 검색·현장 안내로 확인하세요. 도로변 임의 정차는 금지합니다.",color=Muted)
    PrimaryButton("코스 상세 · 내비 연결",{onRoute(m.route)},Modifier.fillMaxWidth())
   }
  }
 }
 Text("정렬 기준: 조건 불일치·미확인 수 → 직접 남긴 코스 평점 → 코스 총 시간. 자동 학습이나 실시간 혼잡 예측이 아닙니다.",color=Muted)
 SecondaryButton("가족 차 기억 · 피드백",onMemory,Modifier.fillMaxWidth().padding(top=16.dp))
}

@Composable internal fun FamilyMemoriesPage(snapshot:FamilySnapshot,
 onSave:(String,String,Int,String,Boolean)->Boolean,onCompare:()->Unit,
 onEdit:(String,Int,String)->Boolean,onDelete:(String)->Boolean) {
 Eyebrow("OUR CAR / OUR MEMORIES")
 Text("함께한 시간을 다음 길로",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
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
 if(!saved) {
  ChoiceRow("기억할 길",Catalog.routes.map{it.title},Catalog.routes.first{it.id==routeId}.title){title->routeId=Catalog.routes.first{it.title==title}.id}
  ChoiceRow("만족도",listOf("1","2","3","4","5"),rating.toString()){rating=it.toInt()}
  OutlinedTextField(value=note,onValueChange={note=it.take(280)},label={Text("기억하고 싶은 점 (선택, 280자)")},modifier=Modifier.fillMaxWidth(),minLines=2)
  FilterChip(selected=simulated,onClick={simulated=!simulated},label={Text("실제 여행이 아닌 시연 기록 (추천 평점에서 제외)")},modifier=Modifier.heightIn(min=56.dp))
  if(snapshot.memories.size>=500)Text("이 기기의 기억 500개 한도에 도달했어요. 기존 기록은 자동 삭제하지 않습니다. 직접 불필요한 기억을 삭제한 뒤 새 기억을 저장하세요.",color=Coral)
  PrimaryButton("${snapshot.preferences.carName}의 기억 저장",{onSave(id,routeId,rating,note,simulated)},Modifier.fillMaxWidth(),enabled=snapshot.memories.size<500)
 } else {
  Text("기억과 평가가 저장됐어요. 같은 기록은 중복 저장하지 않습니다.",color=Coral)
  SecondaryButton("새로운 기억 남기기",{id=UUID.randomUUID().toString();note=""},Modifier.fillMaxWidth())
 }
 SecondaryButton("평가를 반영한 다음 길 보기",onCompare,Modifier.fillMaxWidth().padding(vertical=16.dp))
 if(snapshot.memories.isEmpty())Text("아직 기억이 없어요. 첫 가족 여행을 남겨보세요.",color=Muted)
 snapshot.memories.asReversed().forEach {m ->
  Card(Modifier.fillMaxWidth().padding(bottom=12.dp),colors=CardDefaults.cardColors(containerColor=White)) {
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(Catalog.routes.first{it.id==m.routeId}.title,fontWeight=FontWeight.Bold,fontSize=21.sp)
    Text("${m.carName} · 만족도 ${m.rating}/5 · ${SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(m.createdMs))}")
    Text(if(m.simulated)"시연 기록 · 추천 평점 제외" else "직접 남긴 경험 · 방문 미인증",color=Muted)
    if(m.note.isNotBlank())Text(m.note)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     TextButton(onClick={editId=m.id;editNote=m.note;editRating=m.rating}){Text("기억 수정")}
     TextButton(onClick={deleteId=m.id}){Text("기억 삭제")}
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
