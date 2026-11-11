package com.driveatlas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FamilyStore(context: Context): FamilyRepository {
 private val prefs=context.getSharedPreferences("atlas_family_v1",Context.MODE_PRIVATE)
 override fun loadFamily(): FamilySnapshot = decodeFamilySnapshot(prefs.getString("snapshot","{}")?:"{}")
 override fun saveFamily(snapshot: FamilySnapshot) {
  check(prefs.edit().putString("snapshot",encodeFamilySnapshot(snapshot)).commit()){ "Family save failed" }
 }
}
internal fun decodeFamilySnapshot(raw:String):FamilySnapshot = runCatching {
  val o=JSONObject(raw)
  val p=o.optJSONObject("preferences")?:JSONObject()
  val preferences=FamilyPreferences(p.optInt("minutes",180),p.optInt("walk",20),p.optBoolean("restroom"),p.optBoolean("stroller"),p.optString("car","우리 차"), if(p.isNull("maxContinuous"))null else p.getInt("maxContinuous"),
   (p.optJSONObject("feedback")?:JSONObject()).let { FamilyFeedback(it.optBoolean("lessDriving"),it.optBoolean("moreRest"),it.optBoolean("lessWalking")) })
  val plans=mutableMapOf<String,FamilyPlan>()
  o.optJSONObject("plans")?.let{ all -> all.keys().forEach{ id -> runCatching {all.getJSONObject(id).let{plans[id]=FamilyPlan(it.getInt("rest"),it.getInt("walk"),if(it.isNull("outbound"))null else it.getInt("outbound"),if(it.isNull("return"))null else it.getInt("return"),if(it.isNull("facility"))null else it.getString("facility"),
 if(it.isNull("courseDrive"))null else it.getInt("courseDrive"),if(it.isNull("etaSource"))null else it.getString("etaSource"),if(it.isNull("etaOrigin"))null else it.getString("etaOrigin"),if(it.isNull("etaAt"))null else it.getLong("etaAt"),it.optBoolean("etaCached",false),
 if(it.isNull("departure"))null else it.getInt("departure"),if(it.isNull("deadline"))null else it.getInt("deadline"),it.optBoolean("nextDay"),it.optInt("buffer",0),
 it.optJSONArray("facilities")?.let { a -> (0 until a.length()).map { n -> a.getString(n) } }?:listOfNotNull(if(it.isNull("facility"))null else it.getString("facility")),
 if(it.isNull("longestDrive"))null else it.getInt("longestDrive"),it.optJSONArray("etaFacilities")?.let { a -> (0 until a.length()).map { n -> a.getString(n) } }?:emptyList(),
 it.optJSONObject("restDurations")?.let { d -> d.keys().asSequence().associateWith { key -> d.getInt(key) } }?:emptyMap(), if(it.isNull("itineraryKey"))null else it.getString("itineraryKey"),it.optJSONObject("restPositions")?.let { d -> d.keys().asSequence().associateWith { key -> d.getInt(key) } }?:emptyMap())}}} }
  val a=o.optJSONArray("memories")?:JSONArray()
  val memories=(0 until a.length()).mapNotNull {i->runCatching {a.getJSONObject(i).let{FamilyMemory(it.getString("id"),it.getString("route"),it.getString("car"),it.getInt("rating"),it.getString("note"),it.getLong("time"),it.optBoolean("simulated",false),if(it.isNull("tripDate"))null else it.getString("tripDate"),it.optJSONArray("feelings")?.let { a -> (0 until a.length()).map { n -> a.getString(n) } }?:emptyList())}}.getOrNull()}
  FamilySnapshot(preferences,plans,memories).validated()
 }.getOrDefault(FamilySnapshot())
 internal fun encodeFamilySnapshot(snapshot: FamilySnapshot):String {
  val s=snapshot.validated();val p=s.preferences
  val o=JSONObject().put("preferences",JSONObject().put("minutes",p.availableMinutes).put("walk",p.walkMinutes).put("restroom",p.restroomRequired).put("stroller",p.strollerRequired).put("car",p.carName).put("maxContinuous",p.maxContinuousDrivingMinutes?:JSONObject.NULL).put("feedback",JSONObject().put("lessDriving",p.feedback.lessDriving).put("moreRest",p.feedback.moreRest).put("lessWalking",p.feedback.lessWalking)))
  o.put("plans",JSONObject().apply{s.plans.forEach{(id,v)->put(id,JSONObject().put("rest",v.restMinutes).put("walk",v.walkMinutes).put("outbound",v.outboundMinutes?:JSONObject.NULL).put("return",v.returnMinutes?:JSONObject.NULL).put("facility",v.restFacilityId?:JSONObject.NULL).put("courseDrive",v.courseDriveMinutes?:JSONObject.NULL).put("etaSource",v.etaSource?:JSONObject.NULL).put("etaOrigin",v.etaOriginLabel?:JSONObject.NULL).put("etaAt",v.etaCalculatedAt?:JSONObject.NULL).put("etaCached",v.etaWasCached).put("departure",v.departureMinuteOfDay?:JSONObject.NULL).put("deadline",v.returnDeadlineMinuteOfDay?:JSONObject.NULL).put("nextDay",v.returnNextDay).put("buffer",v.bufferMinutes).put("facilities",JSONArray(v.effectiveRestFacilityIds)).put("longestDrive",v.longestContinuousDrivingMinutes?:JSONObject.NULL).put("etaFacilities",JSONArray(v.etaRestFacilityIds)).put("restDurations",JSONObject(v.restMinutesByFacility)).put("itineraryKey",v.etaItineraryKey?:JSONObject.NULL).put("restPositions",JSONObject(v.restAfterStopIndex)))}})
  o.put("memories",JSONArray().apply{s.memories.forEach{m->put(JSONObject().put("id",m.id).put("route",m.routeId).put("car",m.carName).put("rating",m.rating).put("note",m.note).put("time",m.createdMs).put("simulated",m.simulated).put("tripDate",m.tripDate?:JSONObject.NULL).put("feelings",JSONArray(m.feelingTags)))}})
  return o.toString()
 }
