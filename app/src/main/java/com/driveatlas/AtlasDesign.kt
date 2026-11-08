package com.driveatlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// UI only. Safety, vehicle SDK, route geometry, storage and journey evaluation are unchanged.
internal val AtlasGreen = Color(0xFF176B4D)
internal val AtlasSoft = Color(0xFFE8F0EA)
internal val AtlasDark = Color(0xFF18251F)
private val CardShape = RoundedCornerShape(18.dp)

@Composable internal fun AtlasSymbol(name: String, color: Color, modifier: Modifier = Modifier) {
 Canvas(modifier) {
  val s = size.minDimension; val st = Stroke(s * .065f, cap = StrokeCap.Round)
  fun p(x: Float, y: Float) = Offset(size.width/2 + (x-.5f)*s, size.height/2+(y-.5f)*s)
  when(name) {
   "explore" -> {
    drawCircle(color, s*.37f, style=st)
    val path=Path().apply { moveTo(p(.65f,.27f).x,p(.65f,.27f).y);lineTo(p(.55f,.56f).x,p(.55f,.56f).y);lineTo(p(.30f,.72f).x,p(.30f,.72f).y);lineTo(p(.43f,.43f).x,p(.43f,.43f).y);close() }
    drawPath(path,color,style=st)
   }
   "roads" -> {
    drawCircle(color,s*.075f,p(.28f,.25f),style=st);drawCircle(color,s*.075f,p(.72f,.75f),style=st)
    val path=Path().apply { moveTo(p(.28f,.36f).x,p(.28f,.36f).y); cubicTo(p(.25f,.72f).x,p(.25f,.72f).y,p(.75f,.26f).x,p(.75f,.26f).y,p(.72f,.64f).x,p(.72f,.64f).y) };drawPath(path,color,style=st)
   }
   "stamp" -> { drawRoundRect(color,p(.20f,.15f),Size(s*.60f,s*.70f),androidx.compose.ui.geometry.CornerRadius(s*.04f),style=st);drawCircle(color,s*.14f,p(.5f,.44f),style=st);drawLine(color,p(.35f,.68f),p(.65f,.68f),s*.06f,StrokeCap.Round) }
   "arrow" -> { drawLine(color,p(.18f,.5f),p(.82f,.5f),s*.065f,StrokeCap.Round);drawLine(color,p(.57f,.25f),p(.82f,.5f),s*.065f,StrokeCap.Round);drawLine(color,p(.57f,.75f),p(.82f,.5f),s*.065f,StrokeCap.Round) }
   "pin" -> {drawCircle(color,s*.22f,p(.5f,.35f),style=st);drawCircle(color,s*.055f,p(.5f,.35f));drawLine(color,p(.32f,.52f),p(.5f,.82f),s*.06f,StrokeCap.Round);drawLine(color,p(.68f,.52f),p(.5f,.82f),s*.06f,StrokeCap.Round)}
   "clock" -> {drawCircle(color,s*.35f,style=st);drawLine(color,p(.5f,.28f),p(.5f,.51f),s*.06f,StrokeCap.Round);drawLine(color,p(.5f,.51f),p(.65f,.59f),s*.06f,StrokeCap.Round)}
   else -> {drawLine(color,p(.2f,.3f),p(.8f,.3f),s*.06f,StrokeCap.Round);drawLine(color,p(.2f,.7f),p(.8f,.7f),s*.06f,StrokeCap.Round);drawCircle(color,s*.09f,p(.4f,.3f));drawCircle(color,s*.09f,p(.65f,.7f))}
  }
 }
}

