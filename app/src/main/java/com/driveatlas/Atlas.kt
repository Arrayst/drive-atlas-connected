package com.driveatlas

import kotlin.math.*

enum class Safety { UNKNOWN, MOVING, PARKED }
enum class LocationStatus { AVAILABLE, DENIED, LOST }
enum class Mode { SIMULATOR, VEHICLE }
data class Point(val lat: Double, val lon: Double)
data class Stop(val name: String, val point: Point)
data class Route(val id: String, val title: String, val subtitle: String, val minutes: Int, val stops: List<Stop>)
object Catalog {
 val routes = listOf(
  Route("coast", "푸른 해안의 여백", "가상 해안 · 바다와 작은 쉼터", 48, listOf(Stop("해안 입구", Point(35.0,129.0)), Stop("파도 쉼터", Point(35.01,129.01)), Stop("노을 전망대", Point(35.02,129.02)))),
  Route("forest", "숲을 천천히 읽는 길", "가상 숲길 · 초록과 느린 오후", 35, listOf(Stop("숲길 입구", Point(36.0,128.0)), Stop("초록 카페", Point(36.01,128.01)), Stop("나무 전망대", Point(36.02,128.02)))),
  Route("lake", "호수를 따라, 한 바퀴", "가상 호수 · 잔잔한 주말", 62, listOf(Stop("호수 입구", Point(37.0,127.0)), Stop("물빛 쉼터", Point(37.01,127.01)), Stop("호수 전망대", Point(37.02,127.02))))
 )
}
data class Evidence(val point: Point, val accuracyM: Double, val timeMs: Long, val simulated: Boolean)
data class Trip(val id: String, val routeId: String, val startedMs: Long, val nextStop: Int = 0, val lastEvidenceMs: Long = 0, val mode: Mode = Mode.SIMULATOR)
data class Award(val tripId: String, val routeId: String, val simulated: Boolean, val timeMs: Long)
enum class VisitStatus { UNCONFIRMED, INTERRUPTED }
data class Visit(val tripId: String, val routeId: String, val simulated: Boolean, val timeMs: Long, val progress: Int, val status: VisitStatus)
data class Snapshot(val trip: Trip? = null, val awards: List<Award> = emptyList(), val visits: List<Visit> = emptyList())
interface AtlasStore { fun load(): Snapshot; fun save(snapshot: Snapshot) }
/** Real integrations must supply fresh location evidence and authoritative parked/gear state.
 * Neither speed=0 nor a stoplight is evidence of PARKED. No real adapter is enabled in this alpha. */
interface VehicleAdapter { fun safety(): Safety; fun locationStatus(): LocationStatus; fun evidence(): Evidence? }
class AtlasEngine(private val store: AtlasStore, val mode: Mode) {
 var snapshot = store.load().validatedForRestore(); private set
 var safety = Safety.UNKNOWN; private set
 var location = LocationStatus.LOST; private set
 val canEdit get() = safety == Safety.PARKED
 fun setSafety(value: Safety) { safety = value }
 fun setLocation(value: LocationStatus) { location = value }
 fun start(routeId: String, now: Long): Boolean {
  if (!canEdit || snapshot.trip != null || Catalog.routes.none { it.id == routeId }) return false
  update(snapshot.copy(trip = Trip(java.util.UUID.randomUUID().toString(), routeId, now, mode = mode)))
  return true
 }
 fun accept(e: Evidence, now: Long): Boolean {
  val t = snapshot.trip ?: return false
  if (t.mode != mode) return false
  val r = Catalog.routes.first { it.id == t.routeId }
  if (location != LocationStatus.AVAILABLE || e.simulated != (mode == Mode.SIMULATOR) ||
   !e.accuracyM.isFinite() || e.accuracyM !in 0.0..40.0 || e.timeMs < t.startedMs ||
   e.timeMs <= t.lastEvidenceMs || e.timeMs > now || now - e.timeMs > 15000 ||
   !e.point.lat.isFinite() || !e.point.lon.isFinite() || e.point.lat !in -90.0..90.0 || e.point.lon !in -180.0..180.0) return false
  if (distance(e.point, r.stops[t.nextStop.coerceAtMost(r.stops.lastIndex)].point) > 100.0) return false
  update(snapshot.copy(trip = t.copy(nextStop = (t.nextStop + 1).coerceAtMost(r.stops.size), lastEvidenceMs = e.timeMs)))
  return true
 }
 fun finish(now: Long): Boolean {
  val t = snapshot.trip ?: return false
  if (t.mode != mode) return false
  val count = Catalog.routes.first { it.id == t.routeId }.stops.size
  if (!canEdit || location != LocationStatus.AVAILABLE || t.nextStop != count || now < t.lastEvidenceMs || now-t.lastEvidenceMs > 15000 || snapshot.awards.any { it.tripId == t.id }) return false
  update(snapshot.copy(trip = null, awards = snapshot.awards + Award(t.id, t.routeId, mode == Mode.SIMULATOR, now)))
  return true
 }
 fun archive(status: VisitStatus, now: Long): Boolean {
  val t = snapshot.trip ?: return false
  if (!canEdit || t.mode != mode || now < t.startedMs) return false
  update(snapshot.copy(trip = null, visits = snapshot.visits + Visit(t.id, t.routeId, mode == Mode.SIMULATOR, now, t.nextStop, status)))
  return true
 }
 fun abandon(): Boolean { if (!canEdit) return false; update(snapshot.copy(trip = null)); return true }
 private fun update(s: Snapshot) { store.save(s); snapshot = s }
 private fun distance(a: Point, b: Point): Double {
  val p1 = Math.toRadians(a.lat); val p2 = Math.toRadians(b.lat)
  val d1 = Math.toRadians(b.lat-a.lat); val d2 = Math.toRadians(b.lon-a.lon)
  val h = sin(d1/2).pow(2)+cos(p1)*cos(p2)*sin(d2/2).pow(2)
  return 6371000 * 2 * asin(sqrt(h.coerceIn(0.0,1.0)))
 }
}
