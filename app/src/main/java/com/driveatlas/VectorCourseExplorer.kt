package com.driveatlas

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point as GeoPoint
import kotlin.math.roundToInt

/** Public course exploration is available in release; only SIM replay stays debug-only. */
@Composable internal fun VectorCourseExplorer(route: Route, modifier: Modifier = Modifier, controlsEnabled: Boolean = true) {
 val context = LocalContext.current
 val largeText = LocalDensity.current.fontScale > 1.5f
 val road = remember(route.id) { readRoad(context, route.id) }
 val selectedRestIds = remember(route.id) { FamilyStore(context).loadFamily().plans[route.id]?.effectiveRestFacilityIds.orEmpty().toSet() }
 val places = remember(route.id, selectedRestIds) { mapPlaces(route, selectedRestIds) }
 val consent = remember { VectorMapConsentState() }
 var filter by remember(route.id) { mutableStateOf<MapPlaceKind?>(null) }
 var selected by remember(route.id) { mutableStateOf<MapPlace?>(null) }
 var listPlaces by remember(route.id) { mutableStateOf<List<MapPlace>?>(null) }
 var offlineDemo by remember(route.id) { mutableStateOf(false) }
 var attributionOpen by remember { mutableStateOf(false) }
 val shown = remember(places, filter) { visibleMapPlaces(places, filter) }
 val latestControls by rememberUpdatedState(controlsEnabled)
 LaunchedEffect(controlsEnabled) { if (!controlsEnabled) { selected = null; listPlaces = null; attributionOpen = false } }
 Column(modifier.clipToBounds().background(White)) {
  Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
   Column(Modifier.weight(1f)) {
    Text("코스와 쉬어 갈 곳", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
    Text(if (largeText) "큰 글씨 · 장소는 목록에서 확인" else "파란 선: 참고 코스 · 점: 대표 좌표", fontSize = 14.sp, color = Muted)
   }
   TextButton(onClick = { if (latestControls) { listPlaces = shown; selected = null } }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("${shown.size}곳 · 목록", fontSize = 16.sp) }
  }
  Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color(0xFFEEF0EF))) {
   if (offlineDemo) Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("오프라인 상황 체험", fontSize = 20.sp, color = Ink)
    Text("연결을 멈췄어요. 빈 지도에는 경로와 핀을 그리지 않습니다. 장소 정보는 목록에서 확인할 수 있어요.", fontSize = 16.sp, color = Muted)
    TextButton(onClick = { if (latestControls) offlineDemo = false }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("온라인으로") }
   } else VectorMapConnectionGate(VectorMapPurpose.PUBLIC_COURSE, consent, Modifier.fillMaxSize(), controlsEnabled) {
    ConnectedCourseExplorer(road, places, shown, controlsEnabled, Modifier.fillMaxSize()) { members ->
     if (latestControls) { listPlaces = members; selected = members.singleOrNull() }
    }
   }
  }
  Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
   listOf<MapPlaceKind?>(null, MapPlaceKind.COURSE, MapPlaceKind.REST, MapPlaceKind.PHOTO).forEach { kind ->
    TextButton(onClick = { if (latestControls) filter = kind }, enabled = controlsEnabled, modifier = Modifier.weight(1f).heightIn(min = 64.dp), contentPadding = PaddingValues(2.dp),
     colors = ButtonDefaults.textButtonColors(contentColor = if (filter == kind) NavigationRouteBlue else Muted)) {
     Text(kind?.label ?: "전체", fontSize = 16.sp, fontWeight = if (filter == kind) FontWeight.Bold else FontWeight.Normal)
    }
   }
  }
  if (shown.isEmpty()) Text("이 필터에서 출처를 확인한 장소가 아직 없어요. 전체 목록은 그대로 볼 수 있습니다.", Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, color = Muted)
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
   if (BuildConfig.SIMULATOR) TextButton(onClick = { if (latestControls) offlineDemo = !offlineDemo }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text(if (offlineDemo) "온라인으로" else "오프라인 체험", fontSize = 15.sp) }
   Spacer(Modifier.weight(1f))
   TextButton(onClick = { if (latestControls) attributionOpen = true }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
    Column(horizontalAlignment = Alignment.End) {
     Text("OpenFreeMap · © OpenMapTiles", fontSize = 14.sp, color = Muted)
     Text("© OpenStreetMap contributors", fontSize = 14.sp, color = Muted)
    }
   }
  }
 }
 if (listPlaces != null || selected != null) MapPlaceDialog(route, listPlaces ?: shown, selected,
  onSelect = { if (latestControls) selected = it }, onDismiss = { selected = null; listPlaces = null }, onBack = { selected = null })
 if (attributionOpen) AlertDialog(onDismissRequest = { attributionOpen = false }, title = { Text("지도 데이터와 이용 안내") },
  text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
   Text("실제 공개 지도와 대표 좌표입니다. 도로 기준점은 주차·정차 가능 지점이 아닙니다. n곳 표시는 가까운 장소 묶음이며, 배지는 그중 한 장소의 실제 대표 좌표에 놓입니다.", fontSize = 16.sp)
   Text("OSRM 참고 경로 · 실시간 교통 미반영 · 인터넷 연결 필요", fontSize = 15.sp)
   for ((name, url) in listOf("OpenFreeMap · 이용약관" to NavigationVectorStyle.terms, "© OpenMapTiles" to "https://www.openmaptiles.org/", "© OpenStreetMap contributors" to "https://www.openstreetmap.org/copyright"))
    TextButton(onClick = { if (latestControls) openWeb(context, url) }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text(name, fontSize = 16.sp) }
  } }, confirmButton = { TextButton(onClick = { attributionOpen = false }, modifier = Modifier.heightIn(min = 64.dp)) { Text("닫기") } })
}