@Composable internal fun DesignSidebar(page:String,unlocked:Boolean,snapshot:Snapshot,wide:Boolean,compact:Boolean,onPage:(String)->Unit,onDebug:()->Unit) {
 Column(Modifier.width(if(wide)136.dp else 116.dp).fillMaxHeight().background(White).border(1.dp,Line).padding(horizontal=12.dp,vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(52.dp).background(AtlasDark,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) { AtlasSymbol("roads",Color.White,Modifier.size(34.dp)) }
  Text("DRIVE ATLAS",color=Ink,fontSize=12.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=.8.sp,modifier=Modifier.padding(top=13.dp))
  Spacer(Modifier.height(if(compact)28.dp else 64.dp))
  listOf(Triple("discover","길 고르기","explore"),Triple("records","다녀온 길","roads"),Triple("atlas","우표 도감","stamp")).forEach { (id,label,icon) ->
   val active=unlocked&&(page==id||(page in listOf("curation","detail","trip")&&id=="discover"))
   Column(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(16.dp)).background(if(active)AtlasSoft else Color.Transparent).clickable(enabled=unlocked){onPage(id)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
    AtlasSymbol(icon,if(active)AtlasGreen else Muted,Modifier.size(30.dp))
    Text(label,color=if(active)AtlasGreen else Muted,fontSize=17.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,modifier=Modifier.padding(top=12.dp))
   }
   Spacer(Modifier.height(12.dp))
  }
  if(snapshot.trip!=null)TextButton(onClick={onPage("trip")},enabled=unlocked,modifier=Modifier.heightIn(min=64.dp)){Text("여정 계속",fontSize=15.sp,color=AtlasGreen)}
  Spacer(Modifier.weight(1f))
  Box(Modifier.size(8.dp).background(if(unlocked)AtlasGreen else Muted,CircleShape))
  Text(if(unlocked)"P · UX 해제 확인" else "차량 상태 확인/제한",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
  TextButton(onClick=onDebug,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),contentPadding=PaddingValues(0.dp)){Text("연결 상태 보기  +",fontSize=11.sp,color=Muted)}
 }
}

@Composable private fun DesignTitle(title:String,subtitle:String,right:(@Composable ()->Unit)?=null) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Column(Modifier.weight(1f)) {Text(title,color=Ink,fontSize=36.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp);Text(subtitle,color=Muted,fontSize=17.sp,modifier=Modifier.padding(top=8.dp))}
  right?.invoke()
 }
}

@Composable private fun SmallPill(text:String,modifier:Modifier=Modifier,dark:Boolean=false) {
 Text(text,color=if(dark)Color.White else AtlasGreen,fontWeight=FontWeight.SemiBold,fontSize=14.sp,modifier=modifier.background(if(dark)AtlasDark else AtlasSoft,RoundedCornerShape(7.dp)).padding(horizontal=12.dp,vertical=8.dp))
}

@Composable private fun InlineStats(route:Route,color:Color=Ink,large:Boolean=false) {
 val context=LocalContext.current;val road=remember(route.id){readRoad(context,route.id)}
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  AtlasSymbol("roads",color,Modifier.size(if(large)23.dp else 19.dp))
  Text(String.format(Locale.KOREA,"%.1f km",road.meters/1000),color=color,fontSize=if(large)22.sp else 17.sp,fontWeight=FontWeight.SemiBold)
  Text("·",color=color.copy(alpha=.6f),fontSize=18.sp)
  AtlasSymbol("clock",color,Modifier.size(if(large)23.dp else 19.dp))
  Text(String.format(Locale.KOREA,"약 %.0f분",road.seconds/60),color=color,fontSize=if(large)22.sp else 17.sp,fontWeight=FontWeight.SemiBold)
 }
}

@Composable private fun PhotoCreditLine(route:Route,modifier:Modifier=Modifier,onDark:Boolean=false) {
 var show by remember(route.id){mutableStateOf(false)}
 val credit=photoCredit(route)
 TextButton(onClick={show=true},contentPadding=PaddingValues(horizontal=0.dp,vertical=4.dp),modifier=modifier.heightIn(min=48.dp)) {
  Text("© ${credit.author} · ${credit.license}  ⓘ",fontSize=12.sp,color=if(onDark)Color.White.copy(alpha=.88f) else Muted,maxLines=1)
 }
 if(show)SourcesDialog(route){show=false}
}

