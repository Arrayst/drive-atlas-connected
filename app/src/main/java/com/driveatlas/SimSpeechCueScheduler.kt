package com.driveatlas

/** No timers or platform side effects. Caller supplies monotonic time and ordered replay progress. */
internal data class SimSpeechDecision(val cancel:Boolean=false,val text:String?=null)
internal const val SimSpeechPlaybackTimeoutMillis = 12_000L
internal class SimSpeechCueScheduler {
 private var activeStep:Int?=null
 private var issuedAt:Long?=null
 private var lastIssuedAt:Long?=null
 private val spoken=mutableSetOf<Pair<Int,Int>>()
 fun reset(){activeStep=null;issuedAt=null;lastIssuedAt=null;spoken.clear()}
 fun tick(step:Int,distance:Double,instruction:String,now:Long,busy:Boolean,allowed:Boolean,startedAtMillis:Long?=null):SimSpeechDecision {
  // Completion releases the active request, not the spoken-band history or cooldown.
  // A later step/timeout must not report cancelling an utterance that already ended.
  if(!busy){activeStep=null;issuedAt=null}
  if(!allowed||step<0||!distance.isFinite()||distance<0||now<0){val cancel=activeStep!=null;reset();return SimSpeechDecision(cancel)}
  val age=issuedAt?.let{now-it}?:0L
  // Allow a valid local utterance beyond six seconds, but never a stale step or an unbounded request.
  val cancel=activeStep!=null&&(activeStep!=step||(if(startedAtMillis!=null)now-startedAtMillis>=SimSpeechPlaybackTimeoutMillis else age>=15000)||age<0)
  if(cancel){activeStep=null;issuedAt=null}
  if(busy&&!cancel)return SimSpeechDecision()
  // Replay stays at 10 m/s independently of speech. Never queue a backlog of turns.
  val band=when {distance<=200->0;distance<=800->1;else->return SimSpeechDecision(cancel)}
  val key=step to band
  if(key in spoken||instruction.isBlank()||lastIssuedAt?.let{now-it<8000}==true)return SimSpeechDecision(cancel)
  spoken+=key;activeStep=step;issuedAt=now;lastIssuedAt=now
  val rounded=(kotlin.math.round(distance/10)*10).toInt()
  val lead=if(distance<=30)"잠시 후" else "약 ${rounded}미터 앞"
  return SimSpeechDecision(cancel,"$lead. ${conciseSimManeuver(instruction)}")
 }
}

/** Only shorten known maneuver words. Never infer a direction from a road name. */
internal fun conciseSimManeuver(instruction:String):String {
 val parts=instruction.split(" · ")
 fun direction(value:String)=when(value.trim()) {
  "왼쪽으로 완만하게"->"완만하게 좌회전"
  "오른쪽으로 완만하게"->"완만하게 우회전"
  "왼쪽으로 크게"->"급한 좌회전"
  "오른쪽으로 크게"->"급한 우회전"
  "좌회전","우회전","직진","유턴"->value.trim()
  else->null
 }
 val first=parts.first().trim()
 direction(first)?.let{return it}
 val junction=when(first){"합류 구간"->"합류";"갈림길"->"갈림길";"진입 연결로"->"진입로";"진출 연결로"->"진출로";else->null}
 if(junction!=null)return "$junction. ${parts.getOrNull(1)?.let{direction(it)}?:"지도 확인"}"
 return when(first){
  "경유 지점 도착"->"경유 지점 도착. 정차 여부 확인"
  "출발 기준점 도착"->first
  "다음 구간 출발"->"다음 구간 출발"
  "회전교차로"->"회전교차로. 지도 확인"
  else->"지도 경로 확인"
 }
}
internal fun simSpeechSentence(text:String,continuousSim:Boolean):String =
 (if(continuousSim)"" else "시연 안내. ")+text.filter{!it.isISOControl()}.take(300)
