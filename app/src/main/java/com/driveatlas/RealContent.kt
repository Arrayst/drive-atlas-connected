package com.driveatlas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

internal data class PhotoCredit(val caption:String,val title:String,val author:String,val license:String,val source:String,val licenseUrl:String)
internal fun photoCredit(r:Route)=when(r.id){
 "coast"->PhotoCredit("정동진 해변 · 코스 인근 풍경","Jeongdongjin Beach","Loewelad","CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Jeongdongjin_Beach.jpg","https://creativecommons.org/licenses/by-sa/3.0/")
 "forest"->PhotoCredit("담양 메타세쿼이아길 · 주차 후 산책 명소","Metasequoia glyptostroboides Avenue","dalgial","CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Metasequoia_glyptostroboides_Avenue.JPG","https://creativecommons.org/licenses/by-sa/3.0/")
 else->PhotoCredit("의암호 풍경 · 공지천에서 촬영","20231226 공지천유원지에서 바라본 의암호","YellowTurtle9","CC BY 4.0","https://commons.wikimedia.org/wiki/File:20231226_공지천유원지에서_바라본_의암호.jpg","https://creativecommons.org/licenses/by/4.0/")
}
@Composable internal fun RoutePhoto(route:Route,modifier:Modifier=Modifier){
 val context=LocalContext.current
 val bitmap=remember(route.id){
  val raw=context.assets.open("${route.id}.jpg").use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=2})!!}
  val orientation=context.assets.open("${route.id}.jpg").use{android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1)}
  val matrix=android.graphics.Matrix().apply{when(orientation){3->postRotate(180f);6->postRotate(90f);8->postRotate(270f)}}
  if(orientation in listOf(3,6,8))android.graphics.Bitmap.createBitmap(raw,0,0,raw.width,raw.height,matrix,true).also{raw.recycle()}.asImageBitmap() else raw.asImageBitmap()
 }
 Image(bitmap,photoCredit(route).caption,modifier,contentScale=ContentScale.Crop,alignment=when(route.id){"lake"->androidx.compose.ui.BiasAlignment(0f,.35f);"forest"->Alignment.BottomCenter;else->Alignment.Center})
}
internal fun openWeb(context:Context,url:String):Boolean=try{
 val i=Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
 if(i.resolveActivity(context.packageManager)==null){if(safePublicUrl(url)){context.startActivity(Intent(context,PublicWebActivity::class.java).putExtra("public_url",url));true}else{Toast.makeText(context,"이 환경에는 링크를 열 앱이 없어요.",Toast.LENGTH_LONG).show();false}}
 else{context.startActivity(i);true}
}catch(_:ActivityNotFoundException){Toast.makeText(context,"연결할 앱을 찾지 못했어요.",Toast.LENGTH_LONG).show();false}catch(_:SecurityException){Toast.makeText(context,"이 환경에서는 외부 링크가 제한되어 있어요.",Toast.LENGTH_LONG).show();false}
/** An accepted launch is not evidence of a calculated route or active guidance. */
internal fun openKakaoRoute(context:Context,route:Route):Boolean{
 if(route.stops.size !in 2..7||route.stops.any{!validNativePoint(it.point)}){
  Toast.makeText(context,"지도 경로는 출발·도착 포함 2~7곳까지예요. 지점을 생략하지 않습니다.",Toast.LENGTH_LONG).show();return false
 }
 val native=Intent(Intent.ACTION_VIEW,Uri.parse(kakaoAppRouteUrl(route))).setPackage("net.daum.android.map")
 try{if(native.resolveActivity(context.packageManager)!=null){context.startActivity(native);return true}}
 catch(_:ActivityNotFoundException){/* Fall back if the app was removed after resolving. */}
 catch(_:SecurityException){/* Keep the restricted, parked-only web fallback. */}
 return try{
  context.startActivity(Intent(context,PublicWebActivity::class.java).putExtra("public_url",directionsUrl(route)))
  true
 }catch(_:ActivityNotFoundException){Toast.makeText(context,"지도 화면을 열지 못했어요.",Toast.LENGTH_LONG).show();false}
 catch(_:SecurityException){Toast.makeText(context,"이 환경에서는 지도 연결이 제한되어 있어요.",Toast.LENGTH_LONG).show();false}
}
internal fun kakaoAppRouteUrl(route:Route):String{
 val stops=route.stops
 require(stops.size in 2..7&&stops.all{validNativePoint(it.point)})
 fun coordinate(index:Int)="${stops[index].point.lat},${stops[index].point.lon}"
 val via=(1 until stops.lastIndex).joinToString(""){index->"&vp${if(index==1) "" else index.toString()}=${coordinate(index)}"}
 return "kakaomap://route?sp=${coordinate(0)}$via&ep=${coordinate(stops.lastIndex)}&by=car"
}
internal fun destinationUrl(route:Route):String{val p=route.stops.last().point;return "https://www.openstreetmap.org/?mlat=${p.lat}&mlon=${p.lon}#map=15/${p.lat}/${p.lon}"}
internal fun directionsUrl(route:Route):String {
 require(route.stops.size in 2..7&&route.stops.all{validNativePoint(it.point)})
 return "https://map.kakao.com/link/by/car/" + route.stops.joinToString("/"){java.net.URLEncoder.encode(it.name,"UTF-8").replace("+","%20")+",${it.point.lat},${it.point.lon}"}
}
internal fun regionFor(route:Route)=when(route.id){"coast"->"강릉";"forest"->"담양";else->"춘천"}
@Composable internal fun RoadStats(route:Route){val c=LocalContext.current;val r=remember(route.id){readRoad(c,route.id)};Text(String.format(Locale.KOREA,"%.1f km · 주행 약 %.0f분",r.meters/1000,r.seconds/60),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink);Text("OSRM 추정 · 정차·교통상황 제외",fontSize=13.sp,color=Muted)}
@Composable internal fun PhotoAttribution(route:Route){val c=LocalContext.current;val p=photoCredit(route);Text(p.caption,fontSize=13.sp,color=Muted);TextButton(onClick={openWeb(c,p.source)},contentPadding=PaddingValues(0.dp)){Text("사진: ${p.author} · ${p.license} ↗",fontSize=12.sp)}}
@Composable internal fun RealNavigationDialog(route:Route,nativeStatus:String,onNativeSearch:()->Unit,onNativeRoute:()->Unit,onNativeOpen:()->Unit,onNativeLeg:(Int)->Unit,onDismiss:()->Unit,onStart:()->Unit){
 val context=LocalContext.current
 var advanced by remember(route.id){mutableStateOf(false)}
 val status=navigationPresentation(nativeStatus)
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  // Explicit dim: some vehicle window themes supply no useful default scrim.
  val window=(androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
  DisposableEffect(window){
   val previous=window?.attributes?.dimAmount
   window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
   window?.setDimAmount(.64f)
   onDispose { if(previous!=null) window?.setDimAmount(previous) }
  }
  Surface(Modifier.padding(24.dp).widthIn(max=760.dp).fillMaxWidth().heightIn(max=780.dp),color=White,
   shape=androidx.compose.foundation.shape.RoundedCornerShape(28.dp),shadowElevation=28.dp){
   Column {
    Row(Modifier.fillMaxWidth().padding(start=28.dp,end=16.dp,top=18.dp,bottom=18.dp),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      Text("지도 연결",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text(route.title,fontSize=16.sp,color=Muted,modifier=Modifier.padding(top=5.dp))
     }
     OutlinedButton(onClick=onDismiss,modifier=Modifier.sizeIn(minWidth=76.dp,minHeight=76.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp)){
      Text("닫기 ×",fontSize=17.sp,color=Ink)
     }
    }
    HorizontalDivider(color=Line)
    Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
     Surface(color=if(status.confirmed) androidx.compose.ui.graphics.Color(0xFFEAF5EF) else Paper,shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp)){
      Column(Modifier.fillMaxWidth().padding(20.dp)){
       Text(status.title,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
       Text(nativeStatus,fontSize=17.sp,lineHeight=25.sp,color=Muted,modifier=Modifier.padding(top=8.dp)
        .semantics { liveRegion=androidx.compose.ui.semantics.LiveRegionMode.Polite })
      }
     }
     Column {
      Text("도착지",fontSize=13.sp,color=Muted)
      Text(route.stops.last().name,fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=5.dp))
      Text("${route.stops.size}개 지점 · 출발 전 내비에서 경로를 확인하세요.",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=7.dp))
      Text("앱 지도 선은 기본 코스입니다. 선택 휴식지는 도로 시간 계산·외부 내비 요청에 반영됩니다.",fontSize=16.sp,lineHeight=23.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
      Text("현재 위치·왕복 출발지는 포함하지 않은 공개 지점 여정입니다. 휴식 장소는 공식 지도 대표점이며 차량 진입구·당일 주차 가능 여부는 미확인입니다.",fontSize=15.sp,color=Muted)
     }
     SecondaryButton(if(status.confirmed) "다른 도착지 검색" else "기본 내비 화면 보기",if(status.confirmed) onNativeSearch else onNativeOpen,Modifier.fillMaxWidth())
     OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.fillMaxWidth().heightIn(min=76.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
       Text("다른 연결 방법 · 실험 기능",color=Muted,fontSize=16.sp)
       Text(if(advanced) "접기 −" else "펼치기 +",color=Ink,fontSize=16.sp)
      }
     }
     if(advanced){
      Text("공개 좌표로 코스 전달",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text("시작점·경유지·도착점을 기본 내비에 보냅니다. 기존 내비 경로가 바뀔 수 있어요. POI ID 없는 요청은 거절될 수 있으며, 완료 콜백과 좌표 대조 전에는 성공이 아닙니다.",fontSize=15.sp,lineHeight=23.sp,color=Muted)
      route.stops.forEachIndexed{index,stop->Text("${index+1}. ${stop.name} · ${String.format(Locale.US,"%.6f, %.6f",stop.point.lat,stop.point.lon)}",fontSize=15.sp,color=Muted)}
      if(RestItinerary.nativeFullRouteSupported(route))SecondaryButton("이 좌표를 기본 내비에 전달 · 실험",onNativeRoute,Modifier.fillMaxWidth())
      else Text("기본 내비는 목적지 포함 최대 3곳만 지원합니다. 전체 전달 없이 아래에서 지점을 하나씩 선택하세요. 지점을 생략하지 않습니다.",fontSize=16.sp,color=Coral)
      HorizontalDivider(color=Line)
      Text("구간별 안내",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
      Text("주차 후 목적지 하나씩 선택하세요. 기존 내비 경로가 바뀝니다. 자동 구간 전환이나 전체 코스 완료 인증은 아닙니다.",fontSize=15.sp,lineHeight=23.sp,color=Muted)
      route.stops.forEachIndexed{index,stop->SecondaryButton("${index+1}. ${stop.name}",{onNativeLeg(index)},Modifier.fillMaxWidth())}
      HorizontalDivider(color=Line)
      Text("외부 지도 · 모의 체험",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
      SecondaryButton("카카오맵에서 코스 열기 ↗",{openKakaoRoute(context,route)},Modifier.fillMaxWidth())
      Text("카카오맵 앱이 있으면 앱으로, 없으면 웹 지도로 연결합니다. 공개 코스 지점만 전달하며 개인 출발지·현재 위치는 보내지 않습니다. 외부 경로는 달라질 수 있고 차량 내비·우표 여정은 시작하지 않습니다.",fontSize=15.sp,lineHeight=23.sp,color=Muted)
      SecondaryButton("OSM에서 도착지 보기 ↗",{openWeb(context,destinationUrl(route))},Modifier.fillMaxWidth())
      SecondaryButton("우표 수집 흐름 체험 · SIM",onStart,Modifier.fillMaxWidth())
     }
    }
    HorizontalDivider(color=Line)
    Column(Modifier.padding(horizontal=28.dp,vertical=20.dp)){
     PrimaryButton(if(status.confirmed) "내비에서 확인하기" else "기본 내비에서 도착지 검색",if(status.confirmed) onNativeOpen else onNativeSearch,Modifier.fillMaxWidth())
     Text("주차 중에만 조작 · 검색 요청은 안내 시작이 아닙니다.",fontSize=16.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
    }
   }
  }
 }
}
@Composable internal fun SourcesDialog(route:Route,onDismiss:()->Unit){
 val context=LocalContext.current;val p=photoCredit(route)
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.widthIn(max=900.dp).padding(24.dp),color=Paper,shape=Corner){Column(Modifier.verticalScroll(rememberScrollState()).padding(32.dp)){
   Text("출처와 이용 안내",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink);Spacer(Modifier.height(22.dp))
   Text("${p.title}\n© ${p.author} · ${p.license}\n원본 파일을 포함하며 화면 비율에 맞게 크롭·축소 표시합니다. 사진은 방문 기록이나 현재 상태의 증거가 아닙니다.",fontSize=17.sp,lineHeight=27.sp,color=Ink)
   Row{TextButton(onClick={openWeb(context,p.source)}){Text("사진 원본 ↗")};TextButton(onClick={openWeb(context,p.licenseUrl)}){Text("사진 라이선스 ↗")}}
   Text("지도·경로: © OpenStreetMap contributors, ODbL 1.0. 지도 타일은 인터넷에서 현재 화면만 불러오며 HTTP 캐시를 사용합니다. 대량 저장·오프라인 지도 기능은 없습니다. 개인 시연용 공용 서비스이며 운영 서비스용 SLA가 없습니다.",fontSize=17.sp,lineHeight=27.sp,color=Ink)
   Row{TextButton(onClick={openWeb(context,"https://www.openstreetmap.org/copyright")}){Text("OSM 저작권 ↗")};TextButton(onClick={openWeb(context,"https://operations.osmfoundation.org/policies/tiles/")}){Text("타일 정책 ↗")}}
   Text("도로선은 2026-11-08 공개 OSRM driving 응답으로 만든 경로 스냅샷입니다. 여행 아이디어용이며 실시간 교통·통제·주차·진입 가능 여부를 보장하지 않습니다. 실제 출발 전 내비와 현장 표지로 확인하세요.",fontSize=17.sp,lineHeight=27.sp,color=Ink)
   TextButton(onClick={openWeb(context,when(route.id){"coast"->"https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=e9c74637-7df6-4ade-b2c1-0868c09a06d7";"forest"->"https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=6cc24bf9-6eb3-43f4-9527-468f4769c88c";else->"https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=ae6bfc18-f90f-46a3-bfd7-6bf0b2f5580c"})}){Text("지역·관광 안내 출처 ↗")}
   PrimaryButton("확인",onDismiss,Modifier.fillMaxWidth())
  }}
 }
}
@Composable internal fun RealDetailPage(route:Route,hasTrip:Boolean,onBack:()->Unit,onStart:()->Unit,onTrip:()->Unit) =
 DesignDetail(route,hasTrip,onBack,onStart,onTrip)
