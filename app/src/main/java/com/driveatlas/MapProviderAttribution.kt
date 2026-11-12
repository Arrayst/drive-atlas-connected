package com.driveatlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class MapAttributionLink(val label: String, val url: String)
internal val mapAttributionLinks = listOf(
 MapAttributionLink("OpenFreeMap", "https://openfreemap.org/"),
 MapAttributionLink("© OpenMapTiles", "https://www.openmaptiles.org/"),
 MapAttributionLink("© OpenStreetMap contributors", "https://www.openstreetmap.org/copyright"),
)

/** Explicit child width prevents an intrinsic-width underestimate; narrow items wrap vertically. */
internal fun mapAttributionItemWidth(available: Int, text: Int, padding: Int, minTouch: Int): Int {
 require(available > 0 && text >= 0 && padding >= 0 && minTouch >= 0)
 return minOf(available.toLong(), maxOf(minTouch.toLong(), text.toLong() + padding)).toInt()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun MapProviderAttribution(controlsEnabled: Boolean, modifier: Modifier = Modifier) {
 val context = LocalContext.current
 val density = LocalDensity.current
 val latestControls by rememberUpdatedState(controlsEnabled)
 val textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, color = Ink)
 val measurer = rememberTextMeasurer(cacheSize = 4)
 Surface(modifier, color = White.copy(alpha = .96f)) {
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val available = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
   val padding = with(density) { 26.dp.roundToPx() } // 12dp each side + rounding allowance.
   val minTouch = with(density) { 64.dp.roundToPx() }
   fun itemWidth(label: String) = with(density) {
    mapAttributionItemWidth(available, measurer.measure(AnnotatedString(label), style = textStyle, softWrap = false, maxLines = 1).size.width, padding, minTouch).toDp()
   }
   FlowRow(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
    mapAttributionLinks.forEach { link ->
     TextButton(onClick = { if (latestControls) openWeb(context, link.url) }, enabled = controlsEnabled,
      modifier = Modifier.width(itemWidth(link.label)).heightIn(min = 64.dp),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
      Text(link.label, style = textStyle, softWrap = true, maxLines = Int.MAX_VALUE)
     }
    }
    val status = "OSRM · SIM · 실시간 교통 미반영"
    Box(Modifier.width(itemWidth(status)).heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
     Text(status, style = textStyle.copy(color = Muted), softWrap = true, maxLines = Int.MAX_VALUE)
    }
   }
  }
 }
}
