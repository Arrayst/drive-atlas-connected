package com.driveatlas

/** Actual measured overlays reserve their full height. Null means use the place list. */
internal fun mapPinVerticalRange(height:Float,topOverlay:Float,bottomOverlay:Float,pinRadius:Float,gap:Float):Pair<Float,Float>? {
 if(listOf(height,topOverlay,bottomOverlay,pinRadius,gap).any{!it.isFinite()||it<0f})return null
 val top=topOverlay+pinRadius+gap
 val bottom=height-bottomOverlay-pinRadius-gap
 return if(top<=bottom)top to bottom else null
}