@Composable internal fun DesignHome(snapshot:Snapshot,onCategory:(String)->Unit,onTrip:()->Unit,onAtlas:()->Unit,onRoute:(Route)->Unit,filters:GuideFilters) {
 var mode by filters.homeMode
 var season by filters.season
 var mood by filters.mood
 DesignTitle("길 고르기","좋은 풍경이 있는 길, 출발 전에 골라보세요.") {Row(verticalAlignment=Alignment.CenterVertically){AtlasSymbol("pin",Muted,Modifier.size(22.dp));Text("강릉 · 담양 · 춘천",color=Muted,fontSize=16.sp,modifier=Modifier.padding(start=8.dp))}}
 Spacer(Modifier.height(28.dp))
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   listOf("recommended" to "추천길","season" to "계절 드라이브","theme" to "테마 드라이브").forEach { (id,label) ->
    Surface(onClick={mode=id},color=if(mode==id)AtlasDark else Color.Transparent,shape=RoundedCornerShape(12.dp)) {
     Text(label,fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=if(mode==id)Color.White else Muted,modifier=Modifier.padding(horizontal=24.dp,vertical=18.dp))
    }
   }
  }
  TextButton(onClick={onCategory(mode)},modifier=Modifier.heightIn(min=64.dp)) { AtlasSymbol("filter",Ink,Modifier.size(22.dp));Spacer(Modifier.width(10.dp));Text("지역·조건",color=Ink,fontSize=17.sp) }
 }
 Spacer(Modifier.height(24.dp))
 if(mode=="season") {ChoiceRow("계절",listOf("봄","여름","가을","겨울"),season){season=it};Spacer(Modifier.height(18.dp))}
 if(mode=="theme") {ChoiceRow("분위기",listOf("전체","탁 트인","잔잔한","초록 충전"),mood){mood=it};Spacer(Modifier.height(18.dp))}
 val routes=filteredRoutes(mode,season,mood,"전체",false)
 if(mode=="recommended") {
  val coast=Catalog.routes.first()
  Surface(color=White,shape=CardShape,modifier=Modifier.fillMaxWidth()) {
   Row(Modifier.height(388.dp)) {
    Column(Modifier.weight(.43f).fillMaxHeight().padding(30.dp),verticalArrangement=Arrangement.SpaceBetween) {
     SmallPill("바다를 따라 달리는 길")
     Column {
      Text("강릉 헌화로",color=Ink,fontSize=42.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1.5).sp)
      Text("금진항에서 정동진까지,\n창밖으로 이어지는 동해.",color=Muted,fontSize=20.sp,lineHeight=31.sp,modifier=Modifier.padding(top=12.dp))
     }
     Column {
      InlineStats(coast,large=true)
      Text("OSRM 주행 추정 · 정차·교통 제외",color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=8.dp,bottom=20.dp))
      Button(onClick={onRoute(coast)},shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=AtlasGreen),contentPadding=PaddingValues(horizontal=25.dp),modifier=Modifier.height(64.dp)) {Text("코스 살펴보기",fontSize=19.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(35.dp));AtlasSymbol("arrow",Color.White,Modifier.size(25.dp))}
     }
    }
    Box(Modifier.weight(.57f).fillMaxHeight()) {
     RoutePhoto(coast,Modifier.fillMaxSize())
     Text("정동진 해변 · 코스 인근 풍경",color=Color.White,fontSize=14.sp,modifier=Modifier.align(Alignment.TopStart).padding(20.dp).background(Color.Black.copy(alpha=.48f),RoundedCornerShape(7.dp)).padding(10.dp))
     Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha=.40f)).padding(horizontal=18.dp)) {PhotoCreditLine(coast,onDark=true)}
    }
   }
  }
  Spacer(Modifier.height(27.dp))
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("다른 풍경도 만나보세요",color=Ink,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text("실제 도로 경로 3개",fontSize=15.sp,color=Muted)}
  Spacer(Modifier.height(18.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
   Catalog.routes.drop(1).forEach { r -> CompactRoadCard(r,Modifier.weight(1f)){onRoute(r)} }
  }
 } else {
  Text(if(mode=="season")"${season}에 어울리는 ${routes.size}개의 길" else "${if(mood=="전체")"기분 따라 고르는" else mood} ${routes.size}개의 길",color=Ink,fontSize=26.sp,fontWeight=FontWeight.Bold)
  Spacer(Modifier.height(20.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){routes.forEach { r-> PhotoRoadCard(r,Modifier.weight(1f),snapshot.awards.any{it.routeId==r.id}){onRoute(r)} };if(routes.size==1)Spacer(Modifier.weight(1f))}
 }
 Spacer(Modifier.height(26.dp))
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AtlasSoft).padding(horizontal=22.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
  AtlasSymbol(if(snapshot.trip!=null)"roads" else "stamp",AtlasGreen,Modifier.size(29.dp));Spacer(Modifier.width(16.dp))
  Column(Modifier.weight(1f)) {Text(if(snapshot.trip!=null)"진행 중인 여정이 있어요" else "좋았던 길은 우표로 남겨요",color=Ink,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text(if(snapshot.trip!=null)"주차한 상태에서 체험을 이어갈 수 있어요." else "${snapshot.awards.map{it.routeId}.distinct().size} / 3종 수집 · 모의 방문 기록",color=Muted,fontSize=14.sp,modifier=Modifier.padding(top=5.dp))}
  TextButton(onClick=if(snapshot.trip!=null)onTrip else onAtlas,modifier=Modifier.heightIn(min=56.dp)){Text(if(snapshot.trip!=null)"여정 이어보기" else "우표 도감",color=AtlasGreen,fontSize=17.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(14.dp));AtlasSymbol("arrow",AtlasGreen,Modifier.size(22.dp))}
 }
}

