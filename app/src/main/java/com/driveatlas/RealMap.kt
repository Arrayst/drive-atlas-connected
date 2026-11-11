package com.driveatlas

import android.content.Context
import android.graphics.BitmapFactory
import android.net.http.HttpResponseCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

internal data class RoadGeometry(val points:List<Point>,val meters:Double,val seconds:Double)
internal fun readRoad(context:Context,id:String):RoadGeometry {
 val j=JSONObject(context.assets.open("$id-route.json").bufferedReader().use{it.readText()})
 val r=j.getJSONArray("routes").getJSONObject(0); val c=r.getJSONObject("geometry").getJSONArray("coordinates")
 return RoadGeometry((0 until c.length()).map{val a=c.getJSONArray(it);Point(a.getDouble(1),a.getDouble(0))},r.getDouble("distance"),r.getDouble("duration"))
}
internal fun worldPoint(p:Point,z:Int):Pair<Double,Double>{val n=256.0*(1 shl z);val lat=Math.toRadians(p.lat.coerceIn(-85.0511,85.0511));return ((p.lon+180)/360*n) to ((1-ln(tan(lat)+1/cos(lat))/Math.PI)/2*n)}
internal data class MapViewport(val zoom:Int,val left:Double,val top:Double,val width:Int,val height:Int)
internal fun fittedViewport(points:List<Point>,width:Int,height:Int,extraZoom:Int=0):MapViewport {
 var zoom=8
 for(z in 8..16){val ps=points.map{worldPoint(it,z)};if(ps.maxOf{it.first}-ps.minOf{it.first}<width-150&&ps.maxOf{it.second}-ps.minOf{it.second}<height-130)zoom=z else break}
 zoom=(zoom+extraZoom).coerceIn(8,16);val ps=points.map{worldPoint(it,zoom)}
 return MapViewport(zoom,(ps.minOf{it.first}+ps.maxOf{it.first})/2-width/2,(ps.minOf{it.second}+ps.maxOf{it.second})/2-height/2,width,height)
}
private data class MapTile(val x:Int,val y:Int,val image:ImageBitmap?)
private suspend fun loadVisibleTiles(context:Context,v:MapViewport):List<MapTile> = withContext(Dispatchers.IO) {
 if(HttpResponseCache.getInstalled()==null)runCatching { HttpResponseCache.install(File(context.cacheDir,"osm-http"),32L*1024*1024) }
 val result=mutableListOf<MapTile>()
 for(y in floor(v.top/256).toInt()..floor((v.top+v.height-1)/256).toInt())for(x in floor(v.left/256).toInt()..floor((v.left+v.width-1)/256).toInt()){
  val bitmap=runCatching{
   val connection=URL("https://tile.openstreetmap.org/${v.zoom}/$x/$y.png").openConnection() as HttpURLConnection
   connection.setRequestProperty("User-Agent","DriveAtlasDemo/0.4 (Android; local proof-of-concept)")
   connection.connectTimeout=7000;connection.readTimeout=7000;connection.useCaches=true
   try{if(connection.responseCode==200)connection.inputStream.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}else null}finally{connection.disconnect()}
  }.getOrNull()
  result+=MapTile(x,y,bitmap)
 }
 HttpResponseCache.getInstalled()?.let { it.flush(); android.util.Log.i("DriveAtlasMap", "visible=${result.size} loaded=${result.count { t -> t.image!=null }} network=${it.networkCount} cacheHits=${it.hitCount}") };result
}
@Composable internal fun RealRoadMap(route:Route,modifier:Modifier=Modifier) {
 val context=LocalContext.current
 val road=remember(route.id){readRoad(context,route.id)}
 var zoom by remember(route.id){mutableIntStateOf(0)}
 var failedMode by remember(route.id){mutableStateOf(false)}
 var refresh by remember(route.id){mutableIntStateOf(0)}
 BoxWithConstraints(modifier.clipToBounds().background(Sage)){
  val density=androidx.compose.ui.platform.LocalDensity.current
  val w=with(density){maxWidth.roundToPx()};val h=with(density){maxHeight.roundToPx()}
  val v=remember(route.id,w,h,zoom){fittedViewport(road.points,w,h,zoom)}
  var tiles by remember(route.id,v,failedMode,refresh){mutableStateOf<List<MapTile>?>(null)}
  LaunchedEffect(route.id,v,failedMode,refresh){tiles=if(failedMode) emptyList() else loadVisibleTiles(context,v)}
  Canvas(Modifier.fillMaxSize()){
   tiles.orEmpty().forEach{t->t.image?.let{drawImage(it,Offset((t.x*256-v.left).toFloat(),(t.y*256-v.top).toFloat()))}}
   if(tiles.orEmpty().any{it.image!=null}){
    val path=Path();road.points.forEachIndexed{i,p->val xy=worldPoint(p,v.zoom);val x=(xy.first-v.left).toFloat();val y=(xy.second-v.top).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
    drawPath(path,Color.White,style=Stroke(width=11f,cap=StrokeCap.Round,join=StrokeJoin.Round));drawPath(path,Coral,style=Stroke(width=6f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    route.stops.forEachIndexed{i,p->val xy=worldPoint(p.point,v.zoom);val c=Offset((xy.first-v.left).toFloat(),(xy.second-v.top).toFloat());drawCircle(Color.White,19f,c);drawCircle(Ink,15f,c)
     drawIntoCanvas{canvas->val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=19f;textAlign=android.graphics.Paint.Align.CENTER;isFakeBoldText=true};canvas.nativeCanvas.drawText("${i+1}",c.x,c.y+7,paint)}
    }
   }
  }
  Surface(Modifier.align(Alignment.TopStart).padding(12.dp),color=White.copy(alpha=.96f),shape=Corner){Column(Modifier.padding(12.dp)){Text("실제 지도 · 도로 경로",fontSize=16.sp,color=Ink);Text("OSRM 스냅샷 · 실시간 안내 아님",fontSize=12.sp,color=Muted)}}
  Column(Modifier.align(Alignment.TopEnd).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   FilledTonalButton(onClick={zoom=(zoom+1).coerceAtMost(2)},enabled=zoom<2,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("+",fontSize=27.sp)}
   FilledTonalButton(onClick={zoom=(zoom-1).coerceAtLeast(-2)},enabled=zoom> -2,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("−",fontSize=27.sp)}
   FilledTonalButton(onClick={zoom=0},contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("전체",fontSize=18.sp)}
  }
  if(tiles==null)Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("실제 지도를 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.orEmpty().none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(70.dp),color=White,shape=Corner){Column(Modifier.padding(24.dp)){Text(if(failedMode)"오프라인 상황 체험" else "지도를 불러오지 못했어요",fontSize=20.sp,color=Ink);Text("인터넷 연결이 필요해요. 경로를 빈 지도 위에 그리지 않습니다.",fontSize=15.sp,color=Muted);TextButton(onClick={failedMode=false;refresh++}){Text("다시 불러오기")}}}
  else if(tiles.orEmpty().any{it.image==null})Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역을 불러오지 못했어요",Modifier.padding(12.dp),color=Ink)}
  Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(White.copy(alpha=.96f)),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   if(BuildConfig.SIMULATOR)TextButton(onClick={failedMode=!failedMode}){Text(if(failedMode)"온라인으로" else "오프라인 체험",fontSize=12.sp)}
   TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap contributors",fontSize=13.sp,color=Ink)}
  }
 }
}
