package com.driveatlas

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject
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

/** Public vector exploration retains the existing detail/list/source contracts. */
@Composable internal fun RealRoadMap(route:Route,modifier:Modifier=Modifier,controlsEnabled:Boolean=true) {
 VectorCourseExplorer(route,modifier,controlsEnabled)
}
@Composable internal fun MapPlaceDialog(route:Route,places:List<MapPlace>,selected:MapPlace?,onSelect:(MapPlace)->Unit,onDismiss:()->Unit,onBack:()->Unit) {
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
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {Text(place.name,fontSize=19.sp,color=Ink);Text(place.kinds.joinToString(" · "){it.label}+if(place.selectedRest)" · 가족 계획에 선택됨" else "",fontSize=16.sp,color=Muted)}
        AtlasSymbol("arrow",AtlasGreen,Modifier.size(24.dp))
       }
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
