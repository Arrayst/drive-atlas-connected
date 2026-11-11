package com.driveatlas

data class FamilyFeedback(val lessDriving:Boolean=false, val moreRest:Boolean=false, val lessWalking:Boolean=false)
object FamilyFeelings { val all=listOf("편안했어요","즐거웠어요","피곤했어요","다시 가고 싶어요") }
private fun validMemoryDetails(date:String?,tags:List<String>) =
 (date==null || (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && runCatching { java.time.LocalDate.parse(date) }.isSuccess)) &&
 tags.size<=FamilyFeelings.all.size && tags.distinct().size==tags.size && tags.all { it in FamilyFeelings.all }

/** Family preferences contain no child identities, medical information or coordinates. */
data class FamilyPreferences(
 val availableMinutes: Int = 180, val walkMinutes: Int = 20,
 val restroomRequired: Boolean = false, val strollerRequired: Boolean = false,
 val carName: String = "우리 차",
 val maxContinuousDrivingMinutes: Int? = null, val feedback: FamilyFeedback = FamilyFeedback()
) {
 fun valid() = availableMinutes in 30..720 && walkMinutes in 0..120 && carName.isNotBlank() && carName.length <= 24 && (maxContinuousDrivingMinutes==null||maxContinuousDrivingMinutes in 15..360)
}
data class FamilyPlan(val restMinutes: Int = 60, val walkMinutes: Int = 20,
 val outboundMinutes: Int? = null, val returnMinutes: Int? = null, val restFacilityId:String? = null,
 val courseDriveMinutes:Int? = null, val etaSource:String? = null, val etaOriginLabel:String? = null, val etaCalculatedAt:Long? = null, val etaWasCached:Boolean=false,
 val departureMinuteOfDay:Int?=null, val returnDeadlineMinuteOfDay:Int?=null, val returnNextDay:Boolean=false,
 val bufferMinutes:Int=0, val restFacilityIds:List<String> = emptyList(),
 val longestContinuousDrivingMinutes:Int?=null, val etaRestFacilityIds:List<String> = emptyList(), val restMinutesByFacility:Map<String,Int> = emptyMap(),
 val etaItineraryKey:String?=null, val restAfterStopIndex:Map<String,Int> = emptyMap()) {
 val effectiveRestMinutes get() = if(restMinutesByFacility.isEmpty())restMinutes else restMinutesByFacility.values.sum()
 val effectiveRestFacilityIds get() = restFacilityIds.ifEmpty { listOfNotNull(restFacilityId) }
 val scheduleWindowMinutes:Int? get() = if(departureMinuteOfDay==null || returnDeadlineMinuteOfDay==null)null else
  returnDeadlineMinuteOfDay + (if(returnNextDay)1440 else 0) - departureMinuteOfDay
 fun valid() = restMinutes in 0..300 && walkMinutes in 0..120 &&
  (outboundMinutes == null || outboundMinutes in 0..720) && (returnMinutes == null || returnMinutes in 0..720) &&
  (courseDriveMinutes==null||courseDriveMinutes in 0..720) && (etaSource==null||etaSource=="OSRM") &&
  (etaOriginLabel==null||etaOriginLabel.length in 1..80) && (etaCalculatedAt==null||etaCalculatedAt>0) && bufferMinutes in 0..300 &&
  ((departureMinuteOfDay==null && returnDeadlineMinuteOfDay==null && !returnNextDay) ||
   (departureMinuteOfDay in 0..1439 && returnDeadlineMinuteOfDay in 0..1439 && (scheduleWindowMinutes?:-1) in 0..1440)) &&
  restFacilityIds.size<=20 && restFacilityIds.distinct().size==restFacilityIds.size && (longestContinuousDrivingMinutes==null||longestContinuousDrivingMinutes in 0..2160) && restMinutesByFacility.keys.all { it in effectiveRestFacilityIds } && restMinutesByFacility.values.all { it in 0..300 } && effectiveRestMinutes in 0..300 && restAfterStopIndex.keys.all { it in effectiveRestFacilityIds } && restAfterStopIndex.values.all { it>=-1 }
 val transferKnown get() = outboundMinutes != null && returnMinutes != null
}
data class FamilyMemory(val id: String, val routeId: String, val carName: String, val rating: Int,
 val note: String, val createdMs: Long, val simulated: Boolean = false,
 val tripDate:String?=null, val feelingTags:List<String> = emptyList())
