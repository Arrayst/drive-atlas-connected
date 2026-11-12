package com.driveatlas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Icons follow OSRM maneuver semantics only; no parsing of Korean instruction text. */
@Composable internal fun NavigationManeuverSymbol(kind:NavManeuverIcon,modifier:Modifier=Modifier) {
 Canvas(modifier.semantics{contentDescription=navigationManeuverLabel(kind)}) {
  val unit=size.minDimension
  fun point(x:Float,y:Float)=Offset(x*unit,y*unit)
  val stroke=Stroke(unit*.09f,cap=StrokeCap.Round,join=StrokeJoin.Round)
  fun line(points:List<Offset>){drawPath(Path().apply{points.forEachIndexed{i,p->if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)}},NavigationRouteBlue,style=stroke)}
  fun arrow(points:List<Offset>){
   line(points)
   val end=points.last();val previous=points[points.lastIndex-1]
   val delta=end-previous;val length=delta.getDistance()
   if(length>0){val direction=delta/length;val perpendicular=Offset(-direction.y,direction.x)
    line(listOf(end-direction*(unit*.22f)+perpendicular*(unit*.18f),end,end-direction*(unit*.22f)-perpendicular*(unit*.18f)))
   }
  }
  when(kind){
   NavManeuverIcon.LEFT->arrow(listOf(point(.55f,.85f),point(.55f,.35f),point(.12f,.35f)))
   NavManeuverIcon.RIGHT->arrow(listOf(point(.45f,.85f),point(.45f,.35f),point(.88f,.35f)))
   NavManeuverIcon.SLIGHT_LEFT->arrow(listOf(point(.60f,.85f),point(.60f,.53f),point(.22f,.15f)))
   NavManeuverIcon.SLIGHT_RIGHT->arrow(listOf(point(.40f,.85f),point(.40f,.53f),point(.78f,.15f)))
   NavManeuverIcon.SHARP_LEFT->arrow(listOf(point(.65f,.85f),point(.65f,.20f),point(.18f,.66f)))
   NavManeuverIcon.SHARP_RIGHT->arrow(listOf(point(.35f,.85f),point(.35f,.20f),point(.82f,.66f)))
   NavManeuverIcon.STRAIGHT->arrow(listOf(point(.5f,.85f),point(.5f,.12f)))
   NavManeuverIcon.DEPART->{drawCircle(NavigationRouteBlue,unit*.09f,point(.5f,.85f));arrow(listOf(point(.5f,.65f),point(.5f,.12f)))}
   NavManeuverIcon.UTURN->{
    drawPath(Path().apply{moveTo(unit*.72f,unit*.85f);lineTo(unit*.72f,unit*.38f);cubicTo(unit*.72f,unit*.04f,unit*.25f,unit*.04f,unit*.25f,unit*.38f);lineTo(unit*.25f,unit*.73f)},NavigationRouteBlue,style=stroke)
    line(listOf(point(.08f,.52f),point(.25f,.73f),point(.42f,.52f)))
   }
   NavManeuverIcon.ROUNDABOUT->{
    drawCircle(NavigationRouteBlue,unit*.25f,point(.5f,.45f),style=stroke)
    line(listOf(point(.5f,.85f),point(.5f,.70f)))
    line(listOf(point(.48f,.13f),point(.67f,.25f),point(.47f,.34f)))
   }
   NavManeuverIcon.ARRIVE->{
    line(listOf(point(.25f,.85f),point(.25f,.14f)))
    val flag=Path().apply{moveTo(unit*.25f,unit*.14f);lineTo(unit*.80f,unit*.14f);lineTo(unit*.66f,unit*.36f);lineTo(unit*.80f,unit*.56f);lineTo(unit*.25f,unit*.56f);close()}
    drawPath(flag,NavigationRouteBlue)
   }
   NavManeuverIcon.UNKNOWN->{drawCircle(Muted,unit*.31f,point(.5f,.5f),style=stroke);line(listOf(point(.36f,.5f),point(.64f,.5f)))}
  }
 }
}

internal fun navigationManeuverLabel(kind:NavManeuverIcon):String=when(kind){
 NavManeuverIcon.LEFT->"좌회전"
 NavManeuverIcon.RIGHT->"우회전"
 NavManeuverIcon.SLIGHT_LEFT->"왼쪽으로 완만하게"
 NavManeuverIcon.SLIGHT_RIGHT->"오른쪽으로 완만하게"
 NavManeuverIcon.SHARP_LEFT->"왼쪽으로 급하게"
 NavManeuverIcon.SHARP_RIGHT->"오른쪽으로 급하게"
 NavManeuverIcon.STRAIGHT->"직진"
 NavManeuverIcon.UTURN->"유턴"
 NavManeuverIcon.ROUNDABOUT->"회전교차로"
 NavManeuverIcon.ARRIVE->"도착"
 NavManeuverIcon.DEPART->"출발"
 NavManeuverIcon.UNKNOWN->"방향 정보 없음 · 텍스트 안내 확인"
}
