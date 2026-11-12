package com.driveatlas

import kotlin.math.*

internal enum class NavigationMarkerKind { DEPARTURE, COURSE, REST }
internal data class NavigationReferenceMarker(val id:String,val name:String,val badge:String,val kind:NavigationMarkerKind,val point:Point)
internal fun navigationReferenceMarkers(request:NavPrototypeRequest):List<NavigationReferenceMarker> {
 if(!request.valid())return emptyList()
 val departure=request.departure()!!
 val course=Catalog.routes.first{it.id==request.routeId}
 return listOf(NavigationReferenceMarker("departure",departure.label,"출",NavigationMarkerKind.DEPARTURE,departure.point))+
  course.stops.mapIndexed{i,stop->NavigationReferenceMarker("course-$i",stop.name,"${i+1}",NavigationMarkerKind.COURSE,stop.point)}+
  request.selectedRestIds.mapNotNull{id->RestFacilities.find(id,request.routeId)?.let{rest->
   rest.point?.let{NavigationReferenceMarker(rest.id,rest.name,"쉼",NavigationMarkerKind.REST,it)}
  }}
}
internal data class NavigationMarkerPixel(val marker:NavigationReferenceMarker,val x:Double,val y:Double)
internal data class NavigationMarkerCluster(val members:List<NavigationMarkerPixel>,val x:Double,val y:Double)
/** Connected groups preserve every source point; no displacement or invented locations. */
internal fun clusterNavigationMarkers(points:List<NavigationMarkerPixel>,radius:Double):List<NavigationMarkerCluster> {
 require(radius>0&&radius.isFinite())
 val remaining=points.toMutableList();val groups=mutableListOf<NavigationMarkerCluster>()
 while(remaining.isNotEmpty()){
  val group=mutableListOf(remaining.removeAt(0));var index=0
  while(index<group.size){
   val current=group[index++];val nearby=remaining.filter{hypot(it.x-current.x,it.y-current.y)<radius}
   group.addAll(nearby);remaining.removeAll(nearby.toSet())
  }
  groups+=NavigationMarkerCluster(group,group.map{it.x}.average(),group.map{it.y}.average())
 }
 return groups
}
/** Segment immediately after the ordered cursor. Overlapping geometry never changes progress. */
internal fun navigationProgressSegment(cumulative:List<Double>,distance:Double):Int {
 if(cumulative.size<2)return 0
 val d=if(distance.isFinite())distance.coerceIn(0.0,cumulative.last())else 0.0
 var upper=cumulative.binarySearch(d).let{if(it<0)-it-1 else it+1}.coerceIn(1,cumulative.lastIndex)
 while(upper<cumulative.lastIndex&&cumulative[upper]<=d)upper++
 return upper-1
}
