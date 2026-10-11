package village.app.spike

import village.api.LevelEntry
import village.compiler.Diagnostic
import village.compiler.PlayerCompiler
import village.compiler.PlayerPipeline
import village.compiler.SourceFile
import kotlin.system.exitProcess

// Throwaway checks for tasks 2.x until the harness (6.x) asserts them properly: ./gradlew :app:smoke

private val helloWorld = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        Text("Hello, village!")
    }
""".trimIndent()

internal var failures = 0

internal fun check(name: String, ok: Boolean, details: () -> String) {
    println("${if (ok) "PASS" else "FAIL"} $name")
    if (!ok) { failures++; println(details().prependIndent("    ")) }
}

fun main() {
    rawCompile()
    pipeline()
    classpathVariants()
    sandbox()
    if (failures > 0) { println("$failures smoke check(s) failed"); exitProcess(1) }
}

/** Task 2.1: the embedded compiler + Compose plugin compile a hello-world composable. */
private fun rawCompile() {
    val compiler = PlayerCompiler()
    repeat(3) { run ->
        val out = compiler.compile(listOf(SourceFile("Player.kt", helloWorld)))
        // The Compose plugin rewrites composables to take (Composer, Int); without it this is ()V.
        val composed = out.classes["PlayerKt"]?.let { String(it, Charsets.ISO_8859_1) }
            ?.contains("(Landroidx/compose/runtime/Composer;I)V") == true
        check("raw compile #${run + 1} (${out.millis} ms)", out.success && composed) { out.diagnostics.joinToString("\n") }
    }
}

private fun pipeline() {
    val pipeline = PlayerPipeline()

    fun run(name: String, source: String, entry: String = "PlayerScreen", expect: (village.compiler.PipelineResult) -> Boolean) {
        val r = pipeline.run(source.trimIndent(), entry)
        check("$name (${r.timings})", expect(r)) { r.diagnostics.joinToString("\n").ifEmpty { "no diagnostics" } }
        r.diagnostics.forEach { println("    $it") }
    }
    fun contracts(r: village.compiler.PipelineResult) = r.diagnostics.filter { it.source == Diagnostic.Source.CONTRACT }

    run("entry cast to LevelEntry", helloWorld) { it.entry is LevelEntry && it.diagnostics.isEmpty() }

    run("player declares PlayerEntry and a package", """
        package bakery

        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        object PlayerEntry

        @Composable
        fun BakeryScreen() { Text("Bread") }
    """, entry = "BakeryScreen") { it.success }

    run("error at exact line/column", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen() {
            Text(greeting)
        }
    """) { r -> !r.success && r.diagnostics.single().let { it.line == 6 && it.column == 10 && it.source == Diagnostic.Source.COMPILER } }

    run("missing entry -> one contract diagnostic", """
        import androidx.compose.runtime.Composable

        @Composable
        fun SomethingElse() {}
    """) { r -> !r.success && r.diagnostics.single().source == Diagnostic.Source.CONTRACT }

    run("entry not @Composable, compiles -> contract", """
        fun PlayerScreen() {
            println("not UI")
        }
    """) { r -> !r.success && contracts(r).single().let { "@Composable" in it.message && it.line == 1 } }

    run("entry not @Composable, calls Text -> contract + compiler error", """
        import androidx.compose.material3.Text

        fun PlayerScreen() {
            Text("hi")
        }
    """) { r -> !r.success && contracts(r).single().line == 3 && r.diagnostics.any { it.source == Diagnostic.Source.COMPILER } }

    run("entry with parameters -> contract", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun PlayerScreen(name: String) { Text(name) }
    """) { r -> !r.success && r.diagnostics.single().let { it.source == Diagnostic.Source.CONTRACT && it.line == 5 && it.column == 5 } }

    run("private entry -> contract", """
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        private fun PlayerScreen() { Text("hidden") }
    """) { r -> !r.success && r.diagnostics.single().let { it.source == Diagnostic.Source.CONTRACT && "private" in it.message } }
}

/** A ~30-line composable touching state, effects, layout and Material3. */
internal val counterApp = """
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Button
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.unit.dp
    import kotlinx.coroutines.delay

    data class Bread(val name: String, val price: Int)

    @Composable
    fun PlayerScreen() {
        var count by remember { mutableStateOf(0) }
        var ticks by remember { mutableStateOf(0) }
        val breads = listOf(Bread("Baguette", 3), Bread("Rye", 4))
        LaunchedEffect(Unit) { while (true) { delay(1000); ticks++ } }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bakery", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF6B4226))
            breads.forEach { Row { Text("${'$'}{it.name}: ${'$'}{it.price} coins") } }
            Button(onClick = { count++ }) { Text("Sold ${'$'}count") }
            Text("Open for ${'$'}ticks s")
        }
    }
"""

/** Task 2.4: the trimmed Compose-only classpath compiles the same code as the host-derived one. */
private fun classpathVariants() {
    for ((name, classpath) in listOf("host" to village.compiler.PlayerClasspath.fromHost(), "trimmed" to village.compiler.PlayerClasspath.trimmed())) {
        val r = PlayerPipeline(PlayerCompiler(classpath = classpath)).run(counterApp.trimIndent())
        val warm = (1..5).map { PlayerPipeline(PlayerCompiler(classpath = classpath)).run(counterApp.trimIndent()).timings.compileMs }
        check("counter app, $name classpath (${classpath.size} jars), first ${r.timings.compileMs} ms, then $warm ms", r.success && r.diagnostics.isEmpty()) {
            r.diagnostics.joinToString("\n")
        }
    }
}
