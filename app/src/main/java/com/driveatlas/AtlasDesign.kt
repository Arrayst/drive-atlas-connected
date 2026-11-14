package com.driveatlas
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// UI only. Safety, vehicle SDK, route geometry, storage and journey evaluation are unchanged.
internal val AtlasGreen = Color(0xFF1769F5)
internal val AtlasSoft = Color(0xFFEAF1FF)
internal val AtlasDark = Color(0xFF101C30)
private val CardShape = RoundedCornerShape(16.dp)

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
   "check" -> {drawLine(color,p(.24f,.50f),p(.43f,.68f),s*.09f,StrokeCap.Round);drawLine(color,p(.43f,.68f),p(.77f,.31f),s*.09f,StrokeCap.Round)}
   else -> {drawLine(color,p(.2f,.3f),p(.8f,.3f),s*.06f,StrokeCap.Round);drawLine(color,p(.2f,.7f),p(.8f,.7f),s*.06f,StrokeCap.Round);drawCircle(color,s*.09f,p(.4f,.3f));drawCircle(color,s*.09f,p(.65f,.7f))}
  }
 }
}

@Composable internal fun DesignSidebar(page:String,unlocked:Boolean,snapshot:Snapshot,wide:Boolean,compact:Boolean,onPage:(String)->Unit,onDebug:()->Unit) {
 BoxWithConstraints(Modifier.fillMaxWidth().background(White).border(1.dp,Line)) {
  val narrow=maxWidth<1200.dp
  val showBrand=maxWidth>=480.dp
  Column(Modifier.padding(horizontal=if(wide)36.dp else 16.dp,vertical=10.dp)) {
   Row(Modifier.fillMaxWidth().heightIn(min=76.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Box(Modifier.size(40.dp).background(AtlasGreen,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){AtlasSymbol("roads",White,Modifier.size(28.dp))}
    if(showBrand)Text("Drive Atlas",color=Ink,fontSize=23.sp,fontWeight=FontWeight.Bold)
    if(!narrow)DesignNavigationTabs(page,unlocked,onPage,Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
    if(snapshot.trip!=null)TextButton(onClick={onPage("trip")},enabled=unlocked,modifier=Modifier.heightIn(min=64.dp)){Text("여정 계속",color=if(unlocked)AtlasGreen else Muted,fontSize=16.sp)}
    TextButton(onClick=onDebug,enabled=unlocked,modifier=Modifier.heightIn(min=64.dp)) {
     Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Text(if(unlocked)"P · 조작 가능" else "차량 상태 확인/제한",fontSize=14.sp,color=Muted)
      Text("연결 상태 보기",fontSize=16.sp,color=Ink)
     }
    }
   }
   if(narrow)DesignNavigationTabs(page,unlocked,onPage,Modifier.fillMaxWidth())
  }
 }
}

@Composable private fun DesignNavigationTabs(page:String,unlocked:Boolean,onPage:(String)->Unit,modifier:Modifier=Modifier) {
 Row(modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   listOf("discover" to "길 고르기","family" to "가족 비교","auto-eta" to "왕복 시간","family-memory" to "우리 차 기억","records" to "다녀온 길","atlas" to "우표 도감").forEach { (id,label) ->
    val active=unlocked&&(page==id||(page in listOf("curation","detail","trip")&&id=="discover"))
    val interaction=remember(id){MutableInteractionSource()}
    val focused by interaction.collectIsFocusedAsState()
    Box(Modifier.height(68.dp).widthIn(min=112.dp).clip(RoundedCornerShape(12.dp)).background(if(active)AtlasSoft else Color.Transparent).border(if(focused)3.dp else 0.dp,if(focused)AtlasGreen else Color.Transparent,RoundedCornerShape(12.dp)).selectable(selected=active,enabled=unlocked,role=Role.Tab,interactionSource=interaction,indication=androidx.compose.foundation.LocalIndication.current){onPage(id)}.padding(horizontal=16.dp),contentAlignment=Alignment.Center) {
     Text(label,color=if(!unlocked)Muted else if(active)AtlasGreen else Ink,fontSize=20.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,maxLines=1,textAlign=TextAlign.Center)
     if(active)Box(Modifier.align(Alignment.BottomCenter).width(32.dp).height(3.dp).background(AtlasGreen))
    }
   }
 }
}

@Composable private fun DesignTitle(title:String,subtitle:String,right:(@Composable ()->Unit)?=null) {
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  if(maxWidth>=720.dp) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
   Column(Modifier.weight(1f)) {
    Text(title,color=Ink,fontSize=36.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
    Text(subtitle,color=Muted,fontSize=18.sp,lineHeight=27.sp,modifier=Modifier.padding(top=8.dp))
   }
   right?.invoke()
  } else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(title,color=Ink,fontSize=30.sp,fontWeight=FontWeight.Bold)
   Text(subtitle,color=Muted,fontSize=18.sp,lineHeight=27.sp)
   right?.invoke()
  }
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
  Text(String.format(Locale.KOREA,"주행 약 %.0f분",road.seconds/60),color=color,fontSize=if(large)22.sp else 17.sp,fontWeight=FontWeight.SemiBold)
 }
}