@Composable private fun CompactRoadCard(route:Route,modifier:Modifier,onClick:()->Unit) {
 Surface(onClick=onClick,color=White,shape=CardShape,modifier=modifier) {
  Row(Modifier.height(228.dp)) {
   RoutePhoto(route,Modifier.width(218.dp).fillMaxHeight())
   Column(Modifier.weight(1f).padding(22.dp)) {
    Text(if(route.id=="forest")"숲 · 주차 후 산책" else "호수 · 한적한 물길",color=AtlasGreen,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
    Text(route.title,color=Ink,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=11.dp),maxLines=2)
    Spacer(Modifier.height(14.dp));InlineStats(route)
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(regionFor(route),fontSize=15.sp,color=Muted,modifier=Modifier.weight(1f));AtlasSymbol("arrow",Ink,Modifier.size(24.dp))}
    Text("© ${photoCredit(route).author} · ${photoCredit(route).license}",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=12.dp),maxLines=1)
   }
  }
 }
}

@Composable private fun PhotoRoadCard(route:Route,modifier:Modifier=Modifier,collected:Boolean=false,onClick:()->Unit) {
 Surface(onClick=onClick,color=White,shape=CardShape,modifier=modifier) {
  Column {
   Box(Modifier.fillMaxWidth().height(242.dp)) {
    RoutePhoto(route,Modifier.fillMaxSize())
    SmallPill(if(collected)"우표 수집 완료" else regionFor(route),Modifier.align(Alignment.TopStart).padding(18.dp))
   }
   Column(Modifier.padding(22.dp)) {
    Text(route.title,color=Ink,fontSize=28.sp,fontWeight=FontWeight.Bold)
    Text(guide(route).reason,color=Muted,fontSize=17.sp,lineHeight=27.sp,modifier=Modifier.padding(top=10.dp),maxLines=3,overflow=TextOverflow.Ellipsis)
    Spacer(Modifier.height(20.dp));InlineStats(route)
    Text("OSRM 추정 · 정차·교통 제외",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
    PhotoCreditLine(route)
    HorizontalDivider(color=Line);Spacer(Modifier.height(17.dp))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("코스 살펴보기",color=AtlasGreen,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));AtlasSymbol("arrow",AtlasGreen,Modifier.size(24.dp))}
   }
  }
 }
}

