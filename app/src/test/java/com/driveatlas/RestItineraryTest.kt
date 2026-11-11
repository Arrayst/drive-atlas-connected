package com.driveatlas

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class RestItineraryTest {
 private val ref=DepartureReferences.all.first()
 private fun request(ids:List<String> = listOf("simgok","hourglass"),slots:Map<String,Int> = emptyMap())=EtaRequest("coast",ref.point,ref.label,ref.id,ids,true,slots)
 private fun body(r:EtaRequest,seconds:List<Double>):String=JSONObject().put("code","Ok").put("routes",JSONArray().put(JSONObject()
  .put("duration",seconds.sum()).put("distance",100.0*seconds.size).put("legs",JSONArray().apply{seconds.forEach{put(JSONObject().put("duration",it).put("distance",100.0))}})))
  .put("waypoints",JSONArray().apply{r.points().forEach{put(JSONObject().put("location",JSONArray().put(it.lon).put(it.lat)).put("distance",0.0))}}).toString()
 @Test fun allFacilitiesHaveExplicitPublicSourcesWithoutEntranceClaims(){RestFacilities.all.forEach{assertNotNull(it.point);assertTrue(it.coordinateSource!!.startsWith("https://"));assertTrue(it.coordinateNote.contains("미확인"))}}
 @Test fun unknownDuplicateCrossRouteAndBadSlotBlockWholeItinerary(){assertFalse(request(listOf("missing")).valid());assertFalse(request(listOf("simgok","simgok")).valid());assertFalse(request(listOf("animation")).valid());assertFalse(request(slots=mapOf("simgok" to 3)).valid());assertFalse(request(slots=mapOf("missing" to 0)).valid())}
 @Test fun approximateRepresentativeRequiresConsent(){assertFalse(request().copy(allowApproximateRestPoints=false).valid())}
 @Test fun explicitSlotsPreserveBaseAndRestArrivalOrder(){val r=request(slots=mapOf("simgok" to -1,"hourglass" to 0));val points=r.points();val base=Catalog.routes.first{it.id=="coast"}.stops;assertEquals(RestFacilities.find("simgok","coast")!!.point,points[1]);assertEquals(base[0].point,points[2]);assertEquals(RestFacilities.find("hourglass","coast")!!.point,points[3]);assertEquals(base[1].point,points[4]);assertEquals(listOf(0,2),RestItinerary.breakAfterLegs("coast",r.selectedRestIds,r.restAfterStopIndex))}
 @Test fun cacheIdentityChangesForOrderSlotsAndCoordinatesVersion(){val r=request();assertNotEquals(publicEtaCacheKey(r),publicEtaCacheKey(request(r.selectedRestIds.reversed())));assertNotEquals(publicEtaCacheKey(r),publicEtaCacheKey(request(slots=mapOf("simgok" to 0))));assertNotEquals(publicEtaCacheKey(r),publicEtaCacheKey(request(emptyList())));assertNull(publicEtaCacheKey(r.copy(publicReferenceId=null)))}
 @Test fun parsingAllLegsCountsPreCourseRestInOutbound(){val r=request(slots=mapOf("simgok" to -1,"hourglass" to 1));val v=parseEta(r,body(r,listOf(60.0,120.0,180.0,240.0,300.0,360.0)),1);assertEquals(3,v.outboundMinutes);assertEquals(12,v.courseMinutes);assertEquals(6,v.returnMinutes);assertEquals(listOf(0,3),v.restBreakAfterLegs);assertEquals(r.itineraryKey(),v.itineraryKey);assertTrue(v.valid());assertFalse(v.copy(outboundSeconds=v.outboundSeconds+60,courseSeconds=v.courseSeconds-60).valid());assertFalse(v.copy(itineraryKey="old").valid())}
 @Test fun shapingPointsNeverResetContinuousDriving(){assertEquals(90,longestDriveMinutes(listOf(1200.0,1800.0,2400.0,600.0),listOf(2)));assertEquals(100,longestDriveMinutes(listOf(1200.0,1800.0,2400.0,600.0),emptyList()));assertNull(longestDriveMinutes(emptyList(),emptyList()))}
 @Test fun postCourseRestLegsBelongToReturn(){val r=request();val v=parseEta(r,body(r,listOf(60.0,120.0,180.0,240.0,300.0,360.0)),1);assertEquals(1,v.outboundMinutes);assertEquals(5,v.courseMinutes);assertEquals(15,v.returnMinutes);assertTrue(v.valid())}
 @Test fun sdkCapacityDoesNotSilentlyDropRests(){assertFalse(RestItinerary.nativeFullRouteSupported(RestItinerary.route("coast",listOf("simgok"))!!));assertTrue(RestItinerary.nativeFullRouteSupported(Catalog.routes.first()));assertEquals(5,RestItinerary.route("coast",listOf("simgok","hourglass"))!!.stops.size)}
 @Test fun webCapacityRejectsOversizedAndInvalidPoints(){val r=Catalog.routes.first();for(bad in listOf(r.copy(stops=r.stops+r.stops+r.stops),r.copy(stops=listOf(Stop("bad",Point(Double.NaN,0.0)))+r.stops))){try{directionsUrl(bad);fail()}catch(_:IllegalArgumentException){}}}
}
