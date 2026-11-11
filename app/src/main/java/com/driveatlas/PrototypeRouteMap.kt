package com.driveatlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed overview: replay never pans/zooms or prefetches a trail of map tiles. */
@Composable internal fun PrototypeRouteMap(route:NavPrototypeRoute,position:Point,modifier:Modifier=Modifier){
 val context=LocalContext.current
 BoxWithConstraints(modifier.clipToBounds().background(Sage)){
  val d=LocalDensity.current;val w=with(d){maxWidth.roundToPx()};val h=with(d){maxHeight.roundToPx()}
  val viewport=remember(route,w,h){prototypeViewport(route.geometry,w,h)}
  var tiles by remember(route,viewport){mutableStateOf<List<MapTile>?>(null)}
  var retry by remember{mutableIntStateOf(0)}
  LaunchedEffect(route,viewport,retry){tiles=loadVisibleTiles(context,viewport)}
  Canvas(Modifier.fillMaxSize()){
   tiles.orEmpty().forEach{t->t.image?.let{drawImage(it,Offset((t.x*256-viewport.left).toFloat(),(t.y*256-viewport.top).toFloat()))}}
   if(tiles.orEmpty().any{it.image!=null}){
    fun pixel(p:Point):Offset{val q=worldPoint(p,viewport.zoom);return Offset((q.first-viewport.left).toFloat(),(q.second-viewport.top).toFloat())}
    val path=Path();route.geometry.forEachIndexed{i,p->val q=pixel(p);if(i==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)}
    drawPath(path,White,style=Stroke(10f));drawPath(path,Coral,style=Stroke(5f))
    route.request.points().forEach{drawCircle(Ink,7f,pixel(it))}
    val current=pixel(position);drawCircle(White,18f,current);drawCircle(Color(0xFF6A42C2),12f,current)
   }
  }
  Surface(Modifier.align(Alignment.TopStart).padding(12.dp),color=White.copy(alpha=.96f),shape=Corner){Text("SIM · 전체 왕복 경로 / 보라색: 재생 위치",Modifier.padding(12.dp),color=Ink,fontSize=16.sp)}
  if(tiles==null)Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("지도 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.orEmpty().none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(30.dp),color=White,shape=Corner){Column(Modifier.padding(20.dp)){Text("지도를 불러오지 못했어요. 빈 지도에 경로를 그리지 않습니다.",color=Ink);TextButton(onClick={retry++}){Text("다시 불러오기")}}}
  else if(tiles.orEmpty().any{it.image==null})Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역 불러오기 실패",Modifier.padding(12.dp),color=Ink)}
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
