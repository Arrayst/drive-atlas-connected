package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class NavigationManeuverSymbolTest {
 @Test fun everySemanticHasDistinctAccessibleLabel(){
  val labels=NavManeuverIcon.entries.map{navigationManeuverLabel(it)}
  assertTrue(labels.all{it.isNotBlank()})
  assertEquals(NavManeuverIcon.entries.size,labels.distinct().size)
 }
 @Test fun unknownDoesNotClaimADirection(){
  assertEquals("방향 정보 없음 · 텍스트 안내 확인",navigationManeuverLabel(NavManeuverIcon.UNKNOWN))
  assertEquals("좌회전",navigationManeuverLabel(NavManeuverIcon.LEFT))
  assertEquals("우회전",navigationManeuverLabel(NavManeuverIcon.RIGHT))
 }
}
