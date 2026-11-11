package com.driveatlas

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray

class RoundTripEtaTest {
 private val ref=DepartureReferences.all.first()
 private val request=EtaRequest("coast",ref.point,ref.label,ref.id)
 private fun response():JSONObject {
  val durations=listOf(1800.0,120.0,180.0,2400.0)
  return JSONObject().put("code","Ok").put("routes",JSONArray().put(JSONObject().put("duration",durations.sum()).put("distance",50000.0).put("legs",JSONArray().apply{durations.forEach{put(JSONObject().put("duration",it).put("distance",12500.0))}})))
   .put("waypoints",JSONArray().apply{request.points().forEach{put(JSONObject().put("location",JSONArray().put(it.lon).put(it.lat)).put("distance",0.0))}})
 }
 private fun fails(body:JSONObject,kind:String){try{parseEta(request,body.toString(),1000);fail("Expected failure")}catch(e:EtaUnavailable){assertEquals(kind,e.kind)}}
 @Test fun outboundAndReturnUseIndependentRoadLegs(){val v=parseEta(request,response().toString(),1000);assertEquals(30,v.outboundMinutes);assertEquals(40,v.returnMinutes);assertEquals(5,v.courseMinutes);assertEquals(75,v.roadMinutes);assertFalse(v.cached)}
 @Test fun requestContainsFullOrderedLoopAndLongitudeFirst(){val points=request.points();assertEquals(5,points.size);assertEquals(points.first(),points.last());assertEquals(Catalog.routes.first{it.id=="coast"}.stops.map{it.point},points.drop(1).dropLast(1));val url=etaUrl(request);assertTrue(url.contains("126.978420,37.566690"));assertTrue(url.contains("continue_straight=false"));assertTrue(url.startsWith("https://router.project-osrm.org/"))}
 @Test fun invalidOriginNeverCreatesUrl(){assertFalse(request.copy(origin=Point(91.0,0.0),publicReferenceId=null).valid());try{etaUrl(request.copy(routeId="missing"));fail()}catch(_:IllegalArgumentException){}}
 @Test fun publicIdCannotDisguisePrivateCoordinates(){val forged=request.copy(origin=Point(37.0,127.0));assertFalse(forged.valid());assertNull(publicEtaCacheKey(forged));assertNull(publicEtaCacheKey(request.copy(publicReferenceId=null)));assertTrue(publicEtaCacheKey(request)!!.startsWith("v2:seoul:"));assertNotEquals("seoul:coast",publicEtaCacheKey(request))}
 @Test fun absentRouteIsNotStraightLineEstimate(){fails(JSONObject().put("code","NoRoute"),"NoRoute")}
 @Test fun absentRoadSegmentIsUnavailable(){fails(JSONObject().put("code","NoSegment"),"NoSegment")}
 @Test fun wrongLegCountRejected(){val o=response();o.getJSONArray("routes").getJSONObject(0).put("legs",JSONArray().put(JSONObject().put("duration",4500)));fails(o,"InvalidResponse")}
 @Test fun snapOutsideBoundRejected(){val o=response();o.getJSONArray("waypoints").getJSONObject(0).put("distance",501);fails(o,"NoSegment")}
 @Test fun wrongSnappedCoordinateRejected(){val o=response();o.getJSONArray("waypoints").getJSONObject(0).put("location",JSONArray().put(0).put(0));fails(o,"NoSegment")}
 @Test fun malformedResponseRejected(){try{parseEta(request,"{bad",1000);fail()}catch(e:EtaUnavailable){assertEquals("InvalidResponse",e.kind)}}
 @Test fun negativeAndNonFiniteDurationsRejected(){for(v in listOf(-1,"NaN")){val o=response();o.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).put("duration",v);fails(o,"InvalidResponse")}}
 @Test fun inconsistentTotalRejected(){val o=response();o.getJSONArray("routes").getJSONObject(0).put("duration",9000);fails(o,"InvalidResponse")}
 @Test fun negativeTotalEvenWithZeroLegsRejected(){val o=response();val r=o.getJSONArray("routes").getJSONObject(0);r.put("duration",-1);for(i in 0..3)r.getJSONArray("legs").getJSONObject(i).put("duration",0);fails(o,"InvalidResponse")}
 @Test fun missingNegativeOrNonFiniteLegDistanceRejected(){for(v in listOf(-1,"NaN",JSONObject.NULL)){val o=response();o.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).put("distance",v);fails(o,"InvalidResponse")}}
 @Test fun inconsistentDistanceRejected(){val o=response();o.getJSONArray("routes").getJSONObject(0).put("distance",50020);fails(o,"InvalidResponse")}
 @Test fun staleCacheAndClockRollbackRejected(){val v=parseEta(request,response().toString(),1000);assertTrue(freshCachedEta(v,1000+86400000));assertFalse(freshCachedEta(v,1001+86400000));assertFalse(freshCachedEta(v,999))}
 @Test fun applyingEtaPreservesRestAndVenue(){val r=request.copy(selectedRestIds=listOf("simgok"),allowApproximateRestPoints=true);val v=EtaEstimate("coast",ref.label,1800.0,240.0,2460.0,50000.0,1000,itineraryKey=r.itineraryKey(),legSeconds=listOf(1800.0,120.0,120.0,60.0,2400.0),restBreakAfterLegs=listOf(3),selectedRestIds=listOf("simgok"));val p=withEta(FamilyPlan(90,10,restFacilityId="simgok"),v);assertEquals(90,p.restMinutes);assertEquals("simgok",p.restFacilityId);assertEquals(30,p.outboundMinutes);assertEquals(41,p.returnMinutes);assertEquals(4,p.courseDriveMinutes);assertEquals("OSRM",p.etaSource)}
 @Test fun manualOverrideClearsAutomaticProvenance(){val p=withEta(FamilyPlan(),parseEta(request,response().toString(),1000));val m=manualTransfers(p,outbound=60);assertEquals(60,m.outboundMinutes);assertEquals(40,m.returnMinutes);assertNull(m.etaSource);assertNull(m.courseDriveMinutes)}
 @Test fun rankingUsesCalculatedCourseTime(){val p=withEta(FamilyPlan(),parseEta(request,response().toString(),1000));val m=rankFamilyRoutes(FamilySnapshot(plans=mapOf("coast" to p))).first{it.route.id=="coast"};assertEquals(155,m.totalMinutes)}
 @Test fun durationsAreRoundedUpPerSection(){val v=EtaEstimate("coast","공개",61.0,1.0,119.0,1000.0,1);assertEquals(2,v.outboundMinutes);assertEquals(1,v.courseMinutes);assertEquals(2,v.returnMinutes);assertEquals(5,v.roadMinutes)}
 @Test fun errorsAreActionableWithoutLeakingRequestUrl(){listOf("NoRoute","NoSegment","RateLimit","Timeout","Network","InvalidResponse","ProductionProviderRequired").forEach{assertFalse(etaErrorMessage(it).isBlank());assertFalse(etaErrorMessage(it).contains("latitude"))}}
}
