package com.jstr14.picaday.ui.theme.logo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.ui.theme.util.Season
import com.jstr14.picaday.ui.theme.util.SeasonManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PicADayLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 120.dp,
) {
    val specialDay = SeasonManager.getSpecialDay()
    val season = SeasonManager.getCurrentSeason()

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(modifier = modifier.size(logoSize * 0.85f, logoSize)) {
        val w = size.width

        drawPolaroidFrame(surface, outline)

        val photoInset = w * 0.1f
        val photoAreaSize = w * 0.8f
        val iconCx = w / 2f
        val iconCy = photoInset + photoAreaSize / 2f
        val iconR = photoAreaSize * 0.27f

        when {
            specialDay == SeasonManager.SpecialDay.HALLOWEEN ->
                drawPumpkin(iconCx, iconCy, iconR, primary, secondary)
            specialDay == SeasonManager.SpecialDay.CHRISTMAS ->
                drawStar(iconCx, iconCy, iconR, primary, secondary)
            specialDay == SeasonManager.SpecialDay.VALENTINE ->
                drawHeart(iconCx, iconCy, iconR, primary)
            specialDay == SeasonManager.SpecialDay.NEW_YEAR ->
                drawSparkle(iconCx, iconCy, iconR, primary, secondary)
            else -> when (season) {
                Season.SPRING -> drawFlower(iconCx, iconCy, iconR, primary, secondary)
                Season.SUMMER -> drawSun(iconCx, iconCy, iconR, primary, secondary)
                Season.AUTUMN -> drawLeaf(iconCx, iconCy, iconR, primary, secondary)
                Season.WINTER -> drawSnowflake(iconCx, iconCy, iconR, primary)
            }
        }
    }
}

private fun DrawScope.drawPolaroidFrame(frameColor: Color, outlineColor: Color) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.08f)

    // Drop shadow
    drawRoundRect(
        color = outlineColor.copy(alpha = 0.15f),
        topLeft = Offset(w * 0.03f, h * 0.03f),
        size = size,
        cornerRadius = corner
    )
    // Frame body
    drawRoundRect(color = frameColor, cornerRadius = corner)
    // Frame border
    drawRoundRect(
        color = outlineColor.copy(alpha = 0.2f),
        cornerRadius = corner,
        style = Stroke(width = w * 0.015f)
    )
    // Photo area
    val inset = w * 0.1f
    val photoSize = w * 0.8f
    drawRoundRect(
        color = outlineColor.copy(alpha = 0.1f),
        topLeft = Offset(inset, inset),
        size = Size(photoSize, photoSize),
        cornerRadius = CornerRadius(w * 0.03f)
    )
}

// Spring: 5-petal flower
private fun DrawScope.drawFlower(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val petalR = r * 0.5f
    val dist = r * 0.55f
    for (i in 0 until 5) {
        val a = (2.0 * PI / 5.0 * i - PI / 2.0).toFloat()
        drawCircle(
            color = primary.copy(alpha = 0.85f),
            radius = petalR,
            center = Offset(cx + cos(a) * dist, cy + sin(a) * dist)
        )
    }
    drawCircle(color = secondary, radius = r * 0.3f, center = Offset(cx, cy))
}