@Composable private fun PhotoCreditLine(route:Route,modifier:Modifier=Modifier,onDark:Boolean=false) {
 var show by remember(route.id){mutableStateOf(false)}
 val credit=photoCredit(route)
 TextButton(onClick={show=true},contentPadding=PaddingValues(horizontal=0.dp,vertical=4.dp),modifier=modifier.heightIn(min=64.dp)) {
  Text("© ${credit.author} · ${credit.license}  ⓘ",fontSize=12.sp,color=if(onDark)Color.White.copy(alpha=.88f) else Muted,maxLines=1)
 }
 if(show)SourcesDialog(route){show=false}
}

@Composable internal fun DesignHome(snapshot:Snapshot,onCategory:(String)->Unit,onTrip:()->Unit,onAtlas:()->Unit,onRoute:(Route)->Unit,filters:GuideFilters) {
 var selectedId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(Catalog.routes.first().id) }
 val selected=Catalog.routes.firstOrNull{it.id==selectedId}?:Catalog.routes.first()
 DesignTitle("어디로 떠나볼까요?","길을 고른 뒤, 가족에게 맞는 시간과 쉼터를 살펴보세요.") {
  Text("드라이브 코스 ${Catalog.routes.size}곳",color=Muted,fontSize=18.sp)
 }
 Spacer(Modifier.height(28.dp))
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  if(maxWidth>=1000.dp) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
    Column(Modifier.weight(.34f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Catalog.routes.forEach { route -> HomeRouteChoice(route,route.id==selected.id){selectedId=route.id} }
     HomeCategories(onCategory)
     PhotoCreditLine(selected)
    }
    HomeRouteConsole(selected,Modifier.weight(.66f)){onRoute(selected)}
   }
  } else {
   Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     Catalog.routes.forEach { route -> Box(Modifier.width(300.dp)){HomeRouteChoice(route,route.id==selected.id){selectedId=route.id}} }
    }
    HomeRouteConsole(selected,Modifier.fillMaxWidth()){onRoute(selected)}
    HomeCategories(onCategory)
    PhotoCreditLine(selected)
   }
  }
 }
 Spacer(Modifier.height(24.dp))
 Row(Modifier.fillMaxWidth().border(1.dp,Line,CardShape).padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
  AtlasSymbol(if(snapshot.trip!=null)"roads" else "stamp",AtlasGreen,Modifier.size(28.dp))
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Text(if(snapshot.trip!=null)"저장한 여정이 있어요" else "다녀온 길을 모아보세요",color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
   Text(if(snapshot.trip!=null)"이어서 살펴볼 수 있는 모의 여정" else "선택은 주차 중에, 풍경은 안전하게",color=Muted,fontSize=16.sp)
  }
  TextButton(onClick=if(snapshot.trip!=null)onTrip else onAtlas,modifier=Modifier.heightIn(min=76.dp)){Text(if(snapshot.trip!=null)"여정 이어보기" else "우표 도감",color=AtlasGreen,fontSize=20.sp)}
 }
}

