package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class RealMapTest {
 @Test fun mercatorOriginIsWorldCenter(){val p=worldPoint(Point(0.0,0.0),10);assertEquals(131072.0,p.first,.001);assertEquals(131072.0,p.second,.001)}
 @Test fun northernPointsAreHigherOnMap(){assertTrue(worldPoint(Point(38.0,128.0),12).second<worldPoint(Point(37.0,128.0),12).second)}
 @Test fun fittedBoundsContainAllStops(){Catalog.routes.forEach{r->val v=fittedViewport(r.stops.map{it.point},800,540);r.stops.forEach{val p=worldPoint(it.point,v.zoom);assertTrue(p.first-v.left in 0.0..800.0);assertTrue(p.second-v.top in 0.0..540.0)}}}
 @Test fun externalMapDoesNotUseDeviceLocation(){Catalog.routes.forEach{val u=destinationUrl(it);assertTrue(u.startsWith("https://www.openstreetmap.org/"));assertTrue(u.contains("mlat=${it.stops.last().point.lat}"));assertFalse(u.contains("current"))}}
 @Test fun kakaoLinkIncludesAllThreePublicStops(){Catalog.routes.forEach{r->val u=directionsUrl(r);assertTrue(u.startsWith("https://map.kakao.com/link/by/car/"));r.stops.forEach{assertTrue(u.contains(",${it.point.lat},${it.point.lon}"))};assertFalse(u.contains(" "))}}
 @Test fun webFallbackRejectsUnsafeAndUnrelatedUrls(){assertTrue(safePublicUrl("https://map.kakao.com/link/by/car/test"));assertFalse(safePublicUrl("file:///etc/passwd"));assertFalse(safePublicUrl("javascript:alert(1)"));assertFalse(safePublicUrl("http://map.kakao.com"));assertFalse(safePublicUrl("https://map.kakao.com.evil.example/"));assertFalse(safePublicUrl("https://x@map.kakao.com/"))}
 @Test fun kakaoAppLinkKeepsPublicStopsInOrder(){Catalog.routes.forEach{r->val u=kakaoAppRouteUrl(r);assertEquals("kakaomap://route?sp=${r.stops[0].point.lat},${r.stops[0].point.lon}&vp=${r.stops[1].point.lat},${r.stops[1].point.lon}&ep=${r.stops[2].point.lat},${r.stops[2].point.lon}&by=car",u)}}
 @Test fun failedPageDoesNotBecomeLoadedOnFinish(){val state=PublicPageLoadState();state.start();assertTrue(state.canShowLoaded);state.fail();assertFalse(state.canShowLoaded);state.start();assertTrue(state.canShowLoaded)}
 @Test fun realRoutesAreNotFictionalCoordinates(){assertEquals(3,Catalog.routes.size);assertTrue(Catalog.routes.all{it.stops.size==3});assertTrue(Catalog.routes.all{r->r.stops.all{it.point.lat in 35.0..38.0&&it.point.lon in 126.0..130.0}})}
}
