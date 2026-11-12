package com.driveatlas

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test

class FamilyMemoryDraftTest {
 private fun complete() = FamilyMemoryDraft(routeId="coast",kind=FamilyMemoryKind.ACTUAL,rating=4)
 private fun restore(draft:FamilyMemoryDraft):FamilyMemoryDraft {
  val scope=object:SaverScope { override fun canBeSaved(value:Any)=true }
  val saved=checkNotNull(with(FamilyMemoryDraftSaver){scope.save(draft)})
  return checkNotNull(FamilyMemoryDraftSaver.restore(saved))
 }

 @Test fun newDraftDoesNotAnswerOnTheUsersBehalf() {
  val draft=FamilyMemoryDraft()
  assertNull(draft.routeId)
  assertNull(draft.kind)
  assertNull(draft.rating)
  assertEquals("",draft.tripDate)
  assertEquals("",draft.note)
  assertTrue(draft.feelingTags.isEmpty())
  assertFalse(draft.requiredChoicesComplete)
  assertFalse(draft.canSave(0))
 }

 @Test fun eachRequiredChoiceMustBeExplicit() {
  assertFalse(complete().copy(routeId=null).canSave(0))
  assertFalse(complete().copy(kind=null).canSave(0))
  assertFalse(complete().copy(rating=null).canSave(0))
  assertFalse(complete().copy(routeId="unknown").canSave(0))
  assertFalse(complete().copy(rating=0).canSave(0))
  assertFalse(complete().copy(rating=6).canSave(0))
  assertTrue(complete().canSave(0))
 }

 @Test fun choosingCourseDoesNotInventRatingKindOrDate() {
  val draft=FamilyMemoryDraft().copy(routeId="lake")
  assertNull(draft.kind)
  assertNull(draft.rating)
  assertTrue(draft.tripDate.isEmpty())
  assertFalse(draft.canSave(0))
 }

 @Test fun anotherNewDraftDoesNotReuseEarlierResponse() {
  val previous=complete().copy(kind=FamilyMemoryKind.SIM,note="메모",tripDate="2026-11-11",feelingTags=listOf("즐거웠어요"))
  val next=FamilyMemoryDraft()
  assertNotEquals(previous.id,next.id)
  assertEquals(FamilyMemoryDraft(id=next.id),next)
  assertNull(next.routeId)
  assertNull(next.kind)
  assertNull(next.rating)
  assertTrue(next.note.isEmpty())
  assertTrue(next.tripDate.isEmpty())
  assertTrue(next.feelingTags.isEmpty())
 }

 @Test fun actualAndSimAreSeparateExplicitChoices() {
  assertFalse(FamilyMemoryKind.ACTUAL.simulated)
  assertTrue(FamilyMemoryKind.SIM.simulated)
  assertTrue(complete().copy(kind=FamilyMemoryKind.SIM).canSave(0))
 }

 @Test fun optionalDetailsCanBeBlankButInvalidDetailsCannotSave() {
  assertTrue(complete().detailsValid)
  assertTrue(complete().copy(tripDate="2024-02-29").canSave(0))
  assertFalse(complete().copy(tripDate="2026-02-29").canSave(0))
  assertFalse(complete().copy(tripDate="2026-2-9").canSave(0))
  assertFalse(complete().copy(note="a".repeat(281)).canSave(0))
  assertFalse(complete().copy(feelingTags=listOf("unknown")).canSave(0))
  assertFalse(complete().copy(feelingTags=listOf("즐거웠어요","즐거웠어요")).canSave(0))
 }

 @Test fun capacityStopsOnlyNewRecords() {
  assertTrue(complete().canSave(499))
  assertFalse(complete().canSave(500))
  assertFalse(complete().canSave(501))
  assertTrue(complete().canSave(501,editing=true))
  assertFalse(complete().copy(rating=null).canSave(501,editing=true))
 }

 @Test fun editingPreservesExistingAnswersAndOptionalDetails() {
  val memory=FamilyMemory("saved","lake","이전 차",2,"기존 메모",123L,true,"2024-02-29",listOf("피곤했어요"))
  val draft=FamilyMemoryDraft.from(memory)
  assertEquals(memory.id,draft.id)
  assertEquals(memory.routeId,draft.routeId)
  assertEquals(FamilyMemoryKind.SIM,draft.kind)
  assertEquals(memory.rating,draft.rating)
  assertEquals(memory.note,draft.note)
  assertEquals(memory.tripDate,draft.tripDate)
  assertEquals(memory.feelingTags,draft.feelingTags)
  val changed=draft.copy(rating=5)
  assertEquals(draft.copy(rating=5),changed)
  assertEquals(2,memory.rating)
  assertEquals("이전 차",memory.carName)
  assertEquals(123L,memory.createdMs)
 }

