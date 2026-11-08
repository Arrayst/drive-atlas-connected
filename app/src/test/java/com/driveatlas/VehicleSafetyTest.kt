package com.driveatlas
import org.junit.Assert.assertEquals
import org.junit.Test
class VehicleSafetyTest {
 @Test fun onlyVerifiedParkUnlocks() { assertEquals(Safety.PARKED, vehicleSafety(true, 4, false, 0)) }
 @Test fun noConnectionStaysUnknown() { assertEquals(Safety.UNKNOWN, vehicleSafety(false, 4, false, 0)) }
 @Test fun missingAnySignalStaysUnknown() {
  assertEquals(Safety.UNKNOWN, vehicleSafety(true, null, false, 0))
  assertEquals(Safety.UNKNOWN, vehicleSafety(true, 4, null, 0))
  assertEquals(Safety.UNKNOWN, vehicleSafety(true, 4, false, null))
 }
 @Test fun driveNeutralAndReverseStayRestricted() { listOf(1,2,8).forEach { assertEquals(Safety.MOVING, vehicleSafety(true,it,false,0)) } }
 @Test fun parkNeverOverridesUxRestrictions() {
  assertEquals(Safety.MOVING, vehicleSafety(true,4,true,0))
  assertEquals(Safety.MOVING, vehicleSafety(true,4,false,80))
 }
}
