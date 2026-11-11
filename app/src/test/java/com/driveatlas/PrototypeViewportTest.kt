package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class PrototypeViewportTest {
 @Test fun longRoundTripFitsWholeScreen(){
  DepartureReferences.all.forEach{departure->Catalog.routes.forEach{route->
   val points=listOf(departure.point)+route.stops.map{it.point}+departure.point
   val v=prototypeViewport(points,980,420)
   points.forEach{val p=worldPoint(it,v.zoom);assertTrue(p.first-v.left in 48.0..932.0);assertTrue(p.second-v.top in 70.0..350.0)}
  }}
 }
}
