package com.driveatlas

import kotlin.math.abs

internal data class ProjectedMapPlace(val place: MapPlace, val position: MapPinPosition)
internal data class MapPlaceCluster(val members: List<ProjectedMapPlace>) {
 // Deliberately a real member coordinate, never a displaced label or invented centroid.
 val anchor: MapPinPosition get() = members.first().position
 val id: String get() = members.joinToString("|") { it.place.id }
}
/** Groups intersecting 64dp touch targets without moving any geographic anchor. */
internal fun clusterMapPlaceTargets(places: List<ProjectedMapPlace>, spacing: Float): List<MapPlaceCluster> {
 require(spacing.isFinite() && spacing > 0)
 val remaining = places.filter { it.position.x.isFinite() && it.position.y.isFinite() }.toMutableList()
 val groups = mutableListOf<MapPlaceCluster>()
 while (remaining.isNotEmpty()) {
  val group = mutableListOf(remaining.removeAt(0)); var index = 0
  while (index < group.size) {
   val anchor = group[index++].position
   val close = remaining.filter { abs(it.position.x - anchor.x) < spacing && abs(it.position.y - anchor.y) < spacing }
   group.addAll(close); remaining.removeAll(close.toSet())
  }
  groups += MapPlaceCluster(group)
 }
 return groups
}
internal fun mapPlaceTargetFits(position: MapPinPosition, width: Float, height: Float, half: Float, rightInset: Float): Boolean =
 listOf(position.x, position.y, width, height, half, rightInset).all { it.isFinite() } &&
 half >= 0 && rightInset >= 0 && position.x >= half && position.y >= half &&
 position.x <= width - rightInset - half && position.y <= height - half

internal fun mapPlaceMarkersEnabled(widthDp: Float, heightDp: Float, fontScale: Float) =
 widthDp >= 192f && heightDp >= 112f && fontScale.isFinite() && fontScale in 0.5f..1.5f

/** At normal text size: 520 - 3×64 chrome = approximately 328dp of unobscured map. */
internal fun publicMapCardHeightDp(fontScale: Float): Float =
 520f + ((if (fontScale.isFinite()) fontScale else 1f) - 1f).coerceIn(0f, 1f) * 160f
