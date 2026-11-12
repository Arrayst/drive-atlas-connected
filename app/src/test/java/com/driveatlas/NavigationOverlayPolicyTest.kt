package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class NavigationOverlayPolicyTest {
 @Test fun instructionCardStaysInsideLandscapeAndPortrait(){
  listOf(1682f,1210f,600f,480f,360f,320f).forEach { width ->
   assertTrue(104f+navigationInstructionWidth(width)<=width-16f)
   assertTrue(navigationInstructionWidth(width)<=400f)
  }
 }
 @Test fun simRunningNeverUnlocksActualVehicleRestriction(){
  for(canEdit in listOf(false,true))for(foreground in listOf(false,true))for(running in listOf(false,true))
   assertEquals(canEdit&&foreground&&!running,canBrowseNavigation(canEdit,foreground,running))
 }
 @Test fun parkedPausedForegroundIsOnlyBrowseState(){
  assertTrue(canBrowseNavigation(true,true,false))
  assertFalse(canBrowseNavigation(true,true,true))
  assertFalse(canBrowseNavigation(false,true,false))
  assertFalse(canBrowseNavigation(true,false,false))
 }
}
