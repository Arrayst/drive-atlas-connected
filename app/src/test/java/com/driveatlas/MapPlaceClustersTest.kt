package com.driveatlas

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MapPlaceClustersTest {
 private fun point(id: String, x: Float, y: Float) = ProjectedMapPlace(
  MapPlace(id, id, Point(37.0, 127.0), setOf(MapPlaceKind.COURSE), "1", "public reference"), MapPinPosition(x, y))
 @Test fun intersectingTouchTargetsClusterWithoutMovingTheAnchor() {
  val points = listOf(point("a", 100f, 100f), point("b", 120f, 120f), point("c", 300f, 100f))
  val clusters = clusterMapPlaceTargets(points, 72f)
  assertEquals(2, clusters.size)
  assertEquals(points.first().position, clusters.first().anchor)
  assertEquals(points, clusters.flatMap { it.members })
  clusters.forEach { assertTrue(it.members.any { member -> member.position == it.anchor }) }
 }
 @Test fun chainGroupsPreserveEveryRealCoordinateAndDoNotDuplicatePlaces() {
  val points = listOf(point("a", 100f, 100f), point("b", 160f, 100f), point("c", 220f, 100f))
  val clusters = clusterMapPlaceTargets(points, 72f)
  assertEquals(1, clusters.size)
  assertEquals(points, clusters.single().members)
  assertEquals(100f, clusters.single().anchor.x, 0f)
 }
 @Test fun distinctGroupsHaveNonOverlappingSixtyFourDpTouchTargets() {
  val points = listOf(point("a", 0f, 0f), point("b", 20f, 10f), point("c", 72f, 90f), point("d", 180f, 0f))
  val clusters = clusterMapPlaceTargets(points, 72f)
  clusters.forEachIndexed { i, first -> clusters.drop(i + 1).forEach { second ->
   assertTrue(abs(first.anchor.x - second.anchor.x) >= 72f || abs(first.anchor.y - second.anchor.y) >= 72f)
  } }
 }
 @Test fun invalidProjectedCoordinatesAreNeverDrawn() {
  assertTrue(clusterMapPlaceTargets(listOf(point("bad", Float.NaN, 0f)), 72f).isEmpty())
 }
 @Test fun viewportAndControlsReserveTheWholeTouchTarget() {
  assertTrue(mapPlaceTargetFits(MapPinPosition(100f, 100f), 600f, 300f, 32f, 80f))
  assertFalse(mapPlaceTargetFits(MapPinPosition(500f, 100f), 600f, 300f, 32f, 80f))
  assertFalse(mapPlaceTargetFits(MapPinPosition(100f, 280f), 600f, 300f, 32f, 80f))
  assertFalse(mapPlaceTargetFits(MapPinPosition(10f, 100f), 600f, 300f, 32f, 80f))
  assertFalse(mapPlaceTargetFits(MapPinPosition(100f, 30f), 600f, 40f, 32f, 80f))
 }
 @Test fun filtersOperateOnTheOriginalSourcedPlacesBeforeClustering() {
  val places = mapPlaces(Catalog.routes.first(), setOf("hourglass"))
  val photo = visibleMapPlaces(places, MapPlaceKind.PHOTO)
  val projected = photo.map { ProjectedMapPlace(it, MapPinPosition(200f, 200f)) }
  assertEquals(photo, clusterMapPlaceTargets(projected, 72f).flatMap { it.members }.map { it.place })
  assertTrue(projected.single().place.selectedRest)
  assertTrue(projected.single().place.description.contains("정확한 위치가 아닙니다"))
 }
 @Test fun tinyMapsAndLargeTextUseTheAccessibleListInsteadOfClippedBadges() {
  assertTrue(mapPlaceMarkersEnabled(600f, 260f, 1f))
  assertFalse(mapPlaceMarkersEnabled(180f, 260f, 1f))
  assertFalse(mapPlaceMarkersEnabled(600f, 90f, 1f))
  assertFalse(mapPlaceMarkersEnabled(600f, 260f, 2f))
  assertFalse(mapPlaceMarkersEnabled(600f, 260f, Float.NaN))
 }
 @Test fun cardReservesAUsefulMapAreaWithoutShrinkingTouchTargets() {
  assertEquals(520f, publicMapCardHeightDp(1f), 0f)
  assertEquals(328f, publicMapCardHeightDp(1f) - 3 * 64f, 0f)
  assertEquals(680f, publicMapCardHeightDp(2f), 0f)
  assertEquals(520f, publicMapCardHeightDp(Float.NaN), 0f)
 }
 @Test fun releaseRetainsPublicExplorationButNeverEnablesSimReplay() {
  assertTrue(vectorMapConnectionAllowed(VectorMapPurpose.PUBLIC_COURSE, false, false, true))
  assertFalse(vectorMapConnectionAllowed(VectorMapPurpose.PUBLIC_COURSE, false, false, false))
  assertFalse(vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, false, false, true))
  assertFalse(vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, true, false, true))
  assertTrue(vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, true, true, true))
 }
 @Test fun providerLegalLinksUseOnlyExactHttpsHosts() {
  assertTrue(safePublicUrl(NavigationVectorStyle.terms))
  assertTrue(safePublicUrl(NavigationVectorStyle.privacy))
  assertTrue(safePublicUrl("https://www.openmaptiles.org/"))
  assertTrue(safePublicUrl("https://openmaptiles.org/"))
  assertFalse(safePublicUrl("http://openfreemap.org/tos/"))
  assertFalse(safePublicUrl("https://openfreemap.org.evil.example/tos/"))
  assertFalse(safePublicUrl("https://private.openfreemap.org/tos/"))
  assertFalse(safePublicUrl("https://www.openmaptiles.org.evil.example/"))
  assertFalse(safePublicUrl("https://user@openfreemap.org/tos/"))
 }
}