 @Test fun editingLegacyRecordDoesNotInventOptionalDetails() {
  val memory=FamilyMemory("legacy","forest","우리 차",3,"",123L)
  val draft=FamilyMemoryDraft.from(memory)
  assertEquals(FamilyMemoryKind.ACTUAL,draft.kind)
  assertEquals("",draft.tripDate)
  assertTrue(draft.feelingTags.isEmpty())
  assertTrue(draft.canSave(1,editing=true))
 }

 @Test fun everyRatingHasADistinctMeaningLabel() {
  val meanings=(1..5).map(::familyRatingMeaning)
  assertEquals(5,meanings.distinct().size)
  assertTrue(meanings.all{it.isNotBlank()})
  assertEquals("매우 아쉬워요",familyRatingMeaning(1))
  assertEquals("보통이에요",familyRatingMeaning(3))
  assertEquals("매우 만족해요",familyRatingMeaning(5))
 }

 @Test fun saveableRoundTripKeepsUnansweredFieldsUnanswered() {
  val draft=FamilyMemoryDraft()
  assertEquals(draft,restore(draft))
  assertFalse(restore(draft).canSave(0))
 }

 @Test fun saveableRoundTripRetainsPartiallyEnteredNewRecord() {
  val draft=FamilyMemoryDraft(routeId="lake",note="입력 중인 메모",tripDate="2026-11-")
  val restored=restore(draft)
  assertEquals(draft,restored)
  assertNull(restored.kind)
  assertNull(restored.rating)
  assertFalse(restored.canSave(0))
 }

 @Test fun saveableRoundTripRetainsAllNewRecordAnswers() {
  val draft=complete().copy(kind=FamilyMemoryKind.SIM,note="아직 저장하지 않은 메모",tripDate="2026-11-12",feelingTags=listOf("편안했어요","다시 가고 싶어요"))
  assertEquals(draft,restore(draft))
 }

 @Test fun restoringUnsavedEditDoesNotChangeTheStoredRecord() {
  val memory=FamilyMemory("saved","lake","이전 차",2,"기존 메모",123L,true,"2024-02-29",listOf("피곤했어요"))
  var stored=FamilySnapshot(memories=listOf(memory))
  var writes=0
  val engine=FamilyEngine(object:FamilyRepository {
   override fun loadFamily()=stored
   override fun saveFamily(snapshot:FamilySnapshot){writes++;stored=snapshot}
  })
  val draft=FamilyMemoryDraft.from(memory).copy(rating=5,note="수정 중인 메모")
  val restored=restore(draft)
  assertEquals(draft,restored)
  assertEquals(listOf(memory),stored.memories)
  assertEquals(0,writes)
  // An interrupted screen must not bypass the engine's current safety check.
  val rating=checkNotNull(restored.rating)
  assertFalse(engine.editMemory(restored.id,rating,restored.note,Safety.UNKNOWN,restored.tripDate,restored.feelingTags))
  assertFalse(engine.editMemory(restored.id,rating,restored.note,Safety.MOVING,restored.tripDate,restored.feelingTags))
  assertEquals(0,writes)
  assertEquals(listOf(memory),stored.memories)
 }

 @Test fun validationGuidanceTakesPriorityOverEarlierSaveFailure() {
  assertEquals(FamilyMemoryFormNotice.REQUIRED_CHOICES,FamilyMemoryDraft().formNotice(0,false,true))
  assertEquals(FamilyMemoryFormNotice.INVALID_DETAILS,complete().copy(tripDate="2026-11-").formNotice(0,false,true))
  assertEquals(FamilyMemoryFormNotice.CAPACITY,complete().formNotice(500,false,true))
 }

 @Test fun failedSaveOnlyAppearsForAnOtherwiseSavableDraft() {
  assertEquals(FamilyMemoryFormNotice.SAVE_FAILED,complete().formNotice(0,false,true))
  assertEquals(FamilyMemoryFormNotice.NONE,complete().formNotice(0,false,false))
  assertEquals(FamilyMemoryFormNotice.SAVE_FAILED,complete().formNotice(500,true,true))
  assertEquals(FamilyMemoryFormNotice.NONE,complete().formNotice(500,true,false))
 }
}
