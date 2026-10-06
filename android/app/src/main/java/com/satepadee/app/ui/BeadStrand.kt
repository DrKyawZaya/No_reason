package com.satepadee.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.satepadee.app.data.Wood
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Close-up strand of beads that rolls down one place per count, like pulling a real ပုတီး
 * through the fingers. Bead number b sits at slot (count + phase − b): slots below the finger
 * line are counted, slots above are still to come. Every [perRound]-th bead is the guru bead.
 */
@Composable
fun BeadStrand(wood: Wood, count: Int, phase: () -> Float, perRound: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sp = strandSpacing(size.height, 76.dp.toPx())
        val radius = sp * 6
        val cx = size.width * 0.58f
        fun pos(slot: Float): Triple<Float, Float, Float> {
            val th = slot * sp / radius
            return Triple(cx - radius * (1 - cos(th)) * 0.6f, size.height / 2 + radius * sin(th), 1 - min(0.42f, abs(slot) * 0.09f))
        }

        val string = Path()
        var k = -7f
        while (k <= 7f) {
            val (x, y) = pos(k)
            if (k == -7f) string.moveTo(x, y) else string.lineTo(x, y)
            k += 0.25f
        }
        drawPath(string, wood.lo, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawLine(
            Palette.line, Offset(size.width * 0.06f, size.height / 2), Offset(size.width * 0.94f, size.height / 2),
            strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        val c = count + phase()
        val beads = (ceil(c - 6).toInt()..floor(c + 6).toInt()).sortedByDescending { abs(c - it) }
        for (b in beads) {
            val slot = c - b
            val (x, y, sc) = pos(slot)
            val guru = Math.floorMod(b, perRound) == 0
            val r = sp * 0.48f * sc * (if (guru) 1.3f else 1f)
            val fade = max(0f, 1 - max(0f, abs(slot) - 3.2f) * 0.45f)
            if (fade <= 0f) continue
            drawBead(wood, Offset(x, y), r, lit = b <= count, now = abs(slot) < 0.5f, alpha = fade)
            if (guru) drawTassel(wood.mid, Offset(x, y + r * 0.9f), r, fade)
        }
    }
}

fun strandSpacing(height: Float, maxSpacing: Float) = min(maxSpacing, height / 6.2f)

private fun DrawScope.drawBead(w: Wood, c: Offset, r: Float, lit: Boolean, now: Boolean, alpha: Float, glow: Boolean = true) {
    if (lit && glow) {
        val g = if (now) 2.0f else 1.55f
        drawCircle(Brush.radialGradient(listOf(w.glow.copy(alpha = 0.5f * alpha), Color.Transparent), c, r * g), r * g, c)
    }
    drawCircle(Brush.radialGradient(listOf(w.hi, w.mid, w.lo), Offset(c.x - r * 0.32f, c.y - r * 0.44f), r * 1.45f), r, c, alpha = alpha)
    // Shadow side and a soft rim give the bead its roundness.
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), Offset(c.x - r * 0.2f, c.y - r * 0.25f), r * 1.2f), r, c, alpha = alpha)
    if (!lit) drawCircle(Color.Black.copy(alpha = 0.42f * alpha), r, c)
}

private fun DrawScope.drawTassel(color: Color, top: Offset, r: Float, alpha: Float) {
    val len = r * 1.1f
    for (dx in listOf(-0.35f, 0f, 0.35f)) {
        drawLine(color.copy(alpha = alpha), top, Offset(top.x + dx * r, top.y + len), strokeWidth = r * 0.12f, cap = StrokeCap.Round)
    }
}

/** A single bead, used for wood swatches. */
@Composable
fun BeadSwatch(wood: Wood, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawBead(wood, center, size.minDimension / 2 * 0.92f, lit = true, now = false, alpha = 1f, glow = false) }
}