@Composable internal fun DesignCuration(mode:String,filters:GuideFilters,awards:List<Award>,onRoute:(Route)->Unit,onBack:()->Unit) {
 var season by filters.season;var mood by filters.mood;var region by filters.region
 TextButton(onClick=onBack,modifier=Modifier.heightIn(min=56.dp),contentPadding=PaddingValues(0.dp)){Text("←  길 고르기",fontSize=17.sp,color=Muted)}
 Spacer(Modifier.height(10.dp))
 DesignTitle(when(mode){"season"->"계절 드라이브";"theme"->"테마 드라이브";else->"추천길"},"지역과 풍경을 고르면, 준비한 코스를 보여드려요.")
 Spacer(Modifier.height(26.dp))
 Surface(color=White,shape=CardShape){Column(Modifier.padding(20.dp)){
  ChoiceRow("지역",listOf("전체","강릉","담양","춘천"),region){region=it}
  if(mode=="season")ChoiceRow("계절",listOf("봄","여름","가을","겨울"),season){season=it}
  ChoiceRow("분위기",listOf("전체","탁 트인","잔잔한","초록 충전"),mood){mood=it}
 }}
 val routes=filteredRoutes(mode,season,mood,region,false)
 Spacer(Modifier.height(26.dp));Text("${routes.size}개의 길",color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(20.dp))
 if(routes.isEmpty()) {
  NoteBlock("이 조건에 맞는 길은 아직 없어요","다른 지역이나 분위기로 다시 찾아보세요.");Spacer(Modifier.height(18.dp));SecondaryButton("조건 초기화",{region="전체";mood="전체";season="가을"})
 } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
  routes.forEach{r->PhotoRoadCard(r,Modifier.weight(1f),awards.any{it.routeId==r.id}){onRoute(r)}}
  if(routes.size==1)Spacer(Modifier.weight(1f))
 }
}

@Composable internal fun DesignDetail(route:Route,hasTrip:Boolean,onBack:()->Unit,onStart:()->Unit,onTrip:()->Unit) {
 var sources by remember(route.id){mutableStateOf(false)}
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp),modifier=Modifier.weight(1f).heightIn(min=56.dp).wrapContentWidth(Alignment.Start)){Text("←  모든 코스",color=Muted,fontSize=17.sp)};TextButton(onClick={sources=true},modifier=Modifier.heightIn(min=56.dp)){Text("출처·이용 안내",color=Muted,fontSize=16.sp)}}
 Spacer(Modifier.height(8.dp))
 DesignTitle(route.title,route.subtitle){SmallPill(when(route.id){"coast"->"해안 드라이브";"forest"->"숲과 산책";else->"호수 드라이브"})}
 Spacer(Modifier.height(26.dp))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
  Column(Modifier.weight(.62f)) {
   Box(Modifier.fillMaxWidth().height(578.dp).clip(CardShape)) {RealRoadMap(route,Modifier.fillMaxSize())}
   Spacer(Modifier.height(20.dp))
   Surface(color=White,shape=CardShape){Row(Modifier.fillMaxWidth().padding(22.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){InlineStats(route,large=true);Text("OSRM 도로 경로 · 정차·교통상황 제외",color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=8.dp))};SmallPill("3개 기준점")}}
   Text("참고 경로입니다. 출발 전 내비와 현장 표지로 통제·진입 조건을 확인하세요.",color=Muted,fontSize=15.sp,lineHeight=24.sp,modifier=Modifier.padding(top=16.dp))
  }
  Column(Modifier.weight(.38f)) {
   Box(Modifier.fillMaxWidth().height(200.dp).clip(CardShape)){RoutePhoto(route,Modifier.fillMaxSize())}
   PhotoCreditLine(route)
   Text("코스 따라보기",color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp,bottom=18.dp))
   route.stops.forEachIndexed{i,s->
    Row(Modifier.fillMaxWidth().height(91.dp)) {
     Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(34.dp).background(if(i==0)AtlasGreen else AtlasSoft,CircleShape),contentAlignment=Alignment.Center){Text("${i+1}",fontSize=16.sp,fontWeight=FontWeight.Bold,color=if(i==0)Color.White else AtlasGreen)};if(i<2)Box(Modifier.width(2.dp).weight(1f).background(Line))}
     Column(Modifier.padding(start=15.dp,top=2.dp)){Text(s.name,color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text(if(route.id=="forest"&&i==2)"나무길은 주차 후 도보로 이동" else if(i==0)"출발 기준점" else if(i==2)"도착 기준점" else "경유 기준점",color=Muted,fontSize=14.sp,modifier=Modifier.padding(top=6.dp))}
    }
   }
   Text("표시한 지점은 주차·정차 가능 여부가 확인되지 않은 도로 기준점입니다.",color=Muted,fontSize=14.sp,lineHeight=22.sp,modifier=Modifier.padding(bottom=20.dp))
   Button(onClick=onStart,colors=ButtonDefaults.buttonColors(containerColor=AtlasGreen),shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().height(68.dp)){Text("지도 연결 옵션",fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));AtlasSymbol("arrow",Color.White,Modifier.size(25.dp))}
   if(hasTrip){Spacer(Modifier.height(10.dp));SecondaryButton("진행 중인 체험 보기",onTrip,Modifier.fillMaxWidth())}
   Text("차량 P·UX 연동 · 내비는 응답 확인 필요 · 방문 인증 미구현",color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=12.dp))
  }
 }
 if(sources)SourcesDialog(route){sources=false}
}

