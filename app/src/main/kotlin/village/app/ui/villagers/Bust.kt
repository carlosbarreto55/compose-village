package village.app.ui.villagers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import village.app.Expression
import village.app.ui.components.VillagerBadge
import village.app.ui.components.tint
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.levels.Villager

/** The busts' coordinate space, from `villager-cast.html`: a 200 × 240 viewBox. */
private const val VIEW_W = 200f
private const val VIEW_H = 240f

/** How often a bust blinks, and for how long (README "Talking villagers"). */
private const val BLINK_EVERY_MILLIS = 3_600L
private const val BLINK_MILLIS = 120L

private val Ink = Color(0xFF2B2118)

/**
 * Where a look's face sits, for the shared rig to draw eyes, cheeks and mouth on (talking-villagers D4).
 * Mouth y is where the closed smile starts; the open mouth and the tongue are ellipses.
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
)

/** One villager's drawing: [back] is everything behind the face, [front] what covers it (glasses, a moustache). */
internal class VillagerLook(val face: Face, val back: DrawScope.() -> Unit, val front: DrawScope.() -> Unit = {})

/** The five busts, keyed by villager id; anyone else is drawn with their profession badge. */
internal object Busts {
    private val looks: Map<String, VillagerLook> by lazy {
        mapOf("rosa" to rosa(), "tom" to tom(), "marco" to marco(), "lucia" to lucia(), "iara" to iara())
    }

    fun forId(id: String): VillagerLook? = looks[id]

