package com.driveatlas

/** Explicit insertion slots, preserving base course order and rest order within each slot. */
internal object RestItinerary {
 const val MAX_RESTS=5
 fun route(routeId:String,ids:List<String>,restAfterStopIndex:Map<String,Int> = emptyMap()):Route? {
  val base=Catalog.routes.firstOrNull{it.id==routeId}?:return null
  if(ids.size>MAX_RESTS||ids.distinct().size!=ids.size)return null
  if(restAfterStopIndex.any{it.key !in ids||it.value !in -1..base.stops.lastIndex})return null
  val rests=ids.map{id->RestFacilities.find(id,routeId)?:return null}
  if(rests.any{it.point==null||!validNativePoint(it.point)||it.coordinateSource?.startsWith("https://")!=true})return null
  val stops=mutableListOf<Stop>()
  for(slot in -1..base.stops.lastIndex){
   if(slot>=0)stops+=base.stops[slot]
   rests.filter{(restAfterStopIndex[it.id]?:base.stops.lastIndex)==slot}.forEach{stops+=Stop(it.name+" (대표점)",it.point!!)}
  }
  return base.copy(stops=stops)
 }
 fun nativeFullRouteSupported(route:Route)=route.stops.size in 1..3&&route.stops.all{validNativePoint(it.point)}
 fun breakAfterLegs(routeId:String,ids:List<String>,restAfterStopIndex:Map<String,Int> = emptyMap()):List<Int> {
  val route=route(routeId,ids,restAfterStopIndex)?:return emptyList()
  return ids.map{id->route.stops.indexOfFirst{it.name==RestFacilities.find(id,routeId)!!.name+" (대표점)"}}
 }
}
