package com.satepadee.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
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
        // Direction of the string at a slot (unit vector), so each bead's holes sit on the string.
        fun along(slot: Float): Offset {
            val th = slot * sp / radius
            val dx = -0.6f * sin(th)
            val dy = cos(th)
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            return Offset(dx / len, dy / len)
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
            drawBead(wood, Offset(x, y), r, lit = b <= count, now = abs(slot) < 0.5f, alpha = fade, seed = b, axis = along(slot), string = wood.lo, stringWidth = 2.5.dp.toPx())
            if (guru) drawTassel(wood.mid, Offset(x, y + r * 0.9f), r, fade)
        }
    }
}

fun strandSpacing(height: Float, maxSpacing: Float) = min(maxSpacing, height / 6.2f)

/**
 * A turned wooden bead: base wood colour, grain lines that bend around the sphere, then
 * shading and a soft matte sheen (wood is not glossy). [seed] gives every bead its own
 * grain angle, spacing and a slight tone difference, as with natural wood.
 */
internal fun DrawScope.drawBead(
    w: Wood, c: Offset, r: Float, lit: Boolean, now: Boolean, alpha: Float, glow: Boolean = true, seed: Int = 0,
    axis: Offset = Offset(0f, 1f), string: Color? = null, stringWidth: Float = 0f,
) {
    if (lit && glow) {
        val g = if (now) 2.0f else 1.55f
        drawCircle(Brush.radialGradient(listOf(w.glow.copy(alpha = 0.5f * alpha), Color.Transparent), c, r * g), r * g, c)
    }
    val tone = 0.92f + 0.14f * rand(seed, 0)
    drawCircle(
        Brush.radialGradient(listOf(w.hi.shade(tone), w.mid.shade(tone), w.lo.shade(tone)), Offset(c.x - r * 0.32f, c.y - r * 0.44f), r * 1.45f),
        r, c, alpha = alpha,
    )

    val bead = Path().apply { addOval(Rect(c, r)) }
    clipPath(bead) {
        rotate(-30f + 60f * rand(seed, 1), c) {
            // Growth rings: fine, uneven bands that arch around the sphere, darker and lighter.
            val rings = 12 + (rand(seed, 2) * 7).toInt()
            for (i in 0 until rings) {
                val t = ((i + rand(seed, 10 + i)) / rings) * 2.2f - 1.1f
                val arch = 0.2f + 0.45f * rand(seed, 20 + i)
                val wobble = 0.02f + 0.05f * rand(seed, 30 + i)
                val ph = 6.28f * rand(seed, 50 + i)
                val dark = rand(seed, 60 + i) < 0.7f
                drawPath(
                    grainPath(c, r, t, arch, wobble, ph),
                    if (dark) w.lo.copy(alpha = (0.14f + 0.26f * rand(seed, 70 + i)) * alpha) else w.hi.copy(alpha = 0.10f * alpha),
                    style = Stroke(width = r * (0.012f + 0.035f * rand(seed, 90 + i)), cap = StrokeCap.Round),
                )
            }
            // Fibres: many hairline strokes that follow the rings.
            for (i in 0 until 26) {
                val t = rand(seed, 200 + i) * 2.2f - 1.1f
                drawPath(
                    grainPath(c, r, t, 0.3f, 0.015f, 6.28f * rand(seed, 240 + i)),
                    w.lo.copy(alpha = 0.07f * alpha), style = Stroke(width = r * 0.006f),
                )
            }
        }
    }

    // Shadow side and a darker rim give roundness; a broad soft sheen keeps it matte like oiled wood.
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f)), Offset(c.x - r * 0.2f, c.y - r * 0.25f), r * 1.15f), r, c, alpha = alpha)
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(c.x - r * 0.36f, c.y - r * 0.42f), r * 0.6f),
        r, c, alpha = alpha,
    )
    // Drilled holes at both ends of the string's path, turned to face along the string,
    // with the string running into each one.
    val angle = Math.toDegrees(kotlin.math.atan2(axis.y.toDouble(), axis.x.toDouble())).toFloat() - 90f
    for (side in listOf(-1f, 1f)) {
        val h = Offset(c.x + axis.x * side * r * 0.93f, c.y + axis.y * side * r * 0.93f)
        rotate(angle, h) {
            drawOval(Color.Black.copy(alpha = 0.6f * alpha), Offset(h.x - r * 0.16f, h.y - r * 0.07f), Size(r * 0.32f, r * 0.14f))
            drawOval(w.lo.copy(alpha = 0.5f * alpha), Offset(h.x - r * 0.16f, h.y - r * 0.07f), Size(r * 0.32f, r * 0.14f), style = Stroke(r * 0.025f))
        }
        if (string != null) {
            val out = Offset(h.x + axis.x * side * r * 0.2f, h.y + axis.y * side * r * 0.2f)
            drawLine(string.copy(alpha = alpha), h, out, strokeWidth = stringWidth, cap = StrokeCap.Round)
        }
    }
    if (!lit) drawCircle(Color.Black.copy(alpha = 0.42f * alpha), r, c)
}

private fun grainPath(c: Offset, r: Float, t: Float, arch: Float, wobble: Float, phase: Float): Path {
    val p = Path()
    var x = -1.1f
    while (x <= 1.1f) {
        val y = t + arch * t * x * x + wobble * sin(x * 3.3f + phase)
        if (x == -1.1f) p.moveTo(c.x + x * r, c.y + y * r) else p.lineTo(c.x + x * r, c.y + y * r)
        x += 0.08f
    }
    return p
}

/** Stable pseudo-random 0..1 for bead [seed] and slot [k], so a bead keeps its look as it rolls. */
private fun rand(seed: Int, k: Int): Float {
    var h = seed * 374761393 + k * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}

private fun Color.shade(f: Float) = Color((red * f).coerceIn(0f, 1f), (green * f).coerceIn(0f, 1f), (blue * f).coerceIn(0f, 1f), alpha)

private fun DrawScope.drawTassel(color: Color, top: Offset, r: Float, alpha: Float) {
    val len = r * 1.1f
    for (dx in listOf(-0.35f, 0f, 0.35f)) {
        drawLine(color.copy(alpha = alpha), top, Offset(top.x + dx * r, top.y + len), strokeWidth = r * 0.12f, cap = StrokeCap.Round)
    }
}

/** A single bead, used for wood swatches. */
@Composable
fun BeadSwatch(wood: Wood, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawBead(wood, center, size.minDimension / 2 * 0.92f, lit = true, now = false, alpha = 1f, glow = false, seed = wood.id.hashCode()) }
}