    private fun rosa() = VillagerLook(Face(), back = {
        circle(100f, 108f, 58f, Color(0xFF5A3820))
        shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFFFFFDF8), Color(0xFFE2D3BC), 3f)
        line("M100 170 V240", Color(0xFFE2D3BC), 2f)
        circle(112f, 198f, 4f, Color(0xFFB8A58C)); circle(112f, 222f, 4f, Color(0xFFB8A58C))
        neck(Color(0xFFB57D55), 30f)
        head(Color(0xFFC98E64), cy = 104f, r = 48f, earY = 108f)
        shape("M54 98 Q62 70 100 68 Q138 70 146 98 Q126 82 100 84 Q74 82 54 98 Z", Color(0xFF5A3820))
        shape("M50 74 C34 74 34 46 56 48 C58 22 90 18 100 34 C110 18 142 22 144 48 C166 46 166 74 150 74 Z", Color.White, Color(0xFFE2D3BC), 3f)
        roundRect(54f, 64f, 92f, 18f, 6f, Color.White, Color(0xFFE2D3BC), 3f)
    }, front = {
        circle(136f, 114f, 3f, Color.White, alpha = 0.85f) // flour on her cheek
    })

    private fun tom() = VillagerLook(Face(), back = {
        shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF4E7A3A))
        shape("M72 180 C82 174 118 174 128 180 L134 240 H66 Z", Color(0xFFF3E3C8))
        circle(116f, 204f, 6f, Color(0xFFE07A8A)); circle(116f, 204f, 2.2f, Color(0xFFF7D9A8))
        neck(Color(0xFFD9A27A), 32f)
        head(Color(0xFFE8B48A))
        shape("M46 92 Q100 4 154 92 Q100 70 46 92 Z", Color(0xFF4E7A3A))
        circle(140f, 64f, 14f, Color(0xFFE07A8A)); circle(140f, 64f, 5f, Color(0xFFF7D9A8))
    })

    private fun marco() = VillagerLook(
        Face(
            mouthY = 136f, mouthHalf = 10f, mouthDepth = 6f, openCy = 138f, openRx = 9f, openRy = 7f,
            tongueCy = 142f, tongueRx = 5f, tongueRy = 2.5f, cheekDx = 32f, cheekY = 122f, cheekAlpha = 0.6f,
        ),
        back = {
            shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFFFFFDF8), Color(0xFFD5D9DE), 3f)
            for (x in listOf(86f, 114f)) for (y in listOf(208f, 228f)) circle(x, y, 4f, Color(0xFFB8A58C))
            neck(Color(0xFFCF9670), 30f)
            shape("M76 166 L100 172 L124 166 L100 196 Z", Color(0xFFC2503A))
            head(Color(0xFFE0A77E))
            shape("M52 104 Q48 82 62 76 L66 104 Z M148 104 Q152 82 138 76 L134 104 Z", Color(0xFF3A2A20))
            shape("M58 78 C42 76 42 48 62 50 C58 18 94 8 100 28 C106 8 142 18 138 50 C158 48 158 76 142 78 Z", Color.White, Color(0xFFD5D9DE), 3f)
            roundRect(58f, 66f, 84f, 18f, 5f, Color.White, Color(0xFFD5D9DE), 3f)
            line("M72 94 L90 91 M110 91 L128 94", Color(0xFF3A2A20), 4f)
        },
        front = {
            shape("M78 126 Q88 116 100 124 Q112 116 122 126 Q112 132 100 127 Q88 132 78 126 Z", Color(0xFF3A2A20)) // moustache
        },
    )

    private fun lucia() = VillagerLook(
        Face(
            mouthY = 130f, openCy = 133f, openRx = 10f, openRy = 8f, tongueCy = 137f, tongueRx = 5.5f, tongueRy = 3f,
            mouthFill = Color(0xFF4A1E12), tongue = Color(0xFFC46A6A),
            cheekDx = 32f, cheekY = 128f, cheek = Color(0xFFB5644E), cheekAlpha = 0.6f,
        ),
        back = {
            circle(100f, 100f, 56f, Color(0xFFCFC6BA))
            circle(100f, 42f, 20f, Color(0xFFCFC6BA)) // her bun
            shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF5E8A6A))
            shape("M80 166 L100 192 L120 166 Z", Color(0xFFFFF9EF))
            line("M100 192 V240", Color(0xFF4A6F55), 2f)
            circle(108f, 208f, 3.5f, Color(0xFFF2C46B)); circle(108f, 226f, 3.5f, Color(0xFFF2C46B))
            neck(Color(0xFF7E5036), 30f)
            head(Color(0xFF8D5B3E))
            shape("M52 98 Q58 58 100 56 Q142 58 148 98 Q132 74 100 76 Q68 74 52 98 Z", Color(0xFFCFC6BA))
            circle(82f, 106f, 13f, Color.White, alpha = 0.35f); circle(118f, 106f, 13f, Color.White, alpha = 0.35f)
        },
        front = {
            ring(82f, 106f, 13f, Color(0xFF3A2A20), 3f); ring(118f, 106f, 13f, Color(0xFF3A2A20), 3f)
            line("M95 106 H105", Color(0xFF3A2A20), 3f)
        },
    )

    private fun iara() = VillagerLook(
        Face(eyeY = 108f, mouthY = 130f, openCy = 133f, tongueCy = 138f, cheekY = 126f, cheek = Color(0xFFD9806A), cheekAlpha = 0.6f),
        back = {
            shape("M46 104 Q40 176 66 186 H134 Q160 176 154 104 Q150 52 100 50 Q50 52 46 104 Z", Color(0xFF2B2118)) // long hair
            shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF2F4A6B))
            line("M76 168 L100 200 L124 168", Color(0xFFF3E3C8), 4f)
            circle(88f, 216f, 4f, Color(0xFFF2C46B)); circle(112f, 216f, 4f, Color(0xFFF2C46B))
            neck(Color(0xFF9C6B46), 30f)
            head(Color(0xFFB07A52), cy = 104f, r = 48f, earY = 108f)
            shape("M56 82 Q54 38 100 36 Q146 38 144 82 Z", Color.White, Color(0xFFCFD8E0), 3f)
            roundRect(56f, 68f, 88f, 14f, 0f, Color(0xFF1E2F45))
            shape("M40 84 Q100 70 160 84 Q158 94 100 90 Q42 94 40 84 Z", Color(0xFF1E2F45))
            circle(100f, 54f, 8f, Color(0xFFF2C46B))
            line("M100 49 V59 M96 56 Q100 61 104 56", Color(0xFF1E2F45), 1.8f) // the anchor
        },
    )
}

/**
 * A villager's bust (spec: Villager busts), drawn from [look] and the shared rig: [expression] picks the
 * eyes, brows and mouth; [mouthOpen] is for talking; [blinking] closes the eyes. Fills its 200:240 box.
 */
@Composable
private fun BustCanvas(look: VillagerLook, expression: Expression, mouthOpen: Boolean, blinking: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width / VIEW_W
        withTransform({ scale(s, s, Offset.Zero) }) { drawBust(look, expression, mouthOpen, blinking) }
    }
}

