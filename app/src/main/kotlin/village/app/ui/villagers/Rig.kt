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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import village.app.Expression
import village.app.ui.components.VillagerBadge
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.app.ui.villagers.cast.Cast
import village.app.ui.villagers.cast.VillagerLook
import village.app.ui.villagers.cast.circle
import village.app.ui.villagers.cast.line
import village.app.ui.villagers.cast.oval
import village.app.ui.villagers.cast.shape
import village.levels.Villager

/** The busts' coordinate space, from `villager-cast.html`: a 200 × 240 viewBox. */
private const val VIEW_W = 200f
private const val VIEW_H = 240f

/** How often a bust blinks, and for how long (README "Talking villagers"). */
private const val BLINK_EVERY_MILLIS = 3_600L
private const val BLINK_MILLIS = 120L

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
        expression == Expression.DELIGHTED -> line("M75 ${e + 3} Q82 ${e - 7} 89 ${e + 3} M111 ${e + 3} Q118 ${e - 7} 125 ${e + 3}", f.ink, 3.5f)
        blinking -> line("M76 ${e + 1} Q82 ${e + 5} 88 ${e + 1} M112 ${e + 1} Q118 ${e + 5} 124 ${e + 1}", f.ink, 3f)
        else -> {
            circle(82f, e, 5.5f, f.ink); circle(118f, e, 5.5f, f.ink)
            circle(84f, e - 2, 1.6f, f.eyeHighlight); circle(120f, e - 2, 1.6f, f.eyeHighlight)
        }
    }
    if (expression == Expression.PUZZLED) line("M72 ${e - 16} L90 ${e - 12}", f.ink, 4f) // one brow up
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
        expression == Expression.PUZZLED -> line("M${100 - f.mouthHalf} ${m + 4} Q${100 - f.mouthHalf / 2} $m 100 ${m + 4} Q${100 + f.mouthHalf / 2} ${m + 8} ${100 + f.mouthHalf} ${m + 3}", f.ink, 3f)
        else -> line("M${100 - f.mouthHalf} $m Q100 ${m + f.mouthDepth} ${100 + f.mouthHalf} $m", f.ink, 3f)
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
    val look = Cast.forId(villager.id)
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
 * circle in the profession's badge tint with the badge ring, or the profession badge when there is no bust.
 */
@Composable
fun VillagerPortrait(villager: Villager, size: Dp, expression: Expression = Expression.NEUTRAL, mouthOpen: Boolean = false) {
    val c = Village.colors
    val look = Cast.forId(villager.id)
    val description = "${villager.name} the ${villager.profession.label.lowercase()}"
    if (look == null) {
        VillagerBadge(villager.profession, size = size, contentDescription = description)
        return
    }
    val talking = mouthOpen && !LocalReducedMotion.current
    val bob = bobFor(talking, 3.dp)
    val blinking = rememberBlinking(villager.id)
    Box(
        Modifier.size(size).clip(CircleShape).background(Village.badges.tint(villager.profession)).border(3.dp, Village.badges.ring, CircleShape)
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

