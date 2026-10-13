package village.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import village.app.SpikeController
import village.app.countCodeLines
import village.app.expressionFor
import village.app.reactionFor
import village.app.ui.theme.Village
import village.app.ui.theme.VillageShapes
import village.app.GameText
import village.levels.Action
import village.levels.Panel
import village.app.ui.villagers.BigDialogueOverlay
import village.app.ui.theme.LocalReducedMotion
import village.app.BigDialogue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box

/** Below this content width the right column moves under the left one (design D5). */
val TWO_COLUMNS_MIN_WIDTH = 1180.dp

/** The level screen (spec: Screen layout): header, the villager and editor beside the preview and its cards, the street. */
@Composable
fun SpikeScreen(controller: SpikeController) {
    val editorFocus = remember { FocusRequester() }

    fun moveCaret(line: Int, column: Int) {
        val code = controller.code
        controller.code = code.copy(selection = TextRange(offsetOf(code.text, line, column)))
        runCatching { editorFocus.requestFocus() }
    }

    // One transition drives the level-complete moment: the sign's glow and the completion card (design D11).
    // A first completion's celebration plays first; the moment starts when it closes (talking-villagers D3).
    val celebrating = controller.bigDialogue?.kind == BigDialogue.Kind.CELEBRATION
    val completion = updateTransition(controller.complete && !celebrating, "complete")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .background(Village.colors.ground)
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown) controller.noteInteraction()
                    val runShortcut = e.type == KeyEventType.KeyDown && e.key == Key.Enter && (e.isMetaPressed || e.isCtrlPressed)
                    if (runShortcut) controller.run()
                    runShortcut
                }
                .pointerInput(controller) {
                    awaitPointerEventScope {
                        while (true) {
                            if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) controller.noteInteraction()
                        }
                    }
                },
        ) {
            Header(controller)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val padding = Modifier.padding(horizontal = VillageShapes.pagePaddingH, vertical = VillageShapes.pagePaddingV)
                val rightWidth = maxOf(460.dp, previewCardWidth(controller.level?.viewport?.widthDp ?: SpikeController.PREVIEW_WIDTH_DP.toFloat()))
                if (maxWidth >= TWO_COLUMNS_MIN_WIDTH) {
                    Row(padding.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(VillageShapes.columnGap)) {
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(VillageShapes.cardGap)) {
                            LeftColumn(controller, editorFocus, ::moveCaret, editorHeight = null)
                        }
                        Column(
                            Modifier.width(rightWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(VillageShapes.cardGap),
                        ) {
                            RightColumn(controller, completion, ::moveCaret)
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).then(padding),
                        verticalArrangement = Arrangement.spacedBy(VillageShapes.cardGap),
                    ) {
                        LeftColumn(controller, editorFocus, ::moveCaret, editorHeight = 460.dp)
                        RightColumn(controller, completion, ::moveCaret)
                    }
                }
            }
            StreetFooter(controller)
        }
        DialogueLayer(controller, onClosed = { runCatching { editorFocus.requestFocus() } })
    }
}

/** The big dialogue over the whole screen: the scrim fades while the bust and box slide up 24 dp (spec: Big dialogue). */
@Composable
private fun DialogueLayer(controller: SpikeController, onClosed: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val level = controller.level
    val dialogue = controller.bigDialogue
    // Keep the last dialogue while it fades out.
    var shown by remember { mutableStateOf(dialogue) }
    if (dialogue != null) shown = dialogue
    val slide = with(LocalDensity.current) { 24.dp.roundToPx() }
    AnimatedVisibility(dialogue != null && level != null, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
        val last = shown ?: return@AnimatedVisibility
        val content = if (reduced) Modifier else Modifier.animateEnterExit(enter = slideInVertically(tween(300)) { slide }, exit = ExitTransition.None)
        if (level != null) {
            BigDialogueOverlay(
                level.villager, last, level.shop, contentModifier = content,
                onClose = { controller.closeDialogue(); onClosed() },
            )
        }
    }
}

