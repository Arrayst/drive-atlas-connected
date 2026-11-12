package com.driveatlas

internal const val SimReplayMetersPerSecond = 10

/** One one-second SIM tick. Audio state cannot change the replay pace. */
internal fun NavPrototypeRoute.advanceSimTick(state: NavReplayState): NavReplayState =
 advance(state, SimReplayMetersPerSecond.toDouble())
