package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class NavigationMapStyleTest {
 @Test fun progressTracksOrderedReturnLegEvenWhenCoordinatesOverlap(){
  val cumulative=listOf(0.0,100.0,200.0,300.0)
  assertEquals(0,navigationProgressSegment(cumulative,50.0))
  assertEquals(1,navigationProgressSegment(cumulative,150.0))
  assertEquals(2,navigationProgressSegment(cumulative,250.0))
  assertEquals(2,navigationProgressSegment(cumulative,300.0))
 }
 @Test fun progressBoundariesAndRepeatedVerticesAreSafe(){
  assertEquals(1,navigationProgressSegment(listOf(0.0,0.0,100.0),0.0))
  assertEquals(1,navigationProgressSegment(listOf(0.0,100.0,200.0),100.0))
  assertEquals(0,navigationProgressSegment(listOf(0.0,100.0),Double.NaN))
  assertEquals(0,navigationProgressSegment(emptyList(),10.0))
 }
 @Test fun markersContainOnlyRequestedSourceBackedReferences(){
  val request=NavPrototypeRequest("coast","seoul",listOf("simgok"))
  val markers=navigationReferenceMarkers(request)
  assertEquals(5,markers.size)
  assertEquals(1,markers.count{it.kind==NavigationMarkerKind.DEPARTURE})
  assertEquals(3,markers.count{it.kind==NavigationMarkerKind.COURSE})
  assertEquals(listOf("simgok"),markers.filter{it.kind==NavigationMarkerKind.REST}.map{it.id})
  assertEquals(RestFacilities.find("simgok","coast")!!.point,markers.last().point)
  assertEquals(request.departure()!!.point,markers.first().point)
 }
 @Test fun invalidRequestsDoNotInventMarkers(){
  assertTrue(navigationReferenceMarkers(NavPrototypeRequest("coast","unknown")).isEmpty())
  assertTrue(navigationReferenceMarkers(NavPrototypeRequest("coast","seoul",listOf("unknown"))).isEmpty())
 }
 private fun pixel(id:String,x:Double,y:Double)=NavigationMarkerPixel(
  NavigationReferenceMarker(id,id,id,NavigationMarkerKind.COURSE,Point(37.0,127.0)),x,y)
 @Test fun nearbyMarkersClusterWithoutLosingMembers(){
  val points=listOf(pixel("a",0.0,0.0),pixel("b",20.0,0.0),pixel("c",200.0,0.0))
  val groups=clusterNavigationMarkers(points,56.0)
  assertEquals(2,groups.size)
  assertEquals(10.0,groups.first().x,0.0)
  assertEquals(points.map{it.marker.id}.sorted(),groups.flatMap{it.members}.map{it.marker.id}.sorted())
 }
 @Test fun panDoesNotChangeClusterMembership(){
  val points=listOf(pixel("a",0.0,0.0),pixel("b",20.0,0.0),pixel("c",200.0,0.0))
  val before=clusterNavigationMarkers(points,56.0)
  val after=clusterNavigationMarkers(points.map{it.copy(x=it.x-1000,y=it.y+50)},56.0)
  assertEquals(before.map{it.members.map{p->p.marker.id}},after.map{it.members.map{p->p.marker.id}})
  assertEquals(before.first().x-1000,after.first().x,0.0)
 }
 @Test fun chainOfNearbyMarkersFormsOneStableGroup(){
  val groups=clusterNavigationMarkers(listOf(pixel("a",0.0,0.0),pixel("b",40.0,0.0),pixel("c",80.0,0.0)),56.0)
  assertEquals(1,groups.size)
  assertEquals(3,groups.single().members.size)
 }
}
