package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class VectorMapConsentStoreTest {
 private class MemoryStorage(var value: String? = null) : VectorConsentStorage {
  var writes = 0; var clears = 0; var fail = false
  override fun read() = value
  override fun write(token: String): Boolean { writes++; if (fail) return false; value = token; return true }
  override fun clear(): Boolean { clears++; if (fail) return false; value = null; return true }
 }
 private val scope = VectorMapConsentScope.Current
 @Test fun firstUseDoesNotGrantOrWriteUntilExplicitAcceptance() {
  val storage = MemoryStorage(); val state = VectorMapConsentState(storage)
  assertFalse(state.granted); assertEquals(0, storage.writes); assertNull(storage.value)
  assertTrue(state.accept()); assertTrue(state.granted); assertEquals(1, storage.writes)
 }
 @Test fun sameScopeSurvivesScreenAndProcessRecreationWithoutAnotherWrite() {
  val storage = MemoryStorage(); assertTrue(VectorMapConsentState(storage).accept())
  repeat(3) { assertTrue(VectorMapConsentState(storage).granted) }
  assertEquals(1, storage.writes)
 }
 @Test fun storedScopeAllowsPublicExplorerAndDebugSimButNeverReleaseSim() {
  val storage = MemoryStorage(scope.token); val state = VectorMapConsentState(storage)
  assertTrue(vectorMapConnectionAllowed(VectorMapPurpose.PUBLIC_COURSE, false, false, state.granted))
  assertTrue(vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, true, true, state.granted))
  assertFalse(vectorMapConnectionAllowed(VectorMapPurpose.SIM_REPLAY, false, false, state.granted))
 }
 @Test fun providerPurposeAndDisclosureChangesEachRequireNewAcceptance() {
  listOf(scope.copy(provider="another provider"),scope.copy(purpose="live GPS"),scope.copy(disclosureVersion=scope.disclosureVersion+1)).forEach { changed ->
   val storage=MemoryStorage(scope.token); val state=VectorMapConsentState(storage,changed)
   assertFalse(state.granted); assertEquals(0,storage.writes)
   assertTrue(state.accept()); assertEquals(changed.token,storage.value)
  }
 }
 @Test fun malformedOrLegacySessionValueDoesNotGrant() {
  listOf("true", "", "OpenFreeMap", scope.token+" extra").forEach { assertFalse(VectorMapConsentState(MemoryStorage(it)).granted) }
 }
 @Test fun withdrawalImmediatelyClosesBothGatesAndPersists() {
  val storage=MemoryStorage(scope.token); val state=VectorMapConsentState(storage)
  assertTrue(state.withdraw()); assertFalse(state.granted); assertNull(storage.value)
  VectorMapPurpose.entries.forEach { assertFalse(vectorMapConnectionAllowed(it,true,true,state.granted)) }
  assertFalse(VectorMapConsentState(storage).granted)
 }
 @Test fun failedSaveDoesNotConnectOrClaimSavedConsent() {
  val storage=MemoryStorage().apply{fail=true}; val state=VectorMapConsentState(storage)
  assertFalse(state.accept()); assertFalse(state.granted); assertTrue(state.error.isNotEmpty()); assertNull(storage.value)
 }
 @Test fun failedWithdrawalStillDisconnectsAndAllowsDeletionRetry() {
  val storage=MemoryStorage(scope.token).apply{fail=true}; val state=VectorMapConsentState(storage)
  assertFalse(state.withdraw()); assertFalse(state.granted); assertTrue(state.withdrawalPending); assertTrue(state.error.isNotEmpty())
  assertFalse(state.accept()); storage.fail=false
  assertTrue(state.withdraw()); assertFalse(state.withdrawalPending); assertNull(storage.value)
 }
 @Test fun recordContainsOnlyProviderScopeAndVersionNeverCoordinatesOrFamilyData() {
  assertTrue(scope.token.contains(NavigationVectorStyle.tileJson)); assertTrue(scope.token.contains(scope.purpose))
  assertFalse(scope.token.contains("37.")); assertFalse(scope.token.contains("family")); assertFalse(scope.token.contains("router.project-osrm.org"))
 }
 @Test fun failedWithdrawalTitleAndMessageNeverClaimSavedRevocation() {
  val storage=MemoryStorage(scope.token).apply{fail=true};val state=VectorMapConsentState(storage)
  val saved=state.withdraw()
  assertFalse(saved);assertFalse(state.granted)
  assertEquals("지도 연결 차단 · 저장 삭제 실패",vectorMapConsentDialogTitle(state.granted,state.withdrawalPending))
  val message=vectorMapWithdrawalMessage(saved,state.error)
  assertEquals(state.error,message);assertTrue(message.contains("삭제하지 못했어요"));assertFalse(message.contains("철회했어요"))
  assertTrue(vectorMapWithdrawalMessage(false,"").contains("삭제하지 못했어요"))
 }
 @Test fun successfulWithdrawalRetryCanReportDurableSuccess() {
  val storage=MemoryStorage(scope.token).apply{fail=true};val state=VectorMapConsentState(storage)
  assertFalse(state.withdraw());storage.fail=false
  val saved=state.withdraw()
  assertTrue(saved);assertFalse(state.withdrawalPending);assertEquals("",state.error)
  assertEquals("OpenFreeMap 지도 연결",vectorMapConsentDialogTitle(state.granted,state.withdrawalPending))
  assertTrue(vectorMapWithdrawalMessage(saved,state.error).contains("철회했어요"));assertNull(storage.value)
 }

}
