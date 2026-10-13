package village.app.ui.villagers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import village.app.BigDialogue
import village.app.DialogueState
import village.app.Expression
import village.app.Typewriter
import village.app.dialogueNow
import village.app.ui.components.Chevron
import village.app.ui.components.PrimaryButton
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.app.ui.theme.VillageFonts
import village.levels.Villager

/** The dialogue's line text: Atkinson Hyperlegible 24 sp, line height 1.5 (README "Dialogue box"). */
private val LineStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 24.sp, lineHeight = 36.sp)

/** Marks the big dialogue for the window check, which waits for it to fade out. */
const val BIG_DIALOGUE_TAG = "big-dialogue"

/** How tall the bust stands beside the dialogue box. */
private val BIG_BUST_HEIGHT = 230.dp

/** The time a [typewriter] is drawn at: follows the frame clock while it types, then stops ticking. */
@Composable
fun typingClock(typewriter: Typewriter): Long {
    var now by remember(typewriter) { mutableStateOf(dialogueNow()) }
    LaunchedEffect(typewriter) {
        while (typewriter.typing(now)) {
            withFrameMillis { }
            now = dialogueNow()
        }
    }
    return now
}

/**
 * [line] with only its first [shown] characters visible (talking-villagers D5): the rest is laid out
 * in transparent ink, so wrapping and height never change while it types, and readers get the full line.
 */
@Composable
fun TypedText(line: String, shown: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, minLines: Int = 1) {
    val text = buildAnnotatedString {
        append(line.substring(0, shown))
        withStyle(SpanStyle(color = Color.Transparent)) { append(line.substring(shown)) }
    }
    Text(text, style = style, color = color, minLines = minLines, modifier = modifier)
}

/**
 * The dialogue box (spec: Dialogue box): name tab, three-line text area, "n of N", and a blinking ▼ once
 * the line is out, or [closing] on the last line. Clicking anywhere on it advances.
 */
@Composable
fun DialogueBox(villager: Villager, state: DialogueState, now: Long, glowing: Boolean, onAdvance: () -> Unit, closing: @Composable () -> Unit) {
    val c = Village.colors
    val shape = RoundedCornerShape(22.dp)
    val glow = c.glow.copy(alpha = 0.45f)
    Box(Modifier.padding(top = 18.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .drawBehind {
                    val r = CornerRadius(22.dp.toPx())
                    if (glowing) {
                        val g = 10.dp.toPx()
                        drawRoundRect(glow, Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(r.x + g))
                    }
                    drawRoundRect(c.woodDark, Offset(0f, 6.dp.toPx()), size, r) // the 6 dp bottom shadow
                }
                .clip(shape).background(c.surface).border(4.dp, c.wood, shape)
                .drawBehind {
                    val inset = 4.dp.toPx() + 5.dp.toPx()
                    drawRoundRect(
                        c.outline, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset),
                        CornerRadius(14.dp.toPx()), style = Stroke(2.dp.toPx()),
                    )
                }
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onAdvance)
                .padding(start = 36.dp, end = 36.dp, top = 34.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val t = state.typewriter
            TypedText(t.line, t.shown(now), LineStyle, c.ink, minLines = 3)
            Row(Modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.pageLabel, style = Village.type.small, color = c.inkMuted)
                Spacer(Modifier.weight(1f))
                when {
                    t.typing(now) -> {}
                    state.isLast -> closing()
                    else -> ContinueHint()
                }
            }
        }
        NameTab(villager, Modifier.offset(x = 48.dp, y = (-18).dp))
    }
}

/** The `wood` pill over the box's top edge: name in Fredoka 18, profession in `chip`. */
@Composable
private fun NameTab(villager: Villager, modifier: Modifier) {
    val c = Village.colors
    Row(
        modifier.clip(RoundedCornerShape(percent = 50)).background(c.wood).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(villager.name, style = Village.type.title, color = c.surface)
        Text(villager.profession.label, style = Village.type.small, color = c.chip)
    }
}

