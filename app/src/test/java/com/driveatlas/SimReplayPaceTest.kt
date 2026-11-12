package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class SimReplayPaceTest {
 private val request = NavPrototypeRequest("coast", "seoul")
 private val route = NavPrototypeRoute(request, listOf(Point(37.0,127.0),Point(37.01,127.0)), emptyList(),1112.0,111.2,emptyList())

 @Test fun oneSecondTickUsesTheSameTenMeterPaceAsTheLabel() {
  assertEquals(10, SimReplayMetersPerSecond)
  assertEquals(SimReplayMetersPerSecond.toDouble(),route.advanceSimTick(NavReplayState(running=true)).distanceMeters,0.0)
 }
 @Test fun voiceAndTextStartChoicesProduceIdenticalProgress() {
  for (withSpeech in listOf(true,false)) {
   val gate=SimNavigationStartGate().apply { begin(SimStartTicket(1,request,withSpeech)) }
   val ticket=gate.consume(1,request,request,true,true,true,true,true)
   assertNotNull(ticket); assertEquals(withSpeech,ticket!!.withSpeech)
   assertEquals(10.0,route.advanceSimTick(NavReplayState(running=true)).distanceMeters,0.0)
  }
 }
 @Test fun consecutiveTicksNeverSelectTheFormerHundredMeterPace() {
  var state=NavReplayState(running=true)
  repeat(10) { tick ->
   state=route.advanceSimTick(state)
   assertEquals((tick+1)*10.0,state.distanceMeters,0.0)
  }
 }
 @Test fun manualResumeKeepsTenMeterPaceAfterPause() {
  val playing=route.advanceSimTick(NavReplayState(running=true))
  val paused=playing.copy(running=false)
  assertEquals(paused,route.advanceSimTick(paused))
  assertEquals(20.0,route.advanceSimTick(paused.copy(running=true)).distanceMeters,0.0)
 }
 @Test fun offRouteStateCannotAdvance() {
  val offRoute=NavReplayState(distanceMeters=50.0,running=true,offRoute=true)
  assertEquals(offRoute,route.advanceSimTick(offRoute))
 }
 @Test fun lastTickClampsToRouteEndAndStops() {
  val finished=route.advanceSimTick(NavReplayState(distanceMeters=route.geometryMeters-3.0,running=true))
  assertEquals(route.geometryMeters,finished.distanceMeters,0.0); assertFalse(finished.running)
  assertEquals(finished,route.advanceSimTick(finished))
 }
}
