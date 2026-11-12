package com.driveatlas

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

/** Must be called only after provider consent. No location component or offline region. */
@Suppress("DEPRECATION")
@Composable internal fun rememberVectorMapView(retry: Int): MapView {
 val context = LocalContext.current
 val owner = LocalLifecycleOwner.current
 val view = remember(context, retry) {
  MapLibre.getInstance(context.applicationContext)
  MapView(context, MapLibreMapOptions.createFromAttributes(context)
   .textureMode(true).compassEnabled(false).logoEnabled(false).attributionEnabled(false)
   .setPrefetchesTiles(false).setPrefetchZoomDelta(0)
   .localIdeographFontFamily("sans-serif")).apply { onCreate(null) }
 }
 DisposableEffect(view, owner) {
  var started = false; var resumed = false
  fun sync() {
   val state = owner.lifecycle.currentState
   if (state.isAtLeast(Lifecycle.State.STARTED) && !started) { view.onStart(); started = true }
   if (state.isAtLeast(Lifecycle.State.RESUMED) && !resumed) { view.onResume(); resumed = true }
   if (!state.isAtLeast(Lifecycle.State.RESUMED) && resumed) { view.onPause(); resumed = false }
   if (!state.isAtLeast(Lifecycle.State.STARTED) && started) { view.onStop(); started = false }
  }
  val observer = LifecycleEventObserver { _, _ -> sync() }
  owner.lifecycle.addObserver(observer); sync()
  onDispose {
   owner.lifecycle.removeObserver(observer)
   if (resumed) view.onPause()
   if (started) view.onStop()
   view.onDestroy()
  }
 }
 return view
}