@Composable internal fun DesignAtlas(awards:List<Award>,onRoute:(Route)->Unit,onExport:(Route,Award)->Unit) {
 val count=awards.map{it.routeId}.distinct().size
 DesignTitle("우표 도감","다녀온 길을 한 장씩 모아요. 지금은 모의 방문으로 수집합니다."){SmallPill("$count / 3종 수집")}
 Spacer(Modifier.height(28.dp))
 Row(Modifier.fillMaxWidth().clip(CardShape).background(AtlasDark).padding(26.dp),verticalAlignment=Alignment.CenterVertically){
  AtlasSymbol("stamp",Color(0xFFB5CFB2),Modifier.size(44.dp));Spacer(Modifier.width(22.dp))
  Column(Modifier.weight(1f)){Text("나의 첫 번째 드라이브 컬렉션",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold);Text("바다, 숲, 호수. 세 가지 풍경을 담은 여행 우표.",color=Color(0xFFB6C3BA),fontSize=16.sp,modifier=Modifier.padding(top=8.dp))}
  repeat(3){i->Box(Modifier.padding(start=8.dp).size(14.dp).background(if(i<count)Color(0xFFB5CFB2) else Color(0xFF425149),CircleShape))}
 }
 Spacer(Modifier.height(28.dp))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
  Catalog.routes.forEachIndexed { i,r ->
   val award=awards.lastOrNull{it.routeId==r.id}
   Column(Modifier.weight(1f)) {
    Surface(color=White,shape=StampEdge){Column(Modifier.padding(20.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(regionFor(r),color=Ink,fontSize=16.sp,fontWeight=FontWeight.Bold);Text("NO. 0${i+1}",color=Muted,fontSize=13.sp)}
     Spacer(Modifier.height(16.dp));PixelLandscape(i,Modifier.fillMaxWidth().height(226.dp).clip(RoundedCornerShape(8.dp)))
     Text(r.title,color=Ink,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=22.dp),maxLines=1,overflow=TextOverflow.Ellipsis)
     Text(if(award==null)"아직 모으지 않은 우표" else "${formatDate(award.timeMs)} · 모의 완주",color=Muted,fontSize=15.sp,modifier=Modifier.padding(top=10.dp,bottom=18.dp))
     SmallPill(if(award==null)"미수집" else "수집 완료 · SIM",dark=award!=null)
    }}
    Spacer(Modifier.height(18.dp))
    if(award==null)SecondaryButton("이 길 살펴보기",{onRoute(r)},Modifier.fillMaxWidth()) else PrimaryButton("우표 PNG 만들기  ↗",{onExport(r,award)},Modifier.fillMaxWidth())
   }
  }
 }
 Spacer(Modifier.height(26.dp))
 Text("우표의 그림은 직접 만든 일러스트입니다. 실제 방문 인증이나 사진이 아니며, PNG는 앱 내부에 보관합니다.",color=Muted,fontSize=15.sp,lineHeight=24.sp)
}

