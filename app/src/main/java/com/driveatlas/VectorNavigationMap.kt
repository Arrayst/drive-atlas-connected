package com.driveatlas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

/** One composition for both preview and replay retains session consent, never device GPS. */
@Composable internal fun CourseVectorNavigationMap(
 course: Route, route: NavPrototypeRoute?, position: Point?, modifier: Modifier = Modifier,
 replayDistance: Double? = null, showStatusLabel: Boolean = false, controlsEnabled: Boolean = true,
) {
 val context = LocalContext.current
 val road = remember(course.id) { readRoad(context, course.id) }
 val references = remember(course.id, route?.request) {
  route?.let { navigationReferenceMarkers(it.request) } ?: course.stops.mapIndexed { i, stop ->
   NavigationReferenceMarker("course-$i", stop.name, "${i + 1}", NavigationMarkerKind.COURSE, stop.point)
  }
 }
 VectorNavigationMap(route?.geometry ?: road.points, route, position, references, modifier, replayDistance, showStatusLabel, controlsEnabled)
}

@Composable private fun VectorNavigationMap(
 points: List<Point>, route: NavPrototypeRoute?, position: Point?, references: List<NavigationReferenceMarker>,
 modifier: Modifier, replayDistance: Double?, showStatusLabel: Boolean, controlsEnabled: Boolean,
) {
 val consent = remember { VectorMapConsentState() }
 VectorMapConnectionGate(VectorMapPurpose.SIM_REPLAY, consent, modifier, controlsEnabled) {
  ConnectedVectorNavigationMap(points, route, position, references, Modifier.fillMaxSize(), replayDistance, showStatusLabel, controlsEnabled)
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ConnectedVectorNavigationMap(
 points: List<Point>, route: NavPrototypeRoute?, position: Point?, references: List<NavigationReferenceMarker>,
 modifier: Modifier, replayDistance: Double?, showStatusLabel: Boolean, controlsEnabled: Boolean,
) {
 val context = LocalContext.current
 val density = LocalDensity.current
 var retry by remember { mutableIntStateOf(0) }
 var follow by remember(route) { mutableStateOf(route != null) }
 var overviewRequest by remember { mutableIntStateOf(0) }
 var map by remember(retry) { mutableStateOf<MapLibreMap?>(null) }
 var style by remember(retry) { mutableStateOf<Style?>(null) }
 var roadsVisible by remember(retry) { mutableStateOf(false) }
 var failed by remember(retry) { mutableStateOf(false) }
 var timedOut by remember(retry) { mutableStateOf(false) }
 val latestControls by rememberUpdatedState(controlsEnabled)
 val fraction = remember(route) { Animatable(1f) }
 var start by remember(route) { mutableStateOf(replayDistance ?: 0.0) }
 var end by remember(route) { mutableStateOf(replayDistance ?: 0.0) }
 val displayedDistance = start + (end - start) * fraction.value
 val progress = if (replayDistance != null) displayedDistance else 0.0
 val segment = route?.let { navigationProgressSegment(it.cumulative, progress) } ?: 0
 val window = remember(route, start, end, replayDistance != null) {
  if (route != null && replayDistance != null) navigationProgressSegment(route.cumulative, minOf(start, end))..navigationProgressSegment(route.cumulative, maxOf(start, end)) else 0..0
 }
 val cursor = route?.let { vectorRouteCursor(it, progress, segment) }
 val displayed = if (route != null && replayDistance != null) cursor else position
 val heading = if (route != null && replayDistance != null) prototypeGeometryHeading(route.geometry, route.cumulative, displayedDistance)?.toDouble() ?: 0.0 else 0.0
 LaunchedEffect(replayDistance) {
  start = displayedDistance; end = replayDistance ?: displayedDistance
  fraction.snapTo(0f); fraction.animateTo(1f, tween(850, easing = LinearEasing))
 }
 LaunchedEffect(controlsEnabled) { if (!controlsEnabled && route != null) follow = true }
 LaunchedEffect(retry) { delay(18_000); timedOut = true }
 val mapView = rememberVectorMapView(retry)
 DisposableEffect(mapView) {
  var disposed = false
  val failure = MapView.OnDidFailLoadingMapListener { if (!disposed) { failed = true; roadsVisible = false } }
  val rendered = MapView.OnDidFinishRenderingMapListener { fully ->
   if (!disposed && mapView.width > 0 && mapView.height > 0) {
    val actualRoads = map?.queryRenderedFeatures(RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat()), *NavigationVectorStyle.roadLayers).orEmpty()
    // Never retain the previous area's route when the current viewport is blank.
    if (fully || actualRoads.isEmpty()) roadsVisible = actualRoads.isNotEmpty() && !failed
   }
  }
  mapView.addOnDidFailLoadingMapListener(failure)
  mapView.addOnDidFinishRenderingMapListener(rendered)
  mapView.getMapAsync { ready -> if (!disposed) {
   map = ready
   ready.setMinZoomPreference(5.0); ready.setMaxZoomPreference(19.0)
   // Start at the actual public course, never a world/null-island fetch.
   ready.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(points.first().latLng()).zoom(navigationFollowZoom()).build()))
   ready.addOnCameraMoveStartedListener { reason -> if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE && latestControls) follow = false }
   ready.setStyle(Style.Builder().fromJson(NavigationVectorStyle.json())) { loaded -> if (!disposed) {
    loaded.addImage("sim-car", navigationCarBitmap())
    loaded.addImage("route-chevron", navigationChevronBitmap())
    references.forEach { marker -> loaded.addImage("reference-${marker.id}", navigationReferenceBitmap(marker)) }
    style = loaded
   } }
  } }
  onDispose { disposed = true; mapView.removeOnDidFailLoadingMapListener(failure); mapView.removeOnDidFinishRenderingMapListener(rendered) }
 }
 LaunchedEffect(map, controlsEnabled) { map?.uiSettings?.setAllGesturesEnabled(controlsEnabled) }
 LaunchedEffect(style, references) {
  style?.let { loaded ->
   references.forEach { loaded.addImage("reference-${it.id}", navigationReferenceBitmap(it)) }
   loaded.getSourceAs<GeoJsonSource>("references")?.setGeoJson(FeatureCollection.fromFeatures(references.map {
    Feature.fromGeometry(it.point.geoPoint()).apply { addStringProperty("icon", "reference-${it.id}") }
   }))
  }
 }
 LaunchedEffect(style, roadsVisible, failed) {
  // A loaded style alone is not evidence of a loaded road map.
  NavigationVectorStyle.routeLayers.forEach { id -> style?.getLayer(id)?.setProperties(PropertyFactory.visibility(if (roadsVisible && !failed) Property.VISIBLE else Property.NONE)) }
 }
 // Long geometry changes at the 1-second replay tick, not for every animation frame/vertex.
 // Animated links contain only that tick's real road vertices (10–100 m in this demo).
 LaunchedEffect(style, route, points, window, replayDistance != null) {
  style?.getSourceAs<GeoJsonSource>("route-passed")?.setGeoJson(vectorLineFeatures(if (replayDistance != null) route?.geometry?.take(window.first + 1).orEmpty() else emptyList()))
  style?.getSourceAs<GeoJsonSource>("route-remaining")?.setGeoJson(vectorLineFeatures(route?.geometry?.drop(window.last + 1) ?: points))
 }
 LaunchedEffect(style, route, segment, cursor, replayDistance != null) {
  style?.getSourceAs<GeoJsonSource>("route-passed-cursor")?.setGeoJson(vectorLineFeatures(if (route != null && cursor != null && replayDistance != null) route.geometry.subList(window.first, segment + 1) + cursor else emptyList()))
  style?.getSourceAs<GeoJsonSource>("route-remaining-cursor")?.setGeoJson(vectorLineFeatures(if (route != null && cursor != null) listOf(cursor) + route.geometry.subList(segment + 1, window.last + 2) else emptyList()))
 }
 LaunchedEffect(style, displayed, heading) {
  style?.getSourceAs<GeoJsonSource>("sim-position")?.setGeoJson(FeatureCollection.fromFeatures(displayed?.let { p ->
   listOf(Feature.fromGeometry(p.geoPoint()).apply { addNumberProperty("heading", heading) }) } ?: emptyList()))
 }
 BoxWithConstraints(modifier) {
  val width = with(density) { maxWidth.roundToPx() }; val height = with(density) { maxHeight.roundToPx() }
  AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
  LaunchedEffect(map, style, follow, displayed, heading, width, height) {
   val ready = map ?: return@LaunchedEffect
   if (style == null || width <= 0 || height <= 0) return@LaunchedEffect
   if (follow && displayed != null) {
    ready.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
     .target(displayed.latLng()).zoom(navigationFollowZoom()).tilt(38.0).bearing(heading)
     .padding(0.0, navigationFollowTopPadding(height).toDouble(), 0.0, 0.0).build()))
   }
  }
  LaunchedEffect(map, style, overviewRequest, route, points, width, height) {
   val ready = map ?: return@LaunchedEffect
   if (style == null || width <= 0 || height <= 0 || follow) return@LaunchedEffect
   if (overviewRequest > 0 || route == null) {
    ready.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().bearing(0.0).tilt(0.0).padding(0.0, 0.0, 0.0, 0.0).build()))
    val padding = with(density) { 96.dp.roundToPx() }.coerceAtMost(minOf(width, height) / 4)
    ready.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points.map { it.latLng() }).build(), padding))
   }
  }
  if (showStatusLabel) Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = White, shape = Corner) {
   Text("SIM · 공개 경로 모의 위치\n${if (follow) "진행 방향 위 · 전방 보기" else "지도 둘러보기"}", Modifier.padding(12.dp), color = Ink, fontSize = 16.sp)
  }
  Surface(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = if (showStatusLabel) 80.dp else 250.dp), color = White, shape = Corner) {
   Row {
    if (route != null) TextButton(onClick = { if (latestControls) { follow = true; overviewRequest = 0 } }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text(if (follow) "✓ 전방 보기" else "전방 보기", fontSize = 16.sp) }
    TextButton(onClick = { if (latestControls) { follow = false; overviewRequest++ } }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("전체 경로", fontSize = 16.sp) }
   }
  }
  if (!roadsVisible) Surface(Modifier.align(Alignment.Center).padding(32.dp).widthIn(max = 520.dp), color = White, shape = Corner) {
   Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(if (failed || timedOut) "이 영역의 도로 지도를 확인하지 못했어요. 빈 지도에는 경로·모의 차량을 표시하지 않습니다." else "실제 도로·건물 지도 불러오는 중…", color = Ink, fontSize = 18.sp)
    if (failed || timedOut) TextButton(onClick = { if (latestControls) retry++ }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("다시 불러오기") }
   }
  }
  MapProviderAttribution(controlsEnabled, Modifier.align(Alignment.BottomStart).fillMaxWidth())
 }
}

