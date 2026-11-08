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
internal fun destinationUrl(route:Route):String{val p=route.stops.last().point;return "https://www.openstreetmap.org/?mlat=${p.lat}&mlon=${p.lon}#map=15/${p.lat}/${p.lon}"}
internal fun directionsUrl(route:Route):String = "https://map.kakao.com/link/by/car/" + route.stops.joinToString("/"){java.net.URLEncoder.encode(it.name,"UTF-8").replace("+","%20")+",${it.point.lat},${it.point.lon}"}
internal fun regionFor(route:Route)=when(route.id){"coast"->"강릉";"forest"->"담양";else->"춘천"}
@Composable internal fun RoadStats(route:Route){val c=LocalContext.current;val r=remember(route.id){readRoad(c,route.id)};Text(String.format(Locale.KOREA,"%.1f km · 주행 약 %.0f분",r.meters/1000,r.seconds/60),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink);Text("OSRM 추정 · 정차·교통상황 제외",fontSize=13.sp,color=Muted)}
@Composable internal fun PhotoAttribution(route:Route){val c=LocalContext.current;val p=photoCredit(route);Text(p.caption,fontSize=13.sp,color=Muted);TextButton(onClick={openWeb(c,p.source)},contentPadding=PaddingValues(0.dp)){Text("사진: ${p.author} · ${p.license} ↗",fontSize=12.sp)}}
@Composable internal fun RealNavigationDialog(route:Route,onDismiss:()->Unit,onStart:()->Unit){
 val context=LocalContext.current
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.widthIn(max=850.dp).padding(24.dp),color=Paper,shape=Corner){Column(Modifier.padding(32.dp)){
   Eyebrow("MAP CONNECTION");Text("길을 확인하고, 안전하게 출발해요",fontSize=29.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(vertical=16.dp))
   Text(route.title,fontSize=22.sp,color=Muted);Spacer(Modifier.height(20.dp))
   NoteBlock("지도는 연결, 차량 내비는 아직 미연동","카카오맵에는 공개 코스의 출발·경유·도착 좌표를 전달합니다. 현재 위치는 요청하지 않아요. PLEOS 내비 경유지 전달·턴바이턴 안내는 SDK 권한과 실제 응답 검증이 남아 있습니다.")
   Spacer(Modifier.height(22.dp));PrimaryButton("카카오맵에서 코스 열기  ↗",{openWeb(context,directionsUrl(route))},Modifier.fillMaxWidth())
   Text("앱 안 웹 보기에서는 ‘설치없이 지도보기’를 선택하세요. 외부 지도에서 재탐색하므로 화면의 코스와 달라질 수 있습니다. 차량 내비나 우표 여정은 시작하지 않습니다.",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=10.dp,bottom=20.dp))
   SecondaryButton("OSM에서 도착지 보기  ↗",{openWeb(context,destinationUrl(route))},Modifier.fillMaxWidth())
   Spacer(Modifier.height(12.dp));SecondaryButton("우표 수집 흐름 체험 · SIM",onStart,Modifier.fillMaxWidth())
   TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End).heightIn(min=56.dp)){Text("닫기",fontSize=18.sp)}
  }}
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
@Composable internal fun RealDetailPage(route:Route,hasTrip:Boolean,onBack:()->Unit,onStart:()->Unit,onTrip:()->Unit){
 var sources by remember{mutableStateOf(false)}
 TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Text("←  모든 코스",fontSize=17.sp)}
 PageHeading("${regionFor(route)} / REAL ROAD COLLECTION",route.title,route.subtitle){SecondaryButton("출처·이용 안내",{sources=true})}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)){
  Column(Modifier.weight(.61f)){
   RealRoadMap(route,Modifier.fillMaxWidth().height(540.dp));Spacer(Modifier.height(16.dp))
   RoadStats(route);Spacer(Modifier.height(16.dp))
   NoteBlock("코스 미리보기","표시한 선은 실제 도로망을 따라 계산한 참고 경로입니다. 교통·통제와 현장 진입 조건은 반영하지 않아요. 도로 위 임의 정차를 유도하지 않습니다.")
  }
  Column(Modifier.weight(.39f)){
   RoutePhoto(route,Modifier.fillMaxWidth().height(190.dp));PhotoAttribution(route)
   route.stops.forEachIndexed{i,s->StopRow(i,s.name,false,false,if(route.id=="forest"&&i==2)"인근 도로 지점 · 나무길은 주차 후 산책" else "도로 기준점 · 주차장/정차 지점 보장 아님",i==2)}
   Spacer(Modifier.height(18.dp));PrimaryButton("지도 연결 옵션  →",onStart,Modifier.fillMaxWidth())
   if(hasTrip){Spacer(Modifier.height(10.dp));SecondaryButton("진행 중인 체험 보기",onTrip,Modifier.fillMaxWidth())}
   Text("GPS·차량 상태·실제 방문 인증은 미연동",fontSize=13.sp,color=Muted,modifier=Modifier.padding(top=14.dp))
  }
 }
 if(sources)SourcesDialog(route){sources=false}
}
