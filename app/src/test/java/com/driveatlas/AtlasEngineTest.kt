package com.driveatlas
import org.junit.Assert.*
import org.junit.Test

class AtlasEngineTest {
 class MemoryStore : AtlasStore { var state=Snapshot();override fun load()=state;override fun save(snapshot: Snapshot){state=snapshot} }
 private val route=Catalog.routes.first()
 private fun ready(s: MemoryStore=MemoryStore()): AtlasEngine = AtlasEngine(s,Mode.SIMULATOR).apply{setSafety(Safety.PARKED);setLocation(LocationStatus.AVAILABLE);assertTrue(start(route.id,1000))}
 private fun ev(i:Int,time:Long=1100L+i*100)=Evidence(route.stops[i].point,5.0,time,true)
 @Test fun unknownAndMovingFailClosed(){val e=AtlasEngine(MemoryStore(),Mode.SIMULATOR);assertFalse(e.start(route.id,1));e.setSafety(Safety.MOVING);assertFalse(e.start(route.id,1));assertFalse(e.canEdit)}
 @Test fun outOfOrderEvidenceRejected(){val e=ready();assertFalse(e.accept(ev(2),1300));assertEquals(0,e.snapshot.trip!!.nextStop);assertTrue(e.accept(ev(0),1100));assertFalse(e.accept(ev(0),1200));assertFalse(e.accept(ev(2),1300))}
 @Test fun invalidAccuracyStaleFutureReplayAndWrongSourceRejected(){val e=ready();assertFalse(e.accept(ev(0).copy(accuracyM=100.0),1100));assertFalse(e.accept(ev(0).copy(accuracyM=Double.NaN),1100));assertFalse(e.accept(ev(0),20000));assertFalse(e.accept(ev(0),1000));assertFalse(e.accept(ev(0).copy(simulated=false),1100));assertTrue(e.accept(ev(0),1100));assertFalse(e.accept(ev(1,1100),1200));assertFalse(e.accept(ev(1).copy(point=Point(Double.NaN,0.0)),1200))}
 @Test fun deniedAndLostLocationBlockEvidenceAndAward(){val e=ready();for(s in listOf(LocationStatus.DENIED,LocationStatus.LOST)){e.setLocation(s);assertFalse(e.accept(ev(0),1100));assertFalse(e.finish(1100))}}
 @Test fun incompleteTripCannotAward(){val e=ready();assertTrue(e.accept(ev(0),1100));assertFalse(e.finish(1200));assertTrue(e.snapshot.awards.isEmpty())}
 @Test fun completionOnceAndSurvivesRestart(){val store=MemoryStore();val e=ready(store);repeat(3){assertTrue(e.accept(ev(it),1300))};assertTrue(e.finish(1400));assertFalse(e.finish(1400));val restored=AtlasEngine(store,Mode.SIMULATOR);assertEquals(1,restored.snapshot.awards.size);assertTrue(restored.snapshot.awards.first().simulated);assertFalse(restored.canEdit);assertEquals(LocationStatus.LOST,restored.location)}
 @Test fun tripRestartNeverRestoresParkedOrLocation(){val s=MemoryStore();val e=ready(s);e.accept(ev(0),1100);val re=AtlasEngine(s,Mode.SIMULATOR);assertEquals(1,re.snapshot.trip!!.nextStop);assertFalse(re.canEdit);assertFalse(re.accept(ev(1),1200));assertFalse(re.finish(1200))}
 @Test fun staleArrivalAndMovingCannotComplete(){val e=ready();repeat(3){e.accept(ev(it),1300)};assertFalse(e.finish(20000));e.setSafety(Safety.MOVING);assertFalse(e.finish(1400));e.setSafety(Safety.UNKNOWN);assertFalse(e.finish(1400))}
 @Test fun arrivalCanBeRefreshedAfterTimeout(){val e=ready();repeat(3){e.accept(ev(it),1300)};assertFalse(e.finish(20000));assertTrue(e.accept(ev(2,20100),20100));assertEquals(3,e.snapshot.trip!!.nextStop);assertTrue(e.finish(20200))}
 @Test fun simulatedTripCannotBeAwardedByVehicleAfterRestart(){val s=MemoryStore();val sim=ready(s);repeat(3){sim.accept(ev(it),1300)};val real=AtlasEngine(s,Mode.VEHICLE);real.setSafety(Safety.PARKED);real.setLocation(LocationStatus.AVAILABLE);assertFalse(real.finish(1400));assertFalse(real.accept(ev(2,1400).copy(simulated=false),1400));assertTrue(real.snapshot.awards.isEmpty())}
 @Test fun vehicleRejectsSimulatorEvidence(){val e=AtlasEngine(MemoryStore(),Mode.VEHICLE);e.setSafety(Safety.PARKED);e.setLocation(LocationStatus.AVAILABLE);e.start(route.id,1000);assertFalse(e.accept(ev(0),1100))}
 @Test fun abandonDoesNotAwardAndRepeatedStartRejected(){val e=ready();assertFalse(e.start(route.id,1100));assertTrue(e.abandon());assertTrue(e.snapshot.awards.isEmpty());assertNull(e.snapshot.trip)}
 @Test fun persistenceFailureNeverMutatesInMemory(){val store=object:AtlasStore{override fun load()=Snapshot();override fun save(snapshot:Snapshot){throw IllegalStateException("disk")}};val e=AtlasEngine(store,Mode.SIMULATOR);e.setSafety(Safety.PARKED);try{e.start(route.id,1);fail()}catch(_:IllegalStateException){};assertNull(e.snapshot.trip)}
}
