package village.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import village.app.GameText
import village.app.ui.components.TextAction
import village.app.ui.components.VillageCard
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.levels.Customer
import village.levels.Panel
import village.preview.PreviewEngine
import village.preview.PreviewState
import village.preview.RuntimeError
import village.preview.Viewport

/** The sign frame's `accent` border; the preview card is this much wider than the viewport, plus its padding. */
val SIGN_BORDER = 10.dp

/**
 * The preview card (spec: Target view switch, Target ghost): the "Your screen | <Name>'s sketch"
 * switch and the Overlay sketch checkbox above the player's frame in a wooden sign frame.
 *
 * Mouse input on the sign goes to the player's scene, except while the sketch is shown in full.
 * [glow] (0..1) lights the frame when the level is complete; [dim] fades the frame while building.
 */
@Composable
fun PreviewCard(
    state: PreviewState, engine: PreviewEngine, viewport: Viewport?,
    villagerName: String?, target: ImageBitmap?,
    showTarget: Boolean, onShowTarget: (Boolean) -> Unit,
    overlay: Boolean, onOverlay: (Boolean) -> Unit,
    building: Boolean, complete: Boolean, glow: Float,
    customerCaption: String? = null, onPlayerPress: () -> Unit = {},
) {
    val c = Village.colors
    val targetShown = showTarget && target != null
    VillageCard(padding = PaddingValues(16.dp), gap = 14.dp) {
        if (villagerName != null && target != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ViewSwitch(GameText.sketchTab(villagerName), showTarget, onShowTarget)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.heightIn(min = 44.dp).clip(Village.shapes.button)
                        .clickable(enabled = !showTarget, role = Role.Checkbox) { onOverlay(!overlay) }.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        overlay, onCheckedChange = null, enabled = !showTarget,
                        colors = CheckboxDefaults.colors(checkedColor = c.accent, uncheckedColor = c.inkMuted),
                    )
                    Text("Overlay sketch", style = Village.type.small, color = c.inkMuted.copy(alpha = if (showTarget) 0.5f else 1f))
                }
            }
        }
        // A level's viewport, even before its first frame (the ghost needs it); otherwise the frame's or engine's size.
        val (width, height) = with(LocalDensity.current) {
            val frame = state.frame
            when {
                viewport != null -> viewport.widthDp.dp to viewport.heightDp.dp
                frame != null -> frame.width.toDp() to frame.height.toDp()
                else -> engine.width.toDp() to engine.height.toDp()
            }
        }
        SignFrame(glow, Modifier.align(Alignment.CenterHorizontally)) {
            val dim by animateFloatAsState(if (building) 0.55f else 1f, tween(200))
            Box(
                Modifier.size(width, height)
                    .clip(Village.shapes.small)
                    .background(c.signBoard)
                    .pointerInput(engine, targetShown) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val forwarded = event.type == PointerEventType.Press || event.type == PointerEventType.Release ||
                                    event.type == PointerEventType.Move || event.type == PointerEventType.Scroll
                                if (forwarded && !targetShown) {
                                    if (event.type == PointerEventType.Press) onPlayerPress()
                                    engine.sendPointer(event.type, engine.toScene(change.position, size.width, size.height), change.scrollDelta)
                                }
                            }
                        }
                    }
                    .border(2.dp, c.accentStrong, Village.shapes.small),
            ) {
                val frame = state.frame
                when {
                    targetShown -> Image(target!!, contentDescription = "Target", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.High)
                    else -> {
                        if (frame != null) Image(frame, contentDescription = "Your screen", Modifier.fillMaxSize().alpha(dim), contentScale = ContentScale.FillBounds)
                        if (overlay && target != null) {
                            Image(target, contentDescription = "Sketch overlay", Modifier.fillMaxSize().alpha(0.4f), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.High)
                        }
                        val caption = when {
                            frame != null && customerCaption != null && !building -> customerCaption
                            frame != null || state.error != null -> null
                            overlay && target != null && villagerName != null -> GameText.ghostCaption(villagerName)
                            else -> "Press Run to see your screen"
                        }
                        if (caption != null) {
                            Text(
                                caption, style = Village.type.small, color = c.inkMuted, textAlign = TextAlign.Center,
                                modifier = Modifier.align(
                                    when {
                                        frame != null && complete -> Alignment.BottomStart // the Matches badge is at the bottom end
                                        frame != null || overlay -> Alignment.BottomCenter
                                        else -> Alignment.Center
                                    },
                                ).padding(14.dp),
                            )
                        }
                        if (complete && frame != null && villagerName != null) MatchesBadge(villagerName, Modifier.align(Alignment.BottomEnd).padding(14.dp))
                    }
                }
            }
        }
    }
}

