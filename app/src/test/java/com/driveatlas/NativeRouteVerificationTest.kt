package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class NativeRouteVerificationTest {
    private val points = Catalog.routes.first().stops.map { it.point }
    private fun completed() = NativeRouteVerification(points).also { it.complete() }

    @Test fun completionAloneDoesNotConfirm() {
        assertFalse(completed().confirmed)
    }

    @Test fun requiresDestinationAndEveryOrderedWaypoint() {
        val check = completed()
        check.destination(points.last())
        assertFalse(check.confirmed)
        check.waypoints(points.dropLast(1))
        assertTrue(check.confirmed)
    }

    @Test fun earlyCallbacksCannotBeReusedAtCompletion() {
        val check = NativeRouteVerification(points)
        check.destination(points.last()); check.waypoints(points.dropLast(1))
        check.complete()
        assertNull(check.destinationMatches); assertNull(check.waypointsMatch)
        assertFalse(check.confirmed)
    }

    @Test fun repeatedCompletionRequiresFreshEvidence() {
        val check = completed()
        check.destination(points.last()); check.waypoints(points.dropLast(1))
        assertTrue(check.confirmed)
        check.complete()
        assertFalse(check.confirmed)
    }

    @Test fun reversedMissingExtraOrDuplicatedWaypointsFail() {
        val expected = points.dropLast(1)
        listOf(expected.reversed(), expected.take(1), points, listOf(points[0], points[0])).forEach {
            val check = completed()
            check.destination(points.last()); check.waypoints(it)
            assertEquals(false, check.waypointsMatch); assertFalse(check.confirmed)
        }
    }

    @Test fun incorrectDestinationCannotBeConfirmed() {
        val check = completed()
        check.destination(points.first()); check.waypoints(points.dropLast(1))
        assertEquals(false, check.destinationMatches); assertFalse(check.confirmed)
    }

    @Test fun changedDestinationOrWaypointsInvalidateEvidence() {
        val check = completed()
        check.destination(points.last()); check.waypoints(points.dropLast(1))
        check.invalidateDestination(); assertFalse(check.confirmed)
        check.destination(points.last()); assertTrue(check.confirmed)
        check.invalidateWaypoints(); assertFalse(check.confirmed)
    }

    @Test fun coordinatesMustBeFiniteAndWithinGeographicRanges() {
        listOf(Point(Double.NaN, 0.0), Point(0.0, Double.POSITIVE_INFINITY),
            Point(91.0, 0.0), Point(0.0, 181.0), Point(0.0, 360.0)).forEach {
            assertFalse(nativePointMatches(it, it))
            assertFalse(nativePointMatches(Point(0.0, 0.0), it))
        }
    }

    @Test fun distanceToleranceRemainsOneHundredMeters() {
        val origin = Point(0.0, 0.0)
        assertTrue(nativePointMatches(origin, Point(0.00089, 0.0)))
        assertFalse(nativePointMatches(origin, Point(0.00091, 0.0)))
        assertFalse(nativePointMatches(origin, Point(0.0, 180.0)))
    }

    @Test fun emptyCourseCannotConfirm() {
        val check = NativeRouteVerification(emptyList())
        check.complete(); check.destination(points.last()); check.waypoints(emptyList())
        assertFalse(check.confirmed)
    }
    @Test fun singleDestinationRequiresCompletionAndNoExtraWaypoints() {
        val check=NativeRouteVerification(listOf(points.first()))
        check.destination(points.first());check.waypoints(emptyList());assertFalse(check.confirmed)
        check.complete();check.destination(points.first());check.waypoints(emptyList());assertTrue(check.confirmed)
        check.waypoints(listOf(points.last()));assertFalse(check.confirmed)
    }
    @Test fun zeroCoordinateMetadataCannotVerifyKoreanWaypoints() {
        val check=completed();check.destination(points.last());check.waypoints(listOf(Point(0.0,0.0),Point(0.0,0.0)));assertFalse(check.confirmed)
    }

}
