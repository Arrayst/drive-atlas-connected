package com.driveatlas

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/** Selects the installed PLEOS on-device service; never uses hybrid/server synthesis. */
private class PrototypeSpeech(context:Context,status:(String)->Unit,interrupted:()->Unit){
 private val pleosPresent=runCatching{context.packageManager.getPackageInfo("ai.pleos.playground.caas",0)}.isSuccess
 private val pleos=if(pleosPresent)PleosOnDevicePreview(context,status,interrupted) else null
 private val standard=if(!pleosPresent)AndroidOfflinePreview(context,status) else null
 val supportsContinuous:Boolean get()=pleos!=null
 val isBusy:Boolean get()=pleos?.isBusy==true
 val startedAtMillis:Long? get()=pleos?.startedAtMillis
 fun speakContinuous(text:String){pleos?.speak(text,continuousSim=true)}
 fun speak(text:String){pleos?.speak(text)?:standard?.speak(text)}
 fun stop(){pleos?.stop()?:standard?.stop()}
 fun close(){pleos?.close()?:standard?.close()}
}

/** Local, explicit paused instruction preview. Never registers a real active navigation route. */
private class AndroidOfflinePreview(context:Context,val status:(String)->Unit){
 private val main=android.os.Handler(android.os.Looper.getMainLooper())
 private val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
 private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
 private val tokens=SpeechRequestTokens()
 private var ready=false
 private var disposed=false
 private var tts:TextToSpeech?=null
 private var timeout:Runnable?=null
 private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
  .setAudioAttributes(attributes)
  .setOnAudioFocusChangeListener({change->
   if(change<=0&&!disposed){stopNow();status("다른 오디오 요청으로 시연 음성을 중지했어요. 자동 재개하지 않습니다.")}
  },main).build()
 init {
  // Always post: an unusually fast OnInit callback must not observe an unassigned tts field.
  tts=TextToSpeech(context.applicationContext){code->main.post{initialize(code)}}
 }
 private fun onMain(action:()->Unit){if(android.os.Looper.myLooper()==main.looper)action() else main.post(action)}
 private fun initialize(code:Int){
  if(disposed)return
  val engine=tts
  if(code!=TextToSpeech.SUCCESS||engine==null){status("음성 엔진 초기화 실패 · 텍스트 안내를 사용합니다.");return}
  try {
   val voice=engine.voices?.filter{isOfflineKoreanVoice(it.locale.language,it.isNetworkConnectionRequired,it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)==true)}?.sortedBy{it.name}?.firstOrNull()
   if(voice==null){status("설치된 오프라인 한국어 음성 없음 · 텍스트 안내만 사용합니다. 설치·다운로드하지 않습니다.");return}
   engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
    override fun onStart(id:String?){main.post{if(!disposed&&tokens.isPreview(id))status("음성 엔진 재생 시작 · 시연 문장")}}
    override fun onDone(id:String?){main.post{if(!disposed&&tokens.isPreview(id))finish(id,"음성 엔진 완료 응답 · 실제 스피커 출력은 별도 확인")}}
    @Deprecated("legacy") override fun onError(id:String?){main.post{if(!disposed)fail(id,"음성 엔진 오류 · 텍스트 안내를 확인하세요.")}}
    override fun onError(id:String?,errorCode:Int){main.post{if(!disposed)fail(id,"음성 엔진 오류($errorCode) · 텍스트 안내를 확인하세요.")}}
    override fun onStop(id:String?,interrupted:Boolean){main.post{if(!disposed&&tokens.matches(id))finish(id,"시연 음성이 중지됐어요.")}}
   })
   val selected=engine.setVoice(voice)==TextToSpeech.SUCCESS
   val actual=engine.voice
   ready=selected&&actual?.name==voice.name&&isOfflineKoreanVoice(actual.locale.language,actual.isNetworkConnectionRequired,actual.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)==true)&&engine.setAudioAttributes(attributes)==TextToSpeech.SUCCESS
   status(if(ready)"오프라인 한국어 음성 준비됨 · 정지 상태에서만 미리듣기" else "오프라인 한국어 음성 선택 실패 · 텍스트 안내만 사용합니다.")
  }catch(_:Exception){ready=false;status("음성 엔진 확인 실패 · 텍스트 안내를 사용합니다.")}
 }
 fun speak(text:String)=onMain {
  if(disposed)return@onMain
  if(!ready){status("이 환경에는 사용할 수 있는 오프라인 한국어 음성이 없어요.");return@onMain}
  stopNow()
  val id=tokens.begin()
  try {
   if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopNow();status("오디오 포커스를 얻지 못해 소리를 재생하지 않았어요.");return@onMain}
   if(!tokens.isPreview(id))return@onMain
   val engine=tts?:run{stopNow();return@onMain}
   if(engine.playSilentUtterance(200,TextToSpeech.QUEUE_FLUSH,tokens.prerollId)!=TextToSpeech.SUCCESS){stopNow();status("음성 준비 실패 · 텍스트 안내를 사용하세요.");return@onMain}
   val sentence="시연 안내. "+text.filter{!it.isISOControl()}.take(300)
   if(engine.speak(sentence,TextToSpeech.QUEUE_ADD,null,id)!=TextToSpeech.SUCCESS){stopNow();status("음성 재생 요청 실패 · 텍스트 안내를 확인하세요.");return@onMain}
   timeout=Runnable{if(tokens.isPreview(id)&&!disposed){stopNow();status("음성 완료 응답을 확인하지 못해 재생을 중지했어요.")}}.also{main.postDelayed(it,30000)}
   status("시연 음성 요청됨 · 재생 시작 응답 대기")
  }catch(_:Exception){stopNow();status("음성 재생을 시작하지 못했어요. 텍스트 안내를 사용하세요.")}
 }
 private fun finish(id:String?,message:String){
  if(!tokens.matches(id))return
  tokens.cancel();timeout?.let{main.removeCallbacks(it)};timeout=null
  runCatching{audio.abandonAudioFocusRequest(focus)};status(message)
 }
 private fun fail(id:String?,message:String){if(tokens.matches(id)){stopNow();status(message)}}
 private fun stopNow(){
  tokens.cancel();timeout?.let{main.removeCallbacks(it)};timeout=null
  runCatching{tts?.stop()};runCatching{audio.abandonAudioFocusRequest(focus)}
 }
 fun stop()=onMain{stopNow()}
 fun close()=onMain{if(!disposed){disposed=true;ready=false;stopNow();runCatching{tts?.shutdown()};tts=null}}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun PrototypeNavigationPage(snapshot:FamilySnapshot,entry:CoursePreviewEntry,onCanRun:()->Boolean,onBack:()->Unit){
 if(!BuildConfig.DEBUG||!BuildConfig.SIMULATOR||!onCanRun()){
  TextButton(onClick=onBack){Text(entry.backLabel)}
  Text("자체 내비 시제품은 주차 상태의 에뮬레이터에서만 사용할 수 있어요.",color=Ink,fontSize=24.sp);return
 }
 val context=LocalContext.current;val scope=rememberCoroutineScope();val owner=LocalLifecycleOwner.current
 var departureId by remember{mutableStateOf(DepartureReferences.all.first().id)}
 var routeId by remember(entry){mutableStateOf(entry.routeId)}
 var result by remember{mutableStateOf<NavPrototypeRoute?>(null)}
 var replay by remember{mutableStateOf(NavReplayState())}
 var simulatedOffset by remember{mutableStateOf<Point?>(null)}
 var busy by remember{mutableStateOf(false)};var consent by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")};var speechStatus by remember{mutableStateOf("음성 기본 꺼짐 · SIM 재생 중 연속 안내를 직접 켜세요")}
 var active by remember{mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
 var generation by remember{mutableIntStateOf(0)}
 var requestJob by remember{mutableStateOf<Job?>(null)}
 var autoSpeech by remember{mutableStateOf(false)}
 val cueScheduler=remember{SimSpeechCueScheduler()}
 val speech=remember(context){PrototypeSpeech(context,{speechStatus=it},{autoSpeech=false})}
 fun stopSpeech(){
  val wasEnabled=autoSpeech;autoSpeech=false;cueScheduler.reset();speech.stop()
  if(wasEnabled)speechStatus="SIM 연속 음성 꺼짐 · 다시 켜기 전에는 자동 재개하지 않습니다."
 }
 val plan=snapshot.plans[routeId]?:FamilyPlan()
 val request=NavPrototypeRequest(routeId,departureId,plan.effectiveRestFacilityIds,plan.restAfterStopIndex)
 fun reset(){generation++;requestJob?.cancel();busy=false;result=null;simulatedOffset=null;replay=NavReplayState();error="";stopSpeech()}
 DisposableEffect(owner,speech){
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE||event==Lifecycle.Event.ON_STOP){active=false;generation++;requestJob?.cancel();busy=false;consent=false;replay=replay.copy(running=false);stopSpeech()}else if(event==Lifecycle.Event.ON_RESUME)active=true}
  owner.lifecycle.addObserver(observer)
  onDispose{generation++;requestJob?.cancel();owner.lifecycle.removeObserver(observer);speech.close()}
 }
 LaunchedEffect(request){reset()}
 LaunchedEffect(replay.running,result,active){
  val r=result
  if(r!=null&&replay.running&&active){
   while(replay.running&&active&&onCanRun()) {delay(1000);if(active&&onCanRun())replay=r.advance(replay,if(autoSpeech)10.0 else 100.0)}
   if(!active||!onCanRun()){replay=replay.copy(running=false);stopSpeech()}
  }
 }
 // One observer for the lifetime of this speech controller. State-keyed effects could
 // cancel an earlier coroutine whose finally then disabled a newly enabled session.
 val currentCanRun by rememberUpdatedState(onCanRun)
 LaunchedEffect(speech){
  try {
   while(true){
    val route=result
    if(autoSpeech){
     if(route==null||!active||!replay.running||replay.offRoute||!currentCanRun())stopSpeech()
     else {
      val v=route.snapshot(replay)
      val decision=cueScheduler.tick(v.stepIndex,v.metersToInstruction,v.nextInstruction,
       android.os.SystemClock.elapsedRealtime(),speech.isBusy,!v.finished,speech.startedAtMillis)
      if(decision.cancel){speech.stop();speechStatus="SIM 연속 음성 켜짐 · 지난 지시는 취소하고 다음 안내를 기다립니다."}
      decision.text?.let{speech.speakContinuous(it)}
     }
    }
    delay(100)
   }
  } finally {speech.stop()}
 }
 var showSetup by remember(result){mutableStateOf(false)}
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  TextButton(onClick=onBack,modifier=Modifier.heightIn(min=64.dp)){Text(entry.backLabel)}
  if(result!=null) {
   Column(Modifier.weight(1f).widthIn(min=240.dp).padding(vertical=8.dp)) {
    Text("SIM · ${Catalog.routes.first{it.id==routeId}.title}",color=Ink,fontSize=26.sp,fontWeight=FontWeight.Bold)
    Text("주차·에뮬레이터 전용 · 실제 차량 길안내 아님",color=Muted,fontSize=16.sp)
   }
   TextButton(onClick={showSetup=!showSetup},modifier=Modifier.heightIn(min=64.dp)) {Text(if(showSetup)"경로 설정 접기" else "경로 설정",fontSize=18.sp)}
  }
 }
 if(result==null||showSetup) {
 Text("SIM / 왕복 경로 미리보기",color=AtlasGreen,fontSize=16.sp,fontWeight=FontWeight.Bold)
 TripHeading(Catalog.routes.first{it.id==routeId}.title,"공개 기준점 → 코스와 선택 쉼터 → 같은 기준점. 실제 차량 길안내가 아닌 위치 재생 시제품입니다.")
 Text("주차·에뮬레이터 전용 / SIM 연속 음성은 직접 켜기 · 실시간 교통·자동 이탈 재탐색 미지원",color=Muted,fontSize=17.sp)
 Spacer(Modifier.height(20.dp))
 HorizontalDivider(color=Line)
 Text("출발·귀가 기준점",color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(vertical=12.dp))
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  DepartureReferences.all.forEach{d->OutlinedButton(onClick={departureId=d.id},enabled=!busy,shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(departureId==d.id)AtlasSoft else White),modifier=Modifier.weight(1f).widthIn(min=170.dp).heightIn(min=64.dp)){Text((if(departureId==d.id)"✓ " else "")+d.label,fontSize=18.sp)}}
 }
 if(!entry.courseLocked)FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Catalog.routes.forEach{r->OutlinedButton(onClick={routeId=r.id},enabled=!busy,shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(routeId==r.id)AtlasSoft else White),modifier=Modifier.weight(1f).widthIn(min=170.dp).heightIn(min=64.dp)){Text((if(routeId==r.id)"✓ " else "")+r.title,fontSize=18.sp)}}
 }
 Text("선택 쉼터 ${plan.effectiveRestFacilityIds.size}곳 · 가족 비교에서 저장한 순서 반영 · 집 주소/GPS는 사용하지 않음",color=Muted,fontSize=16.sp,modifier=Modifier.padding(vertical=10.dp))
 if(result==null){
  Text("기본 코스 지도 · 출발·귀가와 선택 쉼터는 아래 왕복 경로 계산 후 표시됩니다.",color=Muted,fontSize=16.sp)
  RealRoadMap(Catalog.routes.first{it.id==routeId},Modifier.fillMaxWidth().height(320.dp))
 }
 Button(onClick={replay=replay.copy(running=false);stopSpeech();consent=true},enabled=!busy&&active&&request.valid(),modifier=Modifier.heightIn(min=72.dp)){Text(if(busy)"전체 경로를 계산하는 중…" else "전체 왕복 경로 계산",fontSize=20.sp)}
 }
 if(!request.valid())Text("저장된 휴식 계획 좌표·순서가 유효하지 않습니다. 가족 비교에서 확인하세요.",color=Ink,fontSize=18.sp)
 if(error.isNotEmpty())Text(error,color=Ink,fontSize=18.sp,modifier=Modifier.padding(vertical=12.dp))
 result?.let { r ->
  val view=r.snapshot(replay)
  Spacer(Modifier.height(18.dp))
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val split=maxWidth>=900.dp
   val mapWidth=if(split)(maxWidth-20.dp)*.62f else maxWidth
   val instructionWidth=if(split)(maxWidth-20.dp)*.38f else maxWidth
  FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   PrototypeRouteMap(r,simulatedOffset?:view.position,Modifier.width(mapWidth).height(420.dp),replayDistance=if(replay.offRoute) null else replay.distanceMeters)
   Surface(Modifier.width(instructionWidth),color=White,shape=RoundedCornerShape(8.dp)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(if(view.finished)"재생 완료" else if(replay.offRoute)"이탈 시연 · 정지" else if(replay.running)"가상 위치 재생 중" else "재생 일시정지",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink)
    HorizontalDivider(color=Line)
    if(!replay.offRoute&&!view.finished) {
     Text("다음 안내까지",fontSize=17.sp,color=Muted)
     Text("${view.metersToInstruction.toInt()} m",fontSize=52.sp,fontWeight=FontWeight.Bold,color=Ink)
    }
    val instructionParts=view.nextInstruction.split(" · ",limit=2)
    Text(instructionParts.first(),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
    instructionParts.getOrNull(1)?.let{Text(it,fontSize=23.sp,color=Ink)}
    Text((if(replay.offRoute)"마지막 정상 경로 기준 · " else "")+String.format(Locale.KOREA,"남은 도로 약 %.1fkm / 전체 %.1fkm",view.remainingMeters/1000,r.meters/1000),fontSize=18.sp,color=Ink)
    Text("모의 재생: 초당 ${if(autoSpeech)10 else 100}m · 실제 운전 속도 아님\n지점 좌표는 차량 진입구로 검증되지 않았습니다.",fontSize=15.sp,color=Muted)
   }}
  }
  }
  Spacer(Modifier.height(16.dp))
  var showTools by remember(r){mutableStateOf(false)}
  if(showTools) {
  FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedButton(onClick={stopSpeech();error="";simulatedOffset=null;replay=NavReplayState()},modifier=Modifier.weight(1f).widthIn(min=170.dp).heightIn(min=72.dp)){Text("처음부터",fontSize=19.sp)}
   OutlinedButton(onClick={stopSpeech();val offset=Point(view.position.lat+.02,view.position.lon+.02);val deviation=r.offRouteDistance(offset,replay);simulatedOffset=offset;replay=replay.copy(running=false,offRoute=deviation>100);error="공개 경로의 가상 위치를 이동한 탐지 시연: 경로와 약 ${deviation.toInt()}m 차이. 자동 재탐색은 미지원입니다. 처음부터 재생하거나 전체 경로를 다시 요청하세요."},modifier=Modifier.weight(1f).widthIn(min=170.dp).heightIn(min=72.dp)){Text("이탈 상태 체험",fontSize=19.sp)}
   OutlinedButton(onClick={stopSpeech();error="";simulatedOffset=null;replay=NavReplayState();result=null},modifier=Modifier.weight(1f).widthIn(min=170.dp).heightIn(min=72.dp)){Text("경로 지우기",fontSize=19.sp)}
  }
  OutlinedButton(onClick={if(active&&onCanRun()&&!replay.running)speech.speak(view.nextInstruction)},enabled=active&&!replay.running&&!replay.offRoute&&!view.finished,modifier=Modifier.heightIn(min=64.dp)){Text("현재 시연 문장 미리듣기 · 1회",fontSize=18.sp)}
  }
  Spacer(Modifier.height(10.dp))
  FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Button(onClick={stopSpeech();replay=replay.copy(running=!replay.running)},enabled=!replay.offRoute&&!view.finished&&active,modifier=Modifier.widthIn(min=220.dp).heightIn(min=72.dp)){Text(if(replay.running)"일시정지" else "위치 재생",fontSize=21.sp)}
  OutlinedButton(onClick={
   if(autoSpeech)stopSpeech() else if(active&&onCanRun()&&replay.running&&speech.supportsContinuous){
    speech.stop();cueScheduler.reset();autoSpeech=true;speechStatus="SIM 연속 음성 켜짐 · 기기 내 합성만 사용"
   }
  },enabled=active&&replay.running&&!replay.offRoute&&speech.supportsContinuous,
   modifier=Modifier.heightIn(min=72.dp)){
   Text(if(autoSpeech)"SIM 연속 음성 끄기" else "SIM 연속 음성 켜기",fontSize=18.sp)
  }
  TextButton(onClick={showTools=!showTools},modifier=Modifier.heightIn(min=72.dp)){Text(if(showTools)"시연 도구 접기" else "시연 도구",fontSize=18.sp)}
  }
  Text(speechStatus,color=Muted,fontSize=16.sp)
  Text("가상 위치 재생을 시작한 뒤 켜세요. 정지·화면 이탈 시 꺼집니다. 빠른 시연에서는 음성 ON은 10m/s, OFF는 100m/s 모의 재생입니다. 촘촘한 지시는 생략될 수 있으며 실제 운전에 사용할 수 없습니다.",color=Muted,fontSize=16.sp,modifier=Modifier.padding(top=12.dp))
 }
 Text("개발용 OSRM 공개 서버와 OSM 지도: 연결 필요·가동 보장 없음. 공개 기준점 시연이며 실제 운전에 사용하지 마세요.",color=Muted,fontSize=16.sp,modifier=Modifier.padding(vertical=16.dp))
 if(consent&&request.valid())AlertDialog(onDismissRequest={consent=false},title={Text("공개 좌표로 시연 경로를 요청할까요?")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("받는 곳: router.project-osrm.org OSRM 공개 서버. 요청은 서버에 기록될 수 있습니다. 실제 집 주소·현재 GPS·가족 메모는 보내지 않습니다.")
  Text("출발·귀가: ${request.departure()?.label}\n코스: ${Catalog.routes.first{it.id==routeId}.title}\n선택 쉼터: ${request.selectedRestIds.joinToString{RestFacilities.find(it,routeId)?.name?:it}.ifEmpty{"없음"}}")
  Text("전송 좌표: "+request.points().joinToString(" → "){String.format(Locale.US,"%.5f, %.5f",it.lat,it.lon)})
  Text("대표점은 차량 진입구·당일 운영으로 검증되지 않았습니다. 결과는 실제 주행에 사용하지 않는 에뮬레이터 시연 전용입니다.")
 }},confirmButton={TextButton(onClick={
  consent=false
  if(onCanRun()&&active&&request.valid()){val token=++generation;busy=true;result=null;simulatedOffset=null;replay=NavReplayState();error=""
   requestJob=scope.launch{try{val value=NavPrototypeService.calculate(request);if(token==generation&&active&&onCanRun()){result=value;error="전체 경로 계산 완료 · 모의 위치로만 재생합니다."}}catch(e:CancellationException){throw e}catch(e:Exception){if(token==generation)error="전체 경로를 계산하지 못했어요. 네트워크·공개 서버 상태를 확인 후 다시 시도하세요. 직선 경로로 대체하지 않습니다."}finally{if(token==generation)busy=false}}
  }
 }){Text("동의하고 이번 경로 계산")}},dismissButton={TextButton(onClick={consent=false}){Text("취소")}})
}
