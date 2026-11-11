package com.driveatlas

/** Family preferences contain no child identities, medical information or coordinates. */
data class FamilyPreferences(
 val availableMinutes: Int = 180, val walkMinutes: Int = 20,
 val restroomRequired: Boolean = false, val strollerRequired: Boolean = false,
 val carName: String = "우리 차"
) {
 fun valid() = availableMinutes in 30..720 && walkMinutes in 0..120 && carName.isNotBlank() && carName.length <= 24
}
data class FamilyPlan(val restMinutes: Int = 60, val walkMinutes: Int = 20,
 val outboundMinutes: Int? = null, val returnMinutes: Int? = null, val restFacilityId:String? = null) {
 fun valid() = restMinutes in 0..300 && walkMinutes in 0..120 &&
  (outboundMinutes == null || outboundMinutes in 0..720) && (returnMinutes == null || returnMinutes in 0..720)
 val transferKnown get() = outboundMinutes != null && returnMinutes != null
}
data class FamilyMemory(val id: String, val routeId: String, val carName: String, val rating: Int,
 val note: String, val createdMs: Long, val simulated: Boolean = false)
data class FamilySnapshot(val preferences: FamilyPreferences = FamilyPreferences(),
 val plans: Map<String,FamilyPlan> = emptyMap(), val memories: List<FamilyMemory> = emptyList())
interface FamilyRepository { fun loadFamily(): FamilySnapshot; fun saveFamily(snapshot: FamilySnapshot) }
data class FamilyMatch(val route: Route, val plan: FamilyPlan, val totalMinutes: Int,
 val conflicts: List<String>, val unknowns: List<String>, val previousRating: Double?) {
 val matches get() = conflicts.isEmpty() && unknowns.isEmpty()
}
fun FamilySnapshot.validated(): FamilySnapshot = copy(
 preferences = preferences.takeIf { it.valid() } ?: FamilyPreferences(),
 plans = plans.filter { (id,plan) -> Catalog.routes.any { it.id==id } && plan.valid() && (plan.restFacilityId==null||RestFacilities.find(plan.restFacilityId,id)!=null) },
 memories = memories.filter { m -> m.id.isNotBlank() && Catalog.routes.any { it.id==m.routeId } &&
  m.carName.isNotBlank() && m.carName.length<=24 && m.rating in 1..5 && m.note.length<=280 && m.createdMs>0 }
  .distinctBy { it.id }
)
/** Deterministic, explainable local ranking. Facility unknowns never become positive claims. */
fun rankFamilyRoutes(snapshot: FamilySnapshot): List<FamilyMatch> {
 val s=snapshot.validated(); val p=s.preferences
 return Catalog.routes.map { r ->
  val plan=s.plans[r.id]?:FamilyPlan(); val total=r.minutes+plan.restMinutes+plan.walkMinutes+(plan.outboundMinutes?:0)+(plan.returnMinutes?:0)
  val facility=RestFacilities.find(plan.restFacilityId,r.id)
  val conflicts=buildList { if(total>p.availableMinutes)add("왕복 포함 계획 시간이 여유 시간을 넘어요")
   if(plan.walkMinutes>p.walkMinutes)add("계획한 걷기 시간이 가족 조건을 넘어요") }
  val unknown=buildList {
   if(plan.outboundMinutes==null)add("출발지→코스 시간 입력 필요")
   if(plan.returnMinutes==null)add("코스→복귀지 시간 입력 필요")
   if(p.restroomRequired&&facility?.restroom!=true)add("선택 휴식지 화장실 정보 확인 필요")
   if(p.strollerRequired&&facility?.stroller!=true)add("선택 휴식지 유모차 접근성 확인 필요") }
  val ratings=s.memories.filter { it.routeId==r.id && !it.simulated }.map { it.rating }
  FamilyMatch(r,plan,total,conflicts,unknown,ratings.takeIf { it.isNotEmpty() }?.average())
 }.sortedWith(compareBy<FamilyMatch> { it.conflicts.size+it.unknowns.size }
  .thenByDescending { it.previousRating?:3.0 }.thenBy { it.totalMinutes }.thenBy { it.route.id })
}
class FamilyEngine(private val repository: FamilyRepository) {
 var snapshot=repository.loadFamily().validated(); private set
 fun preferences(value: FamilyPreferences, safety: Safety): Boolean {
  if(safety!=Safety.PARKED||!value.valid())return false
  update(snapshot.copy(preferences=value)); return true
 }
 fun plan(routeId: String,value: FamilyPlan,safety: Safety): Boolean {
  if(safety!=Safety.PARKED||!value.valid()||Catalog.routes.none { it.id==routeId }||(value.restFacilityId!=null&&RestFacilities.find(value.restFacilityId,routeId)==null))return false
  update(snapshot.copy(plans=snapshot.plans+(routeId to value)));return true
 }
 fun remember(id: String,routeId: String,rating: Int,note: String,now: Long,safety: Safety,simulated: Boolean=false): Boolean {
  if(snapshot.memories.size>=500||safety!=Safety.PARKED||id.isBlank()||snapshot.memories.any{it.id==id}||Catalog.routes.none{it.id==routeId}||rating !in 1..5||note.length>280||now<=0)return false
  update(snapshot.copy(memories=(snapshot.memories+FamilyMemory(id,routeId,snapshot.preferences.carName,rating,note,now,simulated))));return true
 }
 fun editMemory(id: String,rating: Int,note: String,safety: Safety): Boolean {
  if(safety!=Safety.PARKED||rating !in 1..5||note.length>280||snapshot.memories.none{it.id==id})return false
  update(snapshot.copy(memories=snapshot.memories.map{if(it.id==id)it.copy(rating=rating,note=note) else it}));return true
 }
 fun deleteMemory(id: String,safety: Safety): Boolean {
  if(safety!=Safety.PARKED||snapshot.memories.none{it.id==id})return false
  update(snapshot.copy(memories=snapshot.memories.filterNot{it.id==id}));return true
 }
 private fun update(value: FamilySnapshot) { repository.saveFamily(value);snapshot=value }
}
