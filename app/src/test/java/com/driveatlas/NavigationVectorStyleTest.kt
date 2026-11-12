package com.driveatlas

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NavigationVectorStyleTest {
 private fun style() = JSONObject(NavigationVectorStyle.json())
 private fun layers() = style().getJSONArray("layers").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
 @Test fun providerRequiresSeparateConsentAndSimulator() {
  assertFalse(NavigationVectorStyle.allowed(true, true, false))
  assertFalse(NavigationVectorStyle.allowed(false, true, true))
  assertFalse(NavigationVectorStyle.allowed(true, false, true))
  assertTrue(NavigationVectorStyle.allowed(true, true, true))
 }
 @Test fun originalStyleHasOnlyReviewedProviderAndActualVectorGeography() {
  val j = style(); assertEquals(8, j.getInt("version"))
  val source = j.getJSONObject("sources").getJSONObject("openmaptiles")
  assertEquals("vector", source.getString("type"))
  assertEquals("https://tiles.openfreemap.org/planet/latest", source.getString("url"))
  assertFalse(j.has("sprite"))
  assertTrue(layers().none { it.getString("type") in listOf("raster", "fill-extrusion") })
  assertTrue(layers().none { it.optString("source-layer") == "poi" })
  assertEquals("building", layers().first { it.getString("id") == "buildings" }.getString("source-layer"))
 }
 @Test fun roadsAreThreeIndependentHierarchyClassesAndLabelsStayKoreanFirst() {
  NavigationVectorStyle.roadLayers.forEach { id ->
   val road = layers().first { it.getString("id") == id }
   assertEquals("transportation", road.getString("source-layer"))
   assertEquals("line", road.getString("type"))
   assertTrue(road.getJSONObject("paint").has("line-width"))
  }
  val text = layers().first { it.getString("id") == "road-labels" }.getJSONObject("layout").getJSONArray("text-field").toString()
  assertTrue(text.indexOf("name:ko") < text.indexOf("name:en"))
  assertTrue(text.contains("coalesce"))
 }
 @Test fun routeSourcesAreEmptyAndHiddenBeforeLoadedRoadEvidence() {
  val j = style()
  listOf("route-passed", "route-remaining", "route-passed-cursor", "route-remaining-cursor", "references", "sim-position").forEach {
   assertEquals(0, j.getJSONObject("sources").getJSONObject(it).getJSONObject("data").getJSONArray("features").length())
  }
  NavigationVectorStyle.routeLayers.forEach { id -> assertEquals("none", layers().first { it.getString("id") == id }.getJSONObject("layout").getString("visibility")) }
 }
 @Test fun followUsesForwardLookingAnchorAndStreetLevelZoom() {
  val height = 1000; val top = navigationFollowTopPadding(height)
  assertEquals(750.0, top + (height - top) / 2.0, 0.0)
  assertTrue(navigationFollowZoom() in 17.0..18.5)
 }
 @Test fun fullWindowRouteAndAnimatedLinksHaveMatchingProminentWidths() {
  fun width(id: String) = layers().first { it.getString("id") == id }.getJSONObject("paint").getDouble("line-width")
  assertEquals(11.0, width("route"), 0.0)
  assertEquals(width("route"), width("route-cursor"), 0.0)
  assertEquals(14.0, width("route-casing"), 0.0)
  assertEquals(width("route-casing"), width("route-cursor-casing"), 0.0)
  assertEquals(5.0, width("route-passed"), 0.0)
  assertEquals(7.5, width("route-passed-casing"), 0.0)
  assertEquals(width("route-passed"), width("route-passed-cursor"), 0.0)
  assertEquals(width("route-passed-casing"), width("route-passed-cursor-casing"), 0.0)
 }
 @Test fun routeSplitKeepsOrderedReturnCursorAndSharedEndpoint() {
  val a = Point(37.0, 127.0); val b = Point(37.0, 127.01)
  val route = NavPrototypeRoute(NavPrototypeRequest("coast", "seoul"), listOf(a,b,a), emptyList(), 2000.0, 100.0, emptyList())
  val d = route.geometryMeters * .75
  val parts = vectorRouteParts(route, d)
  assertEquals(route.positionAt(d), parts.cursor)
  assertEquals(parts.passed.last(), parts.remaining.first())
  assertEquals(a, parts.remaining.last())
  assertEquals(listOf(a,b), parts.passed.dropLast(1))
  assertEquals(a, vectorRouteParts(route, Double.NaN).cursor)
  assertEquals(a, vectorRouteParts(route, Double.POSITIVE_INFINITY).cursor)
  for (fraction in listOf(0.0, .25, .5, .75, 1.0)) {
   val distance = route.geometryMeters * fraction
   assertEquals(route.positionAt(distance), vectorRouteCursor(route, distance, navigationProgressSegment(route.cumulative, distance)))
  }
 }
}
