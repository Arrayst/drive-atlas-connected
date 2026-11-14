package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class PlanningTimeOptionsTest {
 @Test fun walkingPresetsIncludeThirtyInNumericOrder() {
  assertEquals(listOf("0", "10", "20", "30", "40"), FamilyWalkMinuteChoices)
 }
 @Test fun restoredValueIsInsertedNumericallyRatherThanPrepended() {
  assertEquals(listOf("0", "10", "20", "30", "40"), planningTimeChoiceValues(listOf("0", "10", "20", "40"), "30", "30"))
 }
 @Test fun changingSelectionDoesNotRemoveTheEditorEntryValue() {
  val presets = listOf("0", "10", "20", "40")
  val first = planningTimeChoiceValues(presets, "30", "30")
  assertEquals(first, planningTimeChoiceValues(presets, "30", "20"))
  assertEquals(first, planningTimeChoiceValues(presets, "30", "0"))
 }
 @Test fun arbitraryRestoredMinuteRemainsAvailableAfterChangingSelection() {
  assertEquals(listOf("60", "120", "150", "180", "240"), planningTimeChoiceValues(listOf("240", "60", "120", "180"), "150", "60"))
 }
 @Test fun unsetLabelsStayBeforeAscendingNumericChoices() {
  assertEquals(listOf("미설정", "30", "45", "60", "120"), planningTimeChoiceValues(listOf("미설정", "120", "30", "60"), "45", "미설정"))
 }
 @Test fun repeatedValuesAreNotDuplicated() {
  assertEquals(listOf("미입력", "0", "15", "30"), planningTimeChoiceValues(listOf("미입력", "0", "15", "15", "30"), "30", "30"))
 }
}
