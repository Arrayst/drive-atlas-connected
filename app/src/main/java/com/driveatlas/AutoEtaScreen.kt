package com.driveatlas

import androidx.compose.foundation.layout.*
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

@Composable internal fun AutoEtaPage(onCurrentPoint:()->Point?,onCanCalculate:()->Boolean,onApply:(EtaEstimate)->Boolean,onFamily:()->Unit) {
 val context=LocalContext.current;val cache=remember{EtaCache(context)};val scope=rememberCoroutineScope()
 var routeId by remember{mutableStateOf(Catalog.routes.first().id)}
 var originId by remember{mutableStateOf<String?>(DepartureReferences.all.first().id)}
 var origin by remember{mutableStateOf(DepartureReferences.all.first().point)}
 var originLabel by remember{mutableStateOf(DepartureReferences.all.first().label)}
 var result by remember{mutableStateOf<EtaEstimate?>(null)}
 var message by remember{mutableStateOf("")};var consent by remember{mutableStateOf<EtaRequest?>(null)}
 var busy by remember{mutableStateOf(false)};var job by remember{mutableStateOf<Job?>(null)}
 var generation by remember{mutableIntStateOf(0)}
 val cached=cache.load(originId,routeId,System.currentTimeMillis())
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
 Eyebrow("ROAD-BASED ROUND TRIP / PROTOTYPE")
 Text("도로 기준 자동 왕복 시간",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
 Text("출발점 → 코스의 모든 지점 → 같은 출발점으로 돌아오는 도로를 한 번에 계산합니다. 편도 시간을 두 배로 추정하지 않습니다.",color=Muted,modifier=Modifier.padding(vertical=16.dp))
 if(!busy){
  ChoiceRow("여행 코스",Catalog.routes.map{it.title},Catalog.routes.first{it.id==routeId}.title){title->routeId=Catalog.routes.first{it.title==title}.id;reset()}
  ChoiceRow("공개 출발점",DepartureReferences.all.map{it.label},originLabel){label->val ref=DepartureReferences.all.first{it.label==label};originId=ref.id;origin=ref.point;originLabel=ref.label;reset()}
  Text("공개 기준점은 건물·역 인근 위치이며 실제 차량 출입구나 현재 내 위치가 아닙니다.",color=Muted,fontSize=14.sp)
  SecondaryButton("SDK 수신 위치를 출발점으로 선택",{
   val p=onCurrentPoint()
   if(p==null){message="새 위치를 확인할 수 없어요. 공개 기준점을 사용하거나 연결 상태에서 위치 권한·수신 상태를 확인하세요."}
   else{originId=null;origin=p;originLabel="SDK 수신 위치 참고값";reset();message="SDK는 미터 단위 정확도를 제공하지 않습니다. 출발 좌표를 직접 확인한 뒤 계산하세요."}
  },Modifier.fillMaxWidth().padding(vertical=12.dp))
 }
 Text("선택: $originLabel · ${String.format(Locale.US,"%.5f, %.5f",origin.lat,origin.lon)}",fontSize=17.sp)
 Text("개인 위치는 자동 전송·기록하지 않습니다. 계산 버튼에서 전송 대상과 좌표를 확인한 뒤 이번 요청에 동의해야 합니다. 공개 기준점 결과만 24시간 캐시합니다.",color=Muted,modifier=Modifier.padding(vertical=12.dp))
 if(busy){LinearProgressIndicator(Modifier.fillMaxWidth());SecondaryButton("계산 취소",{generation++;job?.cancel();busy=false;message="계산을 취소했어요. 요청이 이미 전송됐다면 서버 기록은 취소되지 않습니다."},Modifier.fillMaxWidth().padding(top=12.dp))}
 else PrimaryButton("전송 정보 확인 후 왕복 계산",{
  val p=if(originId==null)onCurrentPoint() else origin
  if(p==null)message="위치 수신이 오래되어 계산을 시작하지 않았어요. 공개 기준점을 선택하세요."
  else{origin=p;consent=EtaRequest(routeId,p,originLabel,originId)}
 },Modifier.fillMaxWidth())
 if(message.isNotBlank())Text(message,color=Coral,modifier=Modifier.padding(vertical=14.dp))
 if(!busy&&cached!=null)TextButton(onClick={result=cached;message="저장된 공개 기준점 결과입니다. 현재 도로·교통 상태를 새로 확인한 값이 아닙니다."}){Text("24시간 이내 저장된 공개 결과 보기")}
 result?.let{v->
  Card(Modifier.fillMaxWidth().padding(vertical=16.dp),colors=CardDefaults.cardColors(containerColor=White)){
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(if(v.cached)"저장된 도로 예상값" else "도로 예상값 계산 완료",fontSize=23.sp,fontWeight=FontWeight.Bold)
    Text("출발→코스 ${v.outboundMinutes}분 + 코스 주행 ${v.courseMinutes}분 + 코스→복귀 ${v.returnMinutes}분",fontSize=20.sp)
    Text("전체 주행 ${v.roadMinutes}분 · ${String.format(Locale.KOREA,"%.1f",v.meters/1000)}km",fontSize=23.sp)
    Text("${v.originLabel}\n계산 ${SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(v.calculatedAt))}\n분 단위 올림 · 실시간 교통 미반영. 휴식·걷기는 가족 계획에서 더해집니다.",color=Muted)
    PrimaryButton("가족 계획에 자동 시간 적용",{if(onApply(v)){message="자동 시간이 가족 계획에 저장됐어요."}else message="지금은 적용할 수 없어요. 주차 상태와 계산 범위를 확인하세요."},Modifier.fillMaxWidth())
   }
  }
 }
 SecondaryButton("가족 계획에서 비교",onFamily,Modifier.fillMaxWidth())
 Text("OSRM/FOSSGIS 공개 서버: 소규모 비상업 시험용 · 요청 기록됨 · 최대 초당 1회 · 서비스 보장 없음. 운영 배포판은 별도 경로 공급자 설정 전까지 이 서버를 호출하지 않습니다.",color=Muted,fontSize=14.sp,modifier=Modifier.padding(top=20.dp))
 TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap 기여자 · 데이터 저작권")}
 TextButton(onClick={openWeb(context,"https://project-osrm.org/")}){Text("Routing: OSRM · 제공 정보")}
 TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/fixthemap")}){Text("지도 오류 수정 안내")}
 consent?.let{request->AlertDialog(onDismissRequest={consent=null},title={Text("이번 경로 요청에 동의할까요?")},text={Column{
  Text("받는 곳: FOSSGIS가 운영하는 OSRM 공개 라우팅 서버 (router.project-osrm.org).")
  Text("보내는 데이터: 선택한 출발 좌표 ${String.format(Locale.US,"%.5f, %.5f",request.origin.lat,request.origin.lon)}와 ${Catalog.routes.first{it.id==request.routeId}.title}의 공개 코스 좌표. 같은 출발점으로 돌아오는 경로를 계산합니다.")
  Text("서버에 요청이 기록됩니다. 이름·가족 메모·평가는 보내지 않습니다. ${if(request.publicReferenceId==null)"개인 위치 전송은 이동 정보를 노출할 수 있으며, 이 앱은 해당 좌표를 캐시하지 않습니다." else "선택한 공개 출발 기준점을 보냅니다."}")
 }},confirmButton={TextButton(onClick={consent=null
  val current=if(request.publicReferenceId==null)onCurrentPoint() else request.origin
  if(current==null||!nativePointMatches(request.origin,current)){message="위치가 변경되었거나 수신이 오래됐어요. 출발점을 다시 확인하세요."}
  else runCalculation(request)}){Text("이번 요청에 동의하고 계산")}},dismissButton={TextButton(onClick={consent=null}){Text("취소")}})}
}
