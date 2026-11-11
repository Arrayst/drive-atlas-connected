package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class FamilyEngineTest {
 private class Store(var state: FamilySnapshot=FamilySnapshot()):FamilyRepository {
  var fail=false
  override fun loadFamily()=state
  override fun saveFamily(snapshot:FamilySnapshot){if(fail)error("disk full");state=snapshot}
 }
 @Test fun preferenceChangesRequireAuthoritativePark(){val e=FamilyEngine(Store());for(s in listOf(Safety.UNKNOWN,Safety.MOVING))assertFalse(e.preferences(FamilyPreferences(carName="다른 차"),s));assertEquals("우리 차",e.snapshot.preferences.carName);assertTrue(e.preferences(FamilyPreferences(carName="다른 차"),Safety.PARKED))}
 @Test fun validatesInputs(){val e=FamilyEngine(Store());assertFalse(e.preferences(FamilyPreferences(carName=""),Safety.PARKED));assertFalse(e.preferences(FamilyPreferences(availableMinutes=-1),Safety.PARKED));assertFalse(e.plan("unknown",FamilyPlan(),Safety.PARKED));assertFalse(e.plan("coast",FamilyPlan(restMinutes=-1),Safety.PARKED));assertFalse(e.remember("x","coast",6,"",1,Safety.PARKED));assertFalse(e.remember("x","coast",4,"a".repeat(281),1,Safety.PARKED))}
 @Test fun unknownFacilitiesCannotMatch(){val s=FamilySnapshot(preferences=FamilyPreferences(restroomRequired=true,strollerRequired=true));val r=rankFamilyRoutes(s);assertTrue(r.all{!it.matches});assertTrue(r.all{it.unknowns.size==4})}
 @Test fun totalIncludesRestAndWalking(){val s=FamilySnapshot(plans=mapOf("coast" to FamilyPlan(90,40)));val r=rankFamilyRoutes(s).first{it.route.id=="coast"};assertEquals(140,r.totalMinutes);assertTrue(r.conflicts.contains("계획한 걷기 시간이 가족 조건을 넘어요"))}
 @Test fun tightBudgetShowsNoMatch(){val r=rankFamilyRoutes(FamilySnapshot(preferences=FamilyPreferences(availableMinutes=30)));assertTrue(r.none{it.matches});assertTrue(r.all{it.conflicts.isNotEmpty()})}
 @Test fun repeatedSaveIsIdempotent(){val e=FamilyEngine(Store());assertTrue(e.remember("x","coast",5,"좋아요",1,Safety.PARKED));assertFalse(e.remember("x","coast",5,"좋아요",1,Safety.PARKED));assertEquals(1,e.snapshot.memories.size)}
 @Test fun memoryLocksWhileDriving(){val e=FamilyEngine(Store());assertFalse(e.remember("x","coast",4,"",1,Safety.MOVING));assertFalse(e.remember("x","coast",4,"",1,Safety.UNKNOWN));assertTrue(e.snapshot.memories.isEmpty())}
 @Test fun feedbackChangesNextRanking(){val e=FamilyEngine(Store());assertEquals("forest",rankFamilyRoutes(e.snapshot).first().route.id);e.remember("x","coast",5,"",1,Safety.PARKED);assertEquals("coast",rankFamilyRoutes(e.snapshot).first().route.id)}
 @Test fun simulatedFeedbackNeverChangesRealRanking(){val e=FamilyEngine(Store());e.remember("x","coast",5,"",1,Safety.PARKED,simulated=true);assertEquals("forest",rankFamilyRoutes(e.snapshot).first().route.id);assertNull(rankFamilyRoutes(e.snapshot).first{it.route.id=="coast"}.previousRating)}
 @Test fun newCarDoesNotRewriteOldMemory(){val e=FamilyEngine(Store());e.remember("x","coast",4,"",1,Safety.PARKED);e.preferences(FamilyPreferences(carName="새 차"),Safety.PARKED);assertEquals("우리 차",e.snapshot.memories.single().carName)}
 @Test fun failedStoreKeepsState(){val store=Store();val e=FamilyEngine(store);store.fail=true;try{e.preferences(FamilyPreferences(carName="새 차"),Safety.PARKED);fail()}catch(_:IllegalStateException){};assertEquals("우리 차",e.snapshot.preferences.carName)}
 @Test fun restoreRejectsInvalidRowsAndDeduplicates(){val good=FamilyMemory("x","coast","우리 차",4,"",1);val e=FamilyEngine(Store(FamilySnapshot(memories=listOf(good,good,good.copy(id="y",routeId="bad")),preferences=FamilyPreferences(carName=""))));assertEquals(1,e.snapshot.memories.size);assertEquals(FamilyPreferences(),e.snapshot.preferences)}
 @Test fun restoreAndRankSurviveRestart(){val s=Store();val first=FamilyEngine(s);first.preferences(FamilyPreferences(carName="가족 차"),Safety.PARKED);first.remember("x","lake",5,"호수",2,Safety.PARKED);val next=FamilyEngine(s);assertEquals("가족 차",next.snapshot.preferences.carName);assertEquals("lake",rankFamilyRoutes(next.snapshot).first().route.id)}
 @Test fun missingTransferNeverConfirmsMatch(){assertTrue(rankFamilyRoutes(FamilySnapshot()).all{!it.matches&&it.unknowns.size==2})}
 @Test fun roundTripIncludedInBudget(){val s=FamilySnapshot(preferences=FamilyPreferences(availableMinutes=180),plans=mapOf("coast" to FamilyPlan(60,20,90,90)));val m=rankFamilyRoutes(s).first{it.route.id=="coast"};assertEquals(270,m.totalMinutes);assertFalse(m.matches);assertTrue(m.conflicts.any{it.contains("왕복")})}
 @Test fun explicitZeroTransfersAreKnown(){val m=rankFamilyRoutes(FamilySnapshot(plans=mapOf("coast" to FamilyPlan(60,20,0,0)))).first{it.route.id=="coast"};assertTrue(m.matches);assertEquals(90,m.totalMinutes)}
 @Test fun verifiedVenuePassesOnlySelectedVenue(){val p=FamilyPreferences(restroomRequired=true,strollerRequired=true);val m=rankFamilyRoutes(FamilySnapshot(p,mapOf("forest" to FamilyPlan(30,10,30,30,"gwanbang")))).first{it.route.id=="forest"};assertTrue(m.matches);val other=rankFamilyRoutes(FamilySnapshot(p)).first{it.route.id=="forest"};assertFalse(other.matches)}
 @Test fun restroomDoesNotImplyStroller(){val p=FamilyPreferences(restroomRequired=true,strollerRequired=true);val m=rankFamilyRoutes(FamilySnapshot(p,mapOf("coast" to FamilyPlan(30,10,0,0,"simgok")))).first{it.route.id=="coast"};assertFalse(m.matches);assertEquals(listOf("선택 휴식지 유모차 접근성 확인 필요"),m.unknowns)}
 @Test fun refusesCrossRouteFacilityAndInvalidTransfer(){val e=FamilyEngine(Store());assertFalse(e.plan("coast",FamilyPlan(restFacilityId="gwanbang"),Safety.PARKED));assertFalse(e.plan("coast",FamilyPlan(outboundMinutes=-1),Safety.PARKED));assertFalse(e.plan("coast",FamilyPlan(returnMinutes=721),Safety.PARKED))}
 @Test fun editPreservesIdentityAndChangesRanking(){val e=FamilyEngine(Store());e.remember("x","coast",1,"old",10,Safety.PARKED);assertTrue(e.editMemory("x",5,"new",Safety.PARKED));assertEquals(10L,e.snapshot.memories.single().createdMs);assertEquals("new",e.snapshot.memories.single().note);assertEquals("coast",rankFamilyRoutes(e.snapshot).first().route.id);assertFalse(e.editMemory("x",2,"",Safety.MOVING))}
 @Test fun memoryDeletionIsScopedAndParked(){val e=FamilyEngine(Store());e.remember("x","coast",4,"",1,Safety.PARKED);e.remember("y","lake",5,"",2,Safety.PARKED);assertFalse(e.deleteMemory("x",Safety.UNKNOWN));assertTrue(e.deleteMemory("x",Safety.PARKED));assertEquals(listOf("y"),e.snapshot.memories.map{it.id});assertFalse(e.deleteMemory("x",Safety.PARKED))}
 @Test fun failedDeleteAndEditDoNotMutate(){val s=Store();val e=FamilyEngine(s);e.remember("x","coast",4,"old",1,Safety.PARKED);s.fail=true;try{e.deleteMemory("x",Safety.PARKED);fail()}catch(_:IllegalStateException){};try{e.editMemory("x",1,"new",Safety.PARKED);fail()}catch(_:IllegalStateException){};assertEquals(1,e.snapshot.memories.size);assertEquals("old",e.snapshot.memories.single().note)}
 @Test fun curatedSourcesArePublicHttpsAndScoped(){assertEquals(3,RestFacilities.all.size);RestFacilities.all.forEach{f->assertTrue(f.source.startsWith("https://"));assertEquals(f,RestFacilities.find(f.id,f.routeId));assertNull(RestFacilities.find(f.id,"wrong"));assertTrue(f.hours.isNotBlank());assertEquals("2026-11-11",f.checkedAt)}}

 @Test fun capacityNeverSilentlyDiscardsOldMemories(){val memories=(0..500).map{FamilyMemory("m$it","coast","우리 차",4,"",it+1L)};val e=FamilyEngine(Store(FamilySnapshot(memories=memories)));assertEquals(501,e.snapshot.memories.size);assertFalse(e.remember("new","lake",5,"",999,Safety.PARKED));assertEquals("m0",e.snapshot.memories.first().id);assertEquals(501,e.snapshot.memories.size)}

}
