package village.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** What a colored span of code is (design D10). Text that is none of these keeps the editor's color. */
enum class TokenKind { KEYWORD, STRING, COMMENT, ANNOTATION, NUMBER, FUNCTION }

/** [kind] covers `text[start until end]`. */
data class Token(val start: Int, val end: Int, val kind: TokenKind)

private val KEYWORDS = setOf(
    "as", "break", "by", "catch", "class", "companion", "const", "continue", "data", "do", "else", "enum", "false", "finally",
    "for", "fun", "if", "import", "in", "inline", "interface", "internal", "is", "lateinit", "null", "object", "out", "override",
    "package", "private", "public", "return", "sealed", "super", "suspend", "this", "throw", "true", "try", "typealias", "val",
    "var", "vararg", "when", "while",
)

/**
 * Splits Kotlin source into colored tokens: a small hand-written lexer, enough for player files. Call
 * names are identifiers followed by `(` or `{`. String templates (`$x`, `${x}`) are left uncolored.
 */
fun kotlinTokens(text: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val n = text.length
    fun at(k: Int) = if (k < n) text[k] else '\u0000'
    fun string(quote: String) {
        // From just after the opening quote to just after the closing one; templates split the token.
        var start = i
        i += quote.length
        while (i < n) {
            when {
                text.startsWith(quote, i) -> { i += quote.length; break }
                quote == "\"" && text[i] == '\n' -> break
                quote == "\"" && text[i] == '\\' -> i += 2
                text[i] == '$' && at(i + 1) == '{' -> {
                    tokens += Token(start, i, TokenKind.STRING)
                    var depth = 0
                    while (i < n) {
                        if (text[i] == '{') depth++
                        if (text[i] == '}' && --depth == 0) { i++; break }
                        i++
                    }
                    start = i
                }
                text[i] == '$' && (at(i + 1).isLetter() || at(i + 1) == '_') -> {
                    tokens += Token(start, i, TokenKind.STRING)
                    i++
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    start = i
                }
                else -> i++
            }
        }
        i = i.coerceAtMost(n)
        if (i > start) tokens += Token(start, i, TokenKind.STRING)
    }
    while (i < n) {
        val ch = text[i]
        when {
            text.startsWith("//", i) -> {
                val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                tokens += Token(i, end, TokenKind.COMMENT); i = end
            }
            text.startsWith("/*", i) -> {
                val end = text.indexOf("*/", i + 2).let { if (it < 0) n else it + 2 }
                tokens += Token(i, end, TokenKind.COMMENT); i = end
            }
            text.startsWith("\"\"\"", i) -> string("\"\"\"")
            ch == '"' -> string("\"")
            ch == '\'' -> {
                val end = when {
                    at(i + 1) == '\\' -> text.indexOf('\'', i + 2).let { if (it < 0 || it - i > 8) -1 else it + 1 }
                    at(i + 2) == '\'' -> i + 3
                    else -> -1
                }
                if (end > 0) { tokens += Token(i, end, TokenKind.STRING); i = end } else i++
            }
            ch == '@' && (at(i + 1).isLetter()) -> {
                var end = i + 1
                while (end < n && (text[end].isLetterOrDigit() || text[end] == '_')) end++
                tokens += Token(i, end, TokenKind.ANNOTATION); i = end
            }
            ch.isDigit() -> {
                var end = i + 1
                if (ch == '0' && (at(i + 1) == 'x' || at(i + 1) == 'X')) {
                    end = i + 2
                    while (end < n && (text[end].isLetterOrDigit() || text[end] == '_')) end++
                } else {
                    while (end < n && (text[end].isDigit() || text[end] == '_')) end++
                    if (at(end) == '.' && at(end + 1).isDigit()) { end++; while (end < n && text[end].isDigit()) end++ }
                    if (at(end) == 'e' || at(end) == 'E') {
                        var k = end + 1
                        if (at(k) == '+' || at(k) == '-') k++
                        if (at(k).isDigit()) { end = k; while (end < n && text[end].isDigit()) end++ }
                    }
                    while (at(end) in "fFlLuU") end++
                }
                tokens += Token(i, end, TokenKind.NUMBER); i = end
            }
            ch.isLetter() || ch == '_' -> {
                var end = i + 1
                while (end < n && (text[end].isLetterOrDigit() || text[end] == '_')) end++
                val word = text.substring(i, end)
                var next = end
                while (next < n && (text[next] == ' ' || text[next] == '\t')) next++
                val prevIsDot = i > 0 && text[i - 1] == '.'
                when {
                    word in KEYWORDS && !prevIsDot -> tokens += Token(i, end, TokenKind.KEYWORD)
                    at(next) == '(' || at(next) == '{' -> tokens += Token(i, end, TokenKind.FUNCTION)
                }
                i = end
            }
            else -> i++
        }
    }
    return tokens
}

/** The editor's syntax colors, one per [TokenKind]. */
class SyntaxColors(val keyword: Color, val string: Color, val comment: Color, val annotation: Color, val number: Color, val function: Color) {
    fun of(kind: TokenKind) = when (kind) {
        TokenKind.KEYWORD -> keyword
        TokenKind.STRING -> string
        TokenKind.COMMENT -> comment
        TokenKind.ANNOTATION -> annotation
        TokenKind.NUMBER -> number
        TokenKind.FUNCTION -> function
    }
}

/** Colors the code without changing it: identity offsets, so the caret and problem columns stay exact. */
class KotlinHighlighting(private val colors: SyntaxColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = AnnotatedString.Builder(text)
        for (t in kotlinTokens(text.text)) styled.addStyle(SpanStyle(color = colors.of(t.kind)), t.start, t.end)
        return TransformedText(styled.toAnnotatedString(), OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is KotlinHighlighting && other.colors === colors
    override fun hashCode() = colors.hashCode()
}
