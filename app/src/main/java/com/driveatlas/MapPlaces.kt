package com.driveatlas

internal enum class MapPlaceKind(val label:String) { COURSE("코스"), REST("쉴 곳"), PHOTO("사진 산책") }
internal data class MapPlace(
 val id:String,val name:String,val point:Point,val kinds:Set<MapPlaceKind>,val badge:String,
 val description:String,val source:String?=null,val selectedRest:Boolean=false
)
/** Public references only. A photo destination is not an exact camera position or safe stopping point. */
internal fun mapPlaces(route:Route,selectedRestIds:Set<String> = emptySet()):List<MapPlace> {
 val base=Catalog.routes.firstOrNull{it.id==route.id}?:route
 val course=base.stops.mapIndexed { i,s -> MapPlace("course-$i",s.name,s.point,setOf(MapPlaceKind.COURSE),"${i+1}",
  "코스 도로 기준점입니다. 주차·정차·촬영 가능 지점이 아닙니다. 현장 표지와 안전한 주차 장소를 확인하세요.") }
 val rests=RestFacilities.forRoute(route.id).filter{it.point?.let(::validNativePoint)==true&&it.coordinateSource?.startsWith("https://")==true}.map { r ->
  val photo=r.id=="hourglass"
  MapPlace("rest-${r.id}",if(photo)"모래시계공원 · 사진 산책" else r.name,r.point!!,
   if(photo)setOf(MapPlaceKind.REST,MapPlaceKind.PHOTO) else setOf(MapPlaceKind.REST),if(photo)"쉼·사진" else "쉼",
   (if(photo)"공식 안내의 열린바다포토존을 둘러볼 수 있는 공원입니다. 핀은 인근 주차장 대표점이며, 포토존의 정확한 위치가 아닙니다. 주차 후 현장 안내를 따라 걸어가세요.\n\n" else "")+
    r.coordinateNote+"\n"+r.access+"\n"+r.hours,
   if(photo)"https://opentour.gtdc.or.kr/dzdocument.php?xch=intro&xid=opentour_map" else r.source,
   r.id in selectedRestIds)
 }
 return course+rests
}
internal fun visibleMapPlaces(places:List<MapPlace>,filter:MapPlaceKind?):List<MapPlace> = places.filter{filter==null||filter in it.kinds}

internal data class MapPinPosition(val x:Float,val y:Float)
/** Declutters touch targets only; a leader line must retain the true geographic anchor. */
internal fun separateMapPins(anchors:List<MapPinPosition>,left:Float,top:Float,right:Float,bottom:Float,spacing:Float):List<MapPinPosition> {
 val result=mutableListOf<MapPinPosition>()
 anchors.forEach { a ->
  val candidates= mutableListOf<MapPinPosition>()
  for(row in 0..6)for(col in 0..12){
   val dx=if(col%2==0)col/2 else -(col+1)/2
   val dy=if(row%2==0)row/2 else -(row+1)/2
   candidates+=MapPinPosition((a.x+dx*spacing).coerceIn(left,right.coerceAtLeast(left)),(a.y+dy*spacing).coerceIn(top,bottom.coerceAtLeast(top)))
  }
  result+=candidates.firstOrNull { c -> result.none { kotlin.math.abs(c.x-it.x)<spacing&&kotlin.math.abs(c.y-it.y)<spacing } }?:candidates.first()
 }
 return result
}
