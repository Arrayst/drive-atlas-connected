package com.driveatlas

/** Fail closed: only a fresh P gear AND unrestricted platform UX permit browsing. */
internal fun vehicleSafety(connected: Boolean, gear: Int?, requiresDistractionOptimization: Boolean?, restrictions: Int?): Safety =
    when {
        !connected || gear == null || requiresDistractionOptimization == null || restrictions == null -> Safety.UNKNOWN
        gear == 4 && !requiresDistractionOptimization && restrictions == 0 -> Safety.PARKED
        else -> Safety.MOVING // Means restricted/non-parked, not a claim that speed is nonzero.
    }
