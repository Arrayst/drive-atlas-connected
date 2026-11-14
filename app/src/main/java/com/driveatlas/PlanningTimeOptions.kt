package com.driveatlas

/** Standard walking presets include the commonly restored 30-minute condition. */
internal val FamilyWalkMinuteChoices = listOf("0", "10", "20", "30", "40")

/** Keep editor-entry values selectable; selection changes must not reorder numeric chips. */
internal fun planningTimeChoiceValues(
 presets: List<String>, initialSelection: String, currentSelection: String,
): List<String> {
 val distinct = (presets + initialSelection + currentSelection).distinct()
 val (labels, minutes) = distinct.partition { it.toIntOrNull() == null }
 return labels + minutes.sortedBy { it.toInt() }
}
