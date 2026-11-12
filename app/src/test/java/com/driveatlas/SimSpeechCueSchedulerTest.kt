package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class SimSpeechCueSchedulerTest {
 @Test fun conciseDirectionsKeepMeaningWithoutRoadNames(){assertEquals("완만하게 우회전",conciseSimManeuver("오른쪽으로 완만하게 · 어떤 길"));assertEquals("완만하게 좌회전",conciseSimManeuver("왼쪽으로 완만하게"));assertEquals("급한 우회전",conciseSimManeuver("오른쪽으로 크게"));assertEquals("합류. 우회전",conciseSimManeuver("합류 구간 · 우회전 · 도로명"))}
 @Test fun unknownRoadNameNeverCreatesTurn(){assertEquals("지도 경로 확인",conciseSimManeuver("미지원 지시 · 우회전"));assertEquals("갈림길. 지도 확인",conciseSimManeuver("갈림길 · 경로 확인 · 좌회전"));assertEquals("회전교차로. 지도 확인",conciseSimManeuver("회전교차로 · 지도에서 경로 확인"))}
 @Test fun stoppingCaveatSurvivesShortening(){assertEquals("경유 지점 도착. 정차 여부 확인",conciseSimManeuver("경유 지점 도착 · 안전한 정차 여부는 별도 확인"))}
 @Test fun manualPreviewKeepsSimPrefixContinuousDoesNotRepeatIt(){assertEquals("시연 안내. 우회전",simSpeechSentence("우회전",false));assertEquals("약 200미터 앞. 완만하게 우회전",simSpeechSentence("약 200미터 앞. 완만하게 우회전",true));assertEquals("좌회전",simSpeechSentence("좌\n회전",true))}

 @Test fun nearTurnDoesNotInventHundredMeters(){val s=SimSpeechCueScheduler();assertEquals("잠시 후. 좌회전",s.tick(1,10.0,"좌회전 · 긴 도로명",0,false,true).text);s.reset();assertEquals("약 60미터 앞. 우회전",s.tick(1,64.0,"우회전",0,false,true).text)}
 @Test fun noDistantOrInvalidSpeech(){val s=SimSpeechCueScheduler();assertNull(s.tick(1,900.0,"좌회전",0,false,true).text);assertNull(s.tick(1,Double.NaN,"좌회전",0,false,true).text);assertNull(s.tick(1,100.0,"좌회전",0,false,false).text)}
 @Test fun initializationBudgetIsNotPlaybackBudget(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertFalse(s.tick(1,600.0,"좌회전",4000,true,true).cancel);assertFalse(s.tick(1,600.0,"좌회전",14999,true,true).cancel);assertTrue(s.tick(1,600.0,"좌회전",15000,true,true).cancel)}
 @Test fun lateStartGetsBoundedPlaybackWindow(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertFalse(s.tick(1,600.0,"좌회전",8000,true,true,6000).cancel);assertTrue(s.tick(1,600.0,"좌회전",18000,true,true,6000).cancel)}
 @Test fun changedStepCancelsEvenDuringInitialization(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertTrue(s.tick(2,600.0,"우회전",30,true,true).cancel)}
 @Test fun changedStepCancelsBeforeExtendedSpeechDeadline(){val s=SimSpeechCueScheduler();s.tick(1,200.0,"완만하게 우회전",0,false,true);val d=s.tick(2,100.0,"좌회전",100,true,true,0);assertTrue(d.cancel);assertNull(d.text)}
 @Test fun sameBandNeverRepeats(){val s=SimSpeechCueScheduler();assertNotNull(s.tick(1,700.0,"좌회전",0,false,true).text);assertNull(s.tick(1,600.0,"좌회전",9000,false,true).text)}
 @Test fun staleStepCancelledWithoutLateBacklog(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);val d=s.tick(2,700.0,"우회전",1000,true,true);assertTrue(d.cancel);assertNull(d.text);assertNotNull(s.tick(3,700.0,"직진",8000,false,true).text)}
 @Test fun validUtteranceCanFinishAfterSixSecondsButIsStillBounded(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertFalse(s.tick(1,600.0,"좌회전",6000,true,true,0).cancel);assertFalse(s.tick(1,550.0,"좌회전",11999,true,true,0).cancel);assertTrue(s.tick(1,500.0,"좌회전",12000,true,true,0).cancel)}
 @Test fun nearerBandAllowedOnlyAfterCooldown(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertNull(s.tick(1,100.0,"좌회전",1000,false,true).text);assertNotNull(s.tick(1,100.0,"좌회전",8000,false,true).text)}
 @Test fun disableCancelsAndResetStartsNewSession(){val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true);assertTrue(s.tick(1,600.0,"좌회전",100,true,false).cancel);assertNotNull(s.tick(1,600.0,"좌회전",200,false,true).text)}
 @Test fun orderedIndexDisambiguatesSameInstructionOnReturn(){val s=SimSpeechCueScheduler();s.tick(1,600.0,"좌회전",0,false,true);assertNotNull(s.tick(21,600.0,"좌회전",9000,false,true).text)}
 @Test fun completedUtteranceDoesNotReportCancellationAtNextStep(){
  val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true)
  val completed=s.tick(2,600.0,"우회전",4000,false,true)
  assertFalse(completed.cancel);assertNull(completed.text)
  assertNotNull(s.tick(2,500.0,"우회전",8000,false,true).text)
 }
 @Test fun completedUtteranceDoesNotTimeoutOrRepeatItsBand(){
  val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true)
  val completed=s.tick(1,600.0,"좌회전",16000,false,true,0)
  assertFalse(completed.cancel);assertNull(completed.text)
  assertNull(s.tick(1,500.0,"좌회전",24000,false,true).text)
 }
 @Test fun completionKeepsCooldownAndAllowsANewerApproachBand(){
  val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true)
  assertNull(s.tick(1,180.0,"좌회전",7999,false,true).text)
  val nearer=s.tick(1,170.0,"좌회전",8000,false,true)
  assertFalse(nearer.cancel);assertNotNull(nearer.text)
 }
 @Test fun disablingAfterCompletionDoesNotCancelAnIdleEngine(){
  val s=SimSpeechCueScheduler();s.tick(1,700.0,"좌회전",0,false,true)
  assertFalse(s.tick(1,600.0,"좌회전",4000,false,false).cancel)
 }

}
