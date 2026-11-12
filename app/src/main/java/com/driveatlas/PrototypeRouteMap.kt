package com.driveatlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*

// Rendering only: original OSM tile bytes, cache, endpoints and attribution are unchanged.
internal val NavigationTileFilter=ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(.35f)})
internal val NavigationRouteBlue=Color(0xFF1769F5)
private val NavigationPassed=Color(0xFF8996A8)

/** SIM-only camera. Loads only visible tiles, never a route corridor or offline region. */
@Composable internal fun PrototypeRouteMap(route:NavPrototypeRoute,position:Point,modifier:Modifier=Modifier,replayDistance:Double?=null,showStatusLabel:Boolean=true,controlsEnabled:Boolean=true){
 val context=LocalContext.current
 BoxWithConstraints(modifier.clipToBounds().background(Sage)){
  val d=LocalDensity.current;val w=with(d){maxWidth.roundToPx()};val h=with(d){maxHeight.roundToPx()}
  var follow by remember(route){mutableStateOf(true)}
  var manual by remember(route,w,h){mutableStateOf<MapViewport?>(null)}
  LaunchedEffect(controlsEnabled){if(!controlsEnabled){follow=true;manual=null}}
  val fraction=remember(route){Animatable(1f)}
  var start by remember(route){mutableStateOf(replayDistance?:0.0)}
  var end by remember(route){mutableStateOf(replayDistance?:0.0)}
  val displayedDistance=start+(end-start)*fraction.value
  val displayed=if(replayDistance!=null)route.positionAt(displayedDistance) else position
  LaunchedEffect(replayDistance){
   start=displayedDistance;end=replayDistance?:displayedDistance;fraction.snapTo(0f)
   fraction.animateTo(1f,tween(850,easing=LinearEasing))
  }
  val viewport=if(follow)prototypeFollowViewport(displayed,w,h) else manual?:prototypeViewport(route.geometry,w,h)
  val latestViewport by rememberUpdatedState(viewport)
  val latestControlsEnabled by rememberUpdatedState(controlsEnabled)
  val tileKeys=prototypeVisibleTileKeys(viewport)
  val cache=remember(route){mutableStateMapOf<PrototypeTileKey,MapTile>()}
  var loaded by remember(route){mutableStateOf(false)}
  var loading by remember(route){mutableStateOf(false)}
  var retry by remember{mutableIntStateOf(0)}
  LaunchedEffect(route,tileKeys,retry){
   // Debounce drag gestures; a changed sub-tile camera position does not reload tiles.
   delay(250)
   loading=true
   try {
    tileKeys.forEach { key ->
     coroutineContext.ensureActive()
     if(cache[key]?.image==null){
      val tile=loadVisibleTiles(context,MapViewport(key.zoom,key.x*256.0,key.y*256.0,256,256)).firstOrNull()
      if(tile!=null)cache[key]=tile
     }
    }
    while(cache.size>96){cache.keys.firstOrNull{it !in tileKeys}?.let{cache.remove(it)}?:break}
    loaded=true
   } finally {loading=false}
  }
  val tiles=tileKeys.mapNotNull{cache[it]}
  val heading=if(replayDistance!=null)prototypeGeometryHeading(route.geometry,route.cumulative,displayedDistance) else null
  val pathOrigin=remember(route,viewport.zoom){worldPoint(route.geometry.first(),viewport.zoom)}
  val projected=remember(route,viewport.zoom){route.geometry.map{p->val q=worldPoint(p,viewport.zoom);Offset((q.first-pathOrigin.first).toFloat(),(q.second-pathOrigin.second).toFloat())}}
  val segment=navigationProgressSegment(route.cumulative,if(replayDistance!=null)displayedDistance else 0.0)
  val splitPaths=remember(projected,segment){
   fun path(from:Int,to:Int)=Path().apply{for(i in from..to){val p=projected[i];if(i==from)moveTo(p.x,p.y)else lineTo(p.x,p.y)}}
   path(0,segment) to path(segment+1,projected.lastIndex)
  }
  val references=remember(route){navigationReferenceMarkers(route.request)}
  val markerPixels=references.map{marker->val p=worldPoint(marker.point,viewport.zoom);NavigationMarkerPixel(marker,p.first-viewport.left,p.second-viewport.top)}
  val clusters=clusterNavigationMarkers(markerPixels,with(d){56.dp.toPx().toDouble()})
  val markerPaint=remember{android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD}}
  Canvas(Modifier.fillMaxSize().pointerInput(route,w,h){
   detectDragGestures { change,delta ->
    change.consume()
    if(latestControlsEnabled){
     val current=latestViewport
     manual=prototypePanViewport(current,delta.x.toDouble(),delta.y.toDouble());follow=false
    }
   }
  }){
   tiles.orEmpty().forEach{t->t.image?.let{drawImage(it,Offset((t.x*256-viewport.left).toFloat(),(t.y*256-viewport.top).toFloat()),colorFilter=NavigationTileFilter)}}
   if(tiles.orEmpty().any{it.image!=null}){
    fun pixel(p:Point):Offset{val q=worldPoint(p,viewport.zoom);return Offset((q.first-viewport.left).toFloat(),(q.second-viewport.top).toFloat())}
    val current=pixel(displayed)
    translate((pathOrigin.first-viewport.left).toFloat(),(pathOrigin.second-viewport.top).toFloat()){
     val q=worldPoint(if(replayDistance!=null)displayed else route.geometry.first(),viewport.zoom)
     val cursor=Offset((q.first-pathOrigin.first).toFloat(),(q.second-pathOrigin.second).toFloat())
     fun strokePart(path:Path,a:Offset,b:Offset,color:Color,width:Float){
      val stroke=Stroke(width,cap=StrokeCap.Round,join=StrokeJoin.Round)
      drawPath(path,color,style=stroke);drawLine(color,a,b,width,StrokeCap.Round)
     }
     // Passed geometry is drawn first. Future return legs remain blue even on the same road.
     if(replayDistance!=null){
      strokePart(splitPaths.first,projected[segment],cursor,White,10.dp.toPx())
      strokePart(splitPaths.first,projected[segment],cursor,NavigationPassed,5.dp.toPx())
     }
     strokePart(splitPaths.second,cursor,projected[segment+1],Ink.copy(alpha=.20f),16.dp.toPx())
     strokePart(splitPaths.second,cursor,projected[segment+1],White,13.dp.toPx())
     strokePart(splitPaths.second,cursor,projected[segment+1],NavigationRouteBlue,7.dp.toPx())
    }
    clusters.forEach{cluster->
     val center=Offset(cluster.x.toFloat(),cluster.y.toFloat())
     val radius=18.dp.toPx()
     if(center.x in -radius..(size.width+radius)&&center.y in -radius..(size.height+radius)&&(center-current).getDistance()>56.dp.toPx()){
      val marker=cluster.members.first().marker
      val grouped=cluster.members.size>1
      val fill=if(grouped)Ink else when(marker.kind){NavigationMarkerKind.DEPARTURE->Ink;NavigationMarkerKind.COURSE->White;NavigationMarkerKind.REST->Color(0xFF176B52)}
      drawCircle(White,radius+3.dp.toPx(),center)
      if(!grouped&&marker.kind==NavigationMarkerKind.REST){
       drawRoundRect(fill,center-Offset(radius,radius),androidx.compose.ui.geometry.Size(radius*2,radius*2),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
      }else if(!grouped&&marker.kind==NavigationMarkerKind.DEPARTURE){
       val diamond=Path().apply{moveTo(center.x,center.y-radius);lineTo(center.x+radius,center.y);lineTo(center.x,center.y+radius);lineTo(center.x-radius,center.y);close()};drawPath(diamond,fill)
      }else{drawCircle(fill,radius,center);if(!grouped)drawCircle(NavigationRouteBlue,radius,center,style=Stroke(2.dp.toPx()))}
      markerPaint.color=(if(!grouped&&marker.kind==NavigationMarkerKind.COURSE)NavigationRouteBlue else White).toArgb()
      markerPaint.textSize=15.sp.toPx()
      val baseline=center.y-(markerPaint.fontMetrics.ascent+markerPaint.fontMetrics.descent)/2
      drawIntoCanvas{it.nativeCanvas.drawText(if(grouped)"${cluster.members.size}" else marker.badge,center.x,baseline,markerPaint)}
     }
    }
    drawCircle(Ink.copy(alpha=.12f),32.dp.toPx(),current+Offset(0f,3.dp.toPx()))
    drawCircle(White,30.dp.toPx(),current)
    if(heading!=null)rotate(heading,current){
     val arrow=Path().apply{moveTo(current.x,current.y-25.dp.toPx());lineTo(current.x+20.dp.toPx(),current.y+19.dp.toPx());lineTo(current.x,current.y+10.dp.toPx());lineTo(current.x-20.dp.toPx(),current.y+19.dp.toPx());close()}
     drawPath(arrow,NavigationRouteBlue)
     drawLine(White,current+Offset(0f,-10.dp.toPx()),current+Offset(0f,4.dp.toPx()),3.dp.toPx(),StrokeCap.Round)
    }else{
     drawCircle(NavigationRouteBlue,18.dp.toPx(),current)
     drawCircle(White,5.dp.toPx(),current)
    }
   }
  }
  if(showStatusLabel)Surface(Modifier.align(Alignment.TopStart).padding(12.dp),color=White.copy(alpha=.96f),shape=Corner){Text("SIM · 파란 포인터: 모의 위치\n${if(follow)"위치 따라가기 · 북쪽 위" else "지도 둘러보기 · 북쪽 위"}",Modifier.padding(12.dp),color=Ink,fontSize=16.sp)}
  Surface(Modifier.align(Alignment.BottomEnd).padding(end=12.dp,bottom=if(showStatusLabel)66.dp else 250.dp),color=White,shape=Corner){Row{
   TextButton(onClick={if(latestControlsEnabled){follow=true;manual=null}},enabled=controlsEnabled,modifier=Modifier.heightIn(min=64.dp)){Text(if(follow)"✓ 따라가기" else "따라가기",fontSize=16.sp)}
   TextButton(onClick={if(latestControlsEnabled){follow=false;manual=prototypeViewport(route.geometry,w,h)}},enabled=controlsEnabled,modifier=Modifier.heightIn(min=64.dp)){Text("전체 경로",fontSize=16.sp)}
  }}
  if(!loaded||loading&&tiles.none{it.image!=null})Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("지도 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(30.dp),color=White,shape=Corner){Column(Modifier.padding(20.dp)){Text("지도를 불러오지 못했어요. 빈 지도에 경로를 그리지 않습니다.",color=Ink);TextButton(onClick={if(latestControlsEnabled)retry++},enabled=controlsEnabled,modifier=Modifier.heightIn(min=64.dp)){Text("다시 불러오기")}}}
  else if(!loading&&(tiles.size<tileKeys.size||tiles.any{it.image==null}))Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역 불러오기 실패",Modifier.padding(12.dp),color=Ink)}
  Surface(Modifier.align(Alignment.BottomStart).fillMaxWidth(),color=White.copy(alpha=.96f)){TextButton(onClick={if(latestControlsEnabled)openWeb(context,"https://www.openstreetmap.org/copyright")},enabled=controlsEnabled){Text("출: 출발·귀가 · 1–3: 코스 · 쉼: 선택 쉼터 · 진한 숫자: 지점 묶음\n© OpenStreetMap contributors · OSRM 도로 경로 · 실시간 교통 미반영",fontSize=13.sp,color=Ink)}}
 }
}

internal fun prototypeViewport(points:List<Point>,width:Int,height:Int):MapViewport {
 require(points.isNotEmpty()&&width>0&&height>0)
 var z=0
 val usableWidth=(width-96).coerceAtLeast(width/2)
 val usableHeight=(height-160).coerceAtLeast(height/2)
 for(candidate in 0..16){val p=points.map{worldPoint(it,candidate)};if(p.maxOf{it.first}-p.minOf{it.first}<=usableWidth&&p.maxOf{it.second}-p.minOf{it.second}<=usableHeight)z=candidate else break}
 val p=points.map{worldPoint(it,z)}
 return MapViewport(z,(p.minOf{it.first}+p.maxOf{it.first})/2-width/2.0,(p.minOf{it.second}+p.maxOf{it.second})/2-height/2.0,width,height)
}

/** A bundled course preview, without the explorer's filters or fabricated replay position. */
@Composable internal fun BaseCourseNavigationMap(route:Route,modifier:Modifier=Modifier) {
 val context=LocalContext.current
 val road=remember(route.id){readRoad(context,route.id)}
 BoxWithConstraints(modifier.clipToBounds().background(Sage)) {
  val density=LocalDensity.current
  val width=with(density){maxWidth.roundToPx()};val height=with(density){maxHeight.roundToPx()}
  val view=remember(route.id,width,height){prototypeViewport(road.points,width,height)}
  var tiles by remember(view){mutableStateOf<List<MapTile>?>(null)}
  var retry by remember{mutableIntStateOf(0)}
  LaunchedEffect(view,retry){tiles=loadVisibleTiles(context,view)}
  val path=remember(road,view){Path().apply{road.points.forEachIndexed{i,p->
   val world=worldPoint(p,view.zoom);val x=(world.first-view.left).toFloat();val y=(world.second-view.top).toFloat()
   if(i==0)moveTo(x,y)else lineTo(x,y)
  }}}
  Canvas(Modifier.fillMaxSize()){
   tiles.orEmpty().forEach{tile->tile.image?.let{drawImage(it,Offset((tile.x*256-view.left).toFloat(),(tile.y*256-view.top).toFloat()),colorFilter=NavigationTileFilter)}}
   if(tiles.orEmpty().any{it.image!=null}){
    drawPath(path,Ink.copy(alpha=.20f),style=Stroke(16.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
    drawPath(path,White,style=Stroke(13.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
    drawPath(path,NavigationRouteBlue,style=Stroke(7.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
   }
  }
  if(tiles==null)Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("기본 코스 지도 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.orEmpty().none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(24.dp),color=White,shape=Corner){
   Column(Modifier.padding(16.dp)){Text("지도를 불러오지 못했어요. 빈 지도에 경로를 그리지 않습니다.");TextButton(onClick={retry++},modifier=Modifier.heightIn(min=64.dp)){Text("다시 불러오기")}}
  }
  else if(tiles.orEmpty().any{it.image==null})Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역 불러오기 실패",Modifier.padding(12.dp),color=Ink)}
  Surface(Modifier.align(Alignment.BottomStart).fillMaxWidth(),color=White.copy(alpha=.96f)){
   TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")}){
    Text("© OpenStreetMap contributors · OSRM 기본 코스 · 실시간 교통 미반영",fontSize=13.sp,color=Ink)
   }
  }
 }
}

internal fun prototypeFollowViewport(position:Point,width:Int,height:Int):MapViewport {
 require(width>0&&height>0)
 val p=worldPoint(position,16)
 return prototypePanViewport(MapViewport(16,p.first-width/2.0,p.second-height/2.0,width,height),0.0,0.0)
}

internal fun prototypePanViewport(view:MapViewport,dx:Double,dy:Double):MapViewport {
 val world=256.0*(1 shl view.zoom)
 return view.copy(left=(view.left-dx).coerceIn(0.0,(world-view.width).coerceAtLeast(0.0)),top=(view.top-dy).coerceIn(0.0,(world-view.height).coerceAtLeast(0.0)))
}

internal data class PrototypeTileKey(val zoom:Int,val x:Int,val y:Int)
internal fun prototypeVisibleTileKeys(view:MapViewport):List<PrototypeTileKey> {
 val last=(1 shl view.zoom)-1
 val xs=floor(view.left/256).toInt().coerceAtLeast(0)..floor((view.left+view.width-1)/256).toInt().coerceAtMost(last)
 val ys=floor(view.top/256).toInt().coerceAtLeast(0)..floor((view.top+view.height-1)/256).toInt().coerceAtMost(last)
 return ys.flatMap{y->xs.map{x->PrototypeTileKey(view.zoom,x,y)}}
}

/** Ordered replay cursor resolves overlapping outbound/return roads; never a device heading. */
internal fun prototypeGeometryHeading(points:List<Point>,cumulative:List<Double>,distance:Double):Float? {
 if(points.size<2||points.size!=cumulative.size||!distance.isFinite())return null
 val d=distance.coerceIn(0.0,cumulative.last())
 var end=cumulative.binarySearch(d).let{if(it<0)-it-1 else it+1}.coerceIn(1,points.lastIndex)
 while(end<points.lastIndex&&cumulative[end]<=cumulative[end-1])end++
 while(end>1&&cumulative[end]<=cumulative[end-1])end--
 if(cumulative[end]<=cumulative[end-1])return null
 val a=worldPoint(points[end-1],14);val b=worldPoint(points[end],14)
 return Math.toDegrees(atan2(b.first-a.first,a.second-b.second)).toFloat()
}
