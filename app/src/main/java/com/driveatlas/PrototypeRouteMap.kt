package com.driveatlas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.*

internal val NavigationRouteBlue=Color(0xFF1769F5)

/** Both preview and replay use the same genuine vector renderer. */
@Composable internal fun PrototypeRouteMap(route:NavPrototypeRoute,position:Point,modifier:Modifier=Modifier,replayDistance:Double?=null,showStatusLabel:Boolean=true,controlsEnabled:Boolean=true) {
 CourseVectorNavigationMap(Catalog.routes.first{it.id==route.request.routeId},route,position,modifier,replayDistance,showStatusLabel,controlsEnabled)
}
@Composable internal fun BaseCourseNavigationMap(route:Route,modifier:Modifier=Modifier) {
 CourseVectorNavigationMap(route,null,null,modifier)
}

// Projection helpers retained for the independent map explorer and tested geometry contracts.
internal fun prototypeViewport(points:List<Point>,width:Int,height:Int):MapViewport {
 require(points.isNotEmpty()&&width>0&&height>0)
 var z=0
 val usableWidth=(width-96).coerceAtLeast(width/2)
 val usableHeight=(height-160).coerceAtLeast(height/2)
 for(candidate in 0..16){val p=points.map{worldPoint(it,candidate)};if(p.maxOf{it.first}-p.minOf{it.first}<=usableWidth&&p.maxOf{it.second}-p.minOf{it.second}<=usableHeight)z=candidate else break}
 val p=points.map{worldPoint(it,z)}
 return MapViewport(z,(p.minOf{it.first}+p.maxOf{it.first})/2-width/2.0,(p.minOf{it.second}+p.maxOf{it.second})/2-height/2.0,width,height)
}

internal fun prototypeFollowViewport(position:Point,width:Int,height:Int):MapViewport {
 require(width>0&&height>0)
 val p=worldPoint(position,16)
 return prototypePanViewport(MapViewport(16,p.first-width/2.0,p.second-height/2.0,width,height),0.0,0.0)
}

internal fun prototypePanViewport(view:MapViewport,dx:Double,dy:Double):MapViewport {
 val world=256.0*(1 shl view.zoom)
 return view.copy(left=(view.left-dx).coerceIn(0.0,(world-view.width).coerceAtLeast(0.0)),top=(view.top-dy).coerceIn(0.0,(world-view.height).coerceAtLeast(0.0)))
}

internal data class PrototypeTileKey(val zoom:Int,val x:Int,val y:Int)
internal fun prototypeVisibleTileKeys(view:MapViewport):List<PrototypeTileKey> {
 val last=(1 shl view.zoom)-1
 val xs=floor(view.left/256).toInt().coerceAtLeast(0)..floor((view.left+view.width-1)/256).toInt().coerceAtMost(last)
 val ys=floor(view.top/256).toInt().coerceAtLeast(0)..floor((view.top+view.height-1)/256).toInt().coerceAtMost(last)
 return ys.flatMap{y->xs.map{x->PrototypeTileKey(view.zoom,x,y)}}
}

/** Ordered replay cursor resolves overlapping outbound/return roads; never a device heading. */
internal fun prototypeGeometryHeading(points:List<Point>,cumulative:List<Double>,distance:Double):Float? {
 if(points.size<2||points.size!=cumulative.size||!distance.isFinite())return null
 val d=distance.coerceIn(0.0,cumulative.last())
 var end=cumulative.binarySearch(d).let{if(it<0)-it-1 else it+1}.coerceIn(1,points.lastIndex)
 while(end<points.lastIndex&&cumulative[end]<=cumulative[end-1])end++
 while(end>1&&cumulative[end]<=cumulative[end-1])end--
 if(cumulative[end]<=cumulative[end-1])return null
 val a=worldPoint(points[end-1],14);val b=worldPoint(points[end],14)
 return Math.toDegrees(atan2(b.first-a.first,a.second-b.second)).toFloat()
}
