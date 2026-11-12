package com.driveatlas

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.remember
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Exercises the production host's Compose identity boundary on the Android runtime, without a
 * MapView or network. Compose tracing requires Android; it is not mocked in local JVM tests.
 * A remembered factory is the relevant AndroidView contract: replacing a captured view alone does
 * not rerun that factory. Removing key(view) from WithVectorMapViewInstance breaks the retry test.
 * Native attachment/rendering is covered separately by VectorMapViewHostInstrumentedTest.
 */
@RunWith(AndroidJUnit4::class)
class VectorMapViewInstanceTest {
 private class EmptyApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit
  override fun insertBottomUp(index: Int, instance: Unit) = Unit
  override fun remove(index: Int, count: Int) = Unit
  override fun move(from: Int, to: Int, count: Int) = Unit
  override fun onClear() = Unit
 }
 private class HostHarness {
  private val recomposer = Recomposer(EmptyCoroutineContext)
  private val composition = Composition(EmptyApplier(), recomposer)
  val created = mutableListOf<Any>()
  val detached = mutableListOf<Any>()
  var mounted: Any? = null
   private set

  fun render(view: Any) {
   composition.setContent {
    WithVectorMapViewInstance(view) { instance ->
     val attached = remember { instance.also { created += it } }
     DisposableEffect(attached) {
      mounted = attached
      onDispose { detached += attached; if (mounted === attached) mounted = null }
     }
    }
   }
  }
  fun removeHost() { composition.setContent {} }
  fun close() { composition.dispose(); recomposer.cancel() }
 }

 @Test fun consecutiveRetriesReplaceTheMountedInstanceAndDetachEachPreviousHost() {
  val host = HostHarness()
  val first = Any(); val retryOne = Any(); val retryTwo = Any()
  try {
   host.render(first)
   assertSame(first, host.mounted)
   host.render(retryOne)
   assertSame(retryOne, host.mounted)
   assertEquals(listOf(first), host.detached)
   host.render(retryTwo)
   assertSame(retryTwo, host.mounted)
   assertEquals(listOf(first, retryOne), host.detached)
   assertEquals(listOf(first, retryOne, retryTwo), host.created)
  } finally { host.close() }
  assertEquals(listOf(first, retryOne, retryTwo), host.detached)
 }

 @Test fun ordinaryRecompositionKeepsTheSameNativeHostAndDoesNotDetachIt() {
  val host = HostHarness(); val view = Any()
  try {
   repeat(3) { host.render(view) }
   assertSame(view, host.mounted)
   assertEquals(listOf(view), host.created)
   assertTrue(host.detached.isEmpty())
  } finally { host.close() }
  assertEquals(listOf(view), host.detached)
 }

 @Test fun leavingAndReturningDoesNotReviveTheRemovedHost() {
  val host = HostHarness(); val first = Any(); val returned = Any()
  try {
   host.render(first)
   host.removeHost()
   assertNull(host.mounted)
   assertEquals(listOf(first), host.detached)
   host.render(returned)
   assertSame(returned, host.mounted)
   assertEquals(listOf(first, returned), host.created)
  } finally { host.close() }
  assertEquals(listOf(first, returned), host.detached)
 }
}
