package com.driveatlas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) : AtlasStore {
 private val prefs = context.getSharedPreferences("atlas_v1", Context.MODE_PRIVATE)
 override fun load(): Snapshot = try {
  val raw = prefs.getString("snapshot", null)
  if (raw == null) Snapshot() else {
   val o = JSONObject(raw)
   val t = o.optJSONObject("trip")?.let { Trip(it.getString("id"),it.getString("route"),it.getLong("start"),it.getInt("next"),it.getLong("last"), Mode.valueOf(it.optString("mode", "SIMULATOR"))) }
   require(t == null || (Catalog.routes.any { it.id == t.routeId } && t.nextStop in 0..3))
   val a = o.getJSONArray("awards")
   Snapshot(t, (0 until a.length()).map { a.getJSONObject(it) }.map { Award(it.getString("id"),it.getString("route"),it.getBoolean("sim"),it.getLong("time")) })
  }
 } catch (_: Exception) { Snapshot() } // Corrupt records never create awards or unlock safety.
 override fun save(snapshot: Snapshot) {
  val o = JSONObject()
  snapshot.trip?.let { o.put("trip",JSONObject().put("id",it.id).put("route",it.routeId).put("start",it.startedMs).put("next",it.nextStop).put("last",it.lastEvidenceMs).put("mode",it.mode.name)) }
  o.put("awards",JSONArray().apply { snapshot.awards.forEach { put(JSONObject().put("id",it.tripId).put("route",it.routeId).put("sim",it.simulated).put("time",it.timeMs)) } })
  check(prefs.edit().putString("snapshot",o.toString()).commit()) { "Local save failed" }
 }
}
