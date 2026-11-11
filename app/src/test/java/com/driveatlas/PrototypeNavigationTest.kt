package com.driveatlas

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class PrototypeNavigationTest {
 private val request=NavPrototypeRequest("coast","seoul",listOf("simgok","hourglass"),mapOf("simgok" to 1,"hourglass" to 2))
 private fun coordinate(p:Point)=JSONArray().put(p.lon).put(p.lat)
 private fun line(points:List<Point>)=JSONObject().put("type","LineString").put("coordinates",JSONArray().apply{points.forEach{put(coordinate(it))}})
 private fun step(points:List<Point>,type:String,distance:Double,duration:Double)=JSONObject().put("geometry",line(points)).put("distance",distance).put("duration",duration).put("name","시험 도로").put("maneuver",JSONObject().put("type",type).put("modifier","right").put("location",coordinate(points.first())))
 /** Synthetic response for parser tests only; never bundled or offered as actual navigation. */
 private fun response(points:List<Point> = request.points()):JSONObject {
  val legs=JSONArray().apply{points.zipWithNext().forEach{(a,b)->val m=Point((a.lat+b.lat)/2,(a.lon+b.lon)/2)
   put(JSONObject().put("distance",navDistance(a,m)+navDistance(m,b)).put("duration",20.0).put("steps",JSONArray().put(step(listOf(a,m),"depart",navDistance(a,m),10.0)).put(step(listOf(m,b),"turn",navDistance(m,b),10.0)).put(step(listOf(b),"arrive",0.0,0.0))))
  }}
  return JSONObject().put("code","Ok").put("waypoints",JSONArray().apply{points.forEach{put(JSONObject().put("location",coordinate(it)).put("distance",0.0))}})
   .put("routes",JSONArray().put(JSONObject().put("distance",(0 until legs.length()).sumOf{legs.getJSONObject(it).getDouble("distance")}).put("duration",20.0*(points.size-1)).put("geometry",line(points)).put("legs",legs)))
 }
 private fun parse()=parseNavPrototype(request,response().toString())
 private fun fails(value:JSONObject){try{parseNavPrototype(request,value.toString());fail("Expected rejection")}catch(e:EtaUnavailable){assertTrue(e.kind in listOf("InvalidResponse","NoSegment"))}}
 private fun route(o:JSONObject)=o.getJSONArray("routes").getJSONObject(0)
 private fun firstStep(o:JSONObject)=route(o).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(0)
 @Test fun fullLoopUsesPublicOriginAndSelectedRestOrder(){assertTrue(request.valid());val p=request.points();assertEquals(p.first(),p.last());assertEquals(RestItinerary.route("coast",request.selectedRestIds,request.restAfterStopIndex)!!.stops.map{it.point},p.drop(1).dropLast(1));assertTrue(navPrototypeUrl(request).contains("steps=true"));assertTrue(navPrototypeUrl(request).contains("overview=full"));assertTrue(navPrototypeUrl(request).contains("126.978420,37.566690"))}
 @Test fun onlyKnownPublicReferencesAreAccepted(){assertFalse(request.copy(departureId="my-home").valid());assertFalse(request.copy(routeId="missing").valid());assertFalse(request.copy(selectedRestIds=listOf("simgok","simgok")).valid());assertFalse(request.copy(restAfterStopIndex=mapOf("simgok" to 99)).valid())}
 @Test fun releaseAndNonSimulatorNetworkAreDisabled(){assertTrue(navPrototypeAllowed(true,true));assertFalse(navPrototypeAllowed(false,true));assertFalse(navPrototypeAllowed(true,false));assertFalse(navPrototypeAllowed(false,false))}
 @Test fun parserPreservesLegOrderAndGeometry(){val v=parse();assertEquals(request.points().first(),v.geometry.first());assertEquals(request.points().last(),v.geometry.last());assertEquals(request.points().size-1,v.legEndMeters.size);assertTrue(v.geometryMeters>0);assertEquals(listOf(0,1,2,3,4,5),v.steps.map{it.legIndex}.distinct());assertTrue(v.steps.any{it.instruction.startsWith("우회전")})}
 @Test fun noRouteAndMalformedResponsesFailSafely(){for(body in listOf("not-json","{}","{\"code\":\"NoRoute\"}")){try{parseNavPrototype(request,body);fail()}catch(_:EtaUnavailable){}}}
 @Test fun disconnectedStepGeometryRejected(){val o=response();firstStep(o).getJSONObject("geometry").put("coordinates",JSONArray().put(coordinate(Point(30.0,120.0))).put(coordinate(Point(30.1,120.1))));fails(o)}
 @Test fun badManeuverLocationRejected(){val o=response();firstStep(o).getJSONObject("maneuver").put("location",coordinate(Point(30.0,120.0)));fails(o)}
 @Test fun missingGeometryAndStepsRejected(){val a=response();firstStep(a).remove("geometry");fails(a);val b=response();route(b).getJSONArray("legs").getJSONObject(0).put("steps",JSONArray());fails(b)}
 @Test fun negativeStepTotalsAndInvalidCoordinatesRejected(){val a=response();firstStep(a).put("distance",-1);fails(a);val b=response();firstStep(b).getJSONObject("geometry").put("coordinates",JSONArray().put(JSONArray().put(127).put(95)));fails(b)}
 @Test fun wrongFinalGeometryRejected(){val a=response();route(a).put("geometry",line(listOf(Point(30.0,120.0),Point(30.0,120.0))));fails(a)}
 @Test fun unknownManeuverNeverInventsDirection(){assertTrue(navInstruction("alien","right","",0,2).contains("확인"));assertFalse(navInstruction("alien","right","",0,2).contains("우회전"));assertTrue(navInstruction("turn","alien","",0,2).contains("확인"))}
 @Test fun replayDoesNotTeleportAtRepeatedOriginOrReturnRoad(){val v=parse();val start=NavReplayState(running=true);val s=v.advance(start,100.0);assertEquals(100.0,s.distanceMeters,0.0);assertFalse(v.snapshot(start).finished);assertEquals(0.0,v.offRouteDistance(v.geometry.first(),start),0.01);val middle=v.advance(s,v.geometryMeters/2);assertTrue(middle.distanceMeters>s.distanceMeters);assertFalse(v.snapshot(middle).finished)}
 @Test fun pauseAndOffRouteNeverAdvance(){val v=parse();val paused=NavReplayState(100.0);assertEquals(paused,v.advance(paused,100.0));val lost=NavReplayState(100.0,true,true);assertEquals(lost,v.advance(lost,100.0));assertTrue(v.snapshot(lost).nextInstruction.contains("일시정지"));assertTrue(v.offRouteDistance(Point(0.0,0.0),lost)>1000)}
 @Test fun replayFinishesAndRestartReturnsToFirstPoint(){val v=parse();val end=v.advance(NavReplayState(running=true),v.geometryMeters+1);assertFalse(end.running);assertTrue(v.snapshot(end).finished);assertEquals(0.0,v.snapshot(end).remainingMeters,0.01);assertEquals(v.geometry.first(),v.snapshot(NavReplayState()).position)}
 @Test fun invalidReplayDeltasDoNotCorruptState(){val v=parse();val s=NavReplayState(100.0,true);assertEquals(s,v.advance(s,Double.NaN));assertEquals(s,v.advance(s,-1.0));assertTrue(v.snapshot(NavReplayState(Double.NaN)).remainingMeters.isFinite());assertEquals(v.geometry.first(),v.positionAt(-1.0))}
 @Test fun zeroLengthSnappedRestLegIsAcceptedWithoutDivisionByZero(){val points=request.points().toMutableList();points[3]=points[2];val v=parseNavPrototype(request,response(points).toString());assertEquals(v.legEndMeters[1],v.legEndMeters[2],0.01);assertTrue(v.snapshot(NavReplayState(v.legEndMeters[2])).remainingMeters.isFinite())}
 @Test fun geometricallyImplausibleDistanceRejected(){val o=response();val r=route(o);r.put("distance",r.getDouble("distance")*.1);val legs=r.getJSONArray("legs");for(i in 0 until legs.length()){val l=legs.getJSONObject(i);l.put("distance",l.getDouble("distance")*.1);val steps=l.getJSONArray("steps");for(j in 0 until steps.length()){val s=steps.getJSONObject(j);s.put("distance",s.getDouble("distance")*.1)}};fails(o)}
 @Test fun duplicateConsecutiveGeometryIsCoalesced(){val o=response();val first=firstStep(o);val coords=first.getJSONObject("geometry").getJSONArray("coordinates");coords.put(coords.getJSONArray(coords.length()-1));val v=parseNavPrototype(request,o.toString());assertTrue(v.geometry.zipWithNext().all{it.first!=it.second})}
}
