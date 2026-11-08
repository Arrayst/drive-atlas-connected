package com.driveatlas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) : AtlasStore {
 private val prefs = context.getSharedPreferences("atlas_real_v4", Context.MODE_PRIVATE)
 override fun load(): Snapshot = try {
  val raw = prefs.getString("snapshot", null)
  if (raw == null) Snapshot() else {
   val o = JSONObject(raw)
   val t = runCatching {
    o.optJSONObject("trip")?.let { Trip(it.getString("id"),it.getString("route"),it.getLong("start"),it.getInt("next"),it.getLong("last"), Mode.valueOf(it.optString("mode", "SIMULATOR"))) }
   }.getOrNull()
   val a = o.optJSONArray("awards")
   val awards = if (a == null) emptyList() else (0 until a.length()).mapNotNull { index ->
    runCatching { a.getJSONObject(index).let { Award(it.getString("id"),it.getString("route"),it.getBoolean("sim"),it.getLong("time")) } }.getOrNull()
   }
   val v = o.optJSONArray("visits")
   val visits = if (v == null) emptyList() else (0 until v.length()).mapNotNull { index ->
    runCatching { v.getJSONObject(index).let { Visit(it.getString("id"),it.getString("route"),it.getBoolean("sim"),it.getLong("time"),it.getInt("progress"), VisitStatus.valueOf(it.getString("status"))) } }.getOrNull()
   }
   Snapshot(t, awards, visits).validatedForRestore()
  }
 } catch (_: Exception) { Snapshot() } // Unreadable JSON fails closed; bad records cannot unlock safety.
 override fun save(snapshot: Snapshot) {
  val o = JSONObject()
  snapshot.trip?.let { o.put("trip",JSONObject().put("id",it.id).put("route",it.routeId).put("start",it.startedMs).put("next",it.nextStop).put("last",it.lastEvidenceMs).put("mode",it.mode.name)) }
  o.put("awards",JSONArray().apply { snapshot.awards.forEach { put(JSONObject().put("id",it.tripId).put("route",it.routeId).put("sim",it.simulated).put("time",it.timeMs)) } })
  o.put("visits",JSONArray().apply { snapshot.visits.forEach { put(JSONObject().put("id",it.tripId).put("route",it.routeId).put("sim",it.simulated).put("time",it.timeMs).put("progress",it.progress).put("status",it.status.name)) } })
  check(prefs.edit().putString("snapshot",o.toString()).commit()) { "Local save failed" }
 }
}
