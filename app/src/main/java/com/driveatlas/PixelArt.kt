package com.driveatlas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.random.Random

/** Original, deterministic pixel illustrations. The same master is used on cards and exports. */
@Composable
fun PixelLandscape(kind: Int, modifier: Modifier = Modifier) {
    val image = remember(kind) { landscapeBitmap(kind).asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = when (Math.floorMod(kind, 3)) {
            0 -> "등대와 해안 도로를 달리는 작은 자동차"
            1 -> "숲속 오두막으로 이어지는 구불구불한 길"
            else -> "산과 호수, 나무 선착장이 있는 여행 풍경"
        },
        modifier = modifier,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.None
    )
}

fun drawPixelLandscape(canvas: Canvas, width: Int, height: Int, kind: Int) {
    if (width <= 0 || height <= 0) return
    val bitmap = landscapeBitmap(kind)
    val sourceRatio = 240f / 144f
    val targetRatio = width.toFloat() / height
    val source = if (targetRatio > sourceRatio) {
        val h = (240f / targetRatio).toInt().coerceAtLeast(1)
        Rect(0, (144 - h) / 2, 240, (144 + h) / 2)
    } else {
        val w = (144f * targetRatio).toInt().coerceAtLeast(1)
        Rect((240 - w) / 2, 0, (240 + w) / 2, 144)
    }
    canvas.drawBitmap(bitmap, source, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    })
    bitmap.recycle()
}

private fun landscapeBitmap(kind: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(240, 144, Bitmap.Config.ARGB_8888)
    PixelPainter(Canvas(bitmap), Math.floorMod(kind, 3)).paint()
    return bitmap
}

