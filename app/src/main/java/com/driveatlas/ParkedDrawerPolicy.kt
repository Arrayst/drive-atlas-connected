package com.driveatlas

/** canEdit must come from verified vehicle P + unrestricted UX, never replay speed or SIM mode. */
internal fun canOpenParkedDrawer(canEdit:Boolean,foreground:Boolean=true):Boolean = canEdit&&foreground

/** An open drawer is dismissed before navigation; restriction changes must close it independently. */
internal fun shouldDismissDrawerOnBack(drawerOpen:Boolean):Boolean = drawerOpen
