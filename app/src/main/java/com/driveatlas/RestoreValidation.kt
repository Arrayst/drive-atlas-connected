package com.driveatlas

/** Reject inconsistent restored records without discarding unrelated valid journeys or awards. */
internal fun Snapshot.validatedForRestore(): Snapshot {
    val restoredTrip = trip?.takeIf { candidate ->
        val route = Catalog.routes.firstOrNull { it.id == candidate.routeId }
        val firstPossibleEvidence = maxOf(candidate.startedMs, 1L)
        candidate.id.isNotBlank() && route != null && candidate.startedMs >= 0 &&
            candidate.nextStop in 0..route.stops.size &&
            (if (candidate.nextStop == 0) candidate.lastEvidenceMs == 0L
            else candidate.lastEvidenceMs >= firstPossibleEvidence &&
                candidate.lastEvidenceMs - firstPossibleEvidence >= candidate.nextStop - 1L)
    }
    val restoredAwards = awards.filter { award ->
        award.tripId.isNotBlank() && award.timeMs >= 0 &&
            Catalog.routes.any { it.id == award.routeId }
    }
    return copy(trip = restoredTrip, awards = restoredAwards)
}
