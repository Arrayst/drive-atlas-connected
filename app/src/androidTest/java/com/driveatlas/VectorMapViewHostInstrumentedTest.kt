package com.driveatlas

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real MapLibre/AndroidView regression on an empty debug Activity, not the production MainActivity.
 * Uses the same rememberVectorMapView and VectorMapViewHost as both map screens. Every style is
 * inline and has only a background layer: no URL, tile, sprite, glyph, GPS or stored user data.
 * No network or device-security setting is changed and no real provider request is needed.
 */
@RunWith(AndroidJUnit4::class)
class VectorMapViewHostInstrumentedTest {
 @get:Rule val compose = createAndroidComposeRule<VectorMapHostTestActivity>()

 private class Probe(val view: MapView, val color: Int) {
  val styleLoaded = AtomicBoolean(false)
  val frameRendered = AtomicBoolean(false)
  val snapshotRequested = AtomicBoolean(false)
  val snapshotReady = AtomicBoolean(false)
  val snapshotColor = AtomicInteger(0)
  val disposed = AtomicBoolean(false)
  val disposalCount = AtomicInteger(0)
 }
 private class Fixture {
  val retry = mutableIntStateOf(0)
  val ordinaryRecomposition = mutableIntStateOf(0)
  val visible = mutableStateOf(true)
  val probes = CopyOnWriteArrayList<Probe>()
 }

 private fun installFixture(fixture: Fixture) {
  compose.setContent {
   // Changing this padding forces ordinary UI/layout work, without changing native-view identity.
   Box(Modifier.fillMaxSize().padding(top = fixture.ordinaryRecomposition.intValue.dp)) {
    if (fixture.visible.value) {
     val view = rememberVectorMapView(fixture.retry.intValue)
     val background = when (fixture.retry.intValue % 3) { 0 -> "#E9EDF2"; 1 -> "#DCE8F4"; else -> "#F0E6D6" }
     val probe = remember(view) { Probe(view, Color.parseColor(background)) }
     VectorMapViewHost(view, Modifier.fillMaxSize())
     DisposableEffect(view) {
      fixture.probes += probe
      var nativeMap: MapLibreMap? = null
      val rendered = MapView.OnDidFinishRenderingMapListener { fully ->
       if (fully && probe.styleLoaded.get() && !probe.disposed.get()) {
        probe.frameRendered.set(true)
        if (probe.snapshotRequested.compareAndSet(false, true)) nativeMap?.snapshot { bitmap ->
         if (!probe.disposed.get() && bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
          probe.snapshotColor.set(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
          probe.snapshotReady.set(true)
         }
        }
       }
      }
      view.addOnDidFinishRenderingMapListener(rendered)
      view.getMapAsync { map -> if (!probe.disposed.get()) {
       nativeMap = map
       val localStyle = """{"version":8,"sources":{},"layers":[{"id":"test-background","type":"background","paint":{"background-color":"$background"}}]}"""
       map.setStyle(Style.Builder().fromJson(localStyle)) { if (!probe.disposed.get()) probe.styleLoaded.set(true) }
      } }
      onDispose {
       probe.disposed.set(true)
       probe.disposalCount.incrementAndGet()
       view.removeOnDidFinishRenderingMapListener(rendered)
       // Production rememberVectorMapView owns onDestroy. Never destroy a second time here.
      }
     }
    }
   }
  }
 }

 private fun awaitRendered(fixture: Fixture, index: Int): Probe {
  compose.waitUntil(timeoutMillis = 15_000) {
   fixture.probes.getOrNull(index)?.let { it.styleLoaded.get() && it.frameRendered.get() && it.snapshotReady.get() } == true
  }
  val probe = fixture.probes[index]
  compose.runOnIdle {
   assertFalse("Current native map was destroyed", probe.view.isDestroyed)
   assertTrue("Current native map is not attached", probe.view.isAttachedToWindow)
   assertNotNull("Current native map has no parent", probe.view.parent)
   assertTrue("Current native map has no usable size", probe.view.width > 0 && probe.view.height > 0)
   assertEquals("Snapshot did not render the current local style", probe.color, probe.snapshotColor.get())
  }
  return probe
 }

 private fun assertReleased(probe: Probe) {
  compose.runOnIdle {
   assertTrue("Old native map was not destroyed", probe.view.isDestroyed)
   assertFalse("Old native map is still attached", probe.view.isAttachedToWindow)
   assertNull("Old native map is still parented", probe.view.parent)
   assertTrue(probe.disposed.get())
   assertEquals("Old host was disposed more than once", 1, probe.disposalCount.get())
  }
 }

 @Test fun retriesAttachNewRealMapsAndDestroyAndDetachBothPredecessors() {
  val fixture = Fixture(); installFixture(fixture)
  val first = awaitRendered(fixture, 0)
  compose.runOnIdle { fixture.retry.intValue++ }
  val second = awaitRendered(fixture, 1)
  assertNotSame(first.view, second.view); assertReleased(first)
  compose.runOnIdle { fixture.retry.intValue++ }
  val third = awaitRendered(fixture, 2)
  assertNotSame(second.view, third.view); assertReleased(second)
  compose.runOnIdle { assertEquals(3, fixture.probes.size) }
 }

 @Test fun ordinaryRecompositionRetainsTheAttachedRealMap() {
  val fixture = Fixture(); installFixture(fixture)
  val first = awaitRendered(fixture, 0)
  repeat(3) {
   compose.runOnIdle { fixture.ordinaryRecomposition.intValue++ }
   compose.waitForIdle()
  }
  compose.runOnIdle {
   assertEquals(1, fixture.probes.size)
   assertSame(first.view, fixture.probes.single().view)
   assertTrue(first.view.isAttachedToWindow)
   assertFalse(first.view.isDestroyed)
   assertFalse(first.disposed.get())
  }
 }

 @Test fun removalAndReentryAttachANewRealMapWithoutRevivingTheDestroyedView() {
  val fixture = Fixture(); installFixture(fixture)
  val first = awaitRendered(fixture, 0)
  compose.runOnIdle { fixture.visible.value = false }
  compose.waitForIdle(); assertReleased(first)
  compose.runOnIdle { fixture.visible.value = true }
  val returned = awaitRendered(fixture, 1)
  assertNotSame(first.view, returned.view)
  assertReleased(first)
 }
}
