package village.app.spike

import village.api.LevelEntry
import village.compiler.Diagnostic
import village.compiler.PipelineResult
import village.compiler.PlayerPipeline
import village.compiler.Severity

/**
 * Task 6.3: known-bad sources must produce diagnostics at exact line/column in the player's file,
 * and entry-function mistakes must produce one CONTRACT diagnostic instead of a wrapper error.
 */
internal fun diagnostics(report: Report, pipeline: PlayerPipeline) = report.section("Diagnostics", Criterion.ERROR_LINES) {
    /** A diagnostic the check expects, matched on severity, source, position and a message fragment. */
    data class Expect(val line: Int, val column: Int, val source: Diagnostic.Source = Diagnostic.Source.COMPILER, val mentions: String = "", val severity: Severity = Severity.ERROR)

    val rows = mutableListOf<List<Any?>>()

    /** Passes when the diagnostics are exactly [expected] (warnings included) and success matches [compiles]. */
    fun expect(name: String, source: String, vararg expected: Expect, entry: String = "PlayerScreen", compiles: Boolean = false, extra: (PipelineResult) -> Boolean = { true }) {
        val r = pipeline.run(source.trimIndent(), entry)
        val matches = r.diagnostics.size == expected.size && r.diagnostics.zip(expected).all { (d, e) ->
            d.line == e.line && d.column == e.column && d.source == e.source && d.severity == e.severity && e.mentions in d.message
        }
        val ok = matches && r.success == compiles && extra(r)
        r.diagnostics.forEach { rows += listOf(name, "${it.line}:${it.column}-${it.endLine}:${it.endColumn}", it.source, it.severity, it.message) }
        check(name, ok, Criterion.ERROR_LINES) {
            "expected ${expected.joinToString { "${it.line}:${it.column} ${it.source}" }.ifEmpty { "none" }}, got " +
                r.diagnostics.joinToString { "${it.line}:${it.column} ${it.source} ${it.severity}: ${it.message.lines().first()}" }.ifEmpty { "none" }
        }
    }

    expect("hello world compiles and casts to LevelEntry", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text("Hello, village!")
        }
    """, compiles = true) { it.entry is LevelEntry }

    expect("player declares PlayerEntry and a package", """
        package bakery

        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        object PlayerEntry

        @Composable
        fun BakeryScreen() { Text("Bread") }
    """, entry = "BakeryScreen", compiles = true)

    expect("unresolved reference at 12:9", """
        import androidx.compose.foundation.layout.Column
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            val loaves = 3
            Column {
                Text("Loaves: ${'$'}loaves")
                Text("Price: 2 coins")
                // the baker's helper
                Txet("Open")
            }
        }
    """, Expect(12, 9, mentions = "Unresolved reference"))

    // K2 reports an initializer mismatch on the `=`, not on the value.
    expect("type mismatch at the initializer's `=`", """
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            val price: Int = "two"
        }
    """, Expect(5, 20, mentions = "Initializer type mismatch"))

    expect("wrong argument type at the argument", """
        import androidx.compose.runtime.Composable

        fun price(coins: Int) = coins

        @Composable
        fun PlayerScreen() {
            price("two")
        }
    """, Expect(7, 11, mentions = "Argument type mismatch"))

    // With overloads (String and AnnotatedString) K2 reports at the callee and lists every candidate.
    expect("wrong argument type to overloaded Text at the callee", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text(42)
        }
    """, Expect(6, 5, mentions = "None of the following candidates is applicable"))

    expect("composable call outside a composable context (Compose plugin error)", """
        import androidx.compose.material3.Button
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Button(onClick = { Text("sold") }) { Text("Sell") }
        }
    """, Expect(7, 24, mentions = "@Composable invocations"))

    expect("two errors, both located", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text(bread)
            val n: Int = 1
            n.bake()
        }
    """, Expect(6, 10, mentions = "bread"), Expect(8, 7, mentions = "bake"))

    expect("syntax error: missing closing parenthesis", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text("Hello"
        }
    """, Expect(6, 17, mentions = "Expecting ')'"))

    expect("errors in a file with a package keep their lines", """
        package bakery.counter

        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text(price)
        }
    """, Expect(8, 10, mentions = "price"))

    // Unused-variable warnings come from K2's extended checkers, which the CLI leaves off.
    expect("warning passes through and the attempt still runs", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            val name = "Rye"
            Text(name!!)
        }
    """, Expect(7, 14, severity = Severity.WARNING, mentions = "!!"), compiles = true)

    // ---- the level's entry contract ----
    val contract = Diagnostic.Source.CONTRACT

    expect("missing entry -> one contract diagnostic", """
        import androidx.compose.runtime.Composable

        @Composable
        fun SomethingElse() {}
    """, Expect(1, 1, contract, "@Composable fun PlayerScreen()"))

    expect("entry not @Composable (compiles) -> contract at the declaration", """
        fun PlayerScreen() {
            println("not UI")
        }
    """, Expect(1, 5, contract, "must be annotated `@Composable`"))

    expect("entry not @Composable (calls Text) -> contract + compiler error", """
        import androidx.compose.material3.Text

        fun PlayerScreen() {
            Text("hi")
        }
    """, Expect(3, 5, contract, "must be annotated `@Composable`"), Expect(3, 5, mentions = "must be marked with the @Composable annotation"),
        Expect(4, 5, mentions = "@Composable invocations"))

    expect("entry with parameters -> contract at the declaration", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen(name: String) { Text(name) }
    """, Expect(5, 5, contract, "with no arguments"))

    expect("private entry -> contract", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        private fun PlayerScreen() { Text("hidden") }
    """, Expect(5, 13, contract, "must not be private"))

    expect("level with another entry name", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() { Text("wrong screen") }
    """, Expect(1, 1, contract, "@Composable fun BakeryScreen()"), entry = "BakeryScreen")

    text("Every diagnostic the checks produced, as the editor shows it:")
    table(listOf("Check", "Range", "Source", "Severity", "Message"), rows)
}
