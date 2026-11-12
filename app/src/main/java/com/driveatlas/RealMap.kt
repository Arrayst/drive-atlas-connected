package com.driveatlas

import android.content.Context
import android.graphics.BitmapFactory
import android.net.http.HttpResponseCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.layout.onSizeChanged
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
 for(z in 8..16){val ps=points.map{worldPoint(it,z)};if(ps.maxOf{it.first}-ps.minOf{it.first}<width-minOf(150.0,width*.2)&&ps.maxOf{it.second}-ps.minOf{it.second}<height-minOf(130.0,height*.25))zoom=z else break}
 zoom=(zoom+extraZoom).coerceIn(8,16);val ps=points.map{worldPoint(it,zoom)}
 return MapViewport(zoom,(ps.minOf{it.first}+ps.maxOf{it.first})/2-width/2,(ps.minOf{it.second}+ps.maxOf{it.second})/2-height/2,width,height)
}
internal data class MapTile(val x:Int,val y:Int,val image:ImageBitmap?)
internal suspend fun loadVisibleTiles(context:Context,v:MapViewport):List<MapTile> = withContext(Dispatchers.IO) {
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
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun RealRoadMap(route:Route,modifier:Modifier=Modifier) {
 val context=LocalContext.current
 val road=remember(route.id){readRoad(context,route.id)}
 val selectedRestIds=remember(route.id){FamilyStore(context).loadFamily().plans[route.id]?.effectiveRestFacilityIds.orEmpty().toSet()}
 val places=remember(route.id,selectedRestIds){mapPlaces(route,selectedRestIds)}
 var filter by remember(route.id){mutableStateOf<MapPlaceKind?>(null)}
 var selected by remember(route.id){mutableStateOf<MapPlace?>(null)}
 var listOpen by remember(route.id){mutableStateOf(false)}
 val shown=visibleMapPlaces(places,filter)
 var zoom by remember(route.id){mutableIntStateOf(0)}
 var zoomMenu by remember(route.id){mutableStateOf(false)}
 var failedMode by remember(route.id){mutableStateOf(false)}
 var refresh by remember(route.id){mutableIntStateOf(0)}
 BoxWithConstraints(modifier.clipToBounds().background(Sage)){
  val density=androidx.compose.ui.platform.LocalDensity.current
  val w=with(density){maxWidth.roundToPx()};val h=with(density){maxHeight.roundToPx()}
  var insetTop by remember(w,density.fontScale){mutableIntStateOf(with(density){144.dp.roundToPx()})}
  var insetBottom by remember(w,density.fontScale){mutableIntStateOf(with(density){if(maxWidth<600.dp)192.dp.roundToPx() else 128.dp.roundToPx()})}
  val v=remember(route.id,w,h,zoom,insetTop,insetBottom){
   val fit=fittedViewport(road.points+places.map{it.point},w,(h-insetTop-insetBottom).coerceAtLeast(90),zoom)
   fit.copy(top=fit.top-insetTop,height=h)
  }
  var tiles by remember(route.id,v,failedMode,refresh){mutableStateOf<List<MapTile>?>(null)}
  LaunchedEffect(route.id,v,failedMode,refresh){tiles=if(failedMode) emptyList() else loadVisibleTiles(context,v)}
  val pinSize=64.dp
  val half=with(density){pinSize.toPx()/2}
  val gap=with(density){8.dp.toPx()}
  val pinRange=mapPinVerticalRange(h.toFloat(),insetTop.toFloat(),insetBottom.toFloat(),half,gap)
  val zoomColumnFits=h-insetBottom>=with(density){264.dp.roundToPx()}
  val anchors=shown.map { val xy=worldPoint(it.point,v.zoom);MapPinPosition((xy.first-v.left).toFloat(),(xy.second-v.top).toFloat()) }
  val pinPositions=if(pinRange!=null)separateMapPins(anchors,half+gap,pinRange.first,(w-half-with(density){104.dp.toPx()}).coerceAtLeast(half+gap),pinRange.second,half*2+gap) else emptyList()
  Canvas(Modifier.fillMaxSize()){
   tiles.orEmpty().forEach{t->t.image?.let{drawImage(it,Offset((t.x*256-v.left).toFloat(),(t.y*256-v.top).toFloat()),colorFilter=NavigationTileFilter)}}
   if(tiles.orEmpty().any{it.image!=null}){
    val path=Path();road.points.forEachIndexed{i,p->val xy=worldPoint(p,v.zoom);val x=(xy.first-v.left).toFloat();val y=(xy.second-v.top).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
    drawPath(path,Color.White,style=Stroke(width=13.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round));drawPath(path,NavigationRouteBlue,style=Stroke(width=7.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
    if(pinRange!=null)anchors.forEachIndexed { i,a -> val pin=pinPositions[i];drawLine(Color.White,Offset(a.x,a.y),Offset(pin.x,pin.y),6f);drawLine(Ink,Offset(a.x,a.y),Offset(pin.x,pin.y),2f);drawCircle(Ink,4f,Offset(a.x,a.y)) }


   }
  }
  if(pinRange!=null&&tiles.orEmpty().any{it.image!=null}) shown.forEachIndexed { index,place ->
   val px=pinPositions[index].x;val py=pinPositions[index].y
   if(px in half..(w-half)&&py in half..(h-half)) {
    Button(onClick={selected=place},modifier=Modifier.offset{IntOffset((px-half).roundToInt(),(py-half).roundToInt())}.size(pinSize)
     .semantics { contentDescription=place.name+if(place.selectedRest)" · 가족 계획에 선택됨" else " · 장소 정보" },
     shape=if(MapPlaceKind.REST in place.kinds)RoundedCornerShape(16.dp) else CircleShape,contentPadding=PaddingValues(0.dp),
     colors=ButtonDefaults.buttonColors(containerColor=when {MapPlaceKind.PHOTO in place.kinds->Color(0xFF6A42C2);MapPlaceKind.REST in place.kinds->Color(0xFF176B52);else->NavigationRouteBlue}),
     border=androidx.compose.foundation.BorderStroke(if(place.selectedRest)4.dp else 2.dp,Color.White)) {
      Text(if(MapPlaceKind.PHOTO in place.kinds)"사진" else place.badge,fontSize=24.sp,fontWeight=FontWeight.Bold)
    }
   }
  }
  Surface(Modifier.align(Alignment.TopStart).onSizeChanged{insetTop=it.height}.padding(12.dp).padding(end=90.dp),color=White.copy(alpha=.97f),shape=Corner){Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)){
   Text("코스와 쉬어 갈 곳",fontSize=17.sp,color=Ink,fontWeight=FontWeight.Bold)
   Text(if(pinRange==null&&shown.isNotEmpty())"작은 지도 · 장소는 목록에서 확인" else "파란 선: 코스 · 핀 연결점: 대표 위치",fontSize=16.sp,color=Muted,minLines=2,maxLines=2)
   TextButton(onClick={listOpen=true},contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=64.dp)){Text("${filter?.label?:"전체"} ${shown.size}곳 · 목록",fontSize=16.sp)}
  }}
  Column(Modifier.align(Alignment.TopEnd).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   if(!zoomColumnFits) {
    FilledTonalButton(shape=RoundedCornerShape(16.dp),onClick={zoomMenu=true},contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("배율",fontSize=18.sp)}
    DropdownMenu(expanded=zoomMenu,onDismissRequest={zoomMenu=false}) {
     DropdownMenuItem(text={Text("확대 +",fontSize=18.sp)},onClick={zoom=(zoom+1).coerceAtMost(2);zoomMenu=false},enabled=zoom<2,modifier=Modifier.heightIn(min=64.dp))
     DropdownMenuItem(text={Text("축소 −",fontSize=18.sp)},onClick={zoom=(zoom-1).coerceAtLeast(-2);zoomMenu=false},enabled=zoom> -2,modifier=Modifier.heightIn(min=64.dp))
     DropdownMenuItem(text={Text("전체 경로",fontSize=18.sp)},onClick={zoom=0;zoomMenu=false},modifier=Modifier.heightIn(min=64.dp))
    }
   } else {
   FilledTonalButton(shape=RoundedCornerShape(16.dp),onClick={zoom=(zoom+1).coerceAtMost(2)},enabled=zoom<2,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("+",fontSize=27.sp)}
   FilledTonalButton(shape=RoundedCornerShape(16.dp),onClick={zoom=(zoom-1).coerceAtLeast(-2)},enabled=zoom> -2,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("−",fontSize=27.sp)}
   FilledTonalButton(shape=RoundedCornerShape(16.dp),onClick={zoom=0},contentPadding=PaddingValues(0.dp),modifier=Modifier.size(76.dp)){Text("전체",fontSize=18.sp)}
   }
  }
  if(tiles==null)Surface(Modifier.align(Alignment.Center),color=White,shape=Corner){Text("실제 지도를 불러오는 중…",Modifier.padding(20.dp),color=Ink)}
  else if(tiles.orEmpty().none{it.image!=null})Surface(Modifier.align(Alignment.Center).padding(70.dp),color=White,shape=Corner){Column(Modifier.padding(24.dp)){Text(if(failedMode)"오프라인 상황 체험" else "지도를 불러오지 못했어요",fontSize=20.sp,color=Ink);Text("인터넷 연결이 필요해요. 경로를 빈 지도 위에 그리지 않습니다.",fontSize=16.sp,color=Muted);TextButton(onClick={failedMode=false;refresh++},modifier=Modifier.heightIn(min=64.dp)){Text("다시 불러오기")}}}
  else if(tiles.orEmpty().any{it.image==null})Surface(Modifier.align(Alignment.Center),color=White){Text("일부 지도 영역을 불러오지 못했어요",Modifier.padding(12.dp),color=Ink)}
  Column(Modifier.align(Alignment.BottomStart).onSizeChanged{insetBottom=it.height}.fillMaxWidth().background(White.copy(alpha=.97f))){
   Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
    listOf<MapPlaceKind?>(null,MapPlaceKind.COURSE,MapPlaceKind.REST,MapPlaceKind.PHOTO).forEach { kind ->
     TextButton(onClick={filter=kind},modifier=Modifier.weight(1f).heightIn(min=64.dp),contentPadding=PaddingValues(2.dp),
      colors=ButtonDefaults.textButtonColors(contentColor=if(filter==kind)Coral else Muted)) { Text(kind?.label?:"전체",fontSize=16.sp,fontWeight=if(filter==kind)FontWeight.Bold else FontWeight.Normal) }
    }
   }
   if(shown.isEmpty())Text("이 코스에는 출처를 확인한 사진 산책 장소가 아직 없어요.",fontSize=16.sp,color=Muted,modifier=Modifier.padding(horizontal=12.dp))
   FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    if(BuildConfig.SIMULATOR)TextButton(onClick={failedMode=!failedMode},modifier=Modifier.heightIn(min=64.dp)){Text(if(failedMode)"온라인으로" else "오프라인 체험",fontSize=16.sp)}
    TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")},modifier=Modifier.heightIn(min=64.dp)){Text("© OpenStreetMap contributors",fontSize=16.sp,color=Ink)}
   }
  }
 }
 if(listOpen||selected!=null) MapPlaceDialog(route,shown,selected,onSelect={selected=it},onDismiss={selected=null;listOpen=false},onBack={selected=null;listOpen=true})
}
@Composable private fun MapPlaceDialog(route:Route,places:List<MapPlace>,selected:MapPlace?,onSelect:(MapPlace)->Unit,onDismiss:()->Unit,onBack:()->Unit) {
 val context=LocalContext.current
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  val window=(androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
  DisposableEffect(window){val old=window?.attributes?.dimAmount;window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);window?.setDimAmount(.64f);onDispose{if(old!=null)window?.setDimAmount(old)}}
  Surface(Modifier.padding(24.dp).widthIn(max=720.dp).fillMaxWidth().heightIn(max=700.dp),color=White,shape=Corner,shadowElevation=24.dp){Column {
   Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically){
    Text(selected?.name?:"${route.title} · 지도 속 장소",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
    OutlinedButton(onClick=onDismiss,modifier=Modifier.heightIn(min=64.dp)){Text("닫기")}
   }
   HorizontalDivider()
   Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    if(selected==null){
     if(places.isEmpty())Text("이 필터에서 확인된 장소가 아직 없어요.",color=Ink,fontSize=19.sp)
     Text("도로 기준점에서는 정차·촬영하지 마세요. 안전하게 주차한 뒤 장소 정보를 확인하세요.",color=Muted,fontSize=17.sp)
     places.forEach { place ->
      OutlinedButton(onClick={onSelect(place)},modifier=Modifier.fillMaxWidth().heightIn(min=76.dp),shape=Corner,contentPadding=PaddingValues(16.dp)){
       Column(Modifier.fillMaxWidth()) {Text(place.name,fontSize=19.sp,color=Ink);Text(place.kinds.joinToString(" · "){it.label}+if(place.selectedRest)" · 가족 계획에 선택됨" else "",fontSize=16.sp,color=Muted)}
      }
     }
    } else {
     Text(selected.kinds.joinToString(" · "){it.label}+if(selected.selectedRest)" · 가족 계획에 선택됨" else "",color=Coral,fontSize=17.sp)
     Text(selected.description,fontSize=19.sp,lineHeight=29.sp,color=Ink)
     Text("사진 장소 선택은 경로·휴식 시간을 자동 변경하지 않습니다. 휴식 계획은 가족 비교에서 편집하세요.",fontSize=16.sp,color=Muted)
     selected.source?.let { source -> OutlinedButton(onClick={openWeb(context,source)},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("공식 안내 확인 ↗",fontSize=18.sp)} }
     TextButton(onClick=onBack,modifier=Modifier.heightIn(min=64.dp)){Text("← 장소 목록",fontSize=18.sp)}
    }
   }
  }}
 }
}
