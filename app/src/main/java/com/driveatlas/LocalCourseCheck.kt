package com.driveatlas

import java.util.Locale
import kotlin.math.*

/** Local-only comparison. A matching coordinate is NOT proof of a real visit. */
internal data class CourseProximity(val route: Route, val stop: Stop, val meters: Double)
internal fun localCourseProximity(point: Point): CourseProximity? {
    if (!point.lat.isFinite() || !point.lon.isFinite() || point.lat !in -90.0..90.0 || point.lon !in -180.0..180.0) return null
    return Catalog.routes.flatMap { route -> route.stops.map { stop ->
        val dlat = Math.toRadians(point.lat - stop.point.lat)
        val dlon = Math.toRadians(point.lon - stop.point.lon)
        val a = sin(dlat/2).pow(2) + cos(Math.toRadians(point.lat)) * cos(Math.toRadians(stop.point.lat)) * sin(dlon/2).pow(2)
        CourseProximity(route, stop, 6371000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0))))
    } }.minByOrNull { it.meters }
}
internal fun localCourseSummary(point: Point): String {
    val nearest = localCourseProximity(point) ?: return "유효한 위치가 없어 코스를 비교하지 않았어요."
    val distance = if (nearest.meters < 1000) String.format(Locale.KOREA, "%.0f m", nearest.meters)
        else String.format(Locale.KOREA, "%.1f km", nearest.meters / 1000)
    return "가장 가까운 코스 지점: ${nearest.route.title} · ${nearest.stop.name}, 약 $distance. 위치값만 비교한 참고 정보이며 방문은 미확인입니다."
}
