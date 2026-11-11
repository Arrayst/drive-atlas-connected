package com.driveatlas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FamilyStore(context: Context): FamilyRepository {
 private val prefs=context.getSharedPreferences("atlas_family_v1",Context.MODE_PRIVATE)
 override fun loadFamily(): FamilySnapshot = runCatching {
  val o=JSONObject(prefs.getString("snapshot","{}")?:"{}")
  val p=o.optJSONObject("preferences")?:JSONObject()
  val preferences=FamilyPreferences(p.optInt("minutes",180),p.optInt("walk",20),p.optBoolean("restroom"),p.optBoolean("stroller"),p.optString("car","우리 차"))
  val plans=mutableMapOf<String,FamilyPlan>()
  o.optJSONObject("plans")?.let{ all -> all.keys().forEach{ id -> runCatching {all.getJSONObject(id).let{plans[id]=FamilyPlan(it.getInt("rest"),it.getInt("walk"),if(it.isNull("outbound"))null else it.getInt("outbound"),if(it.isNull("return"))null else it.getInt("return"),if(it.isNull("facility"))null else it.getString("facility"),
 if(it.isNull("courseDrive"))null else it.getInt("courseDrive"),if(it.isNull("etaSource"))null else it.getString("etaSource"),if(it.isNull("etaOrigin"))null else it.getString("etaOrigin"),if(it.isNull("etaAt"))null else it.getLong("etaAt"),it.optBoolean("etaCached",false))}}} }
  val a=o.optJSONArray("memories")?:JSONArray()
  val memories=(0 until a.length()).mapNotNull {i->runCatching {a.getJSONObject(i).let{FamilyMemory(it.getString("id"),it.getString("route"),it.getString("car"),it.getInt("rating"),it.getString("note"),it.getLong("time"),it.optBoolean("simulated",false))}}.getOrNull()}
  FamilySnapshot(preferences,plans,memories).validated()
 }.getOrDefault(FamilySnapshot())
 override fun saveFamily(snapshot: FamilySnapshot) {
  val s=snapshot.validated();val p=s.preferences
  val o=JSONObject().put("preferences",JSONObject().put("minutes",p.availableMinutes).put("walk",p.walkMinutes).put("restroom",p.restroomRequired).put("stroller",p.strollerRequired).put("car",p.carName))
  o.put("plans",JSONObject().apply{s.plans.forEach{(id,v)->put(id,JSONObject().put("rest",v.restMinutes).put("walk",v.walkMinutes).put("outbound",v.outboundMinutes?:JSONObject.NULL).put("return",v.returnMinutes?:JSONObject.NULL).put("facility",v.restFacilityId?:JSONObject.NULL).put("courseDrive",v.courseDriveMinutes?:JSONObject.NULL).put("etaSource",v.etaSource?:JSONObject.NULL).put("etaOrigin",v.etaOriginLabel?:JSONObject.NULL).put("etaAt",v.etaCalculatedAt?:JSONObject.NULL).put("etaCached",v.etaWasCached))}})
  o.put("memories",JSONArray().apply{s.memories.forEach{m->put(JSONObject().put("id",m.id).put("route",m.routeId).put("car",m.carName).put("rating",m.rating).put("note",m.note).put("time",m.createdMs).put("simulated",m.simulated))}})
  check(prefs.edit().putString("snapshot",o.toString()).commit()){ "Family save failed" }
 }
}
