package com.driveatlas

import java.time.LocalDate
import java.util.UUID
import androidx.compose.runtime.saveable.mapSaver

internal enum class FamilyMemoryKind(val label: String, val simulated: Boolean) {
 ACTUAL("실제 여행", false),
 SIM("SIM · 시연", true)
}

/** An unsaved response. Nothing is inferred from a course, clock or previous response. */
internal data class FamilyMemoryDraft(
 val id: String = UUID.randomUUID().toString(),
 val routeId: String? = null,
 val kind: FamilyMemoryKind? = null,
 val rating: Int? = null,
 val note: String = "",
 val tripDate: String = "",
 val feelingTags: List<String> = emptyList()
) {
 val requiredChoicesComplete get() = Catalog.routes.any { it.id == routeId } && kind != null && rating in 1..5
 val detailsValid get() = validMemoryDate(tripDate) && note.length <= 280 &&
  feelingTags.distinct().size == feelingTags.size && feelingTags.all { it in FamilyFeelings.all }

 fun canSave(memoryCount: Int, editing: Boolean = false) =
  id.isNotBlank() && requiredChoicesComplete && detailsValid && (editing || memoryCount < 500)

 companion object {
  fun from(memory: FamilyMemory) = FamilyMemoryDraft(
   id = memory.id,
   routeId = memory.routeId,
   kind = if (memory.simulated) FamilyMemoryKind.SIM else FamilyMemoryKind.ACTUAL,
   rating = memory.rating,
   note = memory.note,
   tripDate = memory.tripDate.orEmpty(),
   feelingTags = memory.feelingTags
  )
 }
}

/** Used by the outer page state holder when the safety gate removes its composition. */
internal val FamilyMemoryDraftSaver=mapSaver(
 save={draft:FamilyMemoryDraft -> mapOf("id" to draft.id,"route" to draft.routeId.orEmpty(),"kind" to (draft.kind?.name?:""),"rating" to (draft.rating?:0),"note" to draft.note,"date" to draft.tripDate,"feelings" to ArrayList(draft.feelingTags))},
 restore={values -> FamilyMemoryDraft(id=values["id"] as String,routeId=(values["route"] as String).ifEmpty{null},kind=FamilyMemoryKind.entries.firstOrNull{it.name==values["kind"]},rating=(values["rating"] as Int).takeIf{it in 1..5},note=values["note"] as String,tripDate=values["date"] as String,feelingTags=(values["feelings"] as List<*>).filterIsInstance<String>())}
)

internal enum class FamilyMemoryFormNotice { REQUIRED_CHOICES, INVALID_DETAILS, CAPACITY, SAVE_FAILED, NONE }

internal fun FamilyMemoryDraft.formNotice(memoryCount:Int,editing:Boolean,saveFailed:Boolean):FamilyMemoryFormNotice = when {
 !requiredChoicesComplete -> FamilyMemoryFormNotice.REQUIRED_CHOICES
 !detailsValid -> FamilyMemoryFormNotice.INVALID_DETAILS
 !editing && memoryCount>=500 -> FamilyMemoryFormNotice.CAPACITY
 saveFailed -> FamilyMemoryFormNotice.SAVE_FAILED
 else -> FamilyMemoryFormNotice.NONE
}

internal fun validMemoryDate(value: String): Boolean = value.isBlank() ||
 runCatching { LocalDate.parse(value).toString() == value }.getOrDefault(false)

internal fun familyRatingMeaning(rating: Int): String = when (rating) {
 1 -> "매우 아쉬워요"
 2 -> "아쉬워요"
 3 -> "보통이에요"
 4 -> "만족해요"
 5 -> "매우 만족해요"
 else -> "아직 선택하지 않았어요"
}
