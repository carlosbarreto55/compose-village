package village.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import village.app.SpikeController
import village.levels.Panel
import village.compiler.Severity

/** The spike window: code editor and diagnostics on the left, the live preview and its panels on the right. */
@Composable
fun SpikeScreen(controller: SpikeController) {
    val editorFocus = remember { FocusRequester() }
    val preview by controller.engine.state.collectAsState()

    fun moveCaret(line: Int, column: Int) {
        val code = controller.code
        controller.code = code.copy(selection = TextRange(offsetOf(code.text, line, column)))
        runCatching { editorFocus.requestFocus() }
    }

    Row(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { e ->
                val runShortcut = e.type == KeyEventType.KeyDown && e.key == Key.Enter && (e.isMetaPressed || e.isCtrlPressed)
                if (runShortcut) controller.run()
                runShortcut
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Toolbar(controller, onRun = controller::run)
            controller.level?.let { LevelRequest(it) }
            val errorLines = controller.diagnostics.filter { it.severity == Severity.ERROR && it.line > 0 }.map { it.line }.toSet()
            CodeEditor(controller.code, { controller.code = it }, errorLines, editorFocus, Modifier.weight(1f).fillMaxWidth())
            DiagnosticsList(controller.diagnostics, onSelect = { moveCaret(it.line, it.column) }, Modifier.fillMaxWidth().heightIn(max = 160.dp))
        }
        val level = controller.level
        val target = controller.target
        val viewportWidth = level?.viewport?.widthDp ?: SpikeController.PREVIEW_WIDTH_DP.toFloat()
        Column(
            Modifier.width((maxOf(SpikeController.PREVIEW_WIDTH_DP.toFloat(), viewportWidth) + 24).dp)
                .fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (level != null && target != null) TargetSwitch(controller.showTarget, onChange = { controller.showTarget = it })
            PreviewPane(
                preview, controller.engine, viewport = level?.viewport,
                target = target, ghostCaption = level?.let { "${it.resident.substringBefore(',')}'s sketch: press Run to build it" }.orEmpty(),
                showTarget = controller.showTarget,
            )
            level?.let { Checklist(it, controller.verdict, hasNext = controller.nextLevel != null, onContinue = controller::continueToNext) }
            preview.error?.let { RuntimeErrorPanel(it, onSelectLine = { line -> moveCaret(line, 1) }) }
            if (controller.shows(Panel.CONSOLE)) {
                Unlockable(Panel.CONSOLE, Panel.CONSOLE in controller.highlighted, { controller.dismissHighlight(Panel.CONSOLE) }) {
                    ConsolePanel(preview.console)
                }
            }
            if (controller.shows(Panel.RECOMPOSITIONS)) {
                Unlockable(Panel.RECOMPOSITIONS, Panel.RECOMPOSITIONS in controller.highlighted, { controller.dismissHighlight(Panel.RECOMPOSITIONS) }) {
                    RecompositionPanel(preview.recompositions)
                }
            }
        }
    }
}

@Composable
private fun Toolbar(controller: SpikeController, onRun: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(selected = controller.level == null, onClick = { controller.open(null) }, label = { Text("Sandbox") }, enabled = !controller.running)
        for (level in controller.levels) {
            FilterChip(selected = controller.level == level, onClick = { controller.open(level) }, label = { Text(level.displayName()) }, enabled = !controller.running)
        }
        Spacer(Modifier.width(4.dp))
        Button(onClick = onRun, enabled = !controller.running) { Text("Run") }
        Text("⌘↩", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        when {
            controller.running || controller.warmingUp -> {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    if (controller.warmingUp) "Compiler warming up…" else "Compiling…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> controller.timings?.let { t ->
                Text(
                    "compile ${t.compileMs} ms · guard ${t.guardMs} ms · load ${t.loadMs} ms",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } ?: controller.warmUpMillis?.let {
                Text("Compiler ready (warm-up $it ms)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(0.dp))
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