/** The villager card above the editor; the editor fills the rest, or [editorHeight] when the page scrolls. */
@Composable
private fun ColumnScope.LeftColumn(controller: SpikeController, editorFocus: FocusRequester, moveCaret: (Int, Int) -> Unit, editorHeight: Dp?) {
    val level = controller.level
    level?.let {
        VillagerCard(
            it, reactionFor(it, controller.verdict), expressionFor(controller.verdict), controller.runCount,
            onTalk = if (controller.running) null else controller::talk,
        )
    }
    EditorCard(
        fileName = "${level?.entryFunction ?: "PlayerScreen"}.kt",
        value = controller.code, onValueChange = { controller.code = it },
        diagnostics = controller.diagnostics, compiled = controller.lastRunCompiled,
        onSelectProblem = { moveCaret(it.line, it.column) },
        focusRequester = editorFocus,
        onStartOver = if (controller.canStartOver && !controller.running) controller::startOver else null,
        modifier = Modifier.fillMaxWidth().then(if (editorHeight == null) Modifier.weight(1f) else Modifier.height(editorHeight)),
    )
}

/** The preview card, then the completion card, checklist, runtime error and the level's panels. */
@Composable
private fun RightColumn(controller: SpikeController, completion: Transition<Boolean>, moveCaret: (Int, Int) -> Unit) {
    val preview by controller.engine.state.collectAsState()
    val level = controller.level
    val glow by completion.animateFloat({ tween(600) }, label = "glow") { if (it) 1f else 0f }
    PreviewCard(
        preview, controller.engine, viewport = level?.viewport,
        villagerName = level?.villager?.name, target = controller.target,
        showTarget = controller.showTarget, onShowTarget = { controller.showTarget = it },
        overlay = controller.overlaySketch, onOverlay = controller::toggleOverlay,
        building = controller.running, complete = controller.complete, glow = glow,
        customerCaption = controller.customerCaption, onPlayerPress = controller::notePreviewInput,
    )
    if (level != null) {
        CompletionCard(
            completion, controller.dismissedCompletion, level,
            lines = countCodeLines(controller.code.text), tries = controller.runsThisVisit,
            next = controller.nextLevel?.let { numbered(controller.levels, it) },
            onNext = controller::continueToNext, onKeepTinkering = controller::keepTinkering,
            welcomeBack = if (controller.revisiting) controller.progress[level.id] else null, onPlayAgain = controller::startOver,
        )
        ChecklistCard(level, controller.verdict, reactionFor(level, controller.verdict), controller.runCount)
    }
    preview.error?.let { RuntimeErrorPanel(it, onSelectLine = { line -> moveCaret(line, 1) }) }
    for (panel in Panel.entries) {
        if (!controller.shows(panel)) continue
        Unlockable(panel, panel in controller.highlighted, { controller.dismissHighlight(panel) }) {
            when (panel) {
                Panel.CONSOLE -> ConsolePanel(
                    preview.console, controller.customerMarks,
                    empty = if (controller.level?.actions?.any { it is Action.Press } == true) GameText.CONSOLE_EMPTY_CUSTOMER else GameText.CONSOLE_EMPTY,
                )
                Panel.RECOMPOSITIONS -> RecompositionPanel(preview.recompositions)
            }
        }
    }
}

/** Character offset of 1-based [line] and [column] in [text], clamped to the text. */
internal fun offsetOf(text: String, line: Int, column: Int): Int {
    var offset = 0
    repeat((line - 1).coerceAtLeast(0)) {
        val next = text.indexOf('\n', offset)
        if (next < 0) return text.length
        offset = next + 1
    }
    val lineEnd = text.indexOf('\n', offset).let { if (it < 0) text.length else it }
    return (offset + column - 1).coerceIn(offset, lineEnd)
}
