package com.driveatlas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** A deliberately illustrated route, not GPS. Progress is the number of confirmed stops, from 0 to 3. */
@Composable
fun PixelRouteMap(kind: Int, modifier: Modifier = Modifier, progress: Int = 0) {
    val map = remember(kind, progress) { pixelMapBitmap(kind, progress).asImageBitmap() }
    Image(
        bitmap = map,
        contentDescription = "가상 여행 지도. 세 개의 정류장과 자동차. 완료한 정류장 ${progress.coerceIn(0, 3)}개",
        modifier = modifier.background(androidx.compose.ui.graphics.Color(0xFFF3E4BE)),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

/** Draws the same original map as the Compose view, fitting its entire route into the bounds. */
fun drawPixelRouteMap(canvas: Canvas, width: Int, height: Int, kind: Int, progress: Int = 0) {
    if (width <= 0 || height <= 0) return
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.parseColor("#F3E4BE") })
    val bitmap = pixelMapBitmap(kind, progress)
    val scale = minOf(width / 300f, height / 180f)
    val left = (width - 300 * scale) / 2
    val top = (height - 180 * scale) / 2
    canvas.drawBitmap(bitmap, null, RectF(left, top, left + 300 * scale, top + 180 * scale), Paint().apply {
        isAntiAlias = false; isFilterBitmap = false; isDither = false
    })
    bitmap.recycle()
}

private fun pixelMapBitmap(kind: Int, progress: Int): Bitmap =
    Bitmap.createBitmap(300, 180, Bitmap.Config.ARGB_8888).also {
        AtlasMapPainter(Canvas(it), Math.floorMod(kind, 3), progress.coerceIn(0, 3)).render()
    }