private fun DrawScope.drawBust(look: VillagerLook, expression: Expression, mouthOpen: Boolean, blinking: Boolean) {
    val f = look.face
    look.back(this)
    // Eyes: happy arcs when delighted, a closed line while blinking, open with a highlight otherwise.
    val e = f.eyeY
    when {
        expression == Expression.DELIGHTED -> line("M75 ${e + 3} Q82 ${e - 7} 89 ${e + 3} M111 ${e + 3} Q118 ${e - 7} 125 ${e + 3}", Ink, 3.5f)
        blinking -> line("M76 ${e + 1} Q82 ${e + 5} 88 ${e + 1} M112 ${e + 1} Q118 ${e + 5} 124 ${e + 1}", Ink, 3f)
        else -> {
            circle(82f, e, 5.5f, Ink); circle(118f, e, 5.5f, Ink)
            circle(84f, e - 2, 1.6f, Color.White); circle(120f, e - 2, 1.6f, Color.White)
        }
    }
    if (expression == Expression.PUZZLED) line("M72 ${e - 16} L90 ${e - 12}", Ink, 4f) // one brow up
    val cheekR = if (expression == Expression.DELIGHTED) 9f else 8f
    val cheekAlpha = if (expression == Expression.DELIGHTED) 0.8f else f.cheekAlpha
    circle(100f - f.cheekDx, f.cheekY, cheekR, f.cheek, cheekAlpha); circle(100f + f.cheekDx, f.cheekY, cheekR, f.cheek, cheekAlpha)
    val m = f.mouthY
    when {
        mouthOpen -> {
            val grow = if (expression == Expression.DELIGHTED) 1.25f else 1f
            oval(100f, f.openCy, f.openRx * grow, f.openRy * grow, f.mouthFill)
            oval(100f, f.tongueCy + (grow - 1) * 6, f.tongueRx * grow, f.tongueRy * grow, f.tongue)
        }
        expression == Expression.DELIGHTED -> shape("M${100 - f.mouthHalf - 4} ${m - 4} Q100 ${m + 18} ${100 + f.mouthHalf + 4} ${m - 4} Z", f.mouthFill)
        expression == Expression.PUZZLED -> line("M${100 - f.mouthHalf} ${m + 4} Q${100 - f.mouthHalf / 2} $m 100 ${m + 4} Q${100 + f.mouthHalf / 2} ${m + 8} ${100 + f.mouthHalf} ${m + 3}", Ink, 3f)
        else -> line("M${100 - f.mouthHalf} $m Q100 ${m + f.mouthDepth} ${100 + f.mouthHalf} $m", Ink, 3f)
    }
    look.front(this)
}

/** Blinks for 120 ms about every 3.6 s, starting at a per-villager offset so two busts never blink together. */
@Composable
private fun rememberBlinking(id: String): Boolean {
    var blinking by remember { mutableStateOf(false) }
    if (LocalReducedMotion.current) return false
    LaunchedEffect(id) {
        delay(Math.floorMod(id.hashCode(), 7) * 500L + 600L)
        while (true) {
            blinking = true
            delay(BLINK_MILLIS)
            blinking = false
            delay(BLINK_EVERY_MILLIS - BLINK_MILLIS)
        }
    }
    return blinking
}

/** The bust lifts [lift] each time the mouth opens, easing out over 90 ms; still under reduced motion. */
@Composable
private fun bobFor(mouthOpen: Boolean, lift: Dp): Dp {
    val reduced = LocalReducedMotion.current
    val bob by animateDpAsState(if (mouthOpen && !reduced) -lift else 0.dp, tween(90, easing = FastOutSlowInEasing), label = "bob")
    return bob
}

/**
 * The big bust of the dialogues, [height] tall, or the villager's badge when the game has no bust for
 * them. Under reduced motion it never talks, bobs or blinks.
 */
@Composable
fun VillagerBust(villager: Villager, expression: Expression, mouthOpen: Boolean, height: Dp, modifier: Modifier = Modifier) {
    val look = Busts.forId(villager.id)
    val description = "${villager.name} the ${villager.profession.label.lowercase()}"
    if (look == null) {
        Box(modifier.size(height * (VIEW_W / VIEW_H), height), contentAlignment = Alignment.Center) {
            VillagerBadge(villager.profession, size = height * 0.6f, contentDescription = description)
        }
        return
    }
    val talking = mouthOpen && !LocalReducedMotion.current
    val bob = bobFor(talking, 4.dp)
    val blinking = rememberBlinking(villager.id)
    BustCanvas(
        look, expression, talking, blinking,
        modifier.size(height * (VIEW_W / VIEW_H), height).offset(y = bob).semantics { contentDescription = description },
    )
}

