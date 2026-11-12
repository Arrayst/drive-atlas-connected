package com.driveatlas

import org.json.JSONArray
import org.json.JSONObject

/** Original automotive style for the public OpenMapTiles schema. No provider style is copied. */
internal object NavigationVectorStyle {
 const val tileJson = "https://tiles.openfreemap.org/planet/latest"
 const val glyphs = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"
 const val terms = "https://openfreemap.org/tos/"
 const val privacy = "https://openfreemap.org/privacy/"
 val roadLayers = arrayOf("road-local", "road-arterial", "road-motorway")
 val routeLayers = arrayOf("route-passed-casing", "route-passed", "route-passed-cursor-casing", "route-passed-cursor", "route-casing", "route", "route-cursor-casing", "route-cursor", "route-arrows", "references", "sim-position")
 fun allowed(debug: Boolean, simulator: Boolean, consent: Boolean) = vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, debug, simulator, consent)
 fun json(): String {
  val sources = JSONObject().put("openmaptiles", JSONObject().put("type", "vector").put("url", tileJson))
  for (id in listOf("route-passed", "route-remaining", "route-passed-cursor", "route-remaining-cursor", "references", "sim-position"))
   sources.put(id, JSONObject().put("type", "geojson").put("data", JSONObject().put("type", "FeatureCollection").put("features", JSONArray())))
  val layers = JSONArray()
  fun layer(id: String, type: String, sourceLayer: String? = null, paint: String = "{}", layout: String = "{}", filter: String? = null, minZoom: Int? = null, source: String? = null) {
   val l = JSONObject().put("id", id).put("type", type).put("paint", JSONObject(paint)).put("layout", JSONObject(layout))
   if (sourceLayer != null) l.put("source", "openmaptiles").put("source-layer", sourceLayer)
   if (source != null) { l.put("source", source); l.getJSONObject("layout").put("visibility", "none") }
   if (filter != null) l.put("filter", JSONArray(filter))
   if (minZoom != null) l.put("minzoom", minZoom)
   layers.put(l)
  }
  layer("background", "background", paint = """{"background-color":"#EEF0EF"}""")
  layer("woodland", "fill", "landcover", """{"fill-color":"#DDE8D8","fill-opacity":0.85}""", filter = """["in","class","wood","grass"]""")
  layer("park", "fill", "park", """{"fill-color":"#DDE9D7","fill-opacity":0.85}""")
  layer("water", "fill", "water", """{"fill-color":"#BBD8E4"}""")
  layer("waterway", "line", "waterway", """{"line-color":"#BBD8E4","line-width":["interpolate",["linear"],["zoom"],12,1,18,7]}""", minZoom = 12)
  // Real footprints only. No inferred building heights or synthetic roads.
  layer("buildings", "fill", "building", """{"fill-color":"#D8DCDC","fill-outline-color":"#CDD2D3"}""", minZoom = 14)
  val round = """{"line-cap":"round","line-join":"round"}"""
  val locals = """["in","class","minor","service","tertiary"]"""
  val arterials = """["in","class","primary","secondary"]"""
  val motorways = """["in","class","motorway","trunk"]"""
  fun road(id: String, filter: String, color: String, z12: Double, z16: Double, z18: Double, casing: Boolean) {
   val extra = if (casing) 2.0 else 0.0
   layer(id, "line", "transportation", """{"line-color":"$color","line-width":["interpolate",["exponential",1.3],["zoom"],12,${z12 + extra},16,${z16 + extra},18,${z18 + extra}]}""", round, filter, if (filter == locals) 12 else 5)
  }
  road("road-local-casing", locals, "#D2D7D8", 0.6, 6.5, 18.0, true)
  road("road-local", locals, "#FFFFFF", 0.6, 6.5, 18.0, false)
  road("road-arterial-casing", arterials, "#D2CDBA", 1.2, 10.0, 25.0, true)
  road("road-arterial", arterials, "#FFF2C6", 1.2, 10.0, 25.0, false)
  road("road-motorway-casing", motorways, "#D7C696", 2.0, 13.0, 32.0, true)
  road("road-motorway", motorways, "#F8E4AB", 2.0, 13.0, 32.0, false)
  val name = """["case",["!=",["coalesce",["get","name:ko"],""],""],["get","name:ko"],["!=",["coalesce",["get","name"],""],""],["get","name"],["coalesce",["get","name:en"],""]]"""
  layer("road-labels", "symbol", "transportation_name", """{"text-color":"#5A6269","text-halo-color":"#FFFFFF","text-halo-width":1.8}""",
   """{"symbol-placement":"line","symbol-spacing":320,"text-field":$name,"text-font":["Noto Sans Regular"],"text-size":["interpolate",["linear"],["zoom"],13,12,18,17],"text-max-angle":30,"text-padding":7}""",
   """["in","class","motorway","trunk","primary","secondary","tertiary","minor"]""", 13)
  layer("district-labels", "symbol", "place", """{"text-color":"#6B7479","text-halo-color":"#F5F6F4","text-halo-width":2}""",
   """{"text-field":$name,"text-font":["Noto Sans Regular"],"text-size":["interpolate",["linear"],["zoom"],9,13,16,19],"text-padding":18,"text-max-width":8}""",
   """["in","class","city","town","suburb","quarter","neighbourhood"]""", 9)
  fun route(id: String, source: String, color: String, width: Double) = layer(id, "line", paint = """{"line-color":"$color","line-width":$width}""", layout = round, source = source)
  route("route-passed-casing", "route-passed", "#FFFFFF", 7.5)
  route("route-passed", "route-passed", "#8C9AA7", 5.0)
  route("route-passed-cursor-casing", "route-passed-cursor", "#FFFFFF", 7.5)
  route("route-passed-cursor", "route-passed-cursor", "#8C9AA7", 5.0)
  route("route-casing", "route-remaining", "#FFFFFF", 14.0)
  route("route", "route-remaining", "#1769F5", 11.0)
  route("route-cursor-casing", "route-remaining-cursor", "#FFFFFF", 14.0)
  route("route-cursor", "route-remaining-cursor", "#1769F5", 11.0)
  layer("route-arrows", "symbol", layout = """{"symbol-placement":"line","symbol-spacing":160,"icon-image":"route-chevron","icon-size":0.72,"icon-rotation-alignment":"map","icon-pitch-alignment":"map","icon-allow-overlap":false,"icon-ignore-placement":true,"icon-keep-upright":false}""", minZoom = 15, source = "route-remaining")
  layer("references", "symbol", layout = """{"icon-image":["get","icon"],"icon-size":0.82,"icon-allow-overlap":false,"icon-padding":12,"icon-pitch-alignment":"viewport"}""", source = "references")
  layer("sim-position", "symbol", layout = """{"icon-image":"sim-car","icon-size":0.90,"icon-rotate":["get","heading"],"icon-rotation-alignment":"map","icon-pitch-alignment":"viewport","icon-allow-overlap":true,"icon-ignore-placement":true}""", source = "sim-position")
  return JSONObject().put("version", 8).put("name", "Drive Atlas / public demo navigation").put("glyphs", glyphs).put("sources", sources).put("layers", layers).toString()
 }
}

