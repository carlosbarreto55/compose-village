package village.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import village.compiler.Diagnostic
import village.compiler.Severity
import village.levels.Level
import village.levels.Verdict
import village.preview.PreviewEngine
import village.preview.PreviewState
import village.preview.RuntimeError

/** The engine's latest frame at its real pixel size; mouse input is forwarded to the player's scene. */
@Composable
fun PreviewPane(state: PreviewState, engine: PreviewEngine) {
    val shape = RoundedCornerShape(12.dp)
    // The frame's size follows the level's viewport; before the first frame, use the engine's.
    val (width, height) = with(LocalDensity.current) {
        val frame = state.frame
        if (frame != null) frame.width.toDp() to frame.height.toDp() else engine.width.toDp() to engine.height.toDp()
    }
    Box(
        Modifier.size(width, height)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .pointerInput(engine) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val forwarded = event.type == PointerEventType.Press || event.type == PointerEventType.Release ||
                            event.type == PointerEventType.Move || event.type == PointerEventType.Scroll
                        if (forwarded) {
                            engine.sendPointer(event.type, engine.toScene(change.position, size.width, size.height), change.scrollDelta)
                        }
                    }
                }
            },
    ) {
        val frame = state.frame
        if (frame != null) {
            Image(frame, contentDescription = "Your screen", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        } else if (state.error == null) {
            Text("Press Run to see your screen", Modifier.align(Alignment.Center), color = Color.Gray)
        }
    }
}

@Composable
fun DiagnosticsList(diagnostics: List<Diagnostic>, onSelect: (Diagnostic) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (diagnostics.isEmpty()) {
            Text("No problems", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        for (d in diagnostics) {
            val color = if (d.severity == Severity.ERROR) MaterialTheme.colorScheme.error else Color(0xFFB26A00)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable(enabled = d.line > 0) { onSelect(d) }.padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.padding(top = 5.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Text(
                    if (d.line > 0) "${d.line}:${d.column}" else "—",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.width(48.dp),
                )
                Text(d.source.name.lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
                Text(d.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RuntimeErrorPanel(error: RuntimeError, onSelectLine: (Int) -> Unit) {
    val title = when (error.kind) {
        RuntimeError.Kind.EXCEPTION -> "Your code crashed"
        RuntimeError.Kind.INFINITE_LOOP -> "Infinite loop?"
        RuntimeError.Kind.TOO_MUCH_MEMORY -> "Too much memory"
        RuntimeError.Kind.STACK_OVERFLOW -> "Too much recursion"
        RuntimeError.Kind.STOPPED -> "Stopped"
        RuntimeError.Kind.RESTARTED -> "Preview restarted"
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(error.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            error.line?.let { line ->
                Text(
                    "Go to line $line",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.clickable { onSelectLine(line) },
                )
            }
        }
    }
}

@Composable
fun ConsolePanel(console: String) {
    PanelCard("Console") {
        Text(
            console.ifEmpty { "(no output)" }.lines().takeLast(200).joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (console.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()),
        )
    }
}

/** Per-composable recomposition counts as bars: the technical hook for the future heatmap. */
@Composable
fun RecompositionPanel(counts: Map<String, Int>) {
    PanelCard("Recompositions") {
        if (counts.isEmpty()) {
            Text("(none yet)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val max = counts.values.maxOrNull() ?: 1
        for ((name, count) in counts.entries.sortedByDescending { it.value }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayName(name), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.width(150.dp), maxLines = 1)
                Box(Modifier.height(8.dp).width((120f * count / max).dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                Text(count.toString(), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun PanelCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

/** `000-hello-main-street` -> `Hello Main Street`. Levels have no title yet; this is spike UI. */
fun Level.displayName() = id.split('-').dropWhile { part -> part.all(Char::isDigit) }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Who asks and what they want: the level's task. */
@Composable
fun LevelRequest(level: Level) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(level.resident, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("“${level.request}”", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** The latest Run's verdict: every criterion, with the resident's message for each failed one. */
@Composable
fun VerdictPanel(level: Level, verdict: Verdict) {
    val name = level.resident.substringBefore(',')
    val solved = verdict is Verdict.Evaluated && verdict.passed
    val colors = if (solved) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    Card(Modifier.fillMaxWidth(), colors = colors) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (verdict) {
                is Verdict.NotRun -> {
                    Text("$name couldn't look at it yet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (verdict.reason) {
                            Verdict.NotRun.Reason.DID_NOT_COMPILE -> "Your code didn't compile. Fix the problems in the list, then Run again."
                            Verdict.NotRun.Reason.CRASHED -> "Your screen crashed before $name could see it."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is Verdict.Evaluated -> {
                    val passing = verdict.results.count { it.passed }
                    Text(
                        if (verdict.passed) "Solved! $name loves it." else "Not quite: $passing of ${verdict.results.size} checks pass",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    for (r in verdict.results) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (r.passed) "✓" else "✗", color = if (r.passed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                            Text(
                                r.message ?: r.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (r.passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
