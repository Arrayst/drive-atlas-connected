package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class MapPlacesTest {
 @Test fun nearbyPinsRemainSeparateInCompactMap(){val anchors=List(5){MapPinPosition(500f,190f)};val pins=separateMapPins(anchors,40f,144f,856f,222f,72f);assertEquals(5,pins.distinct().size);pins.forEachIndexed { i,p -> pins.drop(i+1).forEach{ assertTrue(kotlin.math.abs(p.x-it.x)>=72f||kotlin.math.abs(p.y-it.y)>=72f) } }}

 @Test fun compactMapFitsCourseWithoutFallingBackToCountryScale(){for(h in listOf(98,142)){val points=Catalog.routes.first().stops.map{it.point};val v=fittedViewport(points,980,h);assertTrue(v.zoom>8);points.forEach{val p=worldPoint(it,v.zoom);assertTrue(p.second-v.top in 0.0..h.toDouble())}}}

 @Test fun photoSourceHostsAreExactHttpsOnly(){assertTrue(safePublicUrl("https://opentour.gtdc.or.kr/dzdocument.php?xch=intro"));assertTrue(safePublicUrl("https://gtdc.or.kr/opentour/"));assertFalse(safePublicUrl("https://opentour.gtdc.or.kr.evil.example/"));assertFalse(safePublicUrl("http://opentour.gtdc.or.kr/"))}

 @Test fun officialRestPointsArePreserved(){Catalog.routes.forEach { r -> val places=mapPlaces(r);RestFacilities.forRoute(r.id).forEach { f -> assertEquals(f.point,places.first{it.id=="rest-${f.id}"}.point) } }}
 @Test fun photoPlaceIsParkingRepresentativeNotInventedPhotoCoordinate(){val p=mapPlaces(Catalog.routes.first()).single{MapPlaceKind.PHOTO in it.kinds};assertEquals(RestFacilities.find("hourglass","coast")!!.point,p.point);assertTrue(p.description.contains("정확한 위치가 아닙니다"));assertTrue(p.source!!.startsWith("https://opentour.gtdc.or.kr/"))}
 @Test fun unverifiedPhotoPlacesAreNotCreated(){Catalog.routes.drop(1).forEach{assertTrue(visibleMapPlaces(mapPlaces(it),MapPlaceKind.PHOTO).isEmpty())}}
 @Test fun selectedRestIsScopedAndDoesNotAlterCourse(){val r=Catalog.routes.first();val p=mapPlaces(r,setOf("hourglass","animation"));assertEquals(listOf("rest-hourglass"),p.filter{it.selectedRest}.map{it.id});assertEquals(3,visibleMapPlaces(p,MapPlaceKind.COURSE).size);assertEquals(5,p.size)}
 @Test fun editedItineraryDoesNotDuplicateCourseMarkers(){val edited=RestItinerary.route("coast",listOf("hourglass","simgok"))!!;assertEquals(5,mapPlaces(edited).size);assertEquals(3,visibleMapPlaces(mapPlaces(edited),MapPlaceKind.COURSE).size)}
}