/** The wooden sign: a 10 dp `accent` border; while [glow] > 0 a warm ring and halo light it up. */
@Composable
private fun SignFrame(glow: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Village.colors
    Box(
        modifier
            .drawBehind {
                if (glow <= 0f) return@drawBehind
                val radius = 18.dp.toPx()
                // A soft halo, kept within the card's padding so it doesn't tint the switch above.
                for (k in 8 downTo 1) {
                    val spread = k * 1.75.dp.toPx()
                    drawRoundRect(
                        c.glow.copy(alpha = 0.12f * glow), topLeft = Offset(-spread, -spread),
                        size = Size(size.width + 2 * spread, size.height + 2 * spread), cornerRadius = CornerRadius(radius + spread),
                    )
                }
                val ring = 6.dp.toPx()
                drawRoundRect(
                    c.glowRing.copy(alpha = glow), topLeft = Offset(-ring / 2, -ring / 2), size = Size(size.width + ring, size.height + ring),
                    cornerRadius = CornerRadius(radius + ring / 2), style = Stroke(ring),
                )
            }
            .clip(Village.shapes.sign).background(c.accent).padding(SIGN_BORDER),
    ) { content() }
}

/** "✓ Matches Tom's sketch", over a completed sign. */
@Composable
private fun MatchesBadge(name: String, modifier: Modifier) {
    val c = Village.colors
    Row(
        modifier.clip(Village.shapes.chip).background(c.primary).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(14.dp)) {
            val s = size.width / 14f
            drawPath(Path().apply { moveTo(3f * s, 7.2f * s); lineTo(5.8f * s, 10f * s); lineTo(11f * s, 4.5f * s) }, c.onPrimary, style = Stroke(2f * s))
        }
        Text("Matches $name's sketch", style = Village.type.caption.copy(fontWeight = FontWeight.Bold), color = c.onPrimary)
    }
}

/** The rounded "Your screen | <Name>'s sketch" switch. */
@Composable
private fun ViewSwitch(sketchLabel: String, showTarget: Boolean, onChange: (Boolean) -> Unit) {
    val c = Village.colors
    Row(Modifier.clip(Village.shapes.viewSwitch).background(c.track).padding(4.dp)) {
        for ((label, target) in listOf("Your screen" to false, sketchLabel to true)) {
            val selected = showTarget == target
            Box(
                Modifier.height(36.dp)
                    .then(if (selected) Modifier.shadow(1.dp, Village.shapes.viewSwitch).background(Color.White, Village.shapes.viewSwitch) else Modifier)
                    .clip(Village.shapes.viewSwitch)
                    .clickable(role = Role.Tab) { onChange(target) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, style = if (selected) Village.type.small.copy(fontWeight = FontWeight.Bold) else Village.type.small,
                    color = if (selected) c.ink else c.inkMuted, maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun RuntimeErrorPanel(error: RuntimeError, onSelectLine: (Int) -> Unit) {
    val c = Village.colors
    val title = when (error.kind) {
        RuntimeError.Kind.EXCEPTION -> "Your code crashed"
        RuntimeError.Kind.INFINITE_LOOP -> "Infinite loop?"
        RuntimeError.Kind.TOO_MUCH_MEMORY -> "Too much memory"
        RuntimeError.Kind.STACK_OVERFLOW -> "Too much recursion"
        RuntimeError.Kind.STOPPED -> "Stopped"
        RuntimeError.Kind.RESTARTED -> "Preview restarted"
    }
    Column(
        Modifier.fillMaxWidth().clip(Village.shapes.card).background(c.errorContainer).border(1.dp, c.errorOutline, Village.shapes.card)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = Village.type.title, color = c.onErrorContainer)
        Text(error.message, style = Village.type.small, color = c.errorDetail)
        error.line?.let { line -> TextAction("Go to line $line", onClick = { onSelectLine(line) }, color = c.onErrorContainer, bold = true) }
    }
}

/** A dark terminal card with a capitalized label, for the Console and Recompositions panels. */
@Composable
private fun TerminalCard(label: String, content: @Composable () -> Unit) {
    val c = Village.colors
    Column(
        Modifier.fillMaxWidth().clip(Village.shapes.editor).background(c.editorBg).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.uppercase(), style = Village.type.panelLabel, color = c.editorText)
        content()
    }
}

/**
 * What the attempt printed, with a customer line before each customer step's output, or
 * `(nothing printed)` after a step that printed nothing (customer-bot D6). [empty] is shown before
 * anything was printed.
 */
@Composable
fun ConsolePanel(console: String, marks: List<Customer.Mark> = emptyList(), empty: String = GameText.CONSOLE_EMPTY) {
    val c = Village.colors
    TerminalCard(PANEL_EXPLANATIONS.getValue(Panel.CONSOLE).first) {
        val lines = if (console.isEmpty() && marks.isEmpty()) listOf(ConsoleLine(empty, ConsoleLine.Kind.CUSTOMER)) else consoleTranscript(console, marks)
        Text(
            buildAnnotatedString {
                lines.takeLast(200).forEachIndexed { i, line ->
                    if (i > 0) append("\n")
                    val color = when (line.kind) {
                        ConsoleLine.Kind.OUTPUT -> c.syntaxString
                        ConsoleLine.Kind.CUSTOMER -> c.syntaxComment
                        ConsoleLine.Kind.NOTHING -> c.editorErrorText
                    }
                    withStyle(SpanStyle(color = color)) { append(line.text) }
                }
            },
            style = Village.type.codeSmall,
            modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()),
        )
    }
}

/** One Console line: something the attempt printed, a customer line, or `(nothing printed)`. */
data class ConsoleLine(val text: String, val kind: Kind) {
    enum class Kind { OUTPUT, CUSTOMER, NOTHING }
}

/** [console] split into lines, with each step's customer line before the output it caused (its [Customer.Mark]). */
fun consoleTranscript(console: String, marks: List<Customer.Mark>): List<ConsoleLine> = buildList {
    fun output(from: Int, to: Int) {
        val text = console.substring(from.coerceIn(0, console.length), to.coerceIn(0, console.length))
        text.removeSuffix("\n").takeIf { it.isNotEmpty() }?.lines()?.forEach { add(ConsoleLine(it, ConsoleLine.Kind.OUTPUT)) }
    }
    var at = 0
    for (mark in marks.sortedBy { it.consoleOffset }) {
        output(at, mark.consoleOffset)
        add(ConsoleLine(GameText.customerLine(mark.action), ConsoleLine.Kind.CUSTOMER))
        if (mark.length == 0) add(ConsoleLine(GameText.NOTHING_PRINTED, ConsoleLine.Kind.NOTHING))
        else output(mark.consoleOffset, mark.consoleOffset + mark.length)
        at = mark.consoleOffset + mark.length
    }
    output(at, console.length)
}

/** Per-composable recomposition counts as bars: the technical hook for the future heatmap. */
@Composable
fun RecompositionPanel(counts: Map<String, Int>) {
    val c = Village.colors
    TerminalCard(PANEL_EXPLANATIONS.getValue(Panel.RECOMPOSITIONS).first) {
        if (counts.isEmpty()) Text("(none yet)", style = Village.type.codeSmall, color = c.syntaxComment)
        val max = counts.values.maxOrNull() ?: 1
        for ((name, count) in counts.entries.sortedByDescending { it.value }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayName(name), style = Village.type.codeSmall, color = c.editorText, modifier = Modifier.width(150.dp), maxLines = 1)
                Box(Modifier.height(8.dp).width((120f * count / max).dp).clip(Village.shapes.chip).background(c.glow))
                Text(count.toString(), style = Village.type.codeSmall, color = c.editorText)
            }
        }
    }
}

