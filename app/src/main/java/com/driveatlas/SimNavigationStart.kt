package com.driveatlas

internal enum class NavigationMapReadiness { DISCONNECTED, LOADING, READY, FAILED }
internal fun isCurrentNavigationViewport(rendered: NavPrototypeRoute?, current: NavPrototypeRoute?): Boolean = rendered != null && rendered === current
internal data class SimStartTicket(val generation: Int, val request: NavPrototypeRequest, val withSpeech: Boolean)
/** One explicit start action, consumed once. A late map/network callback cannot restart paused SIM. */
internal class SimNavigationStartGate {
 private var pending: SimStartTicket? = null
 fun begin(ticket: SimStartTicket) { pending = ticket }
 fun cancel() { pending = null }
 fun consume(generation: Int, request: NavPrototypeRequest, renderedRequest: NavPrototypeRequest?,
  mapReady: Boolean, foreground: Boolean, canRun: Boolean, debug: Boolean, simulator: Boolean): SimStartTicket? {
  val ticket = pending ?: return null
  if (ticket.generation != generation || ticket.request != request || !foreground || !canRun || !navPrototypeAllowed(debug, simulator)) { cancel(); return null }
  if (!mapReady || renderedRequest != ticket.request) return null
  cancel()
  return ticket
 }
}

/** Partial tile loading waits; a provider error, bounded timeout or complete blank frame stops SIM. */
internal fun navigationMapReadiness(styleReady: Boolean, roadsVisible: Boolean, failed: Boolean, timedOut: Boolean, fullyRenderedBlank: Boolean): NavigationMapReadiness = when {
 failed || (styleReady && fullyRenderedBlank) || (timedOut && !roadsVisible) -> NavigationMapReadiness.FAILED
 styleReady && roadsVisible -> NavigationMapReadiness.READY
 else -> NavigationMapReadiness.LOADING
}
