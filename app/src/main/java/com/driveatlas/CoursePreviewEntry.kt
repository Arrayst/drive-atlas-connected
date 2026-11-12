package com.driveatlas

/** A detail preview retains its selected course and returns to that same detail. */
internal data class CoursePreviewEntry(val routeId:String,val returnPage:String) {
 init { require(Catalog.routes.any{it.id==routeId});require(returnPage in setOf("detail","auto-eta")) }
 val courseLocked:Boolean get()=returnPage=="detail"
 val backLabel:String get()=if(courseLocked)"← 코스 상세로" else "← 왕복 시간으로"
}
internal fun coursePreviewAllowed(debug:Boolean,simulator:Boolean,emulator:Boolean,parked:Boolean)=
 debug&&simulator&&emulator&&parked