private fun Point.latLng() = LatLng(lat, lon)
private fun Point.geoPoint() = GeoPoint.fromLngLat(lon, lat)
private fun vectorLineFeatures(points: List<Point>): FeatureCollection = FeatureCollection.fromFeatures(
 if (points.size >= 2 && points.any { it != points.first() }) listOf(Feature.fromGeometry(LineString.fromLngLats(points.map { it.geoPoint() }))) else emptyList())

private fun navigationCarBitmap(): Bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888).also { bitmap ->
 val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
 paint.color = android.graphics.Color.argb(30, 24, 46, 70); canvas.drawCircle(36f, 39f, 32f, paint)
 paint.color = android.graphics.Color.WHITE; canvas.drawCircle(36f, 35f, 30f, paint)
 val arrow = Path().apply { moveTo(36f, 8f); lineTo(59f, 57f); lineTo(36f, 47f); lineTo(13f, 57f); close() }
 paint.color = android.graphics.Color.rgb(23, 105, 245); canvas.drawPath(arrow, paint)
 paint.color = android.graphics.Color.WHITE; paint.strokeWidth = 3f; paint.strokeCap = Paint.Cap.ROUND; canvas.drawLine(36f, 23f, 36f, 38f, paint)
}
private fun navigationChevronBitmap(): Bitmap = Bitmap.createBitmap(24, 20, Bitmap.Config.ARGB_8888).also { bitmap ->
 val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; strokeWidth = 3.6f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; style = Paint.Style.STROKE }
 Canvas(bitmap).drawPath(Path().apply { moveTo(8f, 4f); lineTo(16f, 10f); lineTo(8f, 16f) }, paint)
}
private fun navigationReferenceBitmap(marker: NavigationReferenceMarker): Bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).also { bitmap ->
 val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
 paint.color = android.graphics.Color.WHITE; canvas.drawCircle(24f, 24f, 23f, paint)
 paint.color = when (marker.kind) { NavigationMarkerKind.DEPARTURE -> android.graphics.Color.rgb(39, 54, 67); NavigationMarkerKind.REST -> android.graphics.Color.rgb(23, 107, 82); else -> android.graphics.Color.rgb(23, 105, 245) }
 canvas.drawCircle(24f, 24f, 20f, paint)
 paint.color = android.graphics.Color.WHITE; paint.textSize = 20f; paint.typeface = Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
 canvas.drawText(marker.badge, 24f, 24f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f, paint)
}
