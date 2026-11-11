package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class FamilyPlanningTest {
 private class Store(var state:FamilySnapshot=FamilySnapshot()):FamilyRepository {
  override fun loadFamily()=state
  override fun saveFamily(snapshot:FamilySnapshot){state=snapshot}
 }
 @Test fun legacySnapshotKeepsMemoryAndMigratesRestSelection() {
  val raw="""{"preferences":{"car":"가족 차"},"plans":{"coast":{"rest":30,"walk":10,"facility":"simgok"}},"memories":[{"id":"old","route":"coast","car":"우리 차","rating":4,"note":"오래된 추억","time":1}]}"""
  val s=decodeFamilySnapshot(raw)
  assertEquals("가족 차",s.preferences.carName);assertNull(s.preferences.maxContinuousDrivingMinutes)
  assertEquals(listOf("simgok"),s.plans.getValue("coast").effectiveRestFacilityIds)
  assertEquals("오래된 추억",s.memories.single().note);assertNull(s.memories.single().tripDate)
  assertEquals(s,decodeFamilySnapshot(encodeFamilySnapshot(s)))
 }
 @Test fun fullSnapshotRoundTrips() {
  val p=FamilyPlan(outboundMinutes=0,returnMinutes=0,departureMinuteOfDay=1380,returnDeadlineMinuteOfDay=60,returnNextDay=true,bufferMinutes=15)
  val s=FamilySnapshot(FamilyPreferences(maxContinuousDrivingMinutes=60,feedback=FamilyFeedback(true,true,true)),mapOf("coast" to p),listOf(FamilyMemory("x","coast","차",5,"",1,tripDate="2026-11-11",feelingTags=listOf("편안했어요"))))
  assertEquals(s,decodeFamilySnapshot(encodeFamilySnapshot(s)))
 }
 @Test fun overnightRequiresExplicitChoiceAndAtMostOneDay() {
  val p=FamilyPlan(departureMinuteOfDay=1380,returnDeadlineMinuteOfDay=60)
  assertFalse(p.valid());assertTrue(p.copy(returnNextDay=true).valid());assertEquals(120,p.copy(returnNextDay=true).scheduleWindowMinutes)
  assertFalse(p.copy(departureMinuteOfDay=30,returnNextDay=true).valid())
  assertFalse(FamilyPlan(departureMinuteOfDay=10).valid())
 }
 @Test fun bufferCountsInBudgetAndDeadline() {
  val p=FamilyPlan(restMinutes=0,walkMinutes=0,outboundMinutes=0,returnMinutes=0,bufferMinutes=30,departureMinuteOfDay=600,returnDeadlineMinuteOfDay=630)
  val m=rankFamilyRoutes(FamilySnapshot(plans=mapOf("coast" to p))).first{it.route.id=="coast"}
  assertEquals(40,m.totalMinutes);assertTrue(m.conflicts.any{it.contains("복귀")})
 }
 @Test fun explicitFeedbackCanBeClearedAndNotesNeverSetIt() {
  val e=FamilyEngine(Store());e.remember("x","coast",3,"운전 줄이기",1,Safety.PARKED)
  assertEquals(FamilyFeedback(),e.snapshot.preferences.feedback)
  assertTrue(e.preferences(e.snapshot.preferences.copy(feedback=FamilyFeedback(lessDriving=true)),Safety.PARKED))
  assertTrue(rankFamilyRoutes(e.snapshot).all{it.feedbackReasons.size==1})
  assertTrue(e.preferences(e.snapshot.preferences.copy(feedback=FamilyFeedback()),Safety.PARKED))
  assertTrue(rankFamilyRoutes(e.snapshot).all{it.feedbackReasons.isEmpty()})
 }
 @Test fun metadataEditingClearingAndValidation() {
  val e=FamilyEngine(Store());assertFalse(e.remember("x","coast",4,"",1,Safety.PARKED,tripDate="2026-02-30"))
  assertTrue(e.remember("x","coast",4,"",1,Safety.PARKED,tripDate="2026-11-11",feelingTags=listOf("즐거웠어요")))
  assertTrue(e.editMemory("x",5,"수정",Safety.PARKED));assertEquals("2026-11-11",e.snapshot.memories.single().tripDate)
  assertFalse(e.editMemory("x",5,"",Safety.MOVING,tripDate=null))
  assertTrue(e.editMemory("x",5,"",Safety.PARKED,tripDate=null,feelingTags=emptyList()));assertNull(e.snapshot.memories.single().tripDate)
 }
 @Test fun invalidNewMetadataDoesNotDiscardLegacyRow() {
  val m=FamilyMemory("x","coast","차",4,"기록",1,tripDate="bad",feelingTags=listOf("bad","즐거웠어요"))
  val s=FamilySnapshot(memories=listOf(m)).validated();assertEquals(1,s.memories.size);assertNull(s.memories.single().tripDate);assertEquals(listOf("즐거웠어요"),s.memories.single().feelingTags)
 }
 @Test fun restDurationsAreExplicitAndCountedOnce() {
  val p=FamilyPlan(restMinutes=60,restFacilityIds=listOf("simgok"),restMinutesByFacility=mapOf("simgok" to 10))
  assertTrue(p.valid());assertEquals(10,p.effectiveRestMinutes)
  assertFalse(p.copy(restMinutesByFacility=mapOf("wrong" to 10)).valid())
  assertFalse(p.copy(restFacilityIds=listOf("simgok","simgok")).valid())
 }
 @Test fun missingLegDurationsNeverProveContinuousLimit() {
  val m=rankFamilyRoutes(FamilySnapshot(FamilyPreferences(maxContinuousDrivingMinutes=30),mapOf("coast" to FamilyPlan(outboundMinutes=30,returnMinutes=30)))).first{it.route.id=="coast"}
  assertTrue(m.unknowns.any{it.contains("연속")});assertFalse(m.matches)
 }
 @Test fun onlyExplicitPositiveRestBreaksContinuousDriving() {
  val ids=listOf("simgok");val positions=mapOf("simgok" to 0)
  val v=EtaEstimate("coast","공개",600.0,1800.0,600.0,1000.0,1,
   itineraryKey=itineraryIdentity("coast",ids,positions),legSeconds=List(5){600.0},
   restBreakAfterLegs=RestItinerary.breakAfterLegs("coast",ids,positions),selectedRestIds=ids,restAfterStopIndex=positions)
  val base=FamilyPlan(restFacilityIds=ids,restAfterStopIndex=positions)
  assertTrue(v.valid());assertNull(withEta(base,v).longestContinuousDrivingMinutes)
  assertEquals(50,withEta(base.copy(restMinutesByFacility=mapOf("simgok" to 0)),v).longestContinuousDrivingMinutes)
  assertEquals(30,withEta(base.copy(restMinutesByFacility=mapOf("simgok" to 15)),v).longestContinuousDrivingMinutes)
 }
 @Test fun durationEditKeepsRoadTimeButPositionEditClearsIt() {
  val p=FamilyPlan(outboundMinutes=20,returnMinutes=30,courseDriveMinutes=40,restFacilityIds=listOf("simgok"),restMinutesByFacility=mapOf("simgok" to 10))
  val e=FamilyEngine(Store(FamilySnapshot(plans=mapOf("coast" to p))))
  assertTrue(e.plan("coast",p.copy(restMinutesByFacility=mapOf("simgok" to 20)),Safety.PARKED))
  assertEquals(40,e.snapshot.plans.getValue("coast").courseDriveMinutes)
  assertTrue(e.plan("coast",p.copy(restAfterStopIndex=mapOf("simgok" to 0)),Safety.PARKED))
  assertNull(e.snapshot.plans.getValue("coast").outboundMinutes);assertNull(e.snapshot.plans.getValue("coast").courseDriveMinutes)
 }
 @Test fun zeroingRestClearsExistingContinuousEvidenceButKeepsActualRoadTotals() {
  val ids=listOf("simgok");val positions=mapOf("simgok" to 0)
  val eta=EtaEstimate("coast","공개",600.0,1800.0,600.0,1000.0,1,
   itineraryKey=itineraryIdentity("coast",ids,positions),legSeconds=List(5){600.0},
   restBreakAfterLegs=RestItinerary.breakAfterLegs("coast",ids,positions),selectedRestIds=ids,restAfterStopIndex=positions)
  val plan=withEta(FamilyPlan(restFacilityIds=ids,restAfterStopIndex=positions,restMinutesByFacility=mapOf("simgok" to 15)),eta)
  val engine=FamilyEngine(Store(FamilySnapshot(FamilyPreferences(maxContinuousDrivingMinutes=30),mapOf("coast" to plan))))
  assertEquals(30,engine.snapshot.plans.getValue("coast").longestContinuousDrivingMinutes)
  assertTrue(engine.plan("coast",plan.copy(restMinutesByFacility=mapOf("simgok" to 0)),Safety.PARKED))
  val edited=engine.snapshot.plans.getValue("coast")
  assertNull(edited.longestContinuousDrivingMinutes)
  assertEquals(plan.outboundMinutes,edited.outboundMinutes)
  assertEquals(plan.courseDriveMinutes,edited.courseDriveMinutes)
  assertEquals(plan.returnMinutes,edited.returnMinutes)
  assertEquals(plan.etaItineraryKey,edited.etaItineraryKey)
  assertEquals("OSRM",edited.etaSource)
  val match=rankFamilyRoutes(engine.snapshot).first { it.route.id=="coast" }
  assertFalse(match.matches);assertTrue(match.unknowns.any { it.contains("연속") })
  assertEquals(50,withEta(edited,eta).longestContinuousDrivingMinutes)
 }
}