internal data class VectorRouteParts(val passed: List<Point>, val remaining: List<Point>, val cursor: Point)
/** Same ordered OSRM cursor powers the line split, car, heading and camera, including return roads. */
internal fun vectorRouteParts(route: NavPrototypeRoute, distance: Double): VectorRouteParts {
 val d = if (distance.isFinite()) distance.coerceIn(0.0, route.geometryMeters) else 0.0
 val segment = navigationProgressSegment(route.cumulative, d)
 val cursor = route.positionAt(d)
 return VectorRouteParts(route.geometry.take(segment + 1) + cursor, listOf(cursor) + route.geometry.drop(segment + 1), cursor)
}
internal fun navigationFollowTopPadding(height: Int) = (height * 0.50).toInt().coerceAtLeast(0)
internal fun navigationFollowZoom() = 17.8
/** Constant-time interpolation after the binary-search segment lookup. */
internal fun vectorRouteCursor(route: NavPrototypeRoute, distance: Double, segment: Int): Point {
 val d = if (distance.isFinite()) distance.coerceIn(0.0, route.geometryMeters) else 0.0
 val span = route.cumulative[segment + 1] - route.cumulative[segment]
 val t = if (span > 0) ((d - route.cumulative[segment]) / span).coerceIn(0.0, 1.0) else 0.0
 val a = route.geometry[segment]; val b = route.geometry[segment + 1]
 return Point(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
}
