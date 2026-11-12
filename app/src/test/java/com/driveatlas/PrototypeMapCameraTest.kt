package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class PrototypeMapCameraTest {
 @Test fun followCentersExistingPositionAtBoundedZoom(){
  val point=Point(37.56,126.97)
  val view=prototypeFollowViewport(point,1000,420)
  val pixel=worldPoint(point,view.zoom)
  assertEquals(16,view.zoom)
  assertEquals(500.0,pixel.first-view.left,.001)
  assertEquals(210.0,pixel.second-view.top,.001)
 }
 @Test fun manualPanMovesCameraOppositeFingerAndClampsWorld(){
  val view=MapViewport(14,1000.0,2000.0,600,420)
  val moved=prototypePanViewport(view,100.0,-50.0)
  assertEquals(900.0,moved.left,0.0)
  assertEquals(2050.0,moved.top,0.0)
  val bounded=prototypePanViewport(view,Double.MAX_VALUE,-Double.MAX_VALUE)
  assertEquals(0.0,bounded.left,0.0)
  assertEquals(256.0*(1 shl 14)-420,bounded.top,0.0)
 }
 @Test fun visibleTileIdentityDoesNotChangeWithSubTileMovement(){
  val view=MapViewport(14,260.0,260.0,400,300)
  assertEquals(prototypeVisibleTileKeys(view),prototypeVisibleTileKeys(view.copy(left=270.0,top=270.0)))
  assertEquals(4,prototypeVisibleTileKeys(view).size)
 }
 @Test fun visibleTilesStayWithinWorld(){
  val keys=prototypeVisibleTileKeys(MapViewport(1,-100.0,-100.0,1000,1000))
  assertEquals(4,keys.size)
  assertTrue(keys.all{it.x in 0..1&&it.y in 0..1})
 }
 @Test fun geometryHeadingUsesRoadDirection(){
  val origin=Point(37.0,127.0)
  assertEquals(90f,prototypeGeometryHeading(listOf(origin,Point(37.0,127.01)),listOf(0.0,100.0),0.0)!!,.01f)
  assertEquals(0f,prototypeGeometryHeading(listOf(origin,Point(37.01,127.0)),listOf(0.0,100.0),0.0)!!,.01f)
  assertEquals(180f,prototypeGeometryHeading(listOf(origin,Point(36.99,127.0)),listOf(0.0,100.0),0.0)!!,.01f)
 }
 @Test fun repeatedGeometryPointsAndEmptyGeometryAreSafe(){
  val p=Point(37.0,127.0)
  assertEquals(90f,prototypeGeometryHeading(listOf(p,p,Point(37.0,127.01)),listOf(0.0,0.0,100.0),0.0)!!,.01f)
  assertNull(prototypeGeometryHeading(emptyList(),emptyList(),0.0))
 }
 @Test fun overlappingReturnUsesOrderedCursorNotNearestPoint(){
  val start=Point(37.0,127.0);val turn=Point(37.0,127.01)
  val points=listOf(start,turn,start);val cumulative=listOf(0.0,100.0,200.0)
  assertEquals(90f,prototypeGeometryHeading(points,cumulative,50.0)!!,.01f)
  assertEquals(-90f,prototypeGeometryHeading(points,cumulative,150.0)!!,.01f)
  assertEquals(-90f,prototypeGeometryHeading(points,cumulative,200.0)!!,.01f)
 }
 @Test fun zeroLengthAndInvalidCursorHaveNoInventedHeading(){
  val p=Point(37.0,127.0)
  assertNull(prototypeGeometryHeading(listOf(p,p),listOf(0.0,0.0),0.0))
  assertNull(prototypeGeometryHeading(listOf(p,Point(37.0,128.0)),listOf(0.0,100.0),Double.NaN))
 }
}
