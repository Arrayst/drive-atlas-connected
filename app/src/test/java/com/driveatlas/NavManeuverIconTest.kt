package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class NavManeuverIconTest {
 @Test fun allKnownModifiersMapWithoutLocalizedText(){val expected=listOf("left" to NavManeuverIcon.LEFT,"right" to NavManeuverIcon.RIGHT,"slight left" to NavManeuverIcon.SLIGHT_LEFT,"slight right" to NavManeuverIcon.SLIGHT_RIGHT,"sharp left" to NavManeuverIcon.SHARP_LEFT,"sharp right" to NavManeuverIcon.SHARP_RIGHT,"straight" to NavManeuverIcon.STRAIGHT,"uturn" to NavManeuverIcon.UTURN);expected.forEach{(modifier,icon)->assertEquals(icon,navManeuverIcon("turn",modifier))}}
 @Test fun unknownInputsAreNeutral(){assertEquals(NavManeuverIcon.UNKNOWN,navManeuverIcon("alien","left"));for(modifier in listOf(null,"","alien","우회전"))assertEquals(NavManeuverIcon.UNKNOWN,navManeuverIcon("turn",modifier))}
 @Test fun roundaboutAndEndpointsHaveDedicatedSemantics(){for(type in listOf("roundabout","rotary","roundabout turn","exit roundabout","exit rotary"))assertEquals(NavManeuverIcon.ROUNDABOUT,navManeuverIcon(type,"left"));assertEquals(NavManeuverIcon.ARRIVE,navManeuverIcon("arrive",null));assertEquals(NavManeuverIcon.DEPART,navManeuverIcon("depart",null))}
 @Test fun defaultOldStepsRemainNeutralAndOffRouteDoesNotShowTurn(){val a=Point(37.0,127.0);val b=Point(37.001,127.0);val steps=listOf(NavPrototypeStep("무관한 텍스트",0.0,0,"depart"),NavPrototypeStep("좌회전이라고 써도",50.0,0,"turn","right"));val r=NavPrototypeRoute(NavPrototypeRequest("coast","seoul"),listOf(a,b),steps,111.0,30.0,listOf(111.0));assertEquals(NavManeuverIcon.RIGHT,r.snapshot(NavReplayState()).maneuver);assertEquals(NavManeuverIcon.UNKNOWN,r.snapshot(NavReplayState(offRoute=true)).maneuver);assertNull(NavPrototypeStep("우회전",0.0,0,"turn").modifier)}
}
