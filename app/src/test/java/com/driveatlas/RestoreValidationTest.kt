package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class RestoreValidationTest {
    private val route = Catalog.routes.first()
    private val previousAward = Award("previous-trip", route.id, true, 900)

    private fun restored(snapshot: Snapshot): AtlasEngine = AtlasEngine(
        AtlasEngineTest.MemoryStore().apply { state = snapshot }, Mode.SIMULATOR
    )

    @Test fun arrivalBeforeStartCannotAwardAndPreservesPreviousAwards() {
        // Decoded equivalent of start=3000, next=3, last=1900 in the reported JSON.
        val snapshot = Snapshot(Trip("x", route.id, 3000, 3, 1900), listOf(previousAward))
        val store = AtlasEngineTest.MemoryStore().apply { state = snapshot }
        val engine = AtlasEngine(store, Mode.SIMULATOR)
        assertNull(engine.snapshot.trip)
        engine.setSafety(Safety.PARKED)
        engine.setLocation(LocationStatus.AVAILABLE)
        assertFalse(engine.finish(2000))
        assertEquals(listOf(previousAward), engine.snapshot.awards)
        assertEquals(snapshot, store.state) // Validation itself does not rewrite stored records.
    }

    @Test fun inconsistentProgressAndTimestampsFailClosed() {
        val trip = Trip("current", route.id, 1000)
        val invalid = listOf(
            trip.copy(nextStop = 0, lastEvidenceMs = 1100),
            trip.copy(nextStop = 1, lastEvidenceMs = 0),
            trip.copy(nextStop = 3, lastEvidenceMs = 0),
            trip.copy(nextStop = 1, lastEvidenceMs = 999),
            trip.copy(nextStop = -1),
            trip.copy(nextStop = route.stops.size + 1, lastEvidenceMs = 1100),
            trip.copy(startedMs = -1),
            trip.copy(lastEvidenceMs = -1)
        )
        invalid.forEach { candidate ->
            val engine = restored(Snapshot(candidate, listOf(previousAward)))
            assertNull("Invalid trip survived: $candidate", engine.snapshot.trip)
            engine.setSafety(Safety.PARKED)
            engine.setLocation(LocationStatus.AVAILABLE)
            assertFalse(engine.finish(1200))
            assertEquals(listOf(previousAward), engine.snapshot.awards)
        }
    }

    @Test fun blankTripIdsAndUnknownRoutesAreRejected() {
        val trip = Trip("current", route.id, 1000, 3, 1300)
        listOf(trip.copy(id = ""), trip.copy(id = " \t"), trip.copy(routeId = "missing")).forEach {
            assertNull(restored(Snapshot(it)).snapshot.trip)
        }
    }

    @Test fun validProgressAndAwardRecordsRemainUnchangedAndLocked() {
        for (mode in Mode.entries) {
            for (progress in 0..route.stops.size) {
                val trip = Trip("current", route.id, 1000, progress,
                    if (progress == 0) 0 else 1100 + progress.toLong(), mode)
                val snapshot = Snapshot(trip, listOf(previousAward, previousAward.copy(
                    tripId = "vehicle-trip", simulated = false, timeMs = 950)))
                val engine = restored(snapshot)
                assertEquals(snapshot, engine.snapshot)
                assertEquals(Safety.UNKNOWN, engine.safety)
                assertEquals(LocationStatus.LOST, engine.location)
                assertFalse(engine.canEdit)
                assertFalse(engine.finish(1400))
            }
        }
    }

    @Test fun invalidAwardRecordsDoNotDiscardValidTripOrAwards() {
        val trip = Trip("current", route.id, 1000, 1, 1100)
        val snapshot = Snapshot(trip, listOf(
            previousAward,
            previousAward.copy(tripId = ""),
            previousAward.copy(tripId = " \t"),
            previousAward.copy(tripId = "bad-route", routeId = "missing"),
            previousAward.copy(tripId = "bad-time", timeMs = -1)
        ))
        assertEquals(Snapshot(trip, listOf(previousAward)), restored(snapshot).snapshot)
    }

    @Test fun validCompletedTripCanFinishOnceAfterFreshSafetyConfirmation() {
        val engine = restored(Snapshot(Trip("current", route.id, 1000, 3, 1300), listOf(previousAward)))
        assertFalse(engine.finish(1400))
        engine.setSafety(Safety.PARKED)
        engine.setLocation(LocationStatus.AVAILABLE)
        assertTrue(engine.finish(1400))
        assertFalse(engine.finish(1400))
        assertEquals(2, engine.snapshot.awards.size)
        assertEquals(previousAward, engine.snapshot.awards.first())
        assertNull(engine.snapshot.trip)
    }

    @Test fun firstEvidenceAtStartAndUnstartedEpochTripRemainValid() {
        listOf(Trip("started", route.id, 1000, 1, 1000), Trip("epoch", route.id, 0)).forEach {
            assertEquals(it, restored(Snapshot(it)).snapshot.trip)
        }
    }

    @Test fun sequentialEvidenceRequiresEnoughDistinctTimestampsAfterRestore() {
        for (start in listOf(0L, 1000L, Long.MAX_VALUE - 2)) {
            val firstPossible = maxOf(start, 1L)
            for (last in listOf(firstPossible, firstPossible + 1)) {
                val engine = restored(Snapshot(Trip("impossible", route.id, start, 3, last)))
                assertNull(engine.snapshot.trip)
                engine.setSafety(Safety.PARKED)
                engine.setLocation(LocationStatus.AVAILABLE)
                assertFalse(engine.finish(last))
                assertTrue(engine.snapshot.awards.isEmpty())
            }
            val valid = Trip("minimum-valid", route.id, start, 3, firstPossible + 2)
            assertEquals(valid, restored(Snapshot(valid)).snapshot.trip)
        }
        assertNull(restored(Snapshot(Trip("no-room", route.id, Long.MAX_VALUE, 3, Long.MAX_VALUE))).snapshot.trip)
    }
}
