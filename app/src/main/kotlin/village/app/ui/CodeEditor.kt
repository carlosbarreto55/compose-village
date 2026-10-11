package village.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A monospace editor with a line-number gutter. Lines with errors get a red number and a red
 * band behind the text. Lines never wrap (the text scrolls sideways), so gutter rows and text
 * lines have the same height and stay aligned.
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    errorLines: Set<Int>,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, color = colors.onSurface)
    val errorBand = colors.error.copy(alpha = 0.14f)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val lineCount = value.text.count { it == '\n' } + 1

    Surface(modifier, shape = RoundedCornerShape(8.dp), color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            // One multi-line text with the editor's exact style, so gutter and code share line metrics.
            val numbers = buildAnnotatedString {
                for (line in 1..lineCount) {
                    if (line > 1) append('\n')
                    if (line in errorLines) {
                        withStyle(SpanStyle(color = colors.error, fontWeight = FontWeight.Bold)) { append(line.toString()) }
                    } else {
                        append(line.toString())
                    }
                }
            }
            BasicText(
                numbers,
                style = style.copy(color = colors.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.End),
                modifier = Modifier.width(48.dp).padding(end = 10.dp),
            )
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = style,
                    cursorBrush = SolidColor(colors.primary),
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .widthIn(min = 640.dp)
                        .focusRequester(focusRequester)
                        .drawBehind {
                            val l = layout ?: return@drawBehind
                            for (line in errorLines) {
                                val i = line - 1
                                if (i !in 0 until l.lineCount) continue
                                val top = l.getLineTop(i)
                                drawRect(errorBand, Offset(0f, top), Size(size.width, l.getLineBottom(i) - top))
                            }
                        },
                )
            }
        }
    }
}