/**
 * The villager's round portrait for cards (spec: Villager card): the bust's head and shoulders in a
 * tinted circle with the badges' 3 dp `accent` ring, or the profession badge when there is no bust.
 */
@Composable
fun VillagerPortrait(villager: Villager, size: Dp, expression: Expression = Expression.NEUTRAL, mouthOpen: Boolean = false) {
    val c = Village.colors
    val look = Busts.forId(villager.id)
    val description = "${villager.name} the ${villager.profession.label.lowercase()}"
    if (look == null) {
        VillagerBadge(villager.profession, size = size, contentDescription = description)
        return
    }
    val talking = mouthOpen && !LocalReducedMotion.current
    val bob = bobFor(talking, 3.dp)
    val blinking = rememberBlinking(villager.id)
    Box(
        Modifier.size(size).clip(CircleShape).background(villager.profession.tint).border(3.dp, c.accent, CircleShape)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.size(size).offset(y = bob)) {
            // A 150-unit square around the face (centre 100, 97): head, hat and collar fill the circle.
            val s = this.size.width / 150f
            withTransform({ scale(s, s, Offset.Zero); translate(-25f, -22f) }) { drawBust(look, expression, talking, blinking) }
        }
    }
}

/** Three gold sparkles around a celebrating bust, twinkling with [phase] (0..1); still under reduced motion. */
@Composable
fun Sparkles(phase: Float, modifier: Modifier = Modifier) {
    val c = Village.colors
    Canvas(modifier.aspectRatio(VIEW_W / VIEW_H)) {
        val s = size.width / VIEW_W
        withTransform({ scale(s, s, Offset.Zero) }) {
            val stars = listOf(
                "M22 40 L25 32 L28 40 L36 43 L28 46 L25 54 L22 46 L14 43 Z",
                "M170 24 L172 18 L174 24 L180 26 L174 28 L172 34 L170 28 L164 26 Z",
                "M178 132 L180 127 L182 132 L187 134 L182 136 L180 141 L178 136 L173 134 Z",
                "M30 150 L32 145 L34 150 L39 152 L34 154 L32 159 L30 154 L25 152 Z",
            )
            stars.forEachIndexed { i, d ->
                val t = (phase + i * 0.27f) % 1f
                val alpha = 0.45f + 0.55f * (1f - kotlin.math.abs(t * 2f - 1f))
                shape(d, c.glow, alpha = alpha)
            }
        }
    }
}

// Drawing helpers in the 200 × 240 space; paths are SVG path data from the mockups.

private val parsed = HashMap<String, Path>()

private fun path(d: String): Path = parsed.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private fun DrawScope.shape(d: String, fill: Color, stroke: Color? = null, width: Float = 0f, alpha: Float = 1f) {
    val p = path(d)
    drawPath(p, fill, alpha = alpha)
    if (stroke != null) drawPath(p, stroke, style = Stroke(width, join = StrokeJoin.Round))
}

private fun DrawScope.line(d: String, color: Color, width: Float) =
    drawPath(path(d), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

private fun DrawScope.circle(cx: Float, cy: Float, r: Float, color: Color, alpha: Float = 1f) = drawCircle(color, r, Offset(cx, cy), alpha)

private fun DrawScope.ring(cx: Float, cy: Float, r: Float, color: Color, width: Float) = drawCircle(color, r, Offset(cx, cy), style = Stroke(width))

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) = drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))

private fun DrawScope.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, stroke: Color? = null, width: Float = 0f) {
    val radius = androidx.compose.ui.geometry.CornerRadius(r)
    drawRoundRect(fill, Offset(x, y), Size(w, h), radius)
    if (stroke != null) drawRoundRect(stroke, Offset(x, y), Size(w, h), radius, style = Stroke(width))
}

/** The neck every bust shares, [h] tall under the chin. */
private fun DrawScope.neck(color: Color, h: Float) = roundRect(88f, 140f, 24f, h, 8f, color)

/** Ears and head: two small circles and a big one. */
private fun DrawScope.head(skin: Color, cy: Float = 102f, r: Float = 50f, earY: Float = 106f) {
    circle(52f, earY, 10f, skin); circle(148f, earY, 10f, skin)
    circle(100f, cy, r, skin)
}
