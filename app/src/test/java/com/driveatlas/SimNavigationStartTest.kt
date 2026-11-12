package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class SimNavigationStartTest {
 private val request=NavPrototypeRequest("coast","seoul")
 private fun gate(voice:Boolean=true)=SimNavigationStartGate().apply{begin(SimStartTicket(4,request,voice))}
 private fun consume(g:SimNavigationStartGate,generation:Int=4,current:NavPrototypeRequest=request,rendered:NavPrototypeRequest?=request,ready:Boolean=true,foreground:Boolean=true,canRun:Boolean=true,debug:Boolean=true,simulator:Boolean=true)=
  g.consume(generation,current,rendered,ready,foreground,canRun,debug,simulator)
 @Test fun previousViewportWithIdenticalRequestCannotUnlockReplacementRoute(){
  val old=NavPrototypeRoute(request,listOf(Point(37.0,127.0),Point(37.01,127.0)),emptyList(),100.0,10.0,emptyList())
  val replacement=old.copy()
  assertFalse(isCurrentNavigationViewport(old,replacement));assertFalse(isCurrentNavigationViewport(null,null));assertTrue(isCurrentNavigationViewport(replacement,replacement))
 }
 @Test fun screenEntryAloneNeverStarts(){assertNull(consume(SimNavigationStartGate()))}
 @Test fun explicitStartWaitsForBothCurrentRouteAndMap(){val g=gate();assertNull(consume(g,rendered=null));assertNull(consume(g,ready=false));assertNotNull(consume(g))}
 @Test fun readyConsumesStartOnlyOnceAndKeepsVoiceChoice(){val g=gate(false);assertFalse(consume(g)!!.withSpeech);repeat(4){assertNull(consume(g))}}
 @Test fun oldMapCallbackCannotStartCurrentRoute(){val g=gate();assertNull(consume(g,rendered=request.copy(routeId="lake")));assertNotNull(consume(g))}
 @Test fun generationMismatchCancelsRatherThanWaitingForAReusedNumber(){val g=gate();assertNull(consume(g,generation=5));assertNull(consume(g))}
 @Test fun changedDepartureCourseOrRestPlanCancelsPendingStart(){listOf(request.copy(departureId="other"),request.copy(routeId="lake"),request.copy(selectedRestIds=listOf("rest"))).forEach{val g=gate();assertNull(consume(g,current=it));assertNull(consume(g))}}
 @Test fun backgroundClearsPendingAndReturningCannotAutoStart(){val g=gate();assertNull(consume(g,foreground=false));assertNull(consume(g))}
 @Test fun closedParkOrUxGateCancelsPending(){val g=gate();assertNull(consume(g,canRun=false));assertNull(consume(g))}
 @Test fun releaseAndNonSimulatorCannotStart(){val g=gate();assertNull(consume(g,debug=false));assertNull(consume(g));assertNull(consume(gate(),simulator=false))}
 @Test fun cancelBackFailureOrWithdrawalMakesLateCallbacksHarmless(){repeat(4){val g=gate();g.cancel();assertNull(consume(g));assertNull(consume(g))}}
 @Test fun newerExplicitStartDoesNotReuseOldRouteOrVoiceChoice(){val g=gate();g.cancel();g.begin(SimStartTicket(9,request.copy(routeId="lake"),false));assertNull(consume(g,generation=9,current=request.copy(routeId="lake")));assertFalse(consume(g,generation=9,current=request.copy(routeId="lake"),rendered=request.copy(routeId="lake"))!!.withSpeech)}
 @Test fun partialTileLoadingIsNotReadyOrAnImmediateFailure(){assertEquals(NavigationMapReadiness.LOADING,navigationMapReadiness(false,false,false,false,false));assertEquals(NavigationMapReadiness.LOADING,navigationMapReadiness(true,false,false,false,false))}
 @Test fun roadFeaturesRequireLoadedStyle(){assertEquals(NavigationMapReadiness.LOADING,navigationMapReadiness(false,true,false,false,false));assertEquals(NavigationMapReadiness.READY,navigationMapReadiness(true,true,false,false,false))}
 @Test fun completeBlankFrameProviderFailureAndTimeoutStopReplay(){assertEquals(NavigationMapReadiness.FAILED,navigationMapReadiness(true,false,false,false,true));assertEquals(NavigationMapReadiness.FAILED,navigationMapReadiness(true,true,true,false,false));assertEquals(NavigationMapReadiness.FAILED,navigationMapReadiness(true,false,false,true,false))}
 @Test fun recoveryCanShowReadyButCannotReviveCancelledStart(){val g=gate();g.cancel();assertEquals(NavigationMapReadiness.READY,navigationMapReadiness(true,true,false,true,false));assertNull(consume(g))}
}