@Composable private fun HomeRouteChoice(route:Route,active:Boolean,onSelect:()->Unit) {
 val interaction=remember(route.id){MutableInteractionSource()}
 val focused by interaction.collectIsFocusedAsState()
 val context=LocalContext.current
 val road=remember(route.id){readRoad(context,route.id)}
 val largeType=LocalDensity.current.fontScale>1.4f
 BoxWithConstraints(Modifier.fillMaxWidth()) {
 val showPhoto=maxWidth>=350.dp&&!largeType
 Row(Modifier.fillMaxWidth().heightIn(min=148.dp).clip(CardShape).background(White)
  .border(if(focused)3.dp else if(active)2.dp else 1.dp,if(active||focused)AtlasGreen else Line,CardShape)
  .selectable(selected=active,role=Role.RadioButton,interactionSource=interaction,indication=androidx.compose.foundation.LocalIndication.current,onClick=onSelect)
  .padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
  if(showPhoto)RoutePhoto(route,Modifier.size(92.dp).clip(RoundedCornerShape(8.dp)))
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(regionFor(route),color=Muted,fontSize=16.sp)
   Text(route.title,color=Ink,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=if(largeType)4 else 2,overflow=TextOverflow.Ellipsis)
   Text(String.format(Locale.KOREA,"코스 %.1f km · 약 %.0f분",road.meters/1000,road.seconds/60),color=Muted,fontSize=17.sp)
  }
  TripSelectionMark(active)
 }
 }
}

@Composable private fun HomeRouteConsole(route:Route,modifier:Modifier,onOpen:()->Unit) {
 val context=LocalContext.current
 val road=remember(route.id){readRoad(context,route.id)}
 Column(modifier.clip(CardShape).background(White).border(1.dp,Line,CardShape)) {
  BoxWithConstraints(Modifier.fillMaxWidth().padding(24.dp)) {
   val stacked=maxWidth<620.dp||LocalDensity.current.fontScale>1.4f
   val title:@Composable ()->Unit = {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(route.title,color=Ink,fontSize=28.sp,fontWeight=FontWeight.Bold)
     Text(route.stops.first().name+" → "+route.stops.last().name,color=Muted,fontSize=18.sp)
    }
   }
   val duration:@Composable ()->Unit = {
    Column(horizontalAlignment=if(stacked)Alignment.Start else Alignment.End,verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text(String.format(Locale.KOREA,"약 %.0f분",road.seconds/60),color=Ink,fontSize=30.sp,fontWeight=FontWeight.Bold)
     Text(String.format(Locale.KOREA,"코스 주행 · %.1f km",road.meters/1000),color=Muted,fontSize=16.sp)
    }
   }
   if(stacked)Column(verticalArrangement=Arrangement.spacedBy(16.dp)){title();duration()}
   else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
    Box(Modifier.weight(1f)){title()};duration()
   }
  }
  HorizontalDivider(color=Line)
  key(route.id){RealRoadMap(route,Modifier.fillMaxWidth().height(publicMapCardHeightDp(LocalDensity.current.fontScale).dp))}
  HorizontalDivider(color=Line)
  Column(Modifier.padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("코스 구간의 도로 예상 시간이에요. 왕복·정차·실시간 교통은 포함하지 않아요.",color=Muted,fontSize=16.sp,lineHeight=24.sp)
   Button(onClick=onOpen,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=AtlasGreen,contentColor=White),modifier=Modifier.fillMaxWidth().heightIn(min=76.dp)) {
    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
     Text("이 코스 살펴보기",fontSize=20.sp,fontWeight=FontWeight.Bold)
     AtlasSymbol("arrow",White,Modifier.size(24.dp).align(Alignment.CenterEnd))
    }
   }
  }
 }
}

