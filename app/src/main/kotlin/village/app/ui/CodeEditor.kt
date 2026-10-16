package village.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import village.app.GameText
import village.app.ui.editor.KotlinHighlighting
import village.app.ui.editor.SyntaxColors
import village.app.ui.theme.Village
import village.compiler.Diagnostic
import village.compiler.Severity

/**
 * The dark editor card (spec: Code editor card): file tab, the code with a line-number gutter and
 * Kotlin colors, a status line, and the problems list. Lines never wrap (the text scrolls sideways),
 * so gutter rows and text lines have the same height and stay aligned. Lines with errors get a warm
 * red number and band.
 *
 * @param compiled whether the latest Run compiled, or null before the first Run.
 */
@Composable
fun EditorCard(
    fileName: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    diagnostics: List<Diagnostic>,
    compiled: Boolean?,
    onSelectProblem: (Diagnostic) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onStartOver: (() -> Unit)? = null,
) {
    val c = Village.colors
    val style = Village.type.code.copy(color = c.editorText)
    val errorLines = diagnostics.filter { it.severity == Severity.ERROR && it.line > 0 }.map { it.line }.toSet()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val lineCount = value.text.count { it == '\n' } + 1
    val highlighting = remember(c) {
        KotlinHighlighting(SyntaxColors(c.syntaxKeyword, c.syntaxString, c.syntaxComment, c.syntaxAnnotation, c.editorText, c.syntaxFunction))
    }

    Column(modifier.clip(Village.shapes.editor).background(c.editorBg)) {
        Row(Modifier.fillMaxWidth().divider(c.editorDivider, top = false).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(fileName, style = Village.type.codeSmall, color = c.editorText, modifier = Modifier.weight(1f))
            // Same style as the tab, so showing it doesn't change the row's height (village-progress D7).
            if (onStartOver != null) {
                Text(
                    GameText.START_OVER, style = Village.type.codeSmall, color = c.lineNumber,
                    modifier = Modifier.clip(Village.shapes.button).clickable(role = Role.Button, onClick = onStartOver).padding(horizontal = 6.dp),
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
            // One multi-line text with the editor's exact style, so gutter and code share line metrics.
            val numbers = buildAnnotatedString {
                for (line in 1..lineCount) {
                    if (line > 1) append('\n')
                    if (line in errorLines) withStyle(SpanStyle(color = c.editorErrorText, fontWeight = FontWeight.SemiBold)) { append(line.toString()) }
                    else append(line.toString())
                }
            }
            BasicText(numbers, style = style.copy(color = c.lineNumber, textAlign = TextAlign.End), modifier = Modifier.width(32.dp))
            Spacer(Modifier.width(18.dp))
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = style,
                    cursorBrush = SolidColor(c.editorText),
                    visualTransformation = highlighting,
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .widthIn(min = 560.dp)
                        .focusRequester(focusRequester)
                        .drawBehind {
                            val l = layout ?: return@drawBehind
                            for (line in errorLines) {
                                val i = line - 1
                                if (i !in 0 until l.lineCount) continue
                                val top = l.getLineTop(i)
                                drawRect(c.editorErrorBand, Offset(-18.dp.toPx(), top), Size(size.width + 18.dp.toPx(), l.getLineBottom(i) - top))
                            }
                        },
                )
            }
        }
        StatusLine(diagnostics, compiled)
        if (diagnostics.isNotEmpty()) ProblemsList(diagnostics, onSelectProblem)
    }
}

/** "No problems", "Compiles fine" or "2 problems", under the code. */
@Composable
private fun StatusLine(diagnostics: List<Diagnostic>, compiled: Boolean?) {
    val c = Village.colors
    val ok = diagnostics.isEmpty()
    val text = when {
        !ok -> if (diagnostics.size == 1) "1 problem" else "${diagnostics.size} problems"
        compiled == true -> "Compiles fine"
        else -> "No problems"
    }
    Row(
        Modifier.fillMaxWidth().divider(c.editorDivider, top = true).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val color = if (ok) c.editorOk else c.editorErrorText
        Canvas(Modifier.size(14.dp)) {
            val s = size.width / 14f
            drawCircle(color, 6f * s, style = Stroke(1.5f * s))
            val mark = Path().apply {
                if (ok) { moveTo(4.5f * s, 7f * s); lineTo(6.3f * s, 8.8f * s); lineTo(9.5f * s, 5.4f * s) }
                else { moveTo(7f * s, 4f * s); lineTo(7f * s, 7.6f * s); moveTo(7f * s, 9.6f * s); lineTo(7f * s, 10f * s) }
            }
            drawPath(mark, color, style = Stroke(1.5f * s))
        }
        Text(text, style = Village.type.caption, color = c.syntaxComment)
    }
}

/** The compile problems; clicking one moves the caret to it. */
@Composable
private fun ProblemsList(diagnostics: List<Diagnostic>, onSelect: (Diagnostic) -> Unit) {
    val c = Village.colors
    Column(Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, bottom = 10.dp)) {
        for (d in diagnostics) {
            val color = if (d.severity == Severity.ERROR) c.editorErrorText else c.syntaxKeyword
            Row(
                Modifier.fillMaxWidth().heightIn(min = 32.dp).clip(Village.shapes.small)
                    .clickable(enabled = d.line > 0) { onSelect(d) }.padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.padding(top = 6.dp).size(8.dp).clip(Village.shapes.chip).background(color))
                Text(if (d.line > 0) "${d.line}:${d.column}" else "—", style = Village.type.codeSmall, color = c.lineNumber, modifier = Modifier.width(52.dp))
                Text(d.message, style = Village.type.small, color = c.editorText)
            }
        }
    }
}

/** A 1 dp line along the top or bottom edge. */
private fun Modifier.divider(color: Color, top: Boolean) = drawBehind {
    val y = if (top) 0.5f else size.height - 0.5f
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}
