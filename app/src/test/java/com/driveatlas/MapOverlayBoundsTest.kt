package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class MapOverlayBoundsTest {
 @Test fun wrappedNarrowFooterCannotHidePins(){
  assertNull(mapPinVerticalRange(360f,144f,192f,32f,8f))
 }
 @Test fun normalMapPinsStayFullyBetweenMeasuredOverlays(){
  val bounds=mapPinVerticalRange(500f,144f,128f,32f,8f)!!
  assertEquals(184f,bounds.first,0f)
  assertEquals(332f,bounds.second,0f)
  assertTrue(bounds.first-32>=144+8)
  assertTrue(bounds.second+32<=500-128-8)
 }
 @Test fun largerFontFooterIsReservedInsteadOfClampedAway(){
  assertNull(mapPinVerticalRange(420f,180f,192f,32f,8f))
  val bounds=mapPinVerticalRange(600f,180f,240f,32f,8f)!!
  assertEquals(320f,bounds.second,0f)
 }
 @Test fun exactFitIsSafeAndInvalidDimensionsFailClosed(){
  assertEquals(184f,mapPinVerticalRange(416f,144f,192f,32f,8f)!!.first,0f)
  assertNull(mapPinVerticalRange(Float.NaN,144f,128f,32f,8f))
 }
}
