package com.driveatlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val Ink = Color(0xFF203D38)
internal val Paper = Color(0xFFF5F1E7)
internal val White = Color(0xFFFFFDF7)
internal val Muted = Color(0xFF687970)
internal val Line = Color(0xFFD7DDCF)
internal val Sage = Color(0xFFE5EBDE)
internal val Amber = Color(0xFFF1BD63)
internal val Coral = Color(0xFFD97658)
internal val Mono = FontFamily.Monospace
internal val Corner = RoundedCornerShape(12.dp)

class MainActivity : ComponentActivity() {
    private lateinit var engine: AtlasEngine
    private var revision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = AtlasEngine(LocalStore(this), if (BuildConfig.SIMULATOR) Mode.SIMULATOR else Mode.VEHICLE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = White,
                secondary = Coral, background = Paper, surface = White, onSurface = Ink,
                surfaceVariant = Sage, onSurfaceVariant = Muted, outline = Line)) { AtlasApp() }
        }
    }

    override fun onStop() {
        super.onStop()
        engine.setSafety(Safety.UNKNOWN)
        engine.setLocation(LocationStatus.LOST)
        revision++
    }

    @Composable internal fun AtlasApp() {
        revision
        var page by remember { mutableStateOf("discover") }
        var selected by remember { mutableStateOf(Catalog.routes.first()) }
        var message by remember { mutableStateOf("") }
        var debug by remember { mutableStateOf(false) }
        var curation by remember { mutableStateOf("recommended") }
        var detailReturn by remember { mutableStateOf("curation") }
        val filters = remember { GuideFilters() }
        var navDialog by remember { mutableStateOf(false) }
        var archiveDialog by remember { mutableStateOf(false) }
        var preview by remember { mutableStateOf<Bitmap?>(null) }
        val pageScroll = rememberScrollState()
        LaunchedEffect(page, selected.id) { pageScroll.scrollTo(0) }
        LaunchedEffect(engine.canEdit) { if (!engine.canEdit) { preview = null; navDialog = false; archiveDialog = false } }
        BackHandler(enabled = preview != null || debug || page != "discover") {
            when { debug -> debug = false; preview != null -> preview = null; else -> page = if (page == "detail") detailReturn else "discover" }
        }
        fun act(block: () -> Unit) {
            try { block() } catch (_: Exception) { message = "저장하지 못했어요. 주차 상태에서 다시 시도해 주세요." }
            revision++
        }
        fun open(r: Route) { detailReturn = page; selected = r; page = "detail"; message = "" }
        BoxWithConstraints(Modifier.fillMaxSize().background(Paper)) {
            val wide = maxWidth >= 1100.dp
            val compact = maxHeight < 740.dp
            val heroHeight = (maxHeight * .34f).coerceIn(360.dp, 400.dp)
            Row(Modifier.fillMaxSize()) {
                Sidebar(page, engine.canEdit, engine.snapshot, wide, compact,
                    onPage = { page = it; message = "" }, onDebug = { debug = true })
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (!engine.canEdit) SafetyPage(engine.safety, engine.snapshot.trip)
                    else Column(Modifier.fillMaxSize().verticalScroll(pageScroll)
                        .padding(horizontal = if (wide) 36.dp else 24.dp, vertical = 30.dp)) {
                        when (page) {
                            "discover" -> GuideHome(engine.snapshot, { if (curation != it) filters.mood.value = if (it == "theme") "탁 트인" else "전체"; curation = it; page = "curation" },
                                onTrip = { page = "trip" }, onAtlas = { page = "atlas" })
                            "curation" -> CurationPage(curation, filters, engine.snapshot.awards, ::open, { page = "discover" })
                            "records" -> RecordsPage(engine.snapshot, ::open)
                            "detail" -> DetailPage(selected, engine.snapshot.trip != null, heroHeight,
                                onBack = { page = detailReturn }, onStart = { navDialog = true }, onTrip = { page = "trip" })
                            "trip" -> TripPage(engine.snapshot.trip, engine.location, heroHeight,
                                onDiscover = { page = "discover" }, onEvidence = { act {
                                    engine.snapshot.trip?.let { t ->
                                        val r = Catalog.routes.first { it.id == t.routeId }
                                        val now = System.currentTimeMillis()
                                        message = if (engine.accept(Evidence(r.stops[t.nextStop.coerceAtMost(r.stops.lastIndex)].point, 5.0, now, true), now))
                                            "가상 위치 근거를 기록했어요." else "위치 근거를 받을 수 없어요. 위치 수신 상태를 확인해 주세요."
                                    }
                                } }, onFinish = { act {
                                    if (engine.finish(System.currentTimeMillis())) { page = "atlas"; message = "새로운 가상 여행 우표가 도감에 들어왔어요." }
                                    else message = "완료할 수 없어요. 위치 수신·주차 상태와 15초 이내 도착 근거가 필요해요."
                                } }, onAbandon = { archiveDialog = true })
                            "atlas" -> AtlasPage(engine.snapshot.awards, ::open, onExport = { r, a -> act {
                                if (engine.canEdit) {
                                    val file = exportCard(r, a)
                                    preview = BitmapFactory.decodeFile(file.absolutePath)
                                    message = ""
                                }
                            } })
                        }
                        Spacer(Modifier.height(26.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("가상 코스 · GPS/차량/내비 미연동 · 운전 중 조작 금지", color = Muted, fontSize = 13.sp)
                            Text("DRIVE ATLAS / DEMO 03", color = Muted, fontFamily = Mono, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(if (message.isNotBlank()) 72.dp else 8.dp))
                    }
                    if (message.isNotBlank() && engine.canEdit) Surface(
                        Modifier.align(Alignment.BottomCenter).padding(20.dp).widthIn(max = 900.dp),
                        color = Ink, contentColor = White, shape = Corner, shadowElevation = 8.dp) {
                        Row(Modifier.padding(start = 22.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, Modifier.weight(1f), fontSize = 16.sp)
                            TextButton(onClick = { message = "" }) { Text("닫기", color = Amber) }
                        }
                    }
                }
            }
        }
        if (debug && BuildConfig.SIMULATOR) DebugDialog(engine.safety, engine.location, onDismiss = { debug = false }) { safety, location ->
            act { safety?.let(engine::setSafety); location?.let(engine::setLocation) }
            debug = false
            message = ""
        }
        if (engine.canEdit && navDialog) DemoNavigationDialog(selected, onDismiss = { navDialog = false }, onStart = {
            act { if (engine.start(selected.id, System.currentTimeMillis())) { navDialog = false; page = "trip"; message = "내비 연결 성공을 체험했어요. 실제 내비는 실행하지 않았습니다." } }
        })
        if (engine.canEdit && archiveDialog) ArchiveDialog(onDismiss = { archiveDialog = false }) { status ->
            act { if (engine.archive(status, System.currentTimeMillis())) { archiveDialog = false; page = "records"; message = "여행 기록만 남겼어요. 완주 우표는 발급하지 않았습니다." } }
        }
        if (engine.canEdit) preview?.let { bitmap -> PreviewDialog(bitmap) { preview = null } }
    }

    private fun exportCard(r: Route, a: Award): File {
        check(engine.canEdit)
        val bitmap = Bitmap.createBitmap(1200, 1480, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val paper = android.graphics.Color.rgb(245, 241, 231)
        val ink = android.graphics.Color.rgb(32, 61, 56)
        canvas.drawColor(paper)
        paint.color = android.graphics.Color.rgb(255, 253, 247)
        canvas.drawRect(38f, 38f, 1162f, 1442f, paint)
        paint.color = paper
        for (x in 54..1150 step 28) { canvas.drawCircle(x.toFloat(), 38f, 8f, paint); canvas.drawCircle(x.toFloat(), 1442f, 8f, paint) }
        for (y in 54..1430 step 28) { canvas.drawCircle(38f, y.toFloat(), 8f, paint); canvas.drawCircle(1162f, y.toFloat(), 8f, paint) }
        paint.color = ink
        paint.typeface = Typeface.create("monospace", Typeface.BOLD)
        paint.textSize = 31f
        canvas.drawText("DRIVE ATLAS", 83f, 120f, paint)
        paint.textSize = 22f
        canvas.drawText("STAMP / ${routeNumber(r)}", 870f, 120f, paint)
        canvas.save()
        canvas.translate(82f, 166f)
        drawPixelLandscape(canvas, 1036, 700, Catalog.routes.indexOf(r))
        canvas.restore()
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 60f
        canvas.drawText(r.title, 83f, 974f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 30f
        canvas.drawText("${r.minutes}분의 가상 여정 · 3개의 장면", 85f, 1030f, paint)
        paint.color = android.graphics.Color.rgb(215, 221, 207)
        canvas.drawRect(83f, 1085f, 1117f, 1087f, paint)
        paint.color = ink
        paint.typeface = Typeface.create("monospace", Typeface.BOLD)
        paint.textSize = 29f
        canvas.drawText("${formatDate(a.timeMs)}  /  ${routeKind(r)}", 85f, 1157f, paint)
        paint.color = android.graphics.Color.rgb(217, 118, 88)
        paint.textSize = 38f
        canvas.drawText("SIMULATED JOURNEY", 85f, 1260f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.color = android.graphics.Color.rgb(104, 121, 112)
        paint.textSize = 24f
        canvas.drawText("가상 주행 기록 · 직접 그린 개념 풍경", 85f, 1320f, paint)
        paint.textSize = 22f
        canvas.drawText("실제 사진 및 방문 인증이 아닙니다.", 85f, 1361f, paint)
        val file = File(filesDir, "atlas-${a.tripId}.png")
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return file
    }
}

@Composable internal fun Sidebar(page: String, unlocked: Boolean, snapshot: Snapshot, wide: Boolean, compact: Boolean, onPage: (String) -> Unit, onDebug: () -> Unit) {
    Column(Modifier.width(if (wide) 208.dp else 164.dp).fillMaxHeight().background(Ink).padding(20.dp)) {
        PixelIcon("car", Amber, Modifier.size(if (compact) 37.dp else 53.dp).padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        Text("DRIVE\nATLAS", color = White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = 1.5.sp)
        if (!compact) Text("나의 여행 우표도감", color = Color(0xFFACBDA9), fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(if (compact) 20.dp else 45.dp))
        listOf(Triple("discover", "길 고르기", "compass"), Triple("records", "다녀온 길", "route"), Triple("atlas", "우표 도감", "book")).forEach { (id, title, icon) ->
            val active = unlocked && (page == id || page in listOf("detail", "curation", "trip") && id == "discover")
            Row(Modifier.fillMaxWidth().height(if (compact) 60.dp else 78.dp).clip(Corner)
                .background(if (active) Amber else Color.Transparent)
                .clickable(enabled = unlocked) { onPage(id) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(icon, if (active) Ink else Color(0xFFB8C8BA), Modifier.size(26.dp))
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(title, color = if (active) Ink else Color(0xFFDCE5D7), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (id == "atlas") Text("${snapshot.awards.map { it.routeId }.distinct().size.toString().padStart(2, '0')} STAMPS", color = if (active) Ink.copy(alpha = .65f) else Color(0xFF9EB39E), fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (snapshot.trip != null) TextButton(onClick = { onPage("trip") }, enabled = unlocked, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("진행 중인 여정  →", color = Amber, fontSize = 15.sp) }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF456158)))
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(if (unlocked) Amber else Color(0xFF9BAD9A)))
            Spacer(Modifier.width(8.dp))
            Text(if (unlocked) "가상 주차 상태" else "안전 확인 대기", color = White, fontSize = 13.sp)
        }
        Text(if (BuildConfig.SIMULATOR) "SIMULATOR DEMO" else "VEHICLE UNVERIFIED", color = Color(0xFF91A58F), fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
        if (BuildConfig.SIMULATOR) TextButton(onClick = onDebug, contentPadding = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("시뮬레이터 제어  +", color = Color(0xFFC9D5C3), fontSize = 12.sp)
        }
    }
}

@Composable internal fun PageHeading(eyebrow: String, title: String, subtitle: String, right: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-1).sp, modifier = Modifier.padding(top = 7.dp))
            Text(subtitle, fontSize = 16.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
        }
        right?.invoke()
    }
    Spacer(Modifier.height(27.dp))
}

@Composable internal fun DiscoverPage(snapshot: Snapshot, heroHeight: androidx.compose.ui.unit.Dp, onRoute: (Route) -> Unit, onTrip: () -> Unit, onAtlas: () -> Unit) {
    val coastCollected = snapshot.awards.any { it.routeId == "coast" }
    PageHeading("DRIVE PASSPORT / VOL. 01", "다음 우표는, 어느 길에서?", "작은 길 하나, 오래 간직할 여행 우표 한 장.") {
        Tag("가상 여행 가이드", Sage, Ink)
    }
    Surface(Modifier.fillMaxWidth().height(heroHeight), color = White, shape = Corner, border = BorderStroke(1.dp, Line)) {
        Row {
            Column(Modifier.weight(.42f).fillMaxHeight().padding(30.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Tag(if (coastCollected) "FIELD PICK  /  01" else "FIRST STAMP  /  01", Amber, Ink)
                Column {
                    Text(if (coastCollected) "다시 만나는,\n푸른 바다." else "바다를 따라,\n첫 번째 우표.", fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 50.sp, color = Ink, letterSpacing = (-1.4).sp)
                    Text("지도 위 세 장면을 잇고\n나만의 여행 도감을 채워보세요.", color = Muted, fontSize = 18.sp, lineHeight = 28.sp, modifier = Modifier.padding(top = 16.dp))
                }
                PrimaryButton(if (coastCollected) "해안 코스 다시 보기  →" else "해안 우표 만나러 가기  →", onClick = { onRoute(Catalog.routes.first()) })
            }
            Box(Modifier.weight(.58f).fillMaxHeight()) {
                PixelRouteMap(0, Modifier.fillMaxSize(), progress = 0)
                Tag("VIRTUAL MAP  /  COAST", White.copy(alpha = .94f), Ink, Modifier.align(Alignment.TopStart).padding(16.dp))
                Text("01 출발  ·  02 쉼터  ·  03 전망대", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).background(Ink, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }
    }
    Spacer(Modifier.height(23.dp))
    StatusStrip(snapshot, onTrip, onAtlas)
    Spacer(Modifier.height(27.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("오늘의 우표 여행지", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("3 ROUTES / 3 STAMPS", fontFamily = Mono, fontSize = 13.sp, color = Muted)
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Catalog.routes.forEachIndexed { index, route ->
            RouteCard(route, index, snapshot.awards.any { it.routeId == route.id }, Modifier.weight(1f)) { onRoute(route) }
        }
    }
}

@Composable internal fun StatusStrip(snapshot: Snapshot, onTrip: () -> Unit, onAtlas: () -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, Line, Corner).background(Sage.copy(alpha = .6f), Corner).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(if (snapshot.trip == null) "route" else "car", Ink, Modifier.size(34.dp))
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f)) {
            Text(if (snapshot.trip == null) "다음 여정의 첫 장면을 골라보세요" else "이어 가던 길이 있어요", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text(snapshot.trip?.let { "${Catalog.routes.first { r -> r.id == it.routeId }.title} · ${it.nextStop}/3 지점 확인" }
                ?: "길 고르기 → 세 장면 잇기 → 우표 모으기", fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 5.dp))
        }
        if (snapshot.trip != null) SecondaryButton("여정 이어보기", onTrip)
        else Text("${snapshot.awards.map { it.routeId }.distinct().size} / 3", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Ink)
        Spacer(Modifier.width(22.dp))
        TextButton(onClick = onAtlas) { Text("나의 도감  ↗", color = Ink, fontSize = 16.sp) }
    }
}

@Composable internal fun RouteCard(route: Route, index: Int, collected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = White, shape = StampEdge, border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(12.dp)) {
            Box {
                PixelLandscape(index, Modifier.fillMaxWidth().height(158.dp))
                Tag("0${index + 1}", White, Ink, Modifier.padding(12.dp))
                if (collected) Tag("수집한 우표 ✓", Ink, White, Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 17.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Eyebrow(listOf("COAST / 바다", "FOREST / 숲", "LAKE / 호수")[index])
                    Text("${route.minutes}분", fontFamily = Mono, color = Ink, fontSize = 14.sp)
                }
                Text(route.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 9.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(route.subtitle.substringAfter(" · "), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 7.dp))
                Row(Modifier.fillMaxWidth().padding(top = 17.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("3개의 장면", color = Muted, fontSize = 14.sp)
                    Text("길 살펴보기  →", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable internal fun DetailPage(route: Route, hasTrip: Boolean, heroHeight: androidx.compose.ui.unit.Dp, onBack: () -> Unit, onStart: () -> Unit, onTrip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("←  모든 코스", fontSize = 16.sp) }
        Tag("FIELD GUIDE  /  ${routeNumber(route)}", Sage, Ink)
    }
    Spacer(Modifier.height(15.dp))
    PageHeading("${routeKind(route)} / VIRTUAL ROUTE", route.title, route.subtitle.substringAfter(" · "))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(25.dp)) {
        Column(Modifier.weight(.59f)) {
            Box(Modifier.fillMaxWidth().height(heroHeight + 70.dp).clip(Corner)) {
                PixelRouteMap(Catalog.routes.indexOf(route), Modifier.fillMaxSize(), progress = 0)
                Tag("${route.minutes} MIN  ·  3 SCENES", White, Ink, Modifier.padding(20.dp))
            }
            Spacer(Modifier.height(21.dp))
            Surface(color = Sage, shape = Corner) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon("book", Ink, Modifier.size(36.dp))
                    Spacer(Modifier.width(18.dp))
                    Column { Text("세 장면이 한 장의 우표로", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("세 지점을 순서대로 확인하고 주차 후 완료하면\n이 풍경을 나의 도감에 남길 수 있어요.", fontSize = 16.sp, color = Muted, lineHeight = 25.sp, modifier = Modifier.padding(top = 7.dp)) }
                }
            }
        }
        Surface(Modifier.weight(.41f), color = White, border = BorderStroke(1.dp, Line), shape = Corner) {
            Column(Modifier.padding(28.dp)) {
                Eyebrow("YOUR ITINERARY")
                Text("이 길에서 만날 장면", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
                route.stops.forEachIndexed { i, stop -> StopRow(i, stop.name, false, false, if (i == 2) "여정의 마지막 풍경" else if (i == 1) "잠시 머무는 쉼표" else "오늘의 출발점", last = i == 2) }
                Spacer(Modifier.height(27.dp))
                PrimaryButton(if (hasTrip) "진행 중인 여정 보기  →" else "이 길로 떠나기  →", onClick = if (hasTrip) onTrip else onStart, modifier = Modifier.fillMaxWidth())
                Text(if (hasTrip) "진행 중인 여정을 마친 뒤 새 길을 시작할 수 있어요." else "가상 여정을 시작합니다. 실제 길 안내는 제공하지 않아요.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
    Spacer(Modifier.height(26.dp))
    NoteBlock("시연용으로 만든 길이에요", "코스와 지점은 모두 가상 데이터입니다. 실제 장소·운영시간·도로 상태를 안내하지 않습니다.")
}

@Composable internal fun TripPage(trip: Trip?, location: LocationStatus, heroHeight: androidx.compose.ui.unit.Dp, onDiscover: () -> Unit, onEvidence: () -> Unit, onFinish: () -> Unit, onAbandon: () -> Unit) {
    if (trip == null) {
        PageHeading("YOUR JOURNEY", "아직 펼치지 않은 여정", "마음이 가는 풍경에서 시작해 보세요.")
        Surface(color = White, shape = Corner, border = BorderStroke(1.dp, Line)) {
            Row(Modifier.fillMaxWidth().height(heroHeight + 100.dp)) {
                PixelLandscape(1, Modifier.weight(.57f).fillMaxHeight())
                Column(Modifier.weight(.43f).fillMaxHeight().padding(38.dp), verticalArrangement = Arrangement.Center) {
                    Eyebrow("READY WHEN YOU ARE")
                    Text("다음 모퉁이엔\n어떤 풍경이 있을까요?", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp, color = Ink, modifier = Modifier.padding(vertical = 22.dp))
                    Text("바다, 숲, 호수.\n오늘의 길을 고르면 여정이 시작돼요.", fontSize = 18.sp, color = Muted, lineHeight = 29.sp)
                    Spacer(Modifier.height(27.dp))
                    PrimaryButton("첫 번째 길 찾기  →", onDiscover)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        NoteBlock("내 속도로 남기는 여행", "여정 진행과 도감 수집은 주차 상태에서만 조작할 수 있어요. 진행 중인 여정은 앱을 닫아도 보관됩니다.")
        return
    }
    val route = Catalog.routes.first { it.id == trip.routeId }
    PageHeading("JOURNEY IN PROGRESS", route.title, "한 장면씩, 오늘의 길을 채워가는 중") {
        Tag("SIMULATED · 가상 여정", Amber, Ink)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(25.dp)) {
        Column(Modifier.weight(.56f)) {
            Box(Modifier.fillMaxWidth().height(heroHeight + 80.dp).clip(Corner)) {
                PixelRouteMap(Catalog.routes.indexOf(route), Modifier.fillMaxSize(), progress = trip.nextStop)
                Tag("${trip.nextStop} / 3  SCENES", White, Ink, Modifier.padding(20.dp))
            }
            Spacer(Modifier.height(20.dp))
            Surface(color = Ink, shape = Corner) {
                Row(Modifier.fillMaxWidth().padding(26.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${trip.nextStop.toString().padStart(2, '0')}", fontSize = 48.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, color = Amber)
                    Text(" / 03", fontSize = 19.sp, fontFamily = Mono, color = Color(0xFF9EB39E), modifier = Modifier.padding(end = 25.dp))
                    Column { Text(if (trip.nextStop == 3) "마지막 장면에 도착했어요" else "${route.stops[trip.nextStop].name}을 향해서", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(if (trip.nextStop == 3) "최신 도착 근거와 주차 상태로 완료하세요." else "순서대로 확인한 위치 근거만 기록됩니다.", color = Color(0xFFBDCEBA), fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
        Surface(Modifier.weight(.44f), color = White, shape = Corner, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.padding(27.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Eyebrow("JOURNEY LOG")
                    Tag(if (location == LocationStatus.AVAILABLE) "가상 위치 수신" else "위치 수신 없음", if (location == LocationStatus.AVAILABLE) Sage else Amber, Ink)
                }
                Spacer(Modifier.height(24.dp))
                route.stops.forEachIndexed { i, stop -> StopRow(i, stop.name, i < trip.nextStop, i == trip.nextStop, if (i < trip.nextStop) "위치 근거 확인" else if (i == trip.nextStop) "다음에 확인할 장면" else "아직 만나지 않은 풍경", last = i == 2) }
                Spacer(Modifier.height(24.dp))
                if (BuildConfig.SIMULATOR) SecondaryButton(if (trip.nextStop == 3) "도착 위치 다시 확인 · SIM" else "다음 지점 근거 입력 · SIM", onEvidence, Modifier.fillMaxWidth())
                Spacer(Modifier.height(11.dp))
                PrimaryButton("주차 후 여정 완료  ✓", onFinish, Modifier.fillMaxWidth(), enabled = trip.nextStop == 3)
                Text("완료하려면 15초 이내 도착 근거가 필요해요.\n오래되었다면 도착 위치를 다시 확인하세요.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = onAbandon, modifier = Modifier.padding(top = 10.dp)) { Text("미확인·중단 기록 남기기", color = Muted, fontSize = 14.sp) }
            }
        }
    }
    Spacer(Modifier.height(26.dp))
    NoteBlock("운전할 때는 풍경보다 안전을 먼저", "이 화면은 실제 차량·GPS와 연결되지 않은 가상 여정입니다. 주행 상태이거나 차량 상태를 모르면 조작이 잠깁니다.")
}

@Composable internal fun AtlasPage(awards: List<Award>, onRoute: (Route) -> Unit, onExport: (Route, Award) -> Unit) {
    val collected = awards.map { it.routeId }.distinct().size
    PageHeading("TRAVEL STAMP ALBUM / VOL. 01", "차곡차곡, 나의 여행 우표", "지나온 길을 한 장씩 붙여 만드는 나만의 도감.") {
        Column(horizontalAlignment = Alignment.End) {
            Text(collected.toString().padStart(2, '0'), fontSize = 44.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, color = Ink)
            Eyebrow("COLLECTED STAMPS")
        }
    }
    Surface(color = Ink, shape = Corner) {
        Row(Modifier.fillMaxWidth().padding(27.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelIcon("book", Amber, Modifier.size(46.dp))
            Spacer(Modifier.width(23.dp))
            Column(Modifier.weight(1f)) {
                Text(if (collected == 0) "첫 번째 우표를 기다리고 있어요" else "${collected}종의 여행 우표를 모았어요", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("바다 · 숲 · 호수  /  첫 번째 여행 우표집", color = Color(0xFFB9CBB4), fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
            repeat(3) { index -> Box(Modifier.padding(start = 8.dp).size(23.dp).background(if (index < collected) Amber else Color(0xFF4C6557), RoundedCornerShape(3.dp))) }
            Text("$collected / 3", color = Amber, fontSize = 24.sp, fontFamily = Mono, modifier = Modifier.padding(start = 24.dp))
        }
    }
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Catalog.routes.forEachIndexed { index, route ->
            val award = awards.lastOrNull { it.routeId == route.id }
            Surface(Modifier.weight(1f), color = White, shape = StampEdge, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(15.dp)) {
                    Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Eyebrow("TRAVEL STAMP  0${index + 1}")
                        PixelIcon(if (award == null) "lock" else "check", if (award == null) Muted else Coral, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(6.dp))) {
                        PixelLandscape(index, Modifier.fillMaxSize())
                        if (award == null) Box(Modifier.fillMaxSize().background(Paper.copy(alpha = .36f)))
                        Tag(if (award == null) "미수집 우표" else "수집 완료 · SIMULATED", if (award == null) White else Amber, Ink,
                            Modifier.align(Alignment.BottomStart).padding(12.dp))
                    }
                    Column(Modifier.padding(10.dp)) {
                        Text(route.title, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (award == null) "${route.minutes}분 · 3개 지점을 모아 완성" else "${formatDate(award.timeMs)}  ·  가상 주행 기록", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 11.dp))
                        Spacer(Modifier.height(24.dp))
                        if (award == null) SecondaryButton("이 우표 만나러 가기  →", { onRoute(route) }, Modifier.fillMaxWidth())
                        else PrimaryButton("우표 PNG 만들기  ↗", { onExport(route, award) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    NoteBlock("나의 여행 우표, 한 장으로 간직해요", "수집한 우표는 PNG로 만들어 앱 안에서 크게 볼 수 있어요. 가상 주행 기록이며 실제 방문 인증이나 사진이 아닙니다.")
    if (awards.size > collected) {
        Spacer(Modifier.height(24.dp))
        Text("여정 기록 · ${awards.size}회", fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Bold)
        awards.reversed().forEach { award ->
            val route = Catalog.routes.firstOrNull { it.id == award.routeId } ?: return@forEach
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(award.timeMs), color = Muted, fontFamily = Mono, modifier = Modifier.width(155.dp))
                Text(route.title, Modifier.weight(1f), color = Ink, fontSize = 17.sp)
                TextButton(onClick = { onExport(route, award) }) { Text("PNG 보기  ↗") }
            }
        }
    }
}

@Composable internal fun SafetyPage(safety: Safety, trip: Trip?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(42.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("SAFETY FIRST / DRIVE ATLAS")
            Tag(if (safety == Safety.MOVING) "주행 상태" else "차량 상태 미확인", Sage, Ink)
        }
        Spacer(Modifier.height(44.dp))
        Surface(color = White, shape = Corner, border = BorderStroke(1.dp, Line)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 460.dp)) {
                Box(Modifier.weight(.50f).height(460.dp)) {
                    PixelLandscape(1, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .12f)))
                    Tag("PAUSE & ENJOY THE ROAD", White, Ink, Modifier.align(Alignment.BottomStart).padding(23.dp))
                }
                Column(Modifier.weight(.50f).padding(36.dp), verticalArrangement = Arrangement.Center) {
                    PixelIcon("lock", Coral, Modifier.size(44.dp))
                    Text("잠시,\n운전에 집중해요.", fontSize = 39.sp, lineHeight = 52.sp, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                    Text("안전한 주차가 확인되면\n다시 여행 도감을 펼칠 수 있어요.", fontSize = 20.sp, color = Muted, lineHeight = 32.sp, modifier = Modifier.padding(top = 22.dp))
                    Tag("탐색 · 여정 조작 · 카드 생성 잠금", Sage, Ink, Modifier.padding(top = 26.dp))
                }
            }
        }
        Spacer(Modifier.height(29.dp))
        if (trip != null) {
            Surface(color = Ink, shape = Corner) {
                Row(Modifier.fillMaxWidth().padding(27.dp), verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon("route", Amber, Modifier.size(38.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("진행 중인 여정은 안전하게 보관했어요", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(Catalog.routes.first { it.id == trip.routeId }.title, color = Color(0xFFBDCEBA), fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("${trip.nextStop} / 3", fontFamily = Mono, fontSize = 27.sp, color = Amber)
                }
            }
            Spacer(Modifier.height(25.dp))
        }
        NoteBlock("멈춰 있다고 주차 상태는 아니에요", "신호 대기나 속도 0만으로 잠금을 해제하지 않습니다. 차량 상태가 확인되지 않을 때도 안전을 위해 잠금을 유지합니다.")
        Text(if (BuildConfig.SIMULATOR) "SIMULATOR DEMO · 실제 차량 신호는 연결되지 않았습니다. 개발용 가상 상태는 왼쪽 아래에서 제어합니다."
            else "VEHICLE UNVERIFIED · 실차 어댑터가 연결되지 않아 주차 상태를 확인할 수 없습니다.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 25.dp))
    }
}

@Composable internal fun StopRow(index: Int, title: String, done: Boolean, current: Boolean, subtitle: String, last: Boolean) {
    Row(Modifier.fillMaxWidth().height(97.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(42.dp).background(if (done) Ink else if (current) Amber else Sage, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                if (done) PixelIcon("check", Amber, Modifier.size(22.dp))
                else Text("0${index + 1}", fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(Line))
        }
        Column(Modifier.padding(start = 17.dp, top = 1.dp)) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable internal fun NoteBlock(title: String, body: String) {
    Row(Modifier.fillMaxWidth().border(1.dp, Line, Corner).padding(23.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelIcon("spark", Coral, Modifier.size(30.dp))
        Spacer(Modifier.width(18.dp))
        Column { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(body, fontSize = 15.sp, color = Muted, lineHeight = 24.sp, modifier = Modifier.padding(top = 7.dp)) }
    }
}

@Composable internal fun DebugDialog(safety: Safety, location: LocationStatus, onDismiss: () -> Unit, onChange: (Safety?, LocationStatus?) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 670.dp).heightIn(max = 680.dp).padding(24.dp), color = Paper, shape = Corner) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(30.dp)) {
                Eyebrow("DEVELOPMENT TOOLS / DEBUG ONLY")
                Text("시뮬레이터 제어", color = Ink, fontWeight = FontWeight.Bold, fontSize = 29.sp, modifier = Modifier.padding(top = 10.dp))
                Text("실제 차량 상태나 위치는 변경하지 않습니다.\nRelease 앱에는 이 패널과 가상 주차 해제가 없습니다.", color = Muted, fontSize = 16.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
                Tag("$safety  /  $location", Sage, Ink)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("가상 주차 · 위치 정상", { onChange(Safety.PARKED, LocationStatus.AVAILABLE) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(11.dp))
                SecondaryButton("가상 주행으로 전환", { onChange(Safety.MOVING, null) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(11.dp))
                SecondaryButton("차량 상태 미확인", { onChange(Safety.UNKNOWN, null) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(11.dp))
                SecondaryButton("위치 수신 끊기", { onChange(null, LocationStatus.LOST) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("닫기", fontSize = 17.sp) }
            }
        }
    }
}

@Composable internal fun PreviewDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.82f).fillMaxHeight(.92f), color = Paper, shape = Corner) {
            Column(Modifier.padding(26.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Eyebrow("YOUR TRAVEL STAMP"); Text("오늘의 길을 담은 여행 우표", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 6.dp)) }
                    SecondaryButton("미리보기 닫기  ×", onDismiss)
                }
                Spacer(Modifier.height(20.dp))
                Image(bitmap.asImageBitmap(), "생성된 가상 풍경 카드 PNG", Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(18.dp))
                Text("PNG 생성 완료 · 앱 내부 보관용 / 외부 공유·갤러리 저장은 아직 연결되지 않았어요.", color = Muted, fontSize = 15.sp)
            }
        }
    }
}

@Composable internal fun Eyebrow(text: String) { Text(text, color = Muted, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
@Composable internal fun Tag(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(text, modifier.background(background, RoundedCornerShape(4.dp)).padding(horizontal = 11.dp, vertical = 7.dp), color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .3.sp)
}
@Composable internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 57.dp), shape = RoundedCornerShape(7.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = White, disabledContainerColor = Sage, disabledContentColor = Muted)) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp), shape = RoundedCornerShape(7.dp), border = BorderStroke(1.dp, Line), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 13.dp)) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink) }
}
@Composable internal fun PixelIcon(name: String, color: Color, modifier: Modifier = Modifier) {
    val rows = when (name) {
        "car" -> listOf("  #######   ", " ##     ##  ", "##  ##   ###", "############", "###      ###", " ##      ## ")
        "compass" -> listOf("    #    ", "  #####  ", " ## # ## ", "##  #  ##", "#########", "##  #  ##", " ## # ## ", "  #####  ", "    #    ")
        "route" -> listOf(" ##      ", "####     ", " ##      ", "  ###### ", "       # ", " ####### ", " #       ", " ###  ## ", "     ####", "      ## ")
        "book" -> listOf("#### ####", "#  ###  #", "#   #   #", "#   #   #", "#   #   #", "#   #   #", "##### ###")
        "lock" -> listOf("  #####  ", " ##   ## ", " #     # ", "#########", "#### ####", "#### ####", "#########")
        "check" -> listOf("       ##", "      ## ", "     ##  ", "##  ##   ", " ####    ", "  ##     ")
        else -> listOf("    #    ", "    #    ", "   ###   ", "#########", "   ###   ", "    #    ", "    #    ")
    }
    Canvas(modifier) {
        val unit = minOf(size.width / rows.maxOf { it.length }, size.height / rows.size)
        val left = (size.width - unit * rows.maxOf { it.length }) / 2
        val top = (size.height - unit * rows.size) / 2
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c == '#') drawRect(color, Offset(left + x * unit, top + y * unit), Size(unit + .1f, unit + .1f)) } }
    }
}
internal fun routeNumber(route: Route) = "0${Catalog.routes.indexOf(route) + 1}"
internal fun routeKind(route: Route) = when (route.id) { "coast" -> "COASTLINE"; "forest" -> "FOREST"; else -> "LAKESIDE" }
internal fun formatDate(timeMs: Long) = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(timeMs))

/** Perforated paper edge, shared by available and collected travel stamps. */
internal val StampEdge = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { 3.5.dp.toPx() }
        val step = with(density) { 18.dp.toPx() }
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            var x = step / 2
            while (x < size.width) {
                addOval(Rect(x - radius, -radius, x + radius, radius))
                addOval(Rect(x - radius, size.height - radius, x + radius, size.height + radius))
                x += step
            }
            var y = step / 2
            while (y < size.height) {
                addOval(Rect(-radius, y - radius, radius, y + radius))
                addOval(Rect(size.width - radius, y - radius, size.width + radius, y + radius))
                y += step
            }
        }
        return Outline.Generic(path)
    }
}
