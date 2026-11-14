package com.driveatlas

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun tripDuration(minutes:Int):String = if(minutes<60) "${minutes}분" else "${minutes/60}시간" + if(minutes%60==0) "" else " ${minutes%60}분"

@Composable internal fun TripHeading(title:String,subtitle:String) {
 Text(title,color=Ink,fontSize=38.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
 Text(subtitle,color=Muted,fontSize=20.sp,modifier=Modifier.padding(top=10.dp,bottom=28.dp))
}

@Composable internal fun TripPanel(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
 Surface(modifier,color=White,shape=RoundedCornerShape(12.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line)) {
  Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
 }
}

@Composable internal fun TripStop(number:String,title:String,subtitle:String) {
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
  Box(Modifier.size(44.dp).background(Sage,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) { Text(number,color=Coral,fontSize=20.sp,fontWeight=FontWeight.Bold) }
  Column {Text(title,color=Ink,fontSize=23.sp,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Muted,fontSize=17.sp,modifier=Modifier.padding(top=5.dp))}
 }
}

@Composable internal fun TripWorkspace(firstWeight:Float=.42f,first:@Composable (Modifier)->Unit,second:@Composable (Modifier)->Unit) {
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  if(maxWidth>=1000.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
   first(Modifier.weight(firstWeight));second(Modifier.weight(1f-firstWeight))
  } else Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(24.dp)) {
   first(Modifier.fillMaxWidth());second(Modifier.fillMaxWidth())
  }
 }
}

@Composable internal fun TripTimeChoices(label:String,values:List<String>,selected:String,onSelect:(String)->Unit) {
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(label,Modifier.width(100.dp),fontSize=18.sp,color=Muted)
  if("미입력" !in values)Spacer(Modifier.width(96.dp))
  values.forEach { value ->
   FilterChip(selected=value==selected,onClick={onSelect(value)},label={Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Text(value,fontSize=20.sp)}},modifier=Modifier.width(96.dp).heightIn(min=76.dp),shape=RoundedCornerShape(14.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Coral,selectedLabelColor=White))
  }
 }
}

@Composable internal fun TripSelectionMark(selected:Boolean,modifier:Modifier=Modifier) {
 Canvas(modifier.size(28.dp)) {
  val center=Offset(size.width/2,size.height/2)
  if(selected) {
   drawLine(Coral,Offset(size.width*.22f,size.height*.50f),Offset(size.width*.43f,size.height*.69f),3.dp.toPx(),StrokeCap.Round)
   drawLine(Coral,Offset(size.width*.43f,size.height*.69f),Offset(size.width*.79f,size.height*.30f),3.dp.toPx(),StrokeCap.Round)
  } else drawCircle(Muted,radius=size.minDimension*.28f,center=center,style=Stroke(2.dp.toPx()))
 }
}
