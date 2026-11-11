package com.driveatlas

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

/** A public-reference replay request, never a device or home location request. */
internal data class NavPrototypeRequest(val routeId:String,val departureId:String,
 val selectedRestIds:List<String> = emptyList(),val restAfterStopIndex:Map<String,Int> = emptyMap()) {
 fun departure()=DepartureReferences.all.firstOrNull{it.id==departureId}
 fun valid():Boolean=departure()!=null&&RestItinerary.route(routeId,selectedRestIds,restAfterStopIndex)!=null
 fun points():List<Point> { require(valid());return listOf(departure()!!.point)+RestItinerary.route(routeId,selectedRestIds,restAfterStopIndex)!!.stops.map{it.point}+departure()!!.point }
 internal fun etaRequest():EtaRequest {require(valid());val d=departure()!!;return EtaRequest(routeId,d.point,d.label,d.id,selectedRestIds,true,restAfterStopIndex)}
}
internal fun navPrototypeAllowed(debug:Boolean,simulator:Boolean)=debug&&simulator
internal fun navPrototypeUrl(request:NavPrototypeRequest):String {
 require(request.valid())
 val points=request.points()
 val coords=points.joinToString(";"){String.format(Locale.US,"%.6f,%.6f",it.lon,it.lat)}
 return "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson&steps=true&alternatives=false&generate_hints=false&continue_straight=false&radiuses="+points.joinToString(";"){"500"}
}
internal data class NavPrototypeStep(val instruction:String,val startMeters:Double,val legIndex:Int,val type:String)
internal data class NavReplayState(val distanceMeters:Double=0.0,val running:Boolean=false,val offRoute:Boolean=false)
internal data class NavReplaySnapshot(val position:Point,val remainingMeters:Double,val nextInstruction:String,
 val metersToInstruction:Double,val stepIndex:Int,val finished:Boolean)
