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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*

/** SIM-only camera. Loads only visible tiles, never a route corridor or offline region. */
@Composable internal fun PrototypeRouteMap(route:NavPrototypeRoute,position:Point,modifier:Modifier=Modifier,replayDistance:Double?=null){
 val context=LocalContext.current
 BoxWithConstraints(modifier.clipToBounds().background(Sage)){
  val d=LocalDensity.current;val w=with(d){maxWidth.roundToPx()};val h=with(d){maxHeight.roundToPx()}
  var follow by remember(route){mutableStateOf(true)}
  var manual by remember(route,w,h){mutableStateOf<MapViewport?>(null)}
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
  val routePath=remember(route,viewport.zoom){Path().apply{
   route.geometry.forEachIndexed{i,p->val q=worldPoint(p,viewport.zoom);val x=(q.first-pathOrigin.first).toFloat();val y=(q.second-pathOrigin.second).toFloat();if(i==0)moveTo(x,y)else lineTo(x,y)}
  }}
  Canvas(Modifier.fillMaxSize().pointerInput(route,w,h){
   detectDragGestures { change,delta ->
    change.consume()
    val current=latestViewport
    manual=prototypePanViewport(current,delta.x.toDouble(),delta.y.toDouble());follow=false
   }
  }){
   tiles.orEmpty().forEach{t->t.image?.let{drawImage(it,Offset((t.x*256-viewport.left).toFloat(),(t.y*256-viewport.top).toFloat()))}}
   if(tiles.orEmpty().any{it.image!=null}){
    fun pixel(p:Point):Offset{val q=worldPoint(p,viewport.zoom);return Offset((q.first-viewport.left).toFloat(),(q.second-viewport.top).toFloat())}
    translate((pathOrigin.first-viewport.left).toFloat(),(pathOrigin.second-viewport.top).toFloat()){
     drawPath(routePath,White,style=Stroke(10f));drawPath(routePath,Coral,style=Stroke(5f))
    }
    route.request.points().forEach{drawCircle(Ink,7f,pixel(it))}
    val current=pixel(displayed)
    drawCircle(White,21f,current);drawCircle(Color(0xFF6A42C2),17f,current)
    if(heading!=null)rotate(heading,current){
     val arrow=Path().apply{moveTo(current.x,current.y-13f);lineTo(current.x+10f,current.y+10f);lineTo(current.x,current.y+5f);lineTo(current.x-10f,current.y+10f);close()}
     drawPath(arrow,White)
    }
   }
  }
  Surface(Modifier.align(Alignment.TopStart).padding(12.dp),color=White.copy(alpha=.96f),shape=Corner){Text("SIM · 보라색: 모의 위치\n${if(follow)"위치 따라가기 · 북쪽 위" else "지도 둘러보기 · 북쪽 위"}",Modifier.padding(12.dp),color=Ink,fontSize=16.sp)}
  Surface(Modifier.align(Alignment.BottomEnd).padding(end=12.dp,bottom=66.dp),color=White,shape=Corner){Row{
   TextButton(onClick={follow=true;manual=null},modifier=Modifier.heightIn(min=64.dp)){Text(if(follow)"✓ 따라가기" else "따라가기",fontSize=16.sp)}
   TextButton(onClick={follow=false;manual=prototypeViewport(route.geometry,w,h)},modifier=Modifier.heightIn(min=64.dp)){Text("전체 경로",fontSize=16.sp)}
  }}
  if(!loaded||loading&&tiles.none{it.image!=null})Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("지도 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(30.dp),color=White,shape=Corner){Column(Modifier.padding(20.dp)){Text("지도를 불러오지 못했어요. 빈 지도에 경로를 그리지 않습니다.",color=Ink);TextButton(onClick={retry++},modifier=Modifier.heightIn(min=64.dp)){Text("다시 불러오기")}}}
  else if(!loading&&(tiles.size<tileKeys.size||tiles.any{it.image==null}))Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역 불러오기 실패",Modifier.padding(12.dp),color=Ink)}
  Surface(Modifier.align(Alignment.BottomStart).fillMaxWidth(),color=White.copy(alpha=.96f)){TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap contributors · OSRM 도로 경로 · 실시간 교통 미반영",fontSize=13.sp,color=Ink)}}
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
