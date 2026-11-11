package com.driveatlas

import kotlin.math.*

/** Callback evidence, not a route request acknowledgement or proof of physical travel.
 * The provider has no request IDs. Never reuse observations from before completion.
 */
internal class NativeRouteVerification(private val stops: List<Point>) {
    var completed = false; private set
    var destinationMatches: Boolean? = null; private set
    var waypointsMatch: Boolean? = null; private set
    val confirmed get() = completed && destinationMatches == true && waypointsMatch == true

    fun complete() {
        completed = true
        destinationMatches = null
        waypointsMatch = null
    }

    fun destination(point: Point) {
        if (completed) destinationMatches = stops.lastOrNull()?.let { nativePointMatches(it, point) } ?: false
    }

    fun waypoints(points: List<Point>) {
        if (!completed) return
        val expected = stops.dropLast(1)
        waypointsMatch = stops.isNotEmpty() && expected.size == points.size &&
            expected.indices.all { nativePointMatches(expected[it], points[it]) }
    }

    fun invalidateDestination() { destinationMatches = null }
    fun invalidateWaypoints() { waypointsMatch = null }
}

internal fun validNativePoint(point: Point): Boolean =
    point.lat.isFinite() && point.lon.isFinite() && point.lat in -90.0..90.0 && point.lon in -180.0..180.0

internal fun nativePointMatches(expected: Point, actual: Point): Boolean {
    if (!validNativePoint(expected) || !validNativePoint(actual)) return false
    val dlat = Math.toRadians(expected.lat - actual.lat)
    val dlon = Math.toRadians(expected.lon - actual.lon)
    val a = (sin(dlat / 2).pow(2) + cos(Math.toRadians(expected.lat)) *
        cos(Math.toRadians(actual.lat)) * sin(dlon / 2).pow(2)).coerceIn(0.0, 1.0)
    return 6_371_000 * 2 * atan2(sqrt(a), sqrt(1 - a)) <= 100.0
}