/** `Counter.<anonymous>` -> `Counter { }`; compiler-hoisted lambdas (`ComposableSingletons$PlayerKt.lambda$-1439…`) -> `{ } lambda`. */
private fun displayName(traceName: String): String = when {
    traceName.startsWith("ComposableSingletons") -> "{ } lambda"
    traceName.endsWith(".<anonymous>") -> traceName.removeSuffix(".<anonymous>") + " { }"
    else -> traceName
}

/** What each panel does, shown when a level unlocks it. Game text, not level data. */
val PANEL_EXPLANATIONS = mapOf(
    Panel.CONSOLE to ("Console" to "The Console shows what your code prints with println. Use it to see what your code is doing."),
    Panel.RECOMPOSITIONS to ("Recompositions" to "This panel counts how often each of your composables runs. Fewer runs means a snappier screen."),
)

/**
 * Wraps a panel. While [highlight] is set (the open level unlocks it), the panel expands into view,
 * two rings pulse around it (not under reduced motion), and a "New: <panel>" tip explains it.
 */
@Composable
fun Unlockable(panel: Panel, highlight: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (!highlight) return content()
    val c = Village.colors
    val reduced = LocalReducedMotion.current
    val (title, explanation) = PANEL_EXPLANATIONS.getValue(panel)
    val appear = remember(panel) { MutableTransitionState(false).apply { targetState = true } }
    val ring = remember(panel) { Animatable(0f) }
    LaunchedEffect(panel, reduced) {
        if (reduced) return@LaunchedEffect
        repeat(2) { ring.snapTo(0f); ring.animateTo(1f, tween(600)) }
        ring.snapTo(0f)
    }
    AnimatedVisibility(appear, enter = expandVertically(tween(250)) + fadeIn(tween(250))) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.fillMaxWidth().clip(Village.shapes.row).background(c.chip).border(2.dp, c.glow, Village.shapes.row)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("New: $title", style = Village.type.title, color = c.accentStrong)
                Text(explanation, style = Village.type.small, color = c.ink)
                Box(Modifier.align(Alignment.End)) { TextAction("Got it", onClick = onDismiss, bold = true) }
            }
            Box(Modifier.drawBehind {
                val p = ring.value
                if (p <= 0f) return@drawBehind
                val spread = p * 12.dp.toPx()
                drawRoundRect(
                    c.glow.copy(alpha = 1f - p), topLeft = Offset(-spread, -spread), size = Size(size.width + 2 * spread, size.height + 2 * spread),
                    cornerRadius = CornerRadius(18.dp.toPx() + spread), style = Stroke(3.dp.toPx()),
                )
            }) { content() }
        }
    }
}

/** The preview card's width for a [viewport] (or the sandbox's default): frame + card padding. */
fun previewCardWidth(viewportWidth: Float): Dp = viewportWidth.dp + SIGN_BORDER * 2 + 34.dp
