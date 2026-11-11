package com.driveatlas

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

/** Explicit, parked SIM preview only. No app registration, credentials, server mode or automatic speech. */
internal class PleosOnDevicePreview(context:Context,private val status:(String)->Unit){
 private val app=context.applicationContext
 private val main=Handler(Looper.getMainLooper())
 private val audio=app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
 private val tokens=SpeechRequestTokens()
 private var disposed=false
 private var activeId:String?=null
 private var engine:TextToSpeech?=null
 private var listener:EventListener?=null
 private var timeout:Runnable?=null
 private var pending:Runnable?=null
 private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
  .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
  .setOnAudioFocusChangeListener({change->if(change<=0&&activeId!=null&&!disposed){cleanup();status("다른 오디오 요청으로 시연 음성을 중지했어요. 자동 재개하지 않습니다.")}},main).build()
 private fun onMain(block:()->Unit){if(Looper.myLooper()==main.looper)block() else main.post(block)}
 private fun current(id:String)=!disposed&&tokens.isPreview(id)
 private fun trace(event:String){if(BuildConfig.DEBUG)android.util.Log.i("AtlasSpeech","ON_DEVICE $event")}
 fun speak(text:String)=onMain {
  if(disposed)return@onMain
  if(activeId!=null){status("시연 음성 처리 중입니다. 완료 후 다시 눌러 주세요.");return@onMain}
  val id=tokens.begin();activeId=id
  val sentence="시연 안내. "+text.filter{!it.isISOControl()}.take(300)
  try {
   // Explicit mode is critical: the SDK default is HYBRID, which is not allowed here.
   val sdk=TextToSpeech(app,Mode.ON_DEVICE);engine=sdk
   var peakRms=0.0
   val events=object:EventListener {
    override fun onReady(){main.post{if(current(id))trace("ready")}}
    override fun onStart(){main.post{if(current(id)){trace("start");status("PLEOS 기기 내 음성 재생 시작 · 시연 안내")}}}
    override fun onUpdatedRms(rms:Double){main.post{if(current(id)&&rms.isFinite())peakRms=maxOf(peakRms,kotlin.math.abs(rms))}}
    override fun onDone(){main.post{if(current(id)){trace("done peakRms=$peakRms");cleanup();status("PLEOS 음성 완료 응답 · 실제 스피커 출력은 별도 확인")}}}
    override fun onStop(){main.post{if(current(id)){trace("stopped");cleanup();status("시연 음성이 중지됐어요.")}}}
    override fun onError(errorType:ErrorType){main.post{if(current(id)){trace("error=$errorType");cleanup();status("기기 내 음성 오류($errorType) · 텍스트 안내를 사용하세요.")}}}
   }
   listener=events
   timeout=Runnable{if(current(id)){trace("timeout");cleanup();status("음성 완료 응답을 확인하지 못해 중지했어요.")}}.also{main.postDelayed(it,30000)}
   status("PLEOS 기기 내 음성 준비 중 · 네트워크 음성 사용 안 함")
   sdk.initialize(object:OnInitializeListener {
    override fun onSuccess(){main.post{
     if(!current(id))return@post
     try {
      sdk.addEventListener(events);sdk.setLocale(Locale.KOREA)
      if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){cleanup();status("오디오 포커스를 얻지 못해 소리를 재생하지 않았어요.");return@post}
      if(!current(id))return@post
      // SDK has no PCM/silence API. This is only a focus-settling delay, not PCM pre-roll.
      pending=Runnable{if(current(id)){try{trace("speak-request");sdk.speak(sentence)}catch(_:Exception){cleanup();status("기기 내 음성 요청 실패 · 텍스트 안내를 사용하세요.")}}}.also{main.postDelayed(it,200)}
     }catch(_:Exception){cleanup();status("기기 내 음성 준비 실패 · 텍스트 안내를 사용하세요.")}
    }}
    override fun onFailure(errorType:ErrorType){main.post{if(current(id)){trace("init-error=$errorType");cleanup();status("기기 내 음성 초기화 실패($errorType) · 텍스트 안내를 사용하세요.")}}}
   })
  }catch(_:Exception){cleanup();status("PLEOS 기기 내 음성을 시작하지 못했어요. 텍스트 안내를 사용하세요.")}
 }
 private fun cleanup(){
  tokens.cancel();activeId=null
  timeout?.let{main.removeCallbacks(it)};timeout=null
  pending?.let{main.removeCallbacks(it)};pending=null
  val sdk=engine;engine=null;val events=listener;listener=null
  if(sdk!=null){if(events!=null)runCatching{sdk.removeEventListener(events)};runCatching{sdk.stop()};runCatching{sdk.release()}}
  runCatching{audio.abandonAudioFocusRequest(focus)}
 }
 fun stop()=onMain{cleanup()}
 fun close()=onMain{disposed=true;cleanup()}
}