data class FamilySnapshot(val preferences: FamilyPreferences = FamilyPreferences(),
 val plans: Map<String,FamilyPlan> = emptyMap(), val memories: List<FamilyMemory> = emptyList())
interface FamilyRepository { fun loadFamily(): FamilySnapshot; fun saveFamily(snapshot: FamilySnapshot) }
data class FamilyMatch(val route: Route, val plan: FamilyPlan, val totalMinutes: Int,
 val conflicts: List<String>, val unknowns: List<String>, val previousRating: Double?,
 val feedbackReasons:List<String> = emptyList(), val feedbackPenalty:Int=0) {
 val matches get() = conflicts.isEmpty() && unknowns.isEmpty()
}
fun FamilySnapshot.validated(): FamilySnapshot = copy(
 preferences = preferences.takeIf { it.valid() } ?: FamilyPreferences(),
 plans = plans.mapValues { (id,v) -> if(v.etaSource!=null && (v.etaRestFacilityIds!=v.effectiveRestFacilityIds || v.etaItineraryKey!=itineraryIdentity(id,v.effectiveRestFacilityIds,v.restAfterStopIndex))) manualTransfers(v,outbound=null,back=null) else v }.filter { (id,plan) -> Catalog.routes.any { it.id==id } && plan.valid() && plan.effectiveRestFacilityIds.all { RestFacilities.find(it,id)!=null } && plan.restAfterStopIndex.values.all { it<Catalog.routes.first { r -> r.id==id }.stops.size } },
 memories = memories.filter { m -> m.id.isNotBlank() && Catalog.routes.any { it.id==m.routeId } &&
  m.carName.isNotBlank() && m.carName.length<=24 && m.rating in 1..5 && m.note.length<=280 && m.createdMs>0 }
  .distinctBy { it.id }.map { it.copy(tripDate=it.tripDate.takeIf { d -> validMemoryDetails(d,emptyList()) }, feelingTags=it.feelingTags.filter { t -> t in FamilyFeelings.all }.distinct()) }
)
/** Deterministic, explainable local ranking. Facility unknowns never become positive claims. */
fun rankFamilyRoutes(snapshot: FamilySnapshot): List<FamilyMatch> {
 val s=snapshot.validated(); val p=s.preferences
 return Catalog.routes.map { r ->
  val plan=s.plans[r.id]?:FamilyPlan(); val total=(plan.courseDriveMinutes?:r.minutes)+plan.effectiveRestMinutes+plan.walkMinutes+(plan.outboundMinutes?:0)+(plan.returnMinutes?:0)+plan.bufferMinutes
  val facilities=plan.effectiveRestFacilityIds.mapNotNull { RestFacilities.find(it,r.id) }
  val driving=(plan.courseDriveMinutes?:r.minutes)+(plan.outboundMinutes?:0)+(plan.returnMinutes?:0)
  val conflicts=buildList { if(total>p.availableMinutes)add("왕복 포함 계획 시간이 여유 시간을 넘어요")
   if(plan.walkMinutes>p.walkMinutes)add("계획한 걷기 시간이 가족 조건을 넘어요")
   if(p.maxContinuousDrivingMinutes!=null && plan.longestContinuousDrivingMinutes!=null && plan.longestContinuousDrivingMinutes>p.maxContinuousDrivingMinutes)add("연속 운전 시간이 가족 조건을 넘어요")
   if(plan.scheduleWindowMinutes!=null && total>plan.scheduleWindowMinutes!!)add("여유 시간 포함 계획이 복귀 마감 시간을 넘어요") }
  val unknown=buildList {
   if(plan.outboundMinutes==null)add("출발지→코스 시간 입력 필요")
   if(plan.returnMinutes==null)add("코스→복귀지 시간 입력 필요")
   if(plan.effectiveRestFacilityIds.any { it !in plan.restMinutesByFacility })add("선택 휴식지별 휴식 시간 입력 필요")
   if(plan.effectiveRestFacilityIds.isNotEmpty() && plan.courseDriveMinutes==null)add("휴식 경유지를 포함한 코스 운전 시간 확인 필요")
   if(p.maxContinuousDrivingMinutes!=null && plan.longestContinuousDrivingMinutes==null && (!plan.transferKnown || driving>p.maxContinuousDrivingMinutes))add("연속 운전 한도 확인을 위한 휴식지 사이 구간별 운전 시간 필요")
   if(p.restroomRequired&&(facilities.isEmpty()||facilities.any { it.restroom!=true }))add("선택 휴식지 화장실 정보 확인 필요")
   if(p.strollerRequired&&(facilities.isEmpty()||facilities.any { it.stroller!=true }))add("선택 휴식지 유모차 접근성 확인 필요") }
  val ratings=s.memories.filter { it.routeId==r.id && !it.simulated }.map { it.rating }
  val reasons=buildList {
   if(p.feedback.lessDriving)add("운전 줄이기 반영: 알려진 운전 $driving 분${if(!plan.transferKnown) ", 왕복 일부 미확인" else ""}")
   if(p.feedback.moreRest)add("휴식 늘리기 반영: 휴식 ${plan.effectiveRestMinutes}분")
   if(p.feedback.lessWalking)add("걷기 줄이기 반영: 걷기 ${plan.walkMinutes}분")
  }
  val penalty=(if(p.feedback.lessDriving)driving else 0)-(if(p.feedback.moreRest)plan.effectiveRestMinutes else 0)+(if(p.feedback.lessWalking)plan.walkMinutes else 0)
  FamilyMatch(r,plan,total,conflicts,unknown,ratings.takeIf { it.isNotEmpty() }?.average(),reasons,penalty)
 }.sortedWith(compareBy<FamilyMatch> { it.conflicts.size+it.unknowns.size }
  .thenBy { it.feedbackPenalty }.thenByDescending { it.previousRating?:3.0 }.thenBy { it.totalMinutes }.thenBy { it.route.id })
}
class FamilyEngine(private val repository: FamilyRepository) {
 var snapshot=repository.loadFamily().validated(); private set
 fun preferences(value: FamilyPreferences, safety: Safety): Boolean {
  if(safety!=Safety.PARKED||!value.valid())return false
  update(snapshot.copy(preferences=value)); return true
 }
 fun plan(routeId: String,value: FamilyPlan,safety: Safety): Boolean {
  if(safety!=Safety.PARKED||!value.valid()||Catalog.routes.none { it.id==routeId }||value.effectiveRestFacilityIds.any { RestFacilities.find(it,routeId)==null }||value.restAfterStopIndex.values.any { it>=Catalog.routes.first { r -> r.id==routeId }.stops.size })return false
  val prior=snapshot.plans[routeId]
  val itineraryChanged=prior!=null && (prior.effectiveRestFacilityIds!=value.effectiveRestFacilityIds || prior.restAfterStopIndex!=value.restAfterStopIndex)
  val saved=if(itineraryChanged)manualTransfers(value,outbound=null,back=null)
   else if(prior!=null && prior.restMinutesByFacility!=value.restMinutesByFacility)value.copy(longestContinuousDrivingMinutes=null)
   else if(value.etaSource!=null && value.etaRestFacilityIds!=value.effectiveRestFacilityIds)manualTransfers(value,outbound=null,back=null) else value
  update(snapshot.copy(plans=snapshot.plans+(routeId to saved)));return true
 }
 fun remember(id: String,routeId: String,rating: Int,note: String,now: Long,safety: Safety,simulated: Boolean=false,tripDate:String?=null,feelingTags:List<String> = emptyList()): Boolean {
  if(!validMemoryDetails(tripDate,feelingTags)||snapshot.memories.size>=500||safety!=Safety.PARKED||id.isBlank()||snapshot.memories.any{it.id==id}||Catalog.routes.none{it.id==routeId}||rating !in 1..5||note.length>280||now<=0)return false
  update(snapshot.copy(memories=(snapshot.memories+FamilyMemory(id,routeId,snapshot.preferences.carName,rating,note,now,simulated,tripDate,feelingTags))));return true
 }
 fun editMemory(id: String,rating: Int,note: String,safety: Safety,
  tripDate:String?=snapshot.memories.firstOrNull { it.id==id }?.tripDate,
  feelingTags:List<String> = snapshot.memories.firstOrNull { it.id==id }?.feelingTags?:emptyList()): Boolean {
  if(!validMemoryDetails(tripDate,feelingTags)||safety!=Safety.PARKED||rating !in 1..5||note.length>280||snapshot.memories.none{it.id==id})return false
  update(snapshot.copy(memories=snapshot.memories.map{if(it.id==id)it.copy(rating=rating,note=note,tripDate=tripDate,feelingTags=feelingTags) else it}));return true
 }
 fun deleteMemory(id: String,safety: Safety): Boolean {
  if(safety!=Safety.PARKED||snapshot.memories.none{it.id==id})return false
  update(snapshot.copy(memories=snapshot.memories.filterNot{it.id==id}));return true
 }
 private fun update(value: FamilySnapshot) { repository.saveFamily(value);snapshot=value }
}