// Summer: sun with 8 rays
private fun DrawScope.drawSun(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val innerR = r * 0.48f
    val sw = r * 0.13f
    for (i in 0 until 8) {
        val a = (2.0 * PI / 8.0 * i).toFloat()
        drawLine(
            color = primary,
            start = Offset(cx + cos(a) * (innerR + sw * 2f), cy + sin(a) * (innerR + sw * 2f)),
            end = Offset(cx + cos(a) * r, cy + sin(a) * r),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
    drawCircle(color = primary, radius = innerR, center = Offset(cx, cy))
    drawCircle(color = secondary, radius = innerR * 0.5f, center = Offset(cx, cy))
}

// Autumn: leaf with center vein
private fun DrawScope.drawLeaf(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        cubicTo(cx + r * 0.85f, cy - r * 0.5f, cx + r * 0.85f, cy + r * 0.5f, cx, cy + r)
        cubicTo(cx - r * 0.85f, cy + r * 0.5f, cx - r * 0.85f, cy - r * 0.5f, cx, cy - r)
        close()
    }
    drawPath(path, color = primary)
    drawLine(
        color = secondary,
        start = Offset(cx, cy - r * 0.8f),
        end = Offset(cx, cy + r * 0.8f),
        strokeWidth = r * 0.1f,
        cap = StrokeCap.Round
    )
}

// Winter: snowflake with 6 arms and branches
private fun DrawScope.drawSnowflake(cx: Float, cy: Float, r: Float, primary: Color) {
    val sw = r * 0.12f
    val branchLen = r * 0.3f
    val branchDist = r * 0.55f
    for (i in 0 until 6) {
        val a = (PI / 3.0 * i).toFloat()
        drawLine(
            color = primary,
            start = Offset(cx, cy),
            end = Offset(cx + cos(a) * r, cy + sin(a) * r),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        val bx = cx + cos(a) * branchDist
        val by = cy + sin(a) * branchDist
        for (sign in listOf(1.0, -1.0)) {
            val ba = (a + sign * PI / 3.0).toFloat()
            drawLine(
                color = primary,
                start = Offset(bx, by),
                end = Offset(bx + cos(ba) * branchLen, by + sin(ba) * branchLen),
                strokeWidth = sw * 0.75f,
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(color = primary, radius = sw * 1.5f, center = Offset(cx, cy))
}

// Halloween: pumpkin with three segments, triangle eyes and smile
private fun DrawScope.drawPumpkin(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val ovalW = r * 0.55f
    val ovalH = r * 0.8f
    listOf(-r * 0.5f, 0f, r * 0.5f).forEachIndexed { i, ox ->
        drawOval(
            color = if (i == 1) primary else primary.copy(alpha = 0.88f),
            topLeft = Offset(cx + ox - ovalW, cy - ovalH),
            size = Size(ovalW * 2f, ovalH * 2f)
        )
    }
    // Stem
    drawLine(
        color = secondary,
        start = Offset(cx, cy - ovalH),
        end = Offset(cx + r * 0.12f, cy - ovalH - r * 0.3f),
        strokeWidth = r * 0.1f,
        cap = StrokeCap.Round
    )
    // Triangle eyes
    val eyePath = Path().apply {
        val ey = cy - r * 0.05f
        val es = r * 0.2f
        listOf(cx - r * 0.35f, cx + r * 0.15f).forEach { ex ->
            moveTo(ex, ey)
            lineTo(ex + es / 2f, ey - es)
            lineTo(ex + es, ey)
            close()
        }
    }
    drawPath(eyePath, color = secondary)
    // Smile
    val smilePath = Path().apply {
        moveTo(cx - r * 0.3f, cy + r * 0.35f)
        cubicTo(
            cx - r * 0.1f, cy + r * 0.52f,
            cx + r * 0.1f, cy + r * 0.52f,
            cx + r * 0.3f, cy + r * 0.35f
        )
    }
    drawPath(smilePath, color = secondary, style = Stroke(width = r * 0.1f, cap = StrokeCap.Round))
}

// Christmas: 5-pointed star with accent dots
private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val path = Path()
    val innerR = r * 0.42f
    for (i in 0 until 10) {
        val a = (PI / 5.0 * i - PI / 2.0).toFloat()
        val rad = if (i % 2 == 0) r else innerR
        val x = cx + cos(a) * rad
        val y = cy + sin(a) * rad
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = primary)
    for (i in 0 until 5) {
        val a = (2.0 * PI / 5.0 * i - PI / 2.0).toFloat()
        drawCircle(
            color = secondary.copy(alpha = 0.65f),
            radius = r * 0.07f,
            center = Offset(cx + cos(a) * r * 1.35f, cy + sin(a) * r * 1.35f)
        )
    }
}

// Valentine: heart (two circles + downward triangle)
private fun DrawScope.drawHeart(cx: Float, cy: Float, r: Float, primary: Color) {
    val lobeR = r * 0.5f
    val topY = cy - r * 0.25f
    drawCircle(color = primary, radius = lobeR, center = Offset(cx - lobeR, topY))
    drawCircle(color = primary, radius = lobeR, center = Offset(cx + lobeR, topY))
    val path = Path().apply {
        moveTo(cx, cy + r * 0.75f)
        lineTo(cx - r, topY)
        lineTo(cx + r, topY)
        close()
    }
    drawPath(path, color = primary)
}

// New Year: 4-pointed sparkle with accent dots
private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, primary: Color, secondary: Color) {
    val path = Path()
    val innerR = r * 0.18f
    for (i in 0 until 8) {
        val a = (PI / 4.0 * i - PI / 2.0).toFloat()
        val rad = if (i % 2 == 0) r else innerR
        val x = cx + cos(a) * rad
        val y = cy + sin(a) * rad
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = primary)
    listOf(
        Offset(cx + r * 1.25f, cy - r * 0.65f),
        Offset(cx - r * 1.2f, cy - r * 0.55f),
        Offset(cx + r * 0.65f, cy + r * 1.15f),
        Offset(cx - r * 0.6f, cy + r * 1.1f)
    ).forEach { drawCircle(color = secondary.copy(alpha = 0.75f), radius = r * 0.09f, center = it) }
}
