package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class DemoFlowTest {
 private class Store:AtlasStore { var state=Snapshot();override fun load()=state;override fun save(snapshot:Snapshot){state=snapshot} }
 private fun ready(store:Store=Store())=AtlasEngine(store,Mode.SIMULATOR).apply{setSafety(Safety.PARKED);setLocation(LocationStatus.AVAILABLE);start("coast",1000)}
 @Test fun archivePreservesProgressWithoutStamp(){val e=ready();e.accept(Evidence(Catalog.routes[0].stops[0].point,5.0,1100,true),1100);assertTrue(e.archive(VisitStatus.UNCONFIRMED,1200));assertNull(e.snapshot.trip);assertEquals(1,e.snapshot.visits.single().progress);assertTrue(e.snapshot.awards.isEmpty())}
 @Test fun lockedCannotArchive(){val e=ready();e.setSafety(Safety.MOVING);assertFalse(e.archive(VisitStatus.INTERRUPTED,1200));assertNotNull(e.snapshot.trip)}
 @Test fun archiveSurvivesRestartAndRemainsSimulated(){val s=Store();val e=ready(s);e.archive(VisitStatus.INTERRUPTED,1200);val re=AtlasEngine(s,Mode.SIMULATOR);assertTrue(re.snapshot.visits.single().simulated);assertFalse(re.canEdit);assertTrue(re.snapshot.awards.isEmpty())}
 @Test fun finishedTripKeepsPriorUnconfirmedRecord(){val e=ready();e.archive(VisitStatus.UNCONFIRMED,1200);e.start("coast",2000);repeat(3){i->val t=2100L+i*100;e.accept(Evidence(Catalog.routes[0].stops[i].point,5.0,t,true),t)};assertTrue(e.finish(2400));assertEquals(1,e.snapshot.visits.size);assertEquals(1,e.snapshot.awards.size)}
 @Test fun invalidVisitsAreNotRestored(){val s=Snapshot(visits=listOf(Visit("bad","nope",true,1,2,VisitStatus.INTERRUPTED),Visit("good","coast",true,1,2,VisitStatus.INTERRUPTED)));assertEquals(listOf("good"),s.validatedForRestore().visits.map{it.tripId})}
 @Test fun seasonalAndMoodFiltering(){assertEquals(listOf("lake"),filteredRoutes("season","겨울","전체","전체",false).map{it.id});assertEquals(listOf("forest"),filteredRoutes("theme","가을","초록 충전","전체",false).map{it.id});assertTrue(filteredRoutes("recommended","가을","전체","다른 지역",false).isEmpty())}
 @Test fun regionFilteringUsesRealPlaces(){assertEquals(listOf("forest"),filteredRoutes("recommended","가을","전체","담양",false).map{it.id});assertEquals(listOf("coast"),filteredRoutes("recommended","가을","전체","강릉",false).map{it.id})}
}
