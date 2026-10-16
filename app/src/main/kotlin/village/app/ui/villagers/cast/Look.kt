package village.app.ui.villagers.cast

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Where a look's face sits, and its colors, for the shared rig to draw eyes, cheeks and mouth on
 * (talking-villagers D4). Mouth y is where the closed smile starts; the open mouth and the tongue are ellipses.
 */
internal class Face(
    val eyeY: Float = 106f,
    val mouthY: Float = 128f,
    val mouthHalf: Float = 12f,
    val mouthDepth: Float = 10f,
    val openCy: Float = 131f, val openRx: Float = 11f, val openRy: Float = 9f,
    val tongueCy: Float = 136f, val tongueRx: Float = 6f, val tongueRy: Float = 3.5f,
    val mouthFill: Color = Color(0xFF7E2E1C), val tongue: Color = Color(0xFFE07A8A),
    val cheekDx: Float = 30f, val cheekY: Float = 124f, val cheek: Color = Color(0xFFE89A8A), val cheekAlpha: Float = 0.7f,
    /** Eyes, brows and the closed mouth. */
    val ink: Color = Color(0xFF2B2118),
    val eyeHighlight: Color = Color.White,
)

/** One villager's drawing: [back] is everything behind the face, [front] what covers it (glasses, a moustache). */
internal class VillagerLook(val face: Face, val back: DrawScope.() -> Unit, val front: DrawScope.() -> Unit = {})

// Drawing helpers in the 200 × 240 space; paths are SVG path data from the mockups.

private val parsed = HashMap<String, Path>()

private fun path(d: String): Path = parsed.getOrPut(d) { PathParser().parsePathString(d).toPath() }

internal fun DrawScope.shape(d: String, fill: Color, stroke: Color? = null, width: Float = 0f, alpha: Float = 1f) {
    val p = path(d)
    drawPath(p, fill, alpha = alpha)
    if (stroke != null) drawPath(p, stroke, style = Stroke(width, join = StrokeJoin.Round))
}

internal fun DrawScope.line(d: String, color: Color, width: Float) =
    drawPath(path(d), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

internal fun DrawScope.circle(cx: Float, cy: Float, r: Float, color: Color, alpha: Float = 1f) = drawCircle(color, r, Offset(cx, cy), alpha)

internal fun DrawScope.ring(cx: Float, cy: Float, r: Float, color: Color, width: Float) = drawCircle(color, r, Offset(cx, cy), style = Stroke(width))

internal fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) = drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))

internal fun DrawScope.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, stroke: Color? = null, width: Float = 0f) {
    val radius = CornerRadius(r)
    drawRoundRect(fill, Offset(x, y), Size(w, h), radius)
    if (stroke != null) drawRoundRect(stroke, Offset(x, y), Size(w, h), radius, style = Stroke(width))
}

/** The neck every bust shares, [h] tall under the chin. */
internal fun DrawScope.neck(color: Color, h: Float) = roundRect(88f, 140f, 24f, h, 8f, color)

/** Ears and head: two small circles and a big one. */
internal fun DrawScope.head(skin: Color, cy: Float = 102f, r: Float = 50f, earY: Float = 106f) {
    circle(52f, earY, 10f, skin); circle(148f, earY, 10f, skin)
    circle(100f, cy, r, skin)
}
