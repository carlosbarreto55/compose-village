package village.app.spike

import village.api.LevelEntry
import village.compiler.Diagnostic
import village.compiler.PlayerClassLoader
import village.compiler.PlayerCompiler
import village.compiler.PlayerPipeline
import village.compiler.SourceFile
import village.preview.PreviewEngine
import java.util.concurrent.TimeUnit

private const val HEADER = "import androidx.compose.runtime.Composable\n\n@Composable\nfun PlayerScreen() {\n"

/** A player file whose body starts on line 5. */
private fun screen(body: String) = HEADER + body + "\n}\n"

/**
 * Task 6.6: each attack class is rejected before loading, with a SANDBOX diagnostic at the
 * offending line; ordinary code passes and runs; the class loader holds on its own.
 *
 * [pipeline] must verify instrumented bytecode (`checkInstrumented = true`).
 */
internal fun sandbox(report: Report, pipeline: PlayerPipeline) = report.section("Sandbox", Criterion.ERROR_LINES) {
    val rows = mutableListOf<List<Any?>>()

    fun rejected(name: String, source: String, line: Int, vararg mustMention: String, onlyAtLine: Boolean = true) {
        val r = pipeline.run(source)
        val sandbox = r.diagnostics.filter { it.source == Diagnostic.Source.SANDBOX }
        val ok = !r.success && sandbox.any { it.line == line } && (!onlyAtLine || sandbox.all { it.line == line }) &&
            mustMention.all { m -> sandbox.any { m in it.message } }
        rows += listOf(name, sandbox.joinToString(", ") { "${it.line}" }, sandbox.firstOrNull()?.message)
        check("rejects $name at line $line", ok, Criterion.ERROR_LINES) { r.diagnostics.joinToString("\n").ifEmpty { "no diagnostics" } }
    }

    rejected("java.io.File", screen("""    val text = java.io.File("secret.txt").readText()"""), 5, "java.io.File", "files")
    rejected("java.nio.file", screen("""    java.nio.file.Files.readAllBytes(java.nio.file.Path.of("x"))"""), 5, "files")
    rejected("java.net.URL", screen("""    val u = java.net.URL("https://example.com")"""), 5, "network")
    rejected("Class.forName", screen("""    Class.forName("java.lang.Runtime")"""), 5, "Class.forName")
    rejected("java.lang.reflect", screen("""    String::class.java.declaredMethods.forEach { it.invoke(null) }"""), 5, "reflection")
    rejected("Runtime.exec", screen("""    Runtime.getRuntime().exec("ls")"""), 5, "process")
    rejected("System.exit", screen("""    System.exit(0)"""), 5, "System.exit")
    rejected("exitProcess (inlined)", screen("""    kotlin.system.exitProcess(1)"""), 5, "System.exit")
    rejected("Thread.sleep", screen("""    Thread.sleep(10)"""), 5, "Thread")
    rejected("File::readText reference", screen("""    val read: (java.io.File) -> String = java.io.File::readText"""), 5, "files")
    rejected("Object.wait", screen("""    val lock = Any(); synchronized(lock) { (lock as Object).wait() }"""), 5, "wait")
    rejected("runBlocking", screen("""    kotlinx.coroutines.runBlocking { }"""), 5, "runBlocking")
    rejected("Dispatchers.IO", screen("""    val d = kotlinx.coroutines.Dispatchers.IO"""), 5, "Dispatchers")
    rejected("LocalUriHandler", screen("""    androidx.compose.ui.platform.LocalUriHandler.current.openUri("https://example.com")"""), 5, "getLocalUriHandler")
    rejected(
        "player class shadowing village.api.Guard",
        "package village.api\n\n" + HEADER.replace("@Composable\nfun", "object Guard { fun tick() {} }\n\n@Composable\nfun") + "}\n",
        5, "village.api.Guard", "reserved", onlyAtLine = false,
    )

    // Ordinary code passes verification (and CheckClassAdapter), runs, and its println lands in the console.
    val ok = pipeline.run(allowedCode)
    check("allowed code passes verification and CheckClassAdapter (${ok.timings})", ok.success && ok.diagnostics.isEmpty(), Criterion.ERROR_LINES) {
        ok.diagnostics.joinToString("\n")
    }
    ok.entry?.let { entry ->
        PreviewEngine().use { engine ->
            val first = engine.load(entry, allowedCode.lines().size, ok.loader).get(30, TimeUnit.SECONDS)
            engine.click("Sold 0").get(5, TimeUnit.SECONDS)
            val after = engine.renderNow().get(5, TimeUnit.SECONDS)
            check("allowed code renders, handles a click and its println is captured", first.error == null && after.error == null &&
                after.frameCount > first.frameCount && "menu: Baguette=3, Rye=5 Fresh" in after.console) {
                "console '${after.console.trim()}'; ${describe(after)}"
            }
        }
    }

    // Defense in depth: with bytecode verification skipped, the class loader alone refuses java.io.File.
    val sneaky = PlayerCompiler().compile(listOf(SourceFile("Player.kt", "class Sneaky : Runnable { override fun run() { java.io.File(\"x\").exists() } }")))
    val loader = PlayerClassLoader(sneaky.classes, LevelEntry::class.java.classLoader)
    val error = runCatching { (loader.loadClass("Sneaky").getDeclaredConstructor().newInstance() as Runnable).run() }.exceptionOrNull()
    check("class loader refuses java.io.File without the bytecode check", error is NoClassDefFoundError && "Sandbox" in (error.cause?.message ?: "")) {
        error?.stackTraceToString() ?: "no error: File was linked"
    }

    table(listOf("Attack", "Reported at line(s)", "First message shown"), rows)
}
