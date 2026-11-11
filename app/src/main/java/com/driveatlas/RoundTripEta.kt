package com.driveatlas

import java.util.Locale
import kotlin.math.*
import org.json.JSONObject

internal data class DepartureReference(val id:String,val label:String,val point:Point,val source:String)
internal object DepartureReferences {
 val all=listOf(
  DepartureReference("seoul","서울시청 공개 기준점",Point(37.56669,126.97842),"https://www.openstreetmap.org/way/198561926"),
  DepartureReference("daejeon","대전역 공개 기준점",Point(36.33225,127.43465),"https://www.openstreetmap.org/node/355173691"),
  DepartureReference("busan","부산시청 공개 기준점",Point(35.17994,129.07521),"https://www.openstreetmap.org/way/468977727")
 )
}
internal data class EtaRequest(val routeId:String,val origin:Point,val originLabel:String,val publicReferenceId:String?=null) {
 fun points():List<Point> = listOf(origin)+Catalog.routes.first{it.id==routeId}.stops.map{it.point}+origin
 fun valid() = Catalog.routes.any{it.id==routeId} && validNativePoint(origin) && originLabel.isNotBlank() &&
  (publicReferenceId==null||DepartureReferences.all.any{it.id==publicReferenceId&&it.point==origin})
}
internal data class EtaEstimate(val routeId:String,val originLabel:String,val outboundSeconds:Double,
 val courseSeconds:Double,val returnSeconds:Double,val meters:Double,val calculatedAt:Long,val cached:Boolean=false) {
 val outboundMinutes get()=ceil(outboundSeconds/60).toInt()
 val courseMinutes get()=ceil(courseSeconds/60).toInt()
 val returnMinutes get()=ceil(returnSeconds/60).toInt()
 val roadMinutes get()=outboundMinutes+courseMinutes+returnMinutes
 fun valid()=Catalog.routes.any{it.id==routeId}&&originLabel.isNotBlank()&&
  listOf(outboundSeconds,courseSeconds,returnSeconds).all{it.isFinite()&&it in 0.0..43200.0}&&meters.isFinite()&&meters in 0.0..3_000_000.0&&calculatedAt>0
}
internal class EtaUnavailable(val kind:String):Exception(kind)
internal fun etaUrl(request:EtaRequest):String {
 require(request.valid())
 val points=request.points()
 val coords=points.joinToString(";"){String.format(Locale.US,"%.6f,%.6f",it.lon,it.lat)}
 return "https://router.project-osrm.org/route/v1/driving/$coords?overview=false&steps=false&alternatives=false&generate_hints=false&continue_straight=false&radiuses="+points.joinToString(";"){"500"}
}
private fun snapDistance(a:Point,b:Point):Double {
 val dlat=Math.toRadians(a.lat-b.lat);val dlon=Math.toRadians(a.lon-b.lon)
 val h=(sin(dlat/2).pow(2)+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*sin(dlon/2).pow(2)).coerceIn(0.0,1.0)
 return 6_371_000*2*atan2(sqrt(h),sqrt(1-h))
}
internal fun parseEta(request:EtaRequest,body:String,now:Long):EtaEstimate {
 if(!request.valid())throw EtaUnavailable("InvalidInput")
 try {
  val root=JSONObject(body);val code=root.optString("code","InvalidResponse")
  if(code!="Ok")throw EtaUnavailable(if(code in listOf("NoRoute","NoSegment","TooBig","InvalidQuery","InvalidValue"))code else "InvalidResponse")
  val routes=root.getJSONArray("routes");if(routes.length()!=1)throw EtaUnavailable("InvalidResponse")
  val route=routes.getJSONObject(0);val legs=route.getJSONArray("legs");val expected=request.points()
  if(legs.length()!=expected.size-1)throw EtaUnavailable("InvalidResponse")
  val snapped=root.getJSONArray("waypoints");if(snapped.length()!=expected.size)throw EtaUnavailable("InvalidResponse")
  expected.forEachIndexed{i,p->val w=snapped.getJSONObject(i);val loc=w.getJSONArray("location");val actual=Point(loc.getDouble(1),loc.getDouble(0));val distance=w.getDouble("distance")
   if(!validNativePoint(actual)||!distance.isFinite()||distance !in 0.0..500.0||snapDistance(p,actual)>501)throw EtaUnavailable("NoSegment")}
  val durations=(0 until legs.length()).map{legs.getJSONObject(it).getDouble("duration")}
  if(durations.any{!it.isFinite()||it !in 0.0..43200.0})throw EtaUnavailable("InvalidResponse")
  val distances=(0 until legs.length()).map{legs.getJSONObject(it).getDouble("distance")}
  if(distances.any{!it.isFinite()||it !in 0.0..3_000_000.0})throw EtaUnavailable("InvalidResponse")
  val total=route.getDouble("duration");val totalDistance=route.getDouble("distance")
  if(!total.isFinite()||total<0||abs(total-durations.sum())>1.0||!totalDistance.isFinite()||totalDistance<0||abs(totalDistance-distances.sum())>1.0)throw EtaUnavailable("InvalidResponse")
  val value=EtaEstimate(request.routeId,request.originLabel,durations.first(),durations.drop(1).dropLast(1).sum(),durations.last(),route.getDouble("distance"),now)
  if(!value.valid())throw EtaUnavailable("InvalidResponse")
  return value
 }catch(e:EtaUnavailable){throw e}catch(_:Exception){throw EtaUnavailable("InvalidResponse")}
}
internal fun etaErrorMessage(kind:String)=when(kind){
 "NoRoute"->"해당 출발지와 코스를 잇는 자동차 경로를 찾지 못했어요. 다른 공개 출발점을 선택하거나 수동 시간을 입력하세요."
 "NoSegment"->"입력 지점 가까이에서 주행 가능한 도로를 확인하지 못했어요. 안전한 출발 기준점을 다시 선택하세요."
 "RateLimit"->"경로 서비스가 요청을 제한했어요. 잠시 뒤 직접 다시 시도해 주세요."
 "Timeout"->"경로 응답 시간이 초과됐어요. 저장된 공개 기준점 결과나 수동 계획을 사용할 수 있어요."
 "Network"->"인터넷 또는 경로 서비스에 연결하지 못했어요. 자동 재시도하지 않습니다."
 "ProductionProviderRequired"->"운영용 경로 공급자를 설정해야 배포판에서 사용할 수 있어요. 현재 공개 서버는 소규모 비상업 시험용입니다."
 else->"경로 응답을 안전하게 확인하지 못했어요. 추정값을 만들어 넣지 않습니다."
}

internal fun publicEtaCacheKey(request:EtaRequest):String? =
 if(request.valid()&&request.publicReferenceId!=null)request.publicReferenceId+":"+request.routeId else null
internal fun freshCachedEta(value:EtaEstimate,now:Long)=value.valid()&&now>=value.calculatedAt&&now-value.calculatedAt<=24*60*60*1000L