private class PixelPainter(private val canvas: Canvas, private val kind: Int) {
    private val ink = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
    private val random = Random(470 + kind)
    private fun rect(x: Int, y: Int, w: Int, h: Int, color: String) {
        ink.color = Color.parseColor(color)
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), ink)
    }
    private fun poly(color: String, vararg p: Int) {
        ink.color = Color.parseColor(color)
        val path = Path().apply {
            moveTo(p[0].toFloat(), p[1].toFloat())
            for (i in 2 until p.size step 2) lineTo(p[i].toFloat(), p[i + 1].toFloat())
            close()
        }
        canvas.drawPath(path, ink)
    }
    private fun cloud(x: Int, y: Int, s: Int = 1) {
        rect(x, y + 3 * s, 23 * s, 3 * s, "#F7EBD0")
        rect(x + 5 * s, y, 9 * s, 6 * s, "#FFF5D9")
        rect(x + 13 * s, y + 2 * s, 8 * s, 4 * s, "#FFF5D9")
        rect(x + 3 * s, y + 6 * s, 17 * s, s, "#BCD2CD")
    }
    private fun sky() {
        rect(0, 0, 240, 144, "#93C7C9")
        rect(0, 27, 240, 15, "#B8D7CF")
        rect(0, 42, 240, 17, "#DBE4CA")
        rect(182, 16, 15, 15, "#FFF0B4")
        rect(180, 20, 19, 8, "#FFF0B4")
        cloud(24, 16); cloud(112, 11); cloud(206, 38)
    }
    private fun tree(x: Int, y: Int, scale: Int = 1, light: Boolean = false) {
        val dark = if (light) "#35675B" else "#193F3E"
        val mid = if (light) "#4C8064" else "#28594E"
        val edge = if (light) "#85A575" else "#497B5C"
        rect(x - scale, y - 3 * scale, 2 * scale, 8 * scale, "#745E43")
        poly(dark, x, y - 27 * scale, x + 5 * scale, y - 17 * scale,
            x + 3 * scale, y - 17 * scale, x + 8 * scale, y - 8 * scale,
            x + 5 * scale, y - 8 * scale, x + 10 * scale, y,
            x - 10 * scale, y, x - 5 * scale, y - 8 * scale,
            x - 8 * scale, y - 8 * scale, x - 3 * scale, y - 17 * scale,
            x - 5 * scale, y - 17 * scale)
        poly(mid, x, y - 26 * scale, x, y - 2 * scale, x - 8 * scale, y - 2 * scale,
            x - 4 * scale, y - 9 * scale, x - 6 * scale, y - 9 * scale,
            x - 2 * scale, y - 18 * scale, x - 3 * scale, y - 18 * scale)
        rect(x - 2 * scale, y - 15 * scale, 2 * scale, scale, edge)
        rect(x - 5 * scale, y - 6 * scale, 3 * scale, scale, edge)
    }
    private fun car(x: Int, y: Int) {
        rect(x - 2, y + 10, 28, 3, "#344D48")
        rect(x + 2, y + 8, 5, 5, "#223B3D"); rect(x + 18, y + 8, 5, 5, "#223B3D")
        rect(x + 3, y + 10, 2, 2, "#ADB7A5"); rect(x + 19, y + 10, 2, 2, "#ADB7A5")
        rect(x, y + 4, 25, 5, "#CB6553"); rect(x + 1, y + 3, 22, 4, "#EF9674")
        rect(x + 5, y - 2, 13, 7, "#E7B088")
        rect(x + 6, y - 1, 5, 5, "#3B6264"); rect(x + 12, y - 1, 5, 5, "#3B6264")
        rect(x + 7, y, 3, 1, "#A6C9BC"); rect(x + 13, y, 3, 1, "#A6C9BC")
        rect(x + 5, y - 3, 13, 1, "#F9D2A1")
        rect(x + 7, y - 6, 9, 3, "#896F4D"); rect(x + 9, y - 6, 1, 3, "#E6C99A")
        rect(x + 1, y + 5, 2, 2, "#FFE0A1"); rect(x + 23, y + 5, 2, 2, "#F7CAA0")
        rect(x + 1, y + 9, 23, 1, "#96594D"); rect(x + 12, y + 5, 2, 1, "#874B43")
    }
    private fun grass(x: Int, y: Int, color: String = "#A9B878") {
        rect(x, y, 1, 3, color); rect(x - 2, y + 1, 1, 1, color); rect(x + 2, y - 1, 1, 2, color)
    }
    fun paint() {
        sky()
        when (kind) { 0 -> coast(); 1 -> forest(); else -> lake() }
    }
    private fun coast() {
        poly("#87AAA1", 0, 54, 19, 48, 35, 48, 48, 54, 68, 55, 77, 62, 0, 65)
        rect(0, 62, 240, 82, "#398D99")
        rect(0, 65, 240, 10, "#5AA6AC")
        for (i in 0..95) {
            val y = random.nextInt(69, 143); val x = random.nextInt(240)
            rect(x, y, random.nextInt(3, 13), 1, if (i % 3 == 0) "#A9D3C2" else "#68B1AE")
        }
        poly("#206874", 240, 65, 210, 70, 199, 79, 178, 83, 163, 96, 130, 107, 118, 120, 93, 130, 78, 144, 240, 144)
        poly("#F3DAB0", 240, 54, 212, 59, 206, 68, 180, 74, 168, 87, 139, 96, 127, 109, 100, 119, 81, 144, 240, 144)
        poly("#B89A6E", 240, 61, 215, 64, 210, 73, 184, 81, 172, 93, 146, 104, 136, 117, 107, 127, 96, 144, 240, 144)
        poly("#8B9D6D", 240, 50, 213, 54, 201, 62, 178, 68, 162, 83, 135, 92, 122, 104, 97, 115, 80, 144, 240, 144)
        poly("#DCCDA8", 240, 64, 205, 73, 185, 84, 163, 93, 156, 101, 164, 109, 197, 115, 214, 126, 218, 144, 186, 144, 188, 132, 178, 125, 145, 119, 132, 108, 134, 96, 150, 85, 179, 76, 198, 66, 240, 56)
        poly("#6E7972", 240, 62, 203, 72, 182, 83, 157, 92, 149, 102, 157, 113, 192, 119, 205, 128, 207, 144, 192, 144, 194, 130, 181, 121, 148, 115, 138, 106, 140, 97, 154, 89, 180, 79, 201, 69, 240, 59)
        for (p in arrayOf(227 to 64, 204 to 71, 181 to 82, 158 to 91, 145 to 102, 161 to 116, 188 to 124, 200 to 138)) rect(p.first, p.second, 5, 1, "#F2DDA9")
        // Tiny lighthouse on the grassy headland, with a glowing lantern room.
        rect(191, 62, 18, 3, "#687C60")
        poly("#FFF1CC", 197, 31, 205, 31, 208, 62, 194, 62)
        poly("#D5CDB0", 202, 32, 205, 32, 208, 62, 202, 62)
        rect(195, 46, 11, 4, "#D77C5D"); rect(200, 53, 3, 9, "#455B53")
        rect(199, 36, 2, 4, "#60837C")
        rect(193, 30, 16, 2, "#526B62"); rect(196, 23, 10, 7, "#394F4D")
        rect(198, 24, 6, 5, "#FFD990"); rect(201, 24, 1, 5, "#836E51")
        poly("#C86E54", 194, 23, 201, 17, 208, 23)
        rect(201, 15, 1, 3, "#425953")
        for (i in 0..32) {
            val x = random.nextInt(211, 240); val y = random.nextInt(84, 143)
            grass(x, y)
        }
        tree(233, 114, 1, true); tree(220, 137, 1); tree(238, 151, 2)
        car(164, 112)
        // Foreground bluff and flowering scrub anchor the composition.
        poly("#345F4D", 0, 126, 18, 119, 40, 126, 50, 136, 67, 139, 72, 144, 0, 144)
        for (i in 0..28) {
            val x = random.nextInt(2, 55); val y = random.nextInt(132, 144)
            rect(x, y, 3, 2, if (i % 4 == 0) "#F2C58D" else "#64875B")
        }
        rect(41, 60, 2, 10, "#F1E9C9"); poly("#FFF0CC", 40, 61, 40, 68, 34, 68)
        rect(34, 71, 12, 2, "#315F68")
        rect(75, 35, 3, 1, "#557E7B"); rect(78, 36, 2, 1, "#557E7B"); rect(80, 35, 3, 1, "#557E7B")
    }
    private fun forest() {
        poly("#8EA99A", 0, 62, 33, 36, 66, 52, 94, 34, 128, 58, 160, 43, 204, 53, 240, 37, 240, 100, 0, 100)
        poly("#628E7A", 0, 79, 35, 51, 69, 74, 111, 47, 152, 75, 185, 59, 217, 64, 240, 49, 240, 144, 0, 144)
        for (x in -5..248 step 11) tree(x, 83 + random.nextInt(-7, 8), 1, true)
        rect(0, 89, 240, 55, "#719168")
        poly("#A6AC77", 78, 75, 116, 73, 134, 86, 130, 103, 173, 114, 187, 144, 49, 144, 68, 123, 101, 109, 105, 96)
        poly("#56675B", 99, 73, 109, 74, 123, 85, 119, 94, 97, 103, 94, 111, 111, 116, 142, 124, 157, 144, 113, 144, 107, 135, 76, 125, 72, 112, 78, 100, 104, 89, 107, 84)
        poly("#C6BA92", 99, 72, 109, 73, 125, 84, 123, 93, 100, 103, 96, 110, 114, 115, 146, 123, 161, 144, 118, 144, 110, 134, 80, 124, 76, 112, 81, 102, 107, 89, 109, 84)
        poly("#8F967B", 103, 75, 109, 76, 120, 85, 118, 90, 96, 100, 88, 107, 87, 115, 100, 121, 128, 129, 143, 144, 128, 144, 118, 132, 86, 121, 81, 113, 86, 104, 112, 91, 115, 84)
        for (x in -7..76 step 16) tree(x, 108 + random.nextInt(-7, 9), 1)
        for (x in 148..251 step 16) tree(x, 103 + random.nextInt(-7, 9), 1)
        // Timber cabin and stacked, shaded roof shingles.
        rect(158, 92, 39, 25, "#49634C"); rect(158, 90, 37, 23, "#B08759")
        for (y in 93..112 step 4) rect(158, y, 37, 1, "#8B6848")
        poly("#304E44", 151, 91, 169, 70, 189, 70, 204, 91)
        poly("#AA6650", 151, 89, 168, 72, 187, 72, 201, 89)
        for (y in 77..87 step 4) rect(164 - (y - 77), y, 25 + (y - 77) * 2, 1, "#CC8C65")
        rect(185, 68, 5, 13, "#6D6151"); rect(184, 67, 7, 2, "#D4B083")
        rect(185, 61, 3, 4, "#DCE1C4"); rect(188, 56, 5, 3, "#DCE1C4")
        rect(163, 96, 9, 9, "#3E5145"); rect(164, 97, 7, 7, "#FFD08A")
        rect(167, 97, 1, 7, "#856847"); rect(164, 100, 7, 1, "#856847")
        rect(180, 97, 8, 16, "#514D3B"); rect(181, 98, 6, 7, "#E6B979")
        rect(155, 113, 43, 3, "#D2B789"); rect(174, 116, 18, 2, "#8B7854")
        for (i in 0..80) {
            val x = random.nextInt(240); val y = random.nextInt(112, 144)
            if (x < 64 || x > 169) grass(x, y, if (i % 3 == 0) "#D1C68D" else "#9DB17B")
        }
        car(106, 125)
        tree(18, 144, 2); tree(43, 154, 2); tree(223, 149, 2); tree(244, 131, 2)
        rect(64, 117, 2, 12, "#716346"); rect(59, 116, 12, 5, "#D6B480"); rect(61, 118, 7, 1, "#806448")
        for (x in arrayOf(54, 63, 180, 192)) {
            rect(x, 137, 2, 4, "#E7C5A0"); rect(x - 2, 135, 6, 3, "#BC6D52")
        }
    }
    private fun lake() {
        poly("#7FA4A4", 0, 68, 28, 36, 43, 45, 72, 19, 112, 65, 142, 38, 171, 53, 207, 26, 240, 57, 240, 90, 0, 90)
        poly("#557F88", 27, 68, 72, 19, 112, 65, 127, 78, 74, 70)
        poly("#F3E9CC", 51, 42, 72, 19, 89, 40, 79, 36, 74, 41, 68, 33, 61, 43)
        poly("#AFCEBC", 73, 22, 82, 41, 90, 40, 79, 36, 74, 41)
        poly("#618D91", 157, 73, 207, 26, 240, 58, 240, 82)
        poly("#EEE4C8", 189, 44, 207, 26, 223, 41, 211, 37, 205, 43, 200, 37)
        poly("#426F66", 0, 66, 27, 58, 53, 63, 76, 73, 114, 72, 134, 66, 163, 73, 197, 59, 240, 67, 240, 90, 0, 90)
        rect(0, 81, 240, 63, "#3E91A0"); rect(0, 81, 240, 4, "#8DBFB6")
        poly("#68AAA9", 41, 86, 80, 86, 101, 113, 55, 113)
        poly("#79B6AE", 182, 85, 214, 85, 230, 104, 169, 104)
        for (i in 0..110) {
            val y = random.nextInt(86, 141); val x = random.nextInt(240)
            rect(x, y, random.nextInt(2, 15), 1, if (i % 4 == 0) "#B5D6BC" else "#5AA7AA")
        }
        poly("#BFB88B", 0, 105, 25, 111, 57, 113, 80, 123, 139, 127, 173, 138, 240, 139, 240, 144, 0, 144)
        poly("#75936B", 0, 102, 23, 108, 59, 111, 80, 120, 138, 124, 180, 144, 0, 144)
        poly("#C8BB91", 0, 129, 38, 120, 68, 126, 97, 139, 122, 144, 87, 144, 62, 134, 37, 129, 0, 139)
        poly("#949B7C", 0, 132, 37, 123, 66, 129, 95, 142, 102, 144, 92, 144, 62, 132, 36, 126, 0, 136)
        // Pier with warm plank tops, deep pilings and broken reflection.
        poly("#336D73", 143, 118, 174, 105, 202, 105, 168, 122)
        poly("#916F4B", 139, 119, 172, 101, 194, 101, 157, 122)
        poly("#D9B37C", 139, 116, 172, 98, 194, 98, 157, 119)
        for (i in 0..7) {
            val x = 145 + i * 5; val y = 113 - i * 2
            rect(x, y, 14, 1, "#A48055")
        }
        for (p in arrayOf(144 to 113, 158 to 116, 177 to 97, 190 to 98)) {
            rect(p.first, p.second, 2, 10, "#6E6048"); rect(p.first, p.second, 2, 2, "#ECCC90")
            rect(p.first + 1, p.second + 13, 4, 1, "#74B1AA")
        }
        rect(193, 88, 1, 8, "#6C7863"); poly("#EBDDB9", 194, 88, 202, 93, 194, 93)
        rect(189, 97, 16, 2, "#B16D52"); rect(192, 99, 10, 1, "#356D78")
        for (x in -6..48 step 14) tree(x, 115 + x / 6, 1)
        car(59, 122)
        tree(10, 148, 2); tree(227, 147, 2); tree(247, 126, 2)
        for (i in 0..30) {
            val x = random.nextInt(108, 210); val y = random.nextInt(135, 144)
            grass(x, y, if (i % 4 == 0) "#EBD49A" else "#A5B781")
        }
        rect(111, 125, 12, 3, "#D0C39B"); rect(114, 123, 7, 2, "#E4D5AE")
        rect(113, 128, 10, 1, "#586F58")
    }
}
