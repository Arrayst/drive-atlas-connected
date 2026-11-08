package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class LocalCourseCheckTest {
 @Test fun exactKnownPointHasZeroDistance() { val stop=Catalog.routes[0].stops[1]; val result=localCourseProximity(stop.point)!!;assertEquals(stop,result.stop);assertEquals(0.0,result.meters,.01) }
 @Test fun rejectsInvalidCoordinates() { listOf(Point(Double.NaN,0.0),Point(91.0,0.0),Point(0.0,181.0)).forEach { assertNull(localCourseProximity(it)) } }
 @Test fun remotePointIsNotSilentlyTreatedAsVisited() { assertTrue(localCourseProximity(Point(0.0,0.0))!!.meters>1_000_000) }
 @Test fun exactMatchStillSaysUnverifiedVisit() { assertTrue(localCourseSummary(Catalog.routes[0].stops[0].point).contains("방문은 미확인")) }
 @Test fun resultContainsNoNewAwardOrEvidence() { val store=object:AtlasStore{override fun load()=Snapshot();override fun save(snapshot:Snapshot){throw AssertionError("unexpected write")}};val engine=AtlasEngine(store,Mode.SIMULATOR);localCourseSummary(Catalog.routes[0].stops[0].point);assertTrue(engine.snapshot.awards.isEmpty()) }
}
