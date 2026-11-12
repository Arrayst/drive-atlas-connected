package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class ParkedDrawerPolicyTest {
 @Test fun onlyVerifiedForegroundParkAllowsDrawer(){assertTrue(canOpenParkedDrawer(true,true));assertFalse(canOpenParkedDrawer(false,true));assertFalse(canOpenParkedDrawer(true,false));assertFalse(canOpenParkedDrawer(false,false))}
 @Test fun actualUxOrGearRestrictionClosesEligibility(){assertTrue(canOpenParkedDrawer(vehicleSafety(true,4,false,0)==Safety.PARKED));assertFalse(canOpenParkedDrawer(vehicleSafety(true,4,true,0)==Safety.PARKED));assertFalse(canOpenParkedDrawer(vehicleSafety(true,8,false,0)==Safety.PARKED));assertFalse(canOpenParkedDrawer(vehicleSafety(false,4,false,0)==Safety.PARKED))}
 @Test fun drawerBackNeverNavigatesFirst(){assertTrue(shouldDismissDrawerOnBack(true));assertFalse(shouldDismissDrawerOnBack(false))}
}