/** "Click to continue ▼", blinking every half second; still under reduced motion. */
@Composable
private fun ContinueHint() {
    val c = Village.colors
    val alpha = if (LocalReducedMotion.current) 1f else {
        val blink by rememberInfiniteTransition(label = "continue").animateFloat(
            1f, 0.25f, infiniteRepeatable(tween(500, easing = { if (it < 0.5f) 0f else 1f }), RepeatMode.Reverse), label = "arrow",
        )
        blink
    }
    Row(Modifier.alpha(alpha), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Click to continue", style = Village.type.small, color = c.inkMuted)
        Canvas(Modifier.size(18.dp, 14.dp)) {
            val s = size.width / 18f
            drawPath(Path().apply { moveTo(2 * s, 2 * s); lineTo(16 * s, 2 * s); lineTo(9 * s, 12 * s); close() }, c.wood)
        }
    }
}

/**
 * The big dialogue (spec: Big dialogue): an `ink` 55% scrim that takes every click, Skip intro for an
 * intro, and the villager's bust beside the dialogue box at the bottom. Space and Enter advance, Escape
 * skips an intro. [onClose] runs on Skip and on the closing button: Let's build it! or See <shop>.
 * [contentModifier] goes on the bust and box, so the caller can slide them in while the scrim fades.
 */
@Composable
fun BigDialogueOverlay(villager: Villager, dialogue: BigDialogue, shop: String, onClose: () -> Unit, contentModifier: Modifier = Modifier) {
    val c = Village.colors
    val reduced = LocalReducedMotion.current
    var state by remember(dialogue) { mutableStateOf(DialogueState.start(dialogue.lines, dialogueNow(), reduced)) }
    val now = typingClock(state.typewriter)
    val intro = dialogue.kind == BigDialogue.Kind.INTRO
    val focus = remember { FocusRequester() }
    fun advance() { state = state.advance(dialogueNow()) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        Modifier.fillMaxSize().testTag(BIG_DIALOGUE_TAG).background(c.ink.copy(alpha = 0.55f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true // nothing reaches the screen behind
                when (e.key) {
                    Key.Spacebar, Key.Enter, Key.NumPadEnter -> if (state.atEnd(dialogueNow())) onClose() else advance()
                    Key.Escape -> if (intro) onClose()
                }
                true
            },
    ) {
        Row(
            contentModifier.align(Alignment.BottomCenter).widthIn(max = 1120.dp).fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val talking = state.typewriter.mouthOpen(now)
            Box {
                VillagerBust(villager, if (intro) Expression.NEUTRAL else Expression.DELIGHTED, talking, BIG_BUST_HEIGHT)
                if (!intro) SparklesFor(BIG_BUST_HEIGHT)
            }
            Column(Modifier.weight(1f).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (intro) {
                    // Above the box at its right, as in 06-level-intro-dialogue.html.
                    Text(
                        "Skip intro", style = Village.type.small.copy(fontWeight = FontWeight.Bold), color = c.woodDark,
                        modifier = Modifier.align(Alignment.End).heightIn(min = 40.dp).clip(RoundedCornerShape(percent = 50)).background(c.surface)
                            .clickable(role = Role.Button, onClick = onClose).padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                DialogueBox(villager, state, now, glowing = !intro, onAdvance = ::advance) {
                    ClosingButton(if (intro) "Let's build it!" else "See $shop", onClose)
                }
            }
        }
    }
}

/** Sparkles over a [height]-tall bust, twinkling on a 1.2 s loop; still under reduced motion. */
@Composable
private fun SparklesFor(height: Dp) {
    val phase = if (LocalReducedMotion.current) 0.5f else {
        val p by rememberInfiniteTransition(label = "sparkles").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1_200, easing = LinearEasing)), label = "phase",
        )
        p
    }
    Sparkles(phase, Modifier.size(height * (200f / 240f), height))
}

@Composable
private fun ClosingButton(text: String, onClick: () -> Unit) {
    PrimaryButton(onClick = onClick, height = 48.dp) {
        Text(text, style = Village.type.button, color = Color.White, maxLines = 1)
        Chevron(Color.White, 18.dp)
    }
}