@Composable private fun ConnectedCourseExplorer(
 road: RoadGeometry, allPlaces: List<MapPlace>, shown: List<MapPlace>, controlsEnabled: Boolean,
 modifier: Modifier, onSelect: (List<MapPlace>) -> Unit,
) {
 val density = LocalDensity.current
 var retry by remember { mutableIntStateOf(0) }
 var overview by remember(road) { mutableIntStateOf(0) }
 var map by remember(retry) { mutableStateOf<MapLibreMap?>(null) }
 var style by remember(retry) { mutableStateOf<Style?>(null) }
 var roadsVisible by remember(retry) { mutableStateOf(false) }
 var failed by remember(retry) { mutableStateOf(false) }
 var timedOut by remember(retry) { mutableStateOf(false) }
 var projected by remember(retry) { mutableStateOf<List<ProjectedMapPlace>>(emptyList()) }
 var zoomMenu by remember { mutableStateOf(false) }
 val latestShown by rememberUpdatedState(shown)
 val latestControls by rememberUpdatedState(controlsEnabled)
 val view = rememberVectorMapView(retry)
 fun updateProjection() {
  val current = map ?: return
  projected = latestShown.map { place -> current.projection.toScreenLocation(LatLng(place.point.lat, place.point.lon)).let { ProjectedMapPlace(place, MapPinPosition(it.x, it.y)) } }
 }
 LaunchedEffect(retry) { delay(18_000); timedOut = true }
 DisposableEffect(view) {
  var disposed = false
  val failure = MapView.OnDidFailLoadingMapListener { if (!disposed) { failed = true; roadsVisible = false } }
  val rendered = MapView.OnDidFinishRenderingMapListener { fully ->
   if (!disposed && view.width > 0 && view.height > 0) {
    val roads = map?.queryRenderedFeatures(RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()), *NavigationVectorStyle.roadLayers).orEmpty()
    if (fully || roads.isEmpty()) roadsVisible = roads.isNotEmpty() && !failed
    updateProjection()
   }
  }
  val moved = MapLibreMap.OnCameraMoveListener { if (!disposed) updateProjection() }
  view.addOnDidFailLoadingMapListener(failure); view.addOnDidFinishRenderingMapListener(rendered)
  view.getMapAsync { ready -> if (!disposed) {
   map = ready; ready.setMinZoomPreference(5.0); ready.setMaxZoomPreference(19.0)
   ready.uiSettings.setAllGesturesEnabled(latestControls)
   ready.addOnCameraMoveListener(moved)
   ready.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(LatLng(road.points.first().lat, road.points.first().lon)).zoom(13.0).build()))
   ready.setStyle(Style.Builder().fromJson(NavigationVectorStyle.json())) { loaded -> if (!disposed) style = loaded }
  } }
  onDispose {
   disposed = true; map?.removeOnCameraMoveListener(moved)
   view.removeOnDidFailLoadingMapListener(failure); view.removeOnDidFinishRenderingMapListener(rendered)
  }
 }
 LaunchedEffect(map, controlsEnabled) { map?.uiSettings?.setAllGesturesEnabled(controlsEnabled) }
 LaunchedEffect(style, road) {
  style?.getSourceAs<GeoJsonSource>("route-remaining")?.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(road.points.map { GeoPoint.fromLngLat(it.lon, it.lat) })))))
  // The compact explorer's route is narrower than the full-window guidance route.
  style?.getLayer("route")?.setProperties(PropertyFactory.lineWidth(4.5f))
  style?.getLayer("route-casing")?.setProperties(PropertyFactory.lineWidth(7f))
 }
 LaunchedEffect(style, roadsVisible, failed) {
  for (id in listOf("route", "route-casing")) style?.getLayer(id)?.setProperties(PropertyFactory.visibility(if (roadsVisible && !failed) Property.VISIBLE else Property.NONE))
 }
 LaunchedEffect(map, shown) { updateProjection() }
 BoxWithConstraints(modifier) {
  val width = with(density) { maxWidth.roundToPx() }; val height = with(density) { maxHeight.roundToPx() }
  val zoomColumnFits = maxHeight >= 220.dp
  val half = with(density) { 32.dp.toPx() }; val rightInset = with(density) { 80.dp.toPx() }
  val targetsEnabled = mapPlaceMarkersEnabled(maxWidth.value, maxHeight.value, density.fontScale)
  val visible = if (targetsEnabled) projected.filter { mapPlaceTargetFits(it.position, width.toFloat(), height.toFloat(), half, rightInset) } else emptyList()
  val clusters = clusterMapPlaceTargets(visible, with(density) { 72.dp.toPx() })
  AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
  LaunchedEffect(map, style, road, allPlaces, width, height, overview) {
   val ready = map ?: return@LaunchedEffect
   if (style == null || width <= rightInset || height <= 0) return@LaunchedEffect
   val edge = with(density) { 46.dp.roundToPx() }.coerceAtMost(height / 3)
   val points = road.points + allPlaces.map { it.point }
   ready.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points.map { LatLng(it.lat, it.lon) }).build(), edge, edge, edge + rightInset.toInt(), edge))
   updateProjection()
  }
  if (roadsVisible && !failed) {
   Canvas(Modifier.fillMaxSize()) {
    clusters.filter { it.members.size > 1 }.flatMap { it.members }.forEach {
     drawCircle(Color.White, 3.5.dp.toPx(), Offset(it.position.x, it.position.y))
     drawCircle(Ink, 2.dp.toPx(), Offset(it.position.x, it.position.y))
    }
   }
   clusters.forEach { cluster -> key(cluster.id) {
    val description = if (cluster.members.size == 1) cluster.members.single().place.let { it.name + if (it.selectedRest) " · 가족 계획에 선택됨" else " · 장소 정보" }
     else "가까운 장소 ${cluster.members.size}곳 묶음 · " + cluster.members.joinToString(", ") { it.place.name } + " · 목록 열기"
    Box(Modifier.offset { IntOffset((cluster.anchor.x - half).roundToInt(), (cluster.anchor.y - half).roundToInt()) }.size(64.dp)
     .semantics { contentDescription = description }.clickable(enabled = controlsEnabled, role = Role.Button) { if (latestControls) onSelect(cluster.members.map { it.place }) }, contentAlignment = Alignment.Center) {
     CompactPlaceBadge(cluster)
    }
   } }
  }
  Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
   if (zoomColumnFits) {
    CompactMapControl("+", controlsEnabled) { if (latestControls) map?.animateCamera(CameraUpdateFactory.zoomIn()) }
    CompactMapControl("−", controlsEnabled) { if (latestControls) map?.animateCamera(CameraUpdateFactory.zoomOut()) }
   } else {
    Box {
     CompactMapControl("배율", controlsEnabled) { if (latestControls) zoomMenu = true }
     DropdownMenu(expanded = zoomMenu && controlsEnabled, onDismissRequest = { zoomMenu = false }) {
      DropdownMenuItem(text = { Text("확대 +", fontSize = 18.sp) }, modifier = Modifier.heightIn(min = 64.dp), onClick = { if (latestControls) map?.animateCamera(CameraUpdateFactory.zoomIn()); zoomMenu = false })
      DropdownMenuItem(text = { Text("축소 −", fontSize = 18.sp) }, modifier = Modifier.heightIn(min = 64.dp), onClick = { if (latestControls) map?.animateCamera(CameraUpdateFactory.zoomOut()); zoomMenu = false })
     }
    }
   }
   CompactMapControl("전체", controlsEnabled) { if (latestControls) overview++ }
  }
  if (!roadsVisible) Surface(Modifier.align(Alignment.Center).padding(start = 12.dp, end = 84.dp).widthIn(max = 440.dp), color = White, shape = Corner) {
   Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(if (failed || timedOut) "지도를 불러오지 못했어요. 장소는 목록에서 확인하세요." else "실제 도로 지도 불러오는 중…", color = Ink, fontSize = 17.sp)
    if (failed || timedOut) TextButton(onClick = { if (latestControls) retry++ }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("다시 불러오기") }
   }
  }
 }
}