internal fun withEta(plan:FamilyPlan,value:EtaEstimate):FamilyPlan {
 require(value.valid())
 require(plan.effectiveRestFacilityIds==value.selectedRestIds && plan.restAfterStopIndex==value.restAfterStopIndex) { "ETA rest itinerary changed" }
 return plan.copy(outboundMinutes=value.outboundMinutes,returnMinutes=value.returnMinutes,courseDriveMinutes=value.courseMinutes,
  etaSource="OSRM",etaOriginLabel=value.originLabel,etaCalculatedAt=value.calculatedAt,etaWasCached=value.cached,longestContinuousDrivingMinutes=continuousDrivingFor(plan,value),etaRestFacilityIds=value.selectedRestIds,etaItineraryKey=value.itineraryKey)
}
internal fun manualTransfers(plan:FamilyPlan,outbound:Int?=plan.outboundMinutes,back:Int?=plan.returnMinutes)=
 plan.copy(outboundMinutes=outbound,returnMinutes=back,courseDriveMinutes=null,etaSource=null,etaOriginLabel=null,etaCalculatedAt=null,etaWasCached=false,longestContinuousDrivingMinutes=null,etaRestFacilityIds=emptyList(),etaItineraryKey=null)

private fun continuousDrivingFor(plan:FamilyPlan,value:EtaEstimate):Int? {
 if(value.legSeconds.isEmpty() || plan.effectiveRestFacilityIds.any { it !in plan.restMinutesByFacility })return null
 val breakLegs=value.restBreakAfterLegs.zip(value.selectedRestIds).filter { (_,id) -> (plan.restMinutesByFacility[id]?:0)>0 }.map { it.first }.toSet()
 var current=0.0;var longest=0.0
 value.legSeconds.forEachIndexed { index, seconds -> current+=seconds;longest=maxOf(longest,current);if(index in breakLegs)current=0.0 }
 return kotlin.math.ceil(longest/60.0).toInt()
}