@Composable internal fun DesignRecords(snapshot:Snapshot,onRoute:(Route)->Unit) {
 val rows=(snapshot.awards.map{DesignRecord(it.routeId,it.timeMs,"모의 완주",3)}+snapshot.visits.map{DesignRecord(it.routeId,it.timeMs,if(it.status==VisitStatus.UNCONFIRMED)"미확인" else "중단",it.progress)}).sortedByDescending{it.time}
 val visited=Catalog.routes.filter{r->rows.any{it.id==r.id}}
 var selected by remember(snapshot){mutableStateOf(visited.firstOrNull()?:Catalog.routes.first())}
 var view by remember{mutableStateOf("목록")}
 DesignTitle("다녀온 길","완주한 여행도, 잠시 멈춘 여행도 여기에 남아요."){SmallPill("${rows.size}회 기록")}
 Spacer(Modifier.height(28.dp))
 if(rows.isEmpty()) {
  Surface(color=White,shape=CardShape,modifier=Modifier.fillMaxWidth()){
   Row(Modifier.height(350.dp).padding(38.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     AtlasSymbol("roads",AtlasGreen,Modifier.size(46.dp));Spacer(Modifier.height(24.dp))
     Text("아직 다녀온 길이 없어요",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
     Text("코스를 고르고 모의 여정을 마치면\n날짜와 진행 기록이 이곳에 남습니다.",fontSize=19.sp,lineHeight=30.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
     PrimaryButton("첫 번째 길 찾아보기",{onRoute(Catalog.routes.first())})
    }
    Box(Modifier.width(380.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp))){RoutePhoto(Catalog.routes.first(),Modifier.fillMaxSize());Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha=.4f)).padding(horizontal=14.dp)){PhotoCreditLine(Catalog.routes.first(),onDark=true)}}
   }
  }
 } else {
  ChoiceRow("보기",listOf("목록","지도"),view){view=it};Spacer(Modifier.height(20.dp))
  if(view=="지도") {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){
    Box(Modifier.weight(.65f).height(460.dp).clip(CardShape)){RealRoadMap(selected,Modifier.fillMaxSize())}
    Column(Modifier.weight(.35f),verticalArrangement=Arrangement.spacedBy(14.dp)){
     visited.forEach{r->Surface(onClick={selected=r},color=if(selected.id==r.id)AtlasDark else White,shape=CardShape,modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(24.dp)){Text(r.title,fontSize=22.sp,fontWeight=FontWeight.Bold,color=if(selected.id==r.id)Color.White else Ink);Text("${rows.count{it.id==r.id}}회 기록 · 모의 여정",fontSize=15.sp,color=if(selected.id==r.id)Color(0xFFB6C3BA) else Muted,modifier=Modifier.padding(top=9.dp))}
     }}
     SecondaryButton("선택한 코스 보기",{onRoute(selected)},Modifier.fillMaxWidth())
    }
   }
   Text("코스 참고 경로이며, 실제 이동 궤적이 아닙니다.",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
  }
  rows.forEach {record->
   val route=Catalog.routes.first{it.id==record.id}
   Surface(onClick={onRoute(route)},color=White,shape=CardShape,modifier=Modifier.fillMaxWidth().padding(bottom=14.dp)) {
    Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
     RoutePhoto(route,Modifier.width(144.dp).height(96.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(24.dp))
     Column(Modifier.weight(1f)){Text(formatDate(record.time),fontSize=15.sp,color=Muted);Text(route.title,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=8.dp))}
     SmallPill("${record.status} · ${record.progress}/3 지점");Spacer(Modifier.width(30.dp));AtlasSymbol("arrow",Ink,Modifier.size(26.dp))
    }
   }
  }
 }
 Spacer(Modifier.height(24.dp))
 Text("이 기기에만 저장됩니다. 실제 위치·이동 궤적은 수집하지 않으며 앱을 삭제하면 기록도 사라집니다.",fontSize=15.sp,lineHeight=24.sp,color=Muted)
}
private data class DesignRecord(val id:String,val time:Long,val status:String,val progress:Int)
