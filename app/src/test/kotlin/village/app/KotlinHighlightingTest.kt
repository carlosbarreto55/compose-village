package village.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import village.app.ui.editor.KotlinHighlighting
import village.app.ui.editor.SyntaxColors
import village.app.ui.editor.TokenKind
import village.app.ui.editor.kotlinTokens
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinHighlightingTest {
    private fun spans(code: String) = kotlinTokens(code).map { code.substring(it.start, it.end) to it.kind }

    @Test fun openSignSolution() {
        val code = """
            @Composable
            fun PlayerScreen() {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tom's Flowers") // the name
                    Button(onClick = { println("New order!") }) { Text("Order") }
                }
            }
        """.trimIndent()
        assertEquals(
            listOf(
                "@Composable" to TokenKind.ANNOTATION, "fun" to TokenKind.KEYWORD, "PlayerScreen" to TokenKind.FUNCTION,
                "Row" to TokenKind.FUNCTION, "padding" to TokenKind.FUNCTION, "16" to TokenKind.NUMBER,
                "Text" to TokenKind.FUNCTION, "\"Tom's Flowers\"" to TokenKind.STRING, "// the name" to TokenKind.COMMENT,
                "Button" to TokenKind.FUNCTION, "println" to TokenKind.FUNCTION, "\"New order!\"" to TokenKind.STRING,
                "Text" to TokenKind.FUNCTION, "\"Order\"" to TokenKind.STRING,
            ),
            spans(code),
        )
    }

    @Test fun templatesAreNotColoredAsString() {
        assertEquals(
            listOf("Text" to TokenKind.FUNCTION, "\"Loaves: " to TokenKind.STRING, " and " to TokenKind.STRING, "\"" to TokenKind.STRING),
            spans("Text(\"Loaves: \$count and \${total + 1}\")"),
        )
    }

    @Test fun numbersBlockCommentsAndUnterminatedStrings() {
        assertEquals(
            listOf(
                "Color" to TokenKind.FUNCTION, "0xFFFFF3E0" to TokenKind.NUMBER, "/* sign */" to TokenKind.COMMENT, "1.5f" to TokenKind.NUMBER,
                "Text" to TokenKind.FUNCTION, "\"open" to TokenKind.STRING,
            ),
            spans("Color(0xFFFFF3E0) /* sign */ x = 1.5f\nText(\"open\nfoo"),
        )
    }

    @Test fun keywordsAfterADotAreNames() = assertEquals(emptyList(), spans("state.value.data"))

    @Test fun offsetsAreIdentity() {
        val colors = SyntaxColors(Color.Red, Color.Green, Color.Gray, Color.Magenta, Color.Cyan, Color.Blue)
        val code = "fun PlayerScreen() {\n    Text(\"x\")\n}"
        val out = KotlinHighlighting(colors).filter(AnnotatedString(code))
        assertEquals(code, out.text.text)
        for (offset in 0..code.length) {
            assertEquals(offset, out.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, out.offsetMapping.transformedToOriginal(offset))
        }
    }
}