@Composable private fun CompactMapControl(label: String, enabled: Boolean, onClick: () -> Unit) {
 Box(Modifier.size(64.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = when (label) { "+" -> "지도 확대"; "−" -> "지도 축소"; "배율" -> "지도 배율 선택"; else -> "전체 경로 보기" } }, contentAlignment = Alignment.Center) {
  Surface(Modifier.size(46.dp), color = White.copy(alpha = .98f), shape = RoundedCornerShape(13.dp), shadowElevation = 2.dp) {
   Box(contentAlignment = Alignment.Center) { Text(label, color = Ink, fontSize = if (label.length > 1) 15.sp else 24.sp, fontWeight = FontWeight.Medium) }
  }
 }
}
@Composable private fun CompactPlaceBadge(cluster: MapPlaceCluster) {
 val group = cluster.members.size > 1; val place = cluster.members.first().place
 val photo = !group && MapPlaceKind.PHOTO in place.kinds
 val rest = !group && MapPlaceKind.REST in place.kinds
 val color = when { group -> Ink; photo -> Color(0xFF7355B7); rest -> Color(0xFF26765F); else -> NavigationRouteBlue }
 Surface(Modifier.size(if (group) 38.dp else 32.dp), color = color, shape = if (rest || photo) RoundedCornerShape(10.dp) else CircleShape,
  border = androidx.compose.foundation.BorderStroke(if (cluster.members.any { it.place.selectedRest }) 2.5.dp else 1.5.dp, Color.White), shadowElevation = 2.dp) {
  Box(contentAlignment = Alignment.Center) {
   if (photo) Canvas(Modifier.size(18.dp)) {
    val stroke = Stroke(1.7.dp.toPx())
    drawRoundRect(Color.White, Offset(1.dp.toPx(), 4.dp.toPx()), Size(size.width - 2.dp.toPx(), size.height - 6.dp.toPx()), CornerRadius(2.dp.toPx()), style = stroke)
    drawCircle(Color.White, 3.dp.toPx(), Offset(size.width / 2, size.height / 2 + 1.dp.toPx()), style = stroke)
    drawLine(Color.White, Offset(6.dp.toPx(), 2.dp.toPx()), Offset(12.dp.toPx(), 2.dp.toPx()), 2.dp.toPx())
   } else Text(if (group) "${cluster.members.size}곳" else if (rest) "쉼" else place.badge, color = Color.White, fontSize = if (group) 13.sp else 16.sp, fontWeight = FontWeight.Bold)
  }
 }
}
