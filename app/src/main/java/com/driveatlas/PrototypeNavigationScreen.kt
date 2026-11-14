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
@Composable internal fun PrototypeNavigationPage(snapshot:FamilySnapshot,entry:CoursePreviewEntry,onCanRun:()->Boolean,onBack:()->Unit,onPage:(String)->Unit={},onDebug:()->Unit={},hasTrip:Boolean=false){
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
 var error by remember{mutableStateOf("")};var speechStatus by remember{mutableStateOf("SIM 경로 계산·재생에서 SIM·기기 내 음성 안내를 시작할 수 있어요")}
 var active by remember{mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
 var generation by remember{mutableIntStateOf(0)}
 var requestJob by remember{mutableStateOf<Job?>(null)}
 var autoSpeech by remember{mutableStateOf(false)}
 var startWithSpeech by remember{mutableStateOf(true)}
 var waitingForStart by remember{mutableStateOf(false)}
 var mapReadiness by remember{mutableStateOf(NavigationMapReadiness.LOADING)}
 var mapDialog by remember{mutableStateOf(false)}
 var startAfterMapConsent by remember{mutableStateOf(false)}
 val mapConsent=rememberVectorMapConsentState()
 val startGate=remember{SimNavigationStartGate()}
 val cueScheduler=remember{SimSpeechCueScheduler()}
 val speech=remember(context){PrototypeSpeech(context,{speechStatus=it},{autoSpeech=false})}
 fun stopSpeech(){
  val wasEnabled=autoSpeech;autoSpeech=false;cueScheduler.reset();speech.stop()
  if(wasEnabled)speechStatus="SIM 연속 음성 꺼짐 · 다시 켜기 전에는 자동 재개하지 않습니다."
 }
 fun cancelStart(){
  generation++;requestJob?.cancel();busy=false;waitingForStart=false;startGate.cancel()
  consent=false;startAfterMapConsent=false;replay=replay.copy(running=false);stopSpeech()
 }
 val plan=snapshot.plans[routeId]?:FamilyPlan()
 val request=NavPrototypeRequest(routeId,departureId,plan.effectiveRestFacilityIds,plan.restAfterStopIndex)
 fun reset(){cancelStart();result=null;simulatedOffset=null;replay=NavReplayState();mapReadiness=NavigationMapReadiness.LOADING;error=""}
 fun startRequested(){
  if(active&&onCanRun()&&!replay.running&&!busy&&!waitingForStart&&request.valid()){
   stopSpeech();error=""
   if(mapConsent.granted)consent=true else {startAfterMapConsent=true;mapDialog=true}
  }
 }
 fun mapChanged(renderedRoute:NavPrototypeRoute?,readiness:NavigationMapReadiness){
  // Ignore callbacks from a removed viewport, including an earlier identical request.
  if(renderedRoute==null||!isCurrentNavigationViewport(renderedRoute,result))return
  mapReadiness=readiness
  if(readiness==NavigationMapReadiness.FAILED||readiness==NavigationMapReadiness.DISCONNECTED){
   val wasPendingOrRunning=waitingForStart||replay.running
   cancelStart()
   if(wasPendingOrRunning)error="지도 연결을 확인하지 못해 SIM·음성을 중지했어요. 지도 복구 후 직접 다시 시작하세요."
  }else if(readiness==NavigationMapReadiness.READY){
   startGate.consume(generation,request,renderedRoute.request,mapConsent.granted,active,onCanRun(),BuildConfig.DEBUG,BuildConfig.SIMULATOR)?.let{ticket->
    waitingForStart=false;replay=NavReplayState(running=true);simulatedOffset=null;error=""
    cueScheduler.reset();autoSpeech=ticket.withSpeech&&speech.supportsContinuous
    speechStatus=if(autoSpeech)"SIM·기기 내 음성 안내 시작 · 실제 차량 주행 아님" else if(ticket.withSpeech)"연속 음성을 사용할 수 없어 SIM 텍스트 안내로 시작했어요." else "SIM 텍스트 안내 시작 · 음성 꺼짐"
   }
  }
 }
 DisposableEffect(owner,speech){
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE||event==Lifecycle.Event.ON_STOP){active=false;cancelStart();mapDialog=false}else if(event==Lifecycle.Event.ON_RESUME)active=true}
  owner.lifecycle.addObserver(observer)
  onDispose{startGate.cancel();generation++;requestJob?.cancel();owner.lifecycle.removeObserver(observer);speech.close()}
 }
 LaunchedEffect(request){reset()}
 LaunchedEffect(replay.running,result,active){
  val r=result
  if(r!=null&&replay.running&&active){
   while(replay.running&&active&&onCanRun()) {delay(1000);if(active&&onCanRun()&&mapReadiness==NavigationMapReadiness.READY)replay=r.advanceSimTick(replay)}
   if(!active||!onCanRun()){cancelStart()}
  }
 }
 val currentCanRun by rememberUpdatedState(onCanRun)
 LaunchedEffect(speech){
  try {
   while(true){
    val route=result
    if(autoSpeech){
     if(route==null||!active||!replay.running||replay.offRoute||!currentCanRun()||!mapConsent.granted)stopSpeech()
     else if(mapReadiness==NavigationMapReadiness.READY){
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
 PrototypeNavigationLayout(
  route=Catalog.routes.first{it.id==routeId},entry=entry,plan=plan,departureId=departureId,
  result=result,replay=replay,simulatedOffset=simulatedOffset,busy=busy,active=active,
  canRun=onCanRun(),validRequest=request.valid(),autoSpeech=autoSpeech,canContinuous=speech.supportsContinuous,
  error=error,speechStatus=speechStatus,hasTrip=hasTrip,
  waitingForStart=waitingForStart,mapReadiness=mapReadiness,mapConsent=mapConsent,
  onMapReadiness=::mapChanged,onMapConsent={if(active&&onCanRun()&&!replay.running&&!waitingForStart){startAfterMapConsent=false;mapDialog=true}},
  onCancelStart={cancelStart();error="시작을 취소했어요. 자동 재개하지 않습니다."},
  onBack={cancelStart();onBack()},onPage={cancelStart();onPage(it)},onDebug={cancelStart();onDebug()},
  onDeparture={if(active&&onCanRun()&&!replay.running&&!busy&&!waitingForStart)departureId=it},onCourse={if(active&&onCanRun()&&!replay.running&&!busy&&!waitingForStart)routeId=it},
  onCalculate=::startRequested,
  onToggleReplay={
   if(active&&onCanRun()){
    if(replay.running)cancelStart()
    else if(!waitingForStart&&mapConsent.granted&&mapReadiness==NavigationMapReadiness.READY)result?.let{r->
     if(!replay.offRoute&&!r.snapshot(replay).finished){stopSpeech();replay=replay.copy(running=true);error="";speechStatus="SIM 재개 · 음성 꺼짐. 필요하면 음성을 직접 켜세요."}
    }
   }
  },
  onToggleSpeech={
   if(autoSpeech)stopSpeech() else if(active&&onCanRun()&&replay.running&&speech.supportsContinuous&&mapReadiness==NavigationMapReadiness.READY){
    speech.stop();cueScheduler.reset();autoSpeech=true;speechStatus="SIM 연속 음성 켜짐 · 기기 내 합성만 사용"
   }
  },
  onOneShot={if(active&&onCanRun()&&!replay.running&&!waitingForStart)result?.let{speech.speak(it.snapshot(replay).nextInstruction)}},
  onReset={if(active&&onCanRun()&&!replay.running&&!waitingForStart){cancelStart();error="";simulatedOffset=null;replay=NavReplayState()}},
  onOffRoute={
   if(active&&onCanRun()&&!replay.running&&!waitingForStart)result?.let { r ->
    cancelStart();val view=r.snapshot(replay);val offset=Point(view.position.lat+.02,view.position.lon+.02)
    val deviation=r.offRouteDistance(offset,replay);simulatedOffset=offset
    replay=replay.copy(running=false,offRoute=deviation>100)
    error="공개 경로의 가상 위치를 이동한 탐지 시연: 경로와 약 ${deviation.toInt()}m 차이. 자동 재탐색은 미지원입니다. 처음부터 재생하거나 전체 경로를 다시 요청하세요."
   }
  },
  onClear={if(active&&onCanRun()&&!replay.running&&!waitingForStart){reset()}}
 )
 if(mapDialog)VectorMapConsentDialog(mapConsent,active&&onCanRun(),
  onDismiss={mapDialog=false;startAfterMapConsent=false},
  onAccepted={mapDialog=false;val continueStart=startAfterMapConsent;startAfterMapConsent=false;if(continueStart&&active&&onCanRun())consent=true},
  onWithdrawalResult={saved->cancelStart();mapReadiness=NavigationMapReadiness.DISCONNECTED;error=vectorMapWithdrawalMessage(saved,mapConsent.error)})
 if(consent&&request.valid())AlertDialog(onDismissRequest={cancelStart()},title={Text("공개 좌표로 SIM 경로를 계산·재생할까요?")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("받는 곳: router.project-osrm.org OSRM 공개 서버. 요청은 서버에 기록될 수 있습니다. 실제 집 주소·현재 GPS·가족 메모는 보내지 않습니다.")
  Text("출발·귀가: ${request.departure()?.label}\n코스: ${Catalog.routes.first{it.id==routeId}.title}\n선택 쉼터: ${request.selectedRestIds.joinToString{RestFacilities.find(it,routeId)?.name?:it}.ifEmpty{"없음"}}")
  Text("전송 좌표: "+request.points().joinToString(" → "){String.format(Locale.US,"%.5f, %.5f",it.lat,it.lon)})
  Text("동의하면 이번 경로를 계산하고 도로 지도가 준비된 뒤 SIM 위치 재생·안내를 바로 시작합니다. 대표점은 차량 진입구·당일 운영으로 검증되지 않았으며 실제 주행에 사용할 수 없습니다.")
  if(speech.supportsContinuous)Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
   Checkbox(checked=startWithSpeech,onCheckedChange={startWithSpeech=it},modifier=Modifier.size(64.dp));Text("기기 내 SIM 음성 안내도 시작 · 클라우드 음성 없음")
  }else Text("이 환경에서는 SIM 텍스트 안내만 시작합니다. 사용 가능한 연속 음성 서비스가 없습니다.")
  Text("이번 좌표 전송 동의는 저장하지 않습니다. 일시정지·백그라운드에서는 재생과 음성을 끄며 자동 재개하지 않습니다.")
 }},confirmButton={TextButton(onClick={
  consent=false
  if(onCanRun()&&active&&mapConsent.granted&&request.valid()&&!busy&&!waitingForStart){
   val token=++generation;busy=true;waitingForStart=true;result=null;simulatedOffset=null;replay=NavReplayState();mapReadiness=NavigationMapReadiness.LOADING;error=""
   startGate.begin(SimStartTicket(token,request,startWithSpeech&&speech.supportsContinuous))
   requestJob=scope.launch{try{
    val value=NavPrototypeService.calculate(request)
    if(token==generation&&active&&onCanRun()&&mapConsent.granted){result=value}
   }catch(e:CancellationException){throw e}catch(e:Exception){if(token==generation){startGate.cancel();waitingForStart=false;error="전체 경로를 계산하지 못했어요. 네트워크·공개 서버 상태를 확인 후 다시 시작하세요. 직선 경로로 대체하지 않습니다."}}
   finally{if(token==generation)busy=false}}
  }
 },enabled=active&&onCanRun(),modifier=Modifier.heightIn(min=64.dp)){Text(if(startWithSpeech&&speech.supportsContinuous)"동의하고 SIM·음성 시작" else "동의하고 SIM 시작")}},dismissButton={TextButton(onClick={cancelStart()},modifier=Modifier.heightIn(min=64.dp)){Text("취소")}})
}
