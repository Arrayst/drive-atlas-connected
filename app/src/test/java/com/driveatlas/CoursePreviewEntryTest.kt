package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class CoursePreviewEntryTest {
 @Test fun everyDetailKeepsItsCourseAndBackTarget(){
  Catalog.routes.forEach { route ->
   val entry=CoursePreviewEntry(route.id,"detail")
   assertEquals(route.id,entry.routeId)
   assertEquals("detail",entry.returnPage)
   assertTrue(entry.courseLocked)
   assertEquals("← 코스 상세로",entry.backLabel)
  }
 }
 @Test fun etaPreviewCanSwitchCoursesAndReturnsToEta(){
  val entry=CoursePreviewEntry(Catalog.routes.last().id,"auto-eta")
  assertEquals(Catalog.routes.last().id,entry.routeId)
  assertFalse(entry.courseLocked)
  assertEquals("auto-eta",entry.returnPage)
 }
 @Test fun entryRejectsUnknownCourseOrBackTarget(){
  for(entry in listOf("missing" to "detail",Catalog.routes.first().id to "external")){
   try{CoursePreviewEntry(entry.first,entry.second);fail("Unknown entry accepted")}catch(_:IllegalArgumentException){}
  }
 }
 @Test fun previewRequiresAllFourSafetyConditions(){
  for(debug in listOf(false,true))for(sim in listOf(false,true))for(emulator in listOf(false,true))for(parked in listOf(false,true))
   assertEquals(debug&&sim&&emulator&&parked,coursePreviewAllowed(debug,sim,emulator,parked))
 }
}
