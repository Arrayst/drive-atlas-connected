package com.driveatlas

/** Menu is 72dp wide with a 16dp margin; leave a gap and an end margin for the card. */
internal fun navigationInstructionWidth(windowWidth:Float):Float =
 (windowWidth-120f).coerceIn(1f,400f)

/** P/UX authority is supplied separately; a SIM replay is an additional restriction. */
internal fun canBrowseNavigation(canEdit:Boolean,foreground:Boolean,simRunning:Boolean):Boolean =
 canOpenParkedDrawer(canEdit,foreground)&&!simRunning
