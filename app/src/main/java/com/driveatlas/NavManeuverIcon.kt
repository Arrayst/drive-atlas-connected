package com.driveatlas

internal enum class NavManeuverIcon { LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, SHARP_LEFT, SHARP_RIGHT, STRAIGHT, UTURN, ROUNDABOUT, ARRIVE, DEPART, UNKNOWN }

/** OSRM fields only. Unknown types/modifiers remain neutral, never inferred from localized text. */
internal fun navManeuverIcon(type:String,modifier:String?):NavManeuverIcon {
 when(type){
  "arrive"->return NavManeuverIcon.ARRIVE
  "depart"->return NavManeuverIcon.DEPART
  "roundabout","rotary","roundabout turn","exit roundabout","exit rotary"->return NavManeuverIcon.ROUNDABOUT
 }
 if(type !in setOf("turn","continue","new name","end of road","merge","fork","on ramp","off ramp"))return NavManeuverIcon.UNKNOWN
 return when(modifier){
  "left"->NavManeuverIcon.LEFT
  "right"->NavManeuverIcon.RIGHT
  "slight left"->NavManeuverIcon.SLIGHT_LEFT
  "slight right"->NavManeuverIcon.SLIGHT_RIGHT
  "sharp left"->NavManeuverIcon.SHARP_LEFT
  "sharp right"->NavManeuverIcon.SHARP_RIGHT
  "straight"->NavManeuverIcon.STRAIGHT
  "uturn"->NavManeuverIcon.UTURN
  else->NavManeuverIcon.UNKNOWN
 }
}