private class AtlasMapPainter(private val c: Canvas, private val kind: Int, private val progress: Int) {
    private val p = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
    private val rng = Random(812 + kind)
    private val dark = "#244B43"
    private val cream = "#F8E7BA"
    private fun r(x: Int, y: Int, w: Int, h: Int, color: String) {
        p.color = Color.parseColor(color)
        c.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), p)
    }
    private fun shape(color: String, vararg points: Int) {
        p.color = Color.parseColor(color)
        val path = Path().apply {
            moveTo(points[0].toFloat(), points[1].toFloat())
            for (i in 2 until points.size step 2) lineTo(points[i].toFloat(), points[i + 1].toFloat())
            close()
        }
        c.drawPath(path, p)
    }
    private fun tree(x: Int, y: Int, big: Boolean = false) {
        val s = if (big) 2 else 1
        r(x + 2, y + 4, 10 * s, 5 * s, "#718662")
        r(x + 4 * s, y + 4 * s, 2 * s, 5 * s, "#846D4A")
        r(x + 2 * s, y - 5 * s, 6 * s, 13 * s, "#285C4B")
        r(x, y - 2 * s, 10 * s, 8 * s, "#285C4B")
        r(x + 2 * s, y - 4 * s, 5 * s, 8 * s, "#4F8059")
        r(x + s, y - s, 4 * s, 3 * s, "#75A06A")
        r(x + 6 * s, y + 3 * s, 3 * s, 2 * s, "#1D493F")
    }
    private fun pine(x: Int, y: Int) {
        r(x + 2, y + 8, 11, 3, "#728361")
        r(x + 5, y + 5, 2, 7, "#866D48")
        shape("#285849", x + 5, y - 9, x + 10, y - 1, x + 8, y - 1, x + 12, y + 6, x - 2, y + 6, x + 2, y - 1, x, y - 1)
        shape("#4A7C55", x + 5, y - 9, x + 5, y + 4, x, y + 4, x + 3, y - 1, x + 2, y - 1)
        r(x + 3, y - 2, 2, 1, "#8CA775")
    }
    private fun rock(x: Int, y: Int) {
        r(x, y + 3, 9, 4, "#839580"); r(x + 2, y, 6, 6, "#B7B99B")
        r(x + 2, y, 4, 2, "#E0D8B1"); r(x + 6, y + 3, 2, 3, "#8C9C85")
    }
    private fun water(x: Int, y: Int, w: Int, h: Int) {
        r(x, y, w, h, "#55A9AA")
        for (i in 0 until w * h / 220) {
            val xx = rng.nextInt(x, x + w - 10); val yy = rng.nextInt(y + 2, y + h - 2)
            r(xx, yy, rng.nextInt(3, 10), 1, if (i % 4 == 0) "#BCE0CB" else "#7DC1B7")
        }
    }
    private fun boat(x: Int, y: Int) {
        r(x + 1, y + 13, 16, 2, "#34858D")
        shape("#D07E59", x - 2, y + 9, x + 16, y + 9, x + 12, y + 13, x + 2, y + 13)
        r(x + 6, y - 9, 1, 18, "#4F7568")
        shape("#FFF0CC", x + 8, y - 9, x + 8, y + 6, x + 18, y + 6)
        shape("#D4DFC2", x + 4, y - 3, x + 4, y + 6, x - 2, y + 6)
    }
    private fun house(x: Int, y: Int, cafe: Boolean = false) {
        r(x + 3, y + 4, 33, 25, "#8B976B")
        r(x, y, 30, 25, "#E6C798")
        r(x + 24, y + 4, 6, 21, "#C39A6D")
        for (yy in y + 5..y + 22 step 4) r(x + 1, yy, 23, 1, "#D4B183")
        r(x - 3, y - 4, 36, 13, "#9C5E4D")
        r(x - 1, y - 7, 32, 12, if (cafe) "#DA8260" else "#7B7554")
        for (yy in y - 5..y + 3 step 3) r(x, yy, 30, 1, if (cafe) "#F2AD7C" else "#ABA575")
        r(x + 4, y + 11, 7, 7, dark); r(x + 5, y + 12, 5, 5, "#E9BA73")
        r(x + 18, y + 12, 7, 13, "#5B6850"); r(x + 19, y + 13, 5, 5, "#F9D28D")
        r(x - 1, y + 25, 33, 2, "#F7DCAB")
        if (cafe) {
            r(x - 1, y + 7, 32, 4, cream)
            for (xx in x + 1..x + 30 step 7) r(xx, y + 7, 3, 4, "#D77B58")
            r(x + 39, y + 17, 9, 5, "#EDD19A"); r(x + 42, y + 14, 3, 11, "#EDD19A")
            r(x + 36, y + 17, 2, 5, "#806C4D"); r(x + 50, y + 17, 2, 5, "#806C4D")
        } else {
            r(x + 23, y - 11, 5, 9, "#8B7355"); r(x + 22, y - 12, 7, 2, "#E8CAA1")
        }
    }
    private fun lighthouse(x: Int, y: Int) {
        r(x - 12, y + 20, 28, 8, "#AEB18A")
        r(x - 10, y + 19, 25, 5, cream)
        shape("#F6E6BD", x - 6, y - 12, x + 5, y - 12, x + 8, y + 20, x - 9, y + 20)
        r(x + 2, y - 10, 4, 30, "#D5C8A1")
        r(x - 7, y + 2, 14, 5, "#D37E5A")
        r(x - 2, y + 11, 4, 9, "#40645A")
        r(x - 9, y - 15, 18, 3, dark)
        r(x - 6, y - 23, 12, 8, dark); r(x - 4, y - 21, 8, 5, "#FFD88B")
        r(x, y - 21, 1, 5, "#9B8052")
        shape("#C77757", x - 8, y - 23, x, y - 30, x + 8, y - 23)
    }
    private fun vista(x: Int, y: Int) {
        r(x - 3, y + 1, 29, 20, "#B1B58C"); r(x - 5, y - 1, 29, 17, "#EDDBAE")
        for (yy in y..y + 14 step 4) r(x - 4, yy, 27, 1, "#CBB98D")
        r(x - 5, y - 5, 29, 2, "#65745B")
        for (xx in x - 5..x + 24 step 9) r(xx, y - 5, 2, 10, "#65745B")
        r(x + 7, y + 1, 2, 9, dark); r(x + 4, y - 1, 10, 3, dark)
        r(x + 3, y - 2, 4, 3, "#89A393")
    }
    private fun progressIndex(steps: List<Pair<Int, Int>>): Int {
        if (progress == 0) return 0
        val stops = when (kind) {
            0 -> listOf(196 to 58, 228 to 111, 165 to 158)
            1 -> listOf(82 to 146, 174 to 55, 247 to 77)
            else -> listOf(64 to 157, 251 to 146, 223 to 59)
        }
        val stop = stops[progress - 1]
        return steps.indices.minByOrNull { abs(steps[it].first - stop.first) + abs(steps[it].second - stop.second) } ?: 0
    }
    private fun route(points: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val steps = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until points.lastIndex) {
            val a = points[i]; val b = points[i + 1]
            val count = maxOf(abs(b.first - a.first), abs(b.second - a.second))
            for (j in 0 until count) {
                val t = j.toFloat() / count
                steps.add((a.first + (b.first - a.first) * t).roundToInt() to (a.second + (b.second - a.second) * t).roundToInt())
            }
        }
        steps.add(points.last())
        for ((x, y) in steps) r(x - 6, y - 6, 13, 13, "#C7B58A")
        for ((x, y) in steps) r(x - 5, y - 5, 11, 11, "#F1DFB3")
        for (i in steps.indices step 8) {
            val (x, y) = steps[i]
            r(x - 1, y - 1, 3, 3, if (i <= progressIndex(steps)) "#B67A53" else "#BBA479")
        }
        return steps
    }
    private fun marker(x: Int, y: Int, n: Int) {
        r(x - 9, y - 9, 20, 20, "#65755B")
        r(x - 10, y - 11, 20, 19, dark)
        r(x - 8, y - 9, 16, 15, if (progress >= n) "#ECA17A" else "#FBE9BA")
        shape(dark, x - 4, y + 8, x + 4, y + 8, x, y + 12)
        val glyph = when (n) { 1 -> arrayOf("010", "110", "010", "010", "111"); 2 -> arrayOf("110", "001", "010", "100", "111"); else -> arrayOf("110", "001", "010", "001", "110") }
        glyph.forEachIndexed { yy, row -> row.forEachIndexed { xx, ch -> if (ch == '1') r(x - 3 + xx * 2, y - 7 + yy * 2, 2, 2, dark) } }
    }
    private fun car(x: Int, y: Int, horizontal: Boolean) {
        val save = c.save()
        c.translate(x.toFloat(), y.toFloat())
        if (horizontal) c.rotate(90f)
        r(-7, -9, 15, 22, "#708367")
        r(-7, -7, 3, 5, dark); r(4, -7, 3, 5, dark); r(-7, 4, 3, 5, dark); r(4, 4, 3, 5, dark)
        r(-5, -11, 10, 23, "#BA604E"); r(-4, -12, 8, 23, "#EC9975")
        r(-3, -8, 6, 4, "#395E57"); r(-3, 4, 6, 3, "#395E57")
        r(-3, -3, 6, 6, "#F7BE89"); r(-2, -2, 4, 4, "#D4A573")
        r(-4, -11, 2, 2, "#FFE4A9"); r(2, -11, 2, 2, "#FFE4A9")
        r(-3, 9, 2, 2, "#B34E43"); r(2, 9, 2, 2, "#B34E43")
        c.restoreToCount(save)
    }
    fun render() {
        r(0, 0, 300, 180, "#91AA7A")
        for (i in 0..230) {
            val x = rng.nextInt(300); val y = rng.nextInt(180)
            r(x, y, if (i % 3 == 0) 3 else 1, 1, if (i % 4 == 0) "#C7CC94" else "#A6B989")
        }
        val points = when (kind) { 0 -> coast(); 1 -> forest(); else -> lake() }
        val track = route(points)
        when (kind) {
            0 -> {
                lighthouse(176, 43); house(223, 80, true); vista(155, 133)
                marker(204, 37, 1); marker(257, 72, 2); marker(179, 145, 3)
                tree(258, 127, true); tree(283, 107); tree(280, 153, true)
            }
            1 -> {
                house(47, 125); house(107, 46, true); vista(224, 49)
                marker(82, 131, 1); marker(152, 36, 2); marker(265, 42, 3)
                pine(20, 158); pine(121, 161); pine(276, 151)
            }
            else -> {
                house(39, 125, true); vista(239, 122); lighthouse(208, 37)
                marker(73, 141, 1); marker(270, 132, 2); marker(237, 30, 3)
                tree(12, 139, true); tree(275, 69, true); tree(264, 160)
            }
        }
        val index = progressIndex(track)
        val at = track[index]
        val before = track[(index - 4).coerceAtLeast(0)]; val after = track[(index + 4).coerceAtMost(track.lastIndex)]
        car(at.first, at.second, abs(after.first - before.first) > abs(after.second - before.second))
        // Cartographic north mark, kept small and independent of route labels.
        r(17, 15, 1, 13, "#3F6754")
        shape("#3F6754", 17, 10, 12, 19, 17, 17, 22, 19)
        r(13, 31, 1, 5, dark); r(18, 31, 1, 5, dark); r(14, 32, 1, 1, dark); r(15, 33, 1, 1, dark); r(16, 34, 2, 1, dark)
    }
    private fun coast(): List<Pair<Int, Int>> {
        water(0, 0, 146, 180)
        shape("#77BDB4", 120, 0, 155, 0, 155, 30, 170, 30, 170, 65, 149, 65, 149, 96, 136, 96, 136, 126, 151, 126, 151, 180, 110, 180, 110, 147, 120, 147, 120, 102, 131, 102, 131, 67, 148, 67, 148, 36, 120, 36)
        shape("#EFE0B1", 148, 0, 300, 0, 300, 180, 140, 180, 140, 149, 125, 149, 125, 123, 139, 123, 139, 92, 153, 92, 153, 61, 165, 61, 165, 32, 148, 32)
        shape("#A7B784", 159, 0, 300, 0, 300, 180, 152, 180, 152, 146, 137, 146, 137, 127, 151, 127, 151, 95, 165, 95, 165, 64, 177, 64, 177, 28, 159, 28)
        // Offshore islands have stepped sand rims and shaded tree canopies.
        r(40, 54, 27, 18, "#A3CEB3"); r(44, 50, 19, 26, "#E3D9A7"); r(41, 55, 25, 16, "#E3D9A7")
        r(47, 53, 14, 18, "#9BAE77"); tree(48, 59); rock(54, 66)
        r(90, 130, 20, 11, "#DCCFA1"); r(95, 126, 11, 17, "#DCCFA1"); tree(96, 132)
        boat(62, 108)
        for (x in 221..290 step 22) { tree(x, 19); tree(x + 4, 45) }
        rock(144, 104); rock(155, 163); rock(282, 67)
        r(113, 52, 6, 1, "#D9E5C8"); r(115, 50, 2, 5, "#D9E5C8")
        return listOf(218 to 12, 212 to 12, 212 to 46, 196 to 46, 196 to 58, 211 to 58, 211 to 76, 200 to 76, 200 to 111, 228 to 111, 228 to 124, 199 to 124, 199 to 158, 165 to 158)
    }
    private fun forest(): List<Pair<Int, Int>> {
        // A narrow creek cuts through the upper-left forest.
        shape("#C0C59B", 0, 22, 43, 22, 43, 45, 67, 45, 67, 68, 90, 68, 90, 95, 117, 95, 117, 119, 140, 119, 140, 180, 119, 180, 119, 130, 96, 130, 96, 106, 70, 106, 70, 80, 47, 80, 47, 57, 23, 57, 23, 36, 0, 36)
        shape("#65A5A0", 0, 26, 38, 26, 38, 50, 62, 50, 62, 74, 84, 74, 84, 101, 111, 101, 111, 124, 134, 124, 134, 180, 125, 180, 125, 130, 103, 130, 103, 107, 76, 107, 76, 80, 54, 80, 54, 56, 30, 56, 30, 32, 0, 32)
        for (y in 17..168 step 22) for (x in 8..287 step 20) {
            val xx = x + rng.nextInt(-3, 4); val yy = y + rng.nextInt(-3, 4)
            val clear = (xx in 36..90 && yy > 112) || (xx in 108..163 && yy < 58) || (xx in 211..278 && yy in 23..84) || (xx in 70..103 && yy > 70) || (xx in 162..207 && yy < 138) || (yy in 135..154)
            if (!clear) pine(xx, yy)
        }
        r(103, 136, 37, 17, "#7B6C4C"); r(103, 134, 37, 15, "#C8A678")
        for (x in 105..139 step 4) r(x, 134, 1, 15, "#9C825B")
        r(102, 131, 39, 2, "#5E684E"); r(102, 151, 39, 2, "#5E684E")
        rock(152, 95); rock(30, 96)
        return listOf(31 to 158, 64 to 158, 82 to 146, 161 to 146, 182 to 126, 182 to 101, 146 to 101, 146 to 77, 174 to 77, 174 to 55, 195 to 55, 195 to 90, 247 to 90, 247 to 77)
    }
    private fun lake(): List<Pair<Int, Int>> {
        // Stepped inlet and coves rather than an elliptical generic lake.
        shape("#CFCEA0", 85, 39, 112, 39, 112, 29, 170, 29, 170, 43, 196, 43, 196, 64, 220, 64, 220, 110, 204, 110, 204, 132, 164, 132, 164, 144, 116, 144, 116, 127, 83, 127, 83, 109, 66, 109, 66, 65, 85, 65)
        shape("#58A7A5", 88, 45, 117, 45, 117, 35, 165, 35, 165, 49, 190, 49, 190, 70, 214, 70, 214, 105, 198, 105, 198, 126, 159, 126, 159, 138, 120, 138, 120, 121, 89, 121, 89, 104, 72, 104, 72, 70, 88, 70)
        r(99, 59, 80, 53, "#45979E"); r(117, 47, 43, 79, "#45979E")
        for (i in 0..42) {
            val x = rng.nextInt(99, 174); val y = rng.nextInt(60, 111)
            r(x, y, rng.nextInt(3, 10), 1, if (i % 4 == 0) "#A0CFC0" else "#6EB7B2")
        }
        r(117, 70, 27, 17, "#C9D1A1"); r(122, 66, 16, 24, "#C9D1A1")
        r(121, 70, 19, 15, "#91AC78"); tree(121, 72); rock(132, 80)
        boat(176, 94)
        r(197, 109, 38, 9, "#7B7153"); r(197, 106, 38, 9, "#D9B986")
        for (x in 198..234 step 4) r(x, 107, 1, 7, "#A78D64")
        for (x in arrayOf(198, 231)) { r(x, 103, 2, 5, dark); r(x, 114, 2, 6, dark) }
        for (x in 37..169 step 19) tree(x, 15)
        for (y in 47..104 step 22) tree(22, y, y == 69)
        tree(100, 164); tree(154, 169); tree(183, 151); rock(242, 92)
        return listOf(34 to 157, 64 to 157, 97 to 157, 97 to 151, 171 to 151, 171 to 159, 230 to 159, 251 to 146, 251 to 106, 239 to 106, 239 to 83, 223 to 83, 223 to 59)
    }
}