internal data class NavPrototypeRoute(val request:NavPrototypeRequest,val geometry:List<Point>,val steps:List<NavPrototypeStep>,
 val meters:Double,val seconds:Double,val legEndMeters:List<Double>) {
 internal val cumulative:List<Double> = geometry.runningFoldIndexed(0.0){i,sum,p->sum+if(i==0)0.0 else navDistance(geometry[i-1],p)}.drop(1)
 val geometryMeters:Double get()=cumulative.lastOrNull()?:0.0
 fun advance(state:NavReplayState,deltaMeters:Double):NavReplayState {
  if(!state.running||state.offRoute||!deltaMeters.isFinite()||deltaMeters<0)return state
  val distance=(normalizedDistance(state)+deltaMeters).coerceAtMost(geometryMeters)
  return state.copy(distanceMeters=distance,running=distance<geometryMeters)
 }
 fun snapshot(state:NavReplayState):NavReplaySnapshot {
  val distance=normalizedDistance(state);val finished=distance>=geometryMeters
  val next=if(finished)steps.lastIndex else steps.indexOfFirst{it.startMeters>distance+0.1}.let{if(it<0)steps.lastIndex else it}
  return NavReplaySnapshot(positionAt(distance),((geometryMeters-distance)/geometryMeters*meters).coerceAtLeast(0.0),
   if(finished)"출발 기준점으로 돌아왔어요 · 시연 종료" else if(state.offRoute)"시연 위치가 경로를 벗어났어요 · 재생 일시정지" else steps[next].instruction,
   (steps[next].startMeters-distance).coerceAtLeast(0.0),next,finished)
 }
 /** Searches only the replay's local progress window; outbound/return overlaps cannot jump progress. */
 fun offRouteDistance(point:Point,state:NavReplayState):Double {
  if(!validNativePoint(point))return Double.POSITIVE_INFINITY
  val distance=normalizedDistance(state);var nearest=Double.POSITIVE_INFINITY
  for(i in 0 until geometry.lastIndex)if(cumulative[i+1]>=distance-500&&cumulative[i]<=distance+500){nearest=min(nearest,navSegmentDistance(point,geometry[i],geometry[i+1]))}
  return nearest
 }
 fun positionAt(distanceMeters:Double):Point {
  val d=if(distanceMeters.isFinite())distanceMeters.coerceIn(0.0,geometryMeters) else 0.0
  val end=cumulative.indexOfFirst{it>=d}.coerceAtLeast(0)
  if(end==0)return geometry.first()
  val start=end-1;val span=cumulative[end]-cumulative[start];val t=if(span>0)(d-cumulative[start])/span else 0.0
  return Point(geometry[start].lat+(geometry[end].lat-geometry[start].lat)*t,geometry[start].lon+(geometry[end].lon-geometry[start].lon)*t)
 }
 private fun normalizedDistance(state:NavReplayState)=if(state.distanceMeters.isFinite())state.distanceMeters.coerceIn(0.0,geometryMeters) else 0.0
}
internal fun navDistance(a:Point,b:Point):Double {
 val h=(sin(Math.toRadians(b.lat-a.lat)/2).pow(2)+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*sin(Math.toRadians(b.lon-a.lon)/2).pow(2)).coerceIn(0.0,1.0)
 return 6_371_000*2*atan2(sqrt(h),sqrt(1-h))
}
private fun navSegmentDistance(p:Point,a:Point,b:Point):Double {
 val c=cos(Math.toRadians(p.lat));val ax=(a.lon-p.lon)*c;val ay=a.lat-p.lat;val bx=(b.lon-p.lon)*c;val by=b.lat-p.lat
 val dx=bx-ax;val dy=by-ay;val square=dx*dx+dy*dy;val t=if(square==0.0)0.0 else (-(ax*dx+ay*dy)/square).coerceIn(0.0,1.0)
 return sqrt((ax+t*dx).pow(2)+(ay+t*dy).pow(2))*Math.PI/180*6_371_000
}
private fun navCoordinate(a:JSONArray):Point { if(a.length()!=2)throw EtaUnavailable("InvalidResponse");return Point(a.getDouble(1),a.getDouble(0)).also{if(!validNativePoint(it))throw EtaUnavailable("InvalidResponse")} }
private fun navCoordinates(j:JSONObject):List<Point> {
 if(j.getString("type")!="LineString")throw EtaUnavailable("InvalidResponse")
 val a=j.getJSONArray("coordinates");if(a.length() !in 1..100000)throw EtaUnavailable("InvalidResponse")
 return (0 until a.length()).map{navCoordinate(a.getJSONArray(it))}
}
/** Deliberately conservative language; unknown maneuver semantics are never invented as a turn. */
internal fun navInstruction(type:String,modifier:String?,road:String,legIndex:Int,lastLeg:Int):String {
 val name=road.filter{!it.isISOControl()}.take(80).trim().let{if(it.isBlank())"" else " · $it"}
 val direction=when(modifier){"left"->"좌회전";"right"->"우회전";"slight left"->"왼쪽으로 완만하게";"slight right"->"오른쪽으로 완만하게";"sharp left"->"왼쪽으로 크게";"sharp right"->"오른쪽으로 크게";"uturn"->"유턴";"straight"->"직진";else->null}
 return when(type){
  "depart"->"다음 구간 출발$name"
  "arrive"->if(legIndex==lastLeg)"출발 기준점 도착" else "경유 지점 도착 · 안전한 정차 여부는 별도 확인"
  "turn","continue","new name","end of road"->(direction?:"경로를 확인하세요")+name
  "merge"->"합류 구간 · ${direction?:"경로 확인"}$name"
  "fork"->"갈림길 · ${direction?:"경로 확인"}$name"
  "on ramp"->"진입 연결로 · ${direction?:"경로 확인"}$name"
  "off ramp"->"진출 연결로 · ${direction?:"경로 확인"}$name"
  "roundabout","rotary","roundabout turn","exit roundabout","exit rotary"->"회전교차로 · 지도에서 경로 확인$name"
  else->"다음 경로 지시를 지도에서 확인하세요$name"
 }
}
internal fun parseNavPrototype(request:NavPrototypeRequest,body:String):NavPrototypeRoute {
 if(!request.valid())throw EtaUnavailable("InvalidInput")
 if(body.length>8*1024*1024)throw EtaUnavailable("InvalidResponse")
 try {
  // Reuse the checked ordered-waypoint, leg-distance and duration contract.
  val eta=parseEta(request.etaRequest(),body,1)
  val root=JSONObject(body);val route=root.getJSONArray("routes").getJSONObject(0);val legs=route.getJSONArray("legs")
  val overview=navCoordinates(route.getJSONObject("geometry"));val snapped=root.getJSONArray("waypoints")
  val geometry=mutableListOf<Point>();val steps=mutableListOf<NavPrototypeStep>();val legEnds=mutableListOf<Double>();var distance=0.0;var count=0
  for(l in 0 until legs.length()){
   val leg=legs.getJSONObject(l);val rawSteps=leg.getJSONArray("steps");if(rawSteps.length() !in 2..5000)throw EtaUnavailable("InvalidResponse")
   val legGeometryStart=distance
   var stepDistance=0.0;var stepDuration=0.0
   for(s in 0 until rawSteps.length()){
    if(++count>10000)throw EtaUnavailable("InvalidResponse")
    val step=rawSteps.getJSONObject(s);val sd=step.getDouble("distance");val st=step.getDouble("duration")
    if(!sd.isFinite()||sd !in 0.0..3_000_000.0||!st.isFinite()||st !in 0.0..43200.0)throw EtaUnavailable("InvalidResponse")
    stepDistance+=sd;stepDuration+=st
    val points=navCoordinates(step.getJSONObject("geometry"));val maneuver=step.getJSONObject("maneuver");val type=maneuver.getString("type")
    if((s==0&&type!="depart")||(s==rawSteps.length()-1&&type!="arrive"))throw EtaUnavailable("InvalidResponse")
    if(navDistance(navCoordinate(maneuver.getJSONArray("location")),points.first())>5)throw EtaUnavailable("InvalidResponse")
    if(geometry.isNotEmpty()&&navDistance(geometry.last(),points.first())>5)throw EtaUnavailable("InvalidResponse")
    steps+=NavPrototypeStep(navInstruction(type,maneuver.optString("modifier").takeIf{it.isNotBlank()},step.optString("name"),l,legs.length()-1),distance,l,type)
    points.forEach { p -> if(geometry.isEmpty())geometry+=p else if(p!=geometry.last()){distance+=navDistance(geometry.last(),p);geometry+=p};if(geometry.size>100000)throw EtaUnavailable("InvalidResponse") }
   }
   val expectedEnd=navCoordinate(snapped.getJSONObject(l+1).getJSONArray("location"))
   if(navDistance(geometry.last(),expectedEnd)>5)throw EtaUnavailable("InvalidResponse")
   if(abs(stepDistance-leg.getDouble("distance"))>max(5.0,rawSteps.length()*.2)||abs(stepDuration-leg.getDouble("duration"))>max(5.0,rawSteps.length()*.2))throw EtaUnavailable("InvalidResponse")
   if(abs((distance-legGeometryStart)-stepDistance)>max(100.0,stepDistance*.25))throw EtaUnavailable("InvalidResponse")
   legEnds+=distance
  }
  if(geometry.size<2||distance<=0||eta.meters<=0||navDistance(geometry.first(),navCoordinate(snapped.getJSONObject(0).getJSONArray("location")))>5||navDistance(overview.first(),geometry.first())>5||navDistance(overview.last(),geometry.last())>5)throw EtaUnavailable("InvalidResponse")
  return NavPrototypeRoute(request,geometry.toList(),steps.toList(),eta.meters,eta.legSeconds.sum(),legEnds.toList())
 }catch(e:EtaUnavailable){throw e}catch(_:Exception){throw EtaUnavailable("InvalidResponse")}
}
