package com.driveatlas

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val Forest = Color(0xFF173F35)
private val Cream = Color(0xFFF4F1E8)
class MainActivity : ComponentActivity() {
 private lateinit var engine: AtlasEngine
 private var revision by mutableIntStateOf(0)
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  engine = AtlasEngine(LocalStore(this), if (BuildConfig.SIMULATOR) Mode.SIMULATOR else Mode.VEHICLE)
  setContent { MaterialTheme(colorScheme = lightColorScheme(primary=Forest, background=Cream, surface=Cream, surfaceVariant=Color(0xFFE3E8DA), onSurfaceVariant=Forest)) { AtlasApp() } }
 }
 override fun onStop() { super.onStop(); engine.setSafety(Safety.UNKNOWN); engine.setLocation(LocationStatus.LOST); revision++ }
 @Composable private fun AtlasApp() {
  revision
  var page by remember { mutableStateOf("discover") }
  var selected by remember { mutableStateOf(Catalog.routes.first()) }
  var message by remember { mutableStateOf("") }
  var preview by remember { mutableStateOf<Bitmap?>(null) }
  BackHandler(enabled = preview != null || page != "discover") { if(preview!=null) preview=null else page=if(page=="detail") "discover" else "discover" }
  fun act(block: () -> Unit) { try { block() } catch (_: Exception) { message="저장하지 못했어요. 안전하게 주차 후 다시 시도하세요." }; revision++ }
  Column(Modifier.fillMaxSize().background(Cream).padding(24.dp).verticalScroll(rememberScrollState())) {
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
    Text("DRIVE ATLAS", color=Forest, fontSize=28.sp)
    Text(if(BuildConfig.SIMULATOR) "SIMULATOR · 가상 주행" else "VEHICLE · 연결 준비 중", color=Forest)
   }
   Text("길은 지나가도, 장면은 남으니까.", fontSize=16.sp, color=Forest)
   Spacer(Modifier.height(16.dp))
   if (BuildConfig.SIMULATOR) {
    Text("개발용 제어 · 실제 차량 상태가 아닙니다", fontSize=12.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Button(onClick={act{engine.setSafety(Safety.PARKED);engine.setLocation(LocationStatus.AVAILABLE)}}) { Text("가상 P / 위치 정상") }
     OutlinedButton(onClick={act{engine.setSafety(Safety.MOVING)}}) { Text("가상 주행") }
     OutlinedButton(onClick={act{engine.setSafety(Safety.UNKNOWN)}}) { Text("상태 미확인") }
     OutlinedButton(onClick={act{engine.setLocation(LocationStatus.LOST)}}) { Text("위치 끊김") }
    }
   }
   if (!engine.canEdit) {
    Card(Modifier.fillMaxWidth().padding(top=20.dp)) { Column(Modifier.padding(28.dp)) {
     Text("안전한 주차가 확인되면 만나요", fontSize=30.sp, color=Forest)
     Text("주행 중이거나 차량 상태를 알 수 없어 탐색·편집·카드 생성을 잠갔어요. 신호 대기나 속도 0만으로 해제되지 않아요.", fontSize=18.sp)
     engine.snapshot.trip?.let { Text("진행 중인 길 · ${Catalog.routes.first { r->r.id==it.routeId }.title}\n확인한 지점 ${it.nextStop}/3", fontSize=22.sp) }
     if(!BuildConfig.SIMULATOR) Text("실차 어댑터와 권한이 연결되지 않은 알파입니다. 실제 주차 상태를 확인할 수 없습니다.")
    } }
   } else {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     TextButton(onClick={page="discover"}) {Text("길 발견")}
     TextButton(onClick={page="trip"}) {Text("현재 여정")}
     TextButton(onClick={page="atlas"}) {Text("나의 도감 · ${engine.snapshot.awards.size}")}
    }
    when(page) {
     "discover" -> {
      Text("이번 주말, 어떤 풍경으로 갈까요?", fontSize=30.sp, color=Forest)
      Text("모든 코스·장소는 시연용 가상 데이터입니다. 실제 길 안내가 아닙니다.")
      Row(Modifier.fillMaxWidth().padding(top=18.dp), horizontalArrangement=Arrangement.spacedBy(16.dp)) {
       Catalog.routes.forEachIndexed { index, r -> Card(onClick={selected=r;page="detail"},modifier=Modifier.weight(1f)) {
        Landscape(index, Modifier.fillMaxWidth().height(140.dp))
        Column(Modifier.padding(18.dp)) { Text(r.title,fontSize=23.sp,color=Forest); Text(r.subtitle); Text("${r.minutes}분 · 3개의 장면",fontSize=18.sp) }
       } }
      }
     }
     "detail" -> {
      Text(selected.title,fontSize=34.sp,color=Forest)
      Landscape(Catalog.routes.indexOf(selected),Modifier.fillMaxWidth().height(145.dp))
      Text("가상 코스 · 약 ${selected.minutes}분 · 실제 장소/운영시간 미검증",fontSize=18.sp)
      selected.stops.forEachIndexed { i,s -> Text("0${i+1}   ${s.name}",fontSize=23.sp,modifier=Modifier.padding(vertical=8.dp)) }
      Button(enabled=engine.snapshot.trip==null,onClick={act{if(engine.start(selected.id,System.currentTimeMillis())) {page="trip";message="가상 여정을 시작했어요."}}}){Text("이 길 시작하기")}
     }
     "trip" -> {
      val t=engine.snapshot.trip
      if(t==null) Text("아직 진행 중인 여정이 없어요.",fontSize=28.sp) else {
       val r=Catalog.routes.first{it.id==t.routeId}
       Text(r.title,fontSize=32.sp,color=Forest)
       Text("위치 근거 ${t.nextStop}/${r.stops.size} · ${if(engine.location==LocationStatus.AVAILABLE) "수신 가능" else "수신 불가"}",fontSize=22.sp)
       LinearProgressIndicator(progress={t.nextStop/3f},modifier=Modifier.fillMaxWidth().padding(vertical=20.dp))
       Text(if(t.nextStop==3) "도착 근거 수집됨 · 주차 후 완료를 확인하세요" else "도착 미검증 · 순서대로 위치 근거가 필요해요",fontSize=22.sp)
       if(BuildConfig.SIMULATOR) Button(onClick={act{val now=System.currentTimeMillis();message=if(engine.accept(Evidence(r.stops[t.nextStop.coerceAtMost(r.stops.lastIndex)].point,5.0,now,true),now)) "가상 위치 근거를 기록했어요." else "위치 근거를 받아들이지 못했어요."}}){Text(if(t.nextStop==3) "SIMULATED: 도착 위치 다시 확인" else "SIMULATED: 다음 지점 근거 입력")}
       Button(onClick={act{if(engine.finish(System.currentTimeMillis())) {page="atlas";message="SIMULATED 카드가 도감에 추가됐어요."} else message="완료 불가: 순서·위치·최신 도착 근거·주차 상태를 확인하세요."}}){Text("주차 후 여정 완료")}
       OutlinedButton(onClick={act{engine.abandon();message="여정을 종료했어요. 미완료 여정은 도감에 추가되지 않아요."}}){Text("여정 종료 · 카드 없이")}
      }
     }
     "atlas" -> {
      Text("나의 길, 나의 도감",fontSize=32.sp,color=Forest)
      Text("가상 주행 카드는 실제 방문 인증과 구분됩니다. 이미지는 직접 그린 개념 풍경이며 실제 사진이 아닙니다.")
      if(engine.snapshot.awards.isEmpty()) Text("첫 번째 장면을 기다리고 있어요.",modifier=Modifier.padding(24.dp),fontSize=24.sp)
      engine.snapshot.awards.reversed().forEach { a ->
       val r=Catalog.routes.firstOrNull{it.id==a.routeId}
       if(r!=null) Card(Modifier.fillMaxWidth().padding(vertical=10.dp)) { Column(Modifier.padding(20.dp)) {
        Landscape(Catalog.routes.indexOf(r),Modifier.fillMaxWidth().height(130.dp))
        Text(r.title,fontSize=28.sp,color=Forest)
        Text(if(a.simulated) "SIMULATED · 가상 주행 기록" else "위치 근거 확인 기록")
        Button(onClick={act{if(engine.canEdit){ val file=exportCard(r,a);preview=android.graphics.BitmapFactory.decodeFile(file.absolutePath);message="PNG 미리보기 생성 완료 · 앱 내부 ${file.name}\n외부 공유·갤러리 저장은 아직 연결되지 않았어요." }}}){Text("풍경 카드 PNG 생성")}
       } }
      }
     }
    }
   }
   if(engine.canEdit) preview?.let { Image(it.asImageBitmap(), "생성된 가상 풍경 카드 PNG", Modifier.fillMaxWidth().height(240.dp)); TextButton(onClick={preview=null}) { Text("미리보기 닫기") } }
   if(message.isNotEmpty()) Text(message, color=Forest,modifier=Modifier.padding(top=16.dp),fontSize=17.sp)
  }
 }
 private fun exportCard(r: Route, a: Award): File {
  check(engine.canEdit)
  val bitmap=Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888)
  val c=AndroidCanvas(bitmap); val p=Paint(Paint.ANTI_ALIAS_FLAG)
  c.drawColor(android.graphics.Color.rgb(244,241,232))
  p.color=android.graphics.Color.rgb(168,189,158);c.drawOval(-100f,200f,900f,1000f,p)
  p.color=android.graphics.Color.rgb(23,63,53);c.drawOval(500f,280f,1600f,1100f,p)
  p.color=android.graphics.Color.rgb(244,241,232);p.textSize=55f;c.drawText("DRIVE ATLAS",65f,570f,p)
  p.textSize=38f;c.drawText(r.title,65f,640f,p)
  p.textSize=25f;c.drawText("SIMULATED / CONCEPT LANDSCAPE / NOT A REAL PHOTO",65f,720f,p)
  val f=File(filesDir,"atlas-${a.tripId}.png");f.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle();return f
 }
}
@Composable private fun Landscape(index: Int, modifier: Modifier) {
 Canvas(modifier.clipToBounds().background(Color(0xFFDDE5D6))) {
  drawCircle(Color(0xFFE8CB87),radius=size.height*.19f,center=Offset(size.width*.78f,size.height*.25f))
  val path=Path().apply{moveTo(0f,size.height*.75f);quadraticBezierTo(size.width*.3f,-size.height*.1f,size.width*.62f,size.height*.75f);quadraticBezierTo(size.width*.83f,size.height*.05f,size.width,size.height*.5f);lineTo(size.width,size.height);lineTo(0f,size.height);close()}
  drawPath(path,Color(0xFF8BA78A))
  drawOval(Forest,topLeft=Offset(-size.width*.2f,size.height*.65f),size=androidx.compose.ui.geometry.Size(size.width*1.5f,size.height))
  val road=Path().apply{moveTo(size.width*.4f,size.height);cubicTo(size.width*.8f,size.height*.7f,size.width*.3f,size.height*.7f,size.width*.65f,size.height*.5f)}
  drawPath(road,if(index==2) Color(0xFF91B8BD) else Cream,style=androidx.compose.ui.graphics.drawscope.Stroke(width=12f))
 }
}