@Composable private fun HomeCategories(onCategory:(String)->Unit) {
 Column(Modifier.fillMaxWidth().padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("다른 기준으로 찾기",color=Muted,fontSize=17.sp,modifier=Modifier.padding(start=4.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   listOf("recommended" to "추천길","season" to "계절별","theme" to "테마별").forEach{(id,label)->
    TextButton(onClick={onCategory(id)},shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.textButtonColors(containerColor=White,contentColor=Ink),modifier=Modifier.weight(1f).heightIn(min=64.dp)){Text(label,fontSize=20.sp,fontWeight=FontWeight.Medium)}
   }
  }
  Text("사진 출처는 코스 상세에서 확인할 수 있어요.",color=Muted,fontSize=14.sp,modifier=Modifier.padding(start=4.dp,top=2.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DesignCuration(mode:String,filters:GuideFilters,awards:List<Award>,onRoute:(Route)->Unit,onBack:()->Unit) {
 var season by filters.season;var mood by filters.mood;var region by filters.region
 TextButton(onClick=onBack,modifier=Modifier.heightIn(min=64.dp),contentPadding=PaddingValues(0.dp)){Text("←  길 고르기",fontSize=17.sp,color=Muted)}
 Spacer(Modifier.height(10.dp))
 DesignTitle(when(mode){"season"->"계절 드라이브";"theme"->"테마 드라이브";else->"추천길"},"지역과 풍경을 고르면, 준비한 코스를 보여드려요.")
 Spacer(Modifier.height(26.dp))
 Surface(color=White,shape=CardShape){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  ChoiceRow("지역",listOf("전체","강릉","담양","춘천"),region){region=it}
  if(mode=="season")ChoiceRow("계절",listOf("봄","여름","가을","겨울"),season){season=it}
  ChoiceRow("분위기",listOf("전체","탁 트인","잔잔한","초록 충전"),mood){mood=it}
 }}
 val routes=filteredRoutes(mode,season,mood,region,false)
 Spacer(Modifier.height(26.dp));Text("${routes.size}개의 길",color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(20.dp))
 if(routes.isEmpty()) {
  NoteBlock("이 조건에 맞는 길은 아직 없어요","다른 지역이나 분위기로 다시 찾아보세요.");Spacer(Modifier.height(18.dp));SecondaryButton("조건 초기화",{region="전체";mood="전체";season="가을"})
 } else BoxWithConstraints(Modifier.fillMaxWidth()) {
  val cardWidth=if(maxWidth>=1000.dp)(maxWidth-40.dp)/3 else if(maxWidth>=680.dp)(maxWidth-20.dp)/2 else maxWidth
  FlowRow(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
   routes.forEach{r->PhotoRoadCard(r,Modifier.width(cardWidth),awards.any{it.routeId==r.id}){onRoute(r)}}
  }
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DesignDetail(route:Route,hasTrip:Boolean,onBack:()->Unit,onStart:()->Unit,onTrip:()->Unit,onPreview:(()->Unit)?=null) {
 var sources by remember(route.id){mutableStateOf(false)}
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp),modifier=Modifier.weight(1f).heightIn(min=64.dp).wrapContentWidth(Alignment.Start)){Text("←  모든 코스",color=Muted,fontSize=17.sp)};TextButton(onClick={sources=true},modifier=Modifier.heightIn(min=64.dp)){Text("출처·이용 안내",color=Muted,fontSize=16.sp)}}
 Spacer(Modifier.height(8.dp))
 DesignTitle(route.title,route.subtitle){SmallPill(when(route.id){"coast"->"해안 드라이브";"forest"->"숲과 산책";else->"호수 드라이브"})}
 Spacer(Modifier.height(26.dp))
 BoxWithConstraints(Modifier.fillMaxWidth()) {
 val split=maxWidth>=1000.dp && LocalDensity.current.fontScale<=1.3f
 val leftWidth=if(split)(maxWidth-24.dp)*.60f else maxWidth
 val rightWidth=if(split)(maxWidth-24.dp)*.40f else maxWidth
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
  Column(Modifier.width(leftWidth)) {
   Box(Modifier.fillMaxWidth().height(publicMapCardHeightDp(LocalDensity.current.fontScale).dp).clip(CardShape)) {RealRoadMap(route,Modifier.fillMaxSize())}
   Spacer(Modifier.height(20.dp))
   Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){InlineStats(route);Text("${route.stops.size}개 기준점 · OSRM 도로 경로 · 정차·교통상황 제외",color=Muted,fontSize=16.sp)}
   Text("참고 경로입니다. 출발 전 내비와 현장 표지로 통제·진입 조건을 확인하세요.",color=Muted,fontSize=16.sp,lineHeight=24.sp,modifier=Modifier.padding(top=12.dp))
   Spacer(Modifier.height(20.dp))
   Box(Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(12.dp))){RoutePhoto(route,Modifier.fillMaxSize())}
   PhotoCreditLine(route)
  }
  Column(Modifier.width(rightWidth).clip(RoundedCornerShape(12.dp)).background(White).border(1.dp,Line,RoundedCornerShape(12.dp)).padding(24.dp)) {
   Text("코스 기준점",color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold)
   Text("지도 번호 순서로 확인하세요",color=Muted,fontSize=16.sp,modifier=Modifier.padding(top=6.dp,bottom=22.dp))
   route.stops.forEachIndexed{i,s->
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min=80.dp)) {
     Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(34.dp).background(AtlasSoft,CircleShape),contentAlignment=Alignment.Center){Text("${i+1}",fontSize=16.sp,fontWeight=FontWeight.Bold,color=AtlasGreen)};if(i<route.stops.lastIndex)Box(Modifier.width(2.dp).weight(1f).background(Line))}
     Column(Modifier.weight(1f).padding(start=15.dp,top=2.dp,bottom=20.dp)){Text(s.name,color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text(if(route.id=="forest"&&i==2)"나무길은 주차 후 도보로 이동" else if(i==0)"출발 기준점" else if(i==route.stops.lastIndex)"도착 기준점" else "경유 기준점",color=Muted,fontSize=16.sp,modifier=Modifier.padding(top=6.dp))}
    }
   }
   Text("표시한 지점은 주차·정차 가능 여부가 확인되지 않은 도로 기준점입니다.",color=Muted,fontSize=16.sp,lineHeight=22.sp,modifier=Modifier.padding(bottom=20.dp))
   HorizontalDivider(color=Line)
   Spacer(Modifier.height(20.dp))
   Button(onClick={onPreview?.invoke()},enabled=onPreview!=null,colors=ButtonDefaults.buttonColors(containerColor=AtlasGreen),shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().heightIn(min=76.dp)){Text("앱 경로 미리보기 · SIM",fontSize=20.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)}
   Text(if(onPreview!=null)"선택 코스와 저장한 쉼터를 앱 지도에서 미리 봅니다. 실제 길안내가 아닌 위치 재생 시연입니다." else "앱 경로 시연은 디버그 에뮬레이터의 주차 상태에서만 제공됩니다. 이 환경에서는 사용할 수 없어요.",color=Muted,fontSize=16.sp,modifier=Modifier.padding(top=10.dp))
   TextButton(onClick=onStart,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("다른 지도·내비 연결 옵션",fontSize=16.sp)}
   if(hasTrip){Spacer(Modifier.height(10.dp));SecondaryButton("진행 중인 체험 보기",onTrip,Modifier.fillMaxWidth())}
   Text("차량 P·UX 연동 · 내비는 응답 확인 필요 · 방문 인증 미구현",color=Muted,fontSize=16.sp,modifier=Modifier.padding(top=12.dp))
  }
 }
 }
 if(sources)SourcesDialog(route){sources=false}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DesignAtlas(awards:List<Award>,onRoute:(Route)->Unit,onExport:(Route,Award)->Unit) {
 val count=awards.map{it.routeId}.distinct().size
 DesignTitle("우표 도감","다녀온 길을 한 장씩 모아요. 지금은 모의 방문으로 수집합니다."){SmallPill("$count / 3종 수집")}
 Spacer(Modifier.height(20.dp))
 Text("모의 여정을 마치면 우표가 추가됩니다. 수집한 우표는 PNG로 만들 수 있어요.",color=Muted,fontSize=17.sp,lineHeight=26.sp)
 Spacer(Modifier.height(24.dp))
 BoxWithConstraints(Modifier.fillMaxWidth()) {
 val fontScale=LocalDensity.current.fontScale
 val stampWidth=if(maxWidth>=1000.dp && fontScale<=1.3f)(maxWidth-44.dp)/3 else if(maxWidth>=760.dp && fontScale<=1.6f)(maxWidth-22.dp)/2 else maxWidth
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
  Catalog.routes.forEachIndexed { i,r ->
   val award=awards.lastOrNull{it.routeId==r.id}
   Column(Modifier.width(stampWidth)) {
    Surface(color=White,shape=StampEdge){Column(Modifier.padding(20.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(regionFor(r),color=Ink,fontSize=16.sp,fontWeight=FontWeight.Bold);Text("NO. 0${i+1}",color=Muted,fontSize=13.sp)}
     Spacer(Modifier.height(16.dp));PixelLandscape(i,Modifier.fillMaxWidth().height(226.dp).clip(RoundedCornerShape(8.dp)))
     Text(r.title,color=Ink,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=22.dp),minLines=2)
     Text(if(award==null)"아직 모으지 않은 우표" else "${formatDate(award.timeMs)} · 모의 완주",color=Muted,fontSize=15.sp,modifier=Modifier.padding(top=10.dp,bottom=18.dp))
     SmallPill(if(award==null)"미수집" else "수집 완료 · SIM",dark=award!=null)
    }}
    Spacer(Modifier.height(18.dp))
    if(award==null)SecondaryButton("이 길 살펴보기",{onRoute(r)},Modifier.fillMaxWidth()) else PrimaryButton("우표 PNG 만들기",{onExport(r,award)},Modifier.fillMaxWidth())
   }
  }
 }
 }
 Spacer(Modifier.height(26.dp))
 Text("우표의 그림은 직접 만든 일러스트입니다. 실제 방문 인증이나 사진이 아니며, PNG는 앱 내부에 보관합니다.",color=Muted,fontSize=15.sp,lineHeight=24.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DesignRecords(snapshot:Snapshot,onRoute:(Route)->Unit) {
 val rows=(snapshot.awards.map{DesignRecord(it.routeId,it.timeMs,"모의 완주",3)}+snapshot.visits.map{DesignRecord(it.routeId,it.timeMs,if(it.status==VisitStatus.UNCONFIRMED)"미확인" else "중단",it.progress)}).sortedByDescending{it.time}
 val visited=Catalog.routes.filter{r->rows.any{it.id==r.id}}
 var selected by remember(snapshot){mutableStateOf(visited.firstOrNull()?:Catalog.routes.first())}
 var view by remember{mutableStateOf("목록")}
 DesignTitle("다녀온 길","모의 여정의 완주·중단 기록을 모아 봅니다."){SmallPill("${rows.size}회 기록")}
 Spacer(Modifier.height(28.dp))
 if(rows.isEmpty()) {
  Surface(color=White,shape=CardShape,modifier=Modifier.fillMaxWidth()){
   BoxWithConstraints(Modifier.fillMaxWidth()) {
   val showEmptyPhoto=maxWidth>=840.dp
   Row(Modifier.heightIn(min=350.dp).padding(32.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     AtlasSymbol("roads",AtlasGreen,Modifier.size(46.dp));Spacer(Modifier.height(24.dp))
     Text("아직 다녀온 길이 없어요",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
     Text("코스를 고르고 모의 여정을 마치면\n날짜와 진행 기록이 이곳에 남습니다.",fontSize=19.sp,lineHeight=30.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
     PrimaryButton("첫 번째 길 찾아보기",{onRoute(Catalog.routes.first())})
     if(!showEmptyPhoto)PhotoCreditLine(Catalog.routes.first())
    }
    if(showEmptyPhoto)Box(Modifier.padding(start=24.dp).width(320.dp).height(280.dp).clip(RoundedCornerShape(12.dp))){RoutePhoto(Catalog.routes.first(),Modifier.fillMaxSize());Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha=.4f)).padding(horizontal=14.dp)){PhotoCreditLine(Catalog.routes.first(),onDark=true)}}
   }
  }
 }
 } else {
  ChoiceRow("보기",listOf("목록","지도"),view){view=it};Spacer(Modifier.height(20.dp))
  if(view=="지도") {
   BoxWithConstraints(Modifier.fillMaxWidth()) {
   val mapPane:@Composable (Modifier)->Unit={pane->
    Box(pane.height(publicMapCardHeightDp(LocalDensity.current.fontScale).dp).clip(CardShape)){RealRoadMap(selected,Modifier.fillMaxSize())}
   }
   val listPane:@Composable (Modifier)->Unit={pane->
    Column(pane,verticalArrangement=Arrangement.spacedBy(14.dp)){
     visited.forEach{r->Surface(onClick={selected=r},color=White,shape=CardShape,border=BorderStroke(if(selected.id==r.id)2.dp else 1.dp,if(selected.id==r.id)AtlasGreen else Line),modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(24.dp)){Text(r.title,fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink);Text("${rows.count{it.id==r.id}}회 기록 · 모의 여정",fontSize=15.sp,color=Muted,modifier=Modifier.padding(top=9.dp))}
     }}
     SecondaryButton("선택한 코스 보기",{onRoute(selected)},Modifier.fillMaxWidth())
    }
   }
   // Row distributes rounded pixels once; independently rounded widths can wrap a FlowRow by 1px.
   if(maxWidth>=840.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){
    mapPane(Modifier.weight(.65f));listPane(Modifier.weight(.35f))
   }else Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(20.dp)){
    mapPane(Modifier.fillMaxWidth());listPane(Modifier.fillMaxWidth())
   }
   }
   Text("코스 참고 경로이며, 실제 이동 궤적이 아닙니다.",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
  }
  rows.forEach {record->
   val route=Catalog.routes.first{it.id==record.id}
   Surface(onClick={onRoute(route)},color=White,shape=CardShape,modifier=Modifier.fillMaxWidth().padding(bottom=14.dp)) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
     val narrow=maxWidth<680.dp
     Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)){
      RoutePhoto(route,Modifier.width(if(narrow)80.dp else 144.dp).height(96.dp).clip(RoundedCornerShape(10.dp)))
      Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
       Text(formatDate(record.time),fontSize=15.sp,color=Muted)
       Text(route.title,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
       if(narrow)SmallPill("${record.status} · ${record.progress}/3 지점")
      }
      if(!narrow)SmallPill("${record.status} · ${record.progress}/3 지점")
      AtlasSymbol("arrow",AtlasGreen,Modifier.size(26.dp))
     }
    }
   }
  }
 }
 Spacer(Modifier.height(24.dp))
 Text("이 기기에만 저장됩니다. 실제 위치·이동 궤적은 수집하지 않으며 앱을 삭제하면 기록도 사라집니다.",fontSize=15.sp,lineHeight=24.sp,color=Muted)
}
private data class DesignRecord(val id:String,val time:Long,val status:String,val progress:Int)
