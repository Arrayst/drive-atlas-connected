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
  Route("coast", "강릉 헌화로", "강릉 · 금진항에서 정동진까지", 10, listOf(Stop("금진항 앞 헌화로", Point(37.652838,129.0516881)), Stop("심곡항 앞 헌화로", Point(37.6642503,129.0546272)), Stop("정동진 해변 인근", Point(37.6906013,129.0315582)))),
  Route("forest", "담양 메타길 나들이", "담양 · 주변 도로 주행, 나무길은 도보", 5, listOf(Stop("관방제림 남쪽 도로", Point(35.3240329,126.9854786)), Stop("담양88로", Point(35.3167139,126.99192)), Stop("메타길 인근 도로", Point(35.3230071,127.0022261)))),
  Route("lake", "춘천 의암호 서쪽길", "춘천 · 의암댐에서 춘천댐 방향으로", 19, listOf(Stop("의암댐 서쪽 박사로", Point(37.8370973,127.6741729)), Stop("애니메이션박물관 앞", Point(37.8927957,127.6900156)), Stop("춘천댐 인근 춘성교", Point(37.9691988,127.666728))))
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
