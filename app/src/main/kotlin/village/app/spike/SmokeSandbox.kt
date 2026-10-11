package village.app.spike

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import village.api.Console
import village.api.Guard
import village.api.LevelEntry
import village.api.PlayerAbort
import village.compiler.Diagnostic
import village.compiler.PlayerClassLoader
import village.compiler.PlayerCompiler
import village.compiler.PlayerPipeline
import village.compiler.SourceFile

// Throwaway checks for section 3 (sandbox) until the harness (6.5, 6.6) asserts them properly.

private const val HEADER = "import androidx.compose.runtime.Composable\n\n@Composable\nfun PlayerScreen() {\n"

/** Line 5 of the player's file is the body's first line. */
private fun screen(body: String) = HEADER + body + "\n}\n"

internal fun sandbox() {
    val pipeline = PlayerPipeline(checkInstrumented = true)

    fun rejected(name: String, source: String, line: Int, vararg mustMention: String, onlyAtLine: Boolean = true) {
        val r = pipeline.run(source)
        val sandbox = r.diagnostics.filter { it.source == Diagnostic.Source.SANDBOX }
        val ok = !r.success && sandbox.any { it.line == line } && (!onlyAtLine || sandbox.all { it.line == line }) &&
            mustMention.all { m -> sandbox.any { m in it.message } }
        check("sandbox rejects $name at line $line", ok) { r.diagnostics.joinToString("\n").ifEmpty { "no diagnostics" } }
        if (ok) println("    ${sandbox.first()}")
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
    rejected("Object.wait", screen("""    val lock = Any()
    synchronized(lock) { (lock as Object).wait() }""").replace("\n    synchronized", " ; synchronized"), 5, "wait")
    rejected("runBlocking", screen("""    kotlinx.coroutines.runBlocking { }"""), 5, "runBlocking")
    rejected("Dispatchers", screen("""    val d = kotlinx.coroutines.Dispatchers.IO"""), 5, "Dispatchers")
    rejected("LocalUriHandler", screen("""    androidx.compose.ui.platform.LocalUriHandler.current.openUri("https://example.com")"""), 5, "getLocalUriHandler")
    rejected("player class shadowing village.api.Guard", "package village.api\n\n" + HEADER.replace("@Composable\nfun", "object Guard { fun tick() {} }\n\n@Composable\nfun") + "}\n",
        5, "village.api.Guard", "reserved", onlyAtLine = false)

    // Ordinary code passes, renders inside a slice, and its println lands in the Console.
    val allowed = counterApp.trimIndent().replace(
        "var count by remember { mutableStateOf(0) }",
        "var count by remember { mutableStateOf(0) }\n        println(\"rendering \" + breads().size + \" \" + Kind.Fresh)",
    ).replace("val breads = listOf(", "val unused = listOf(") +
        "\nenum class Kind { Fresh, Stale }\nfun breads() = listOf(Bread(\"Baguette\", 3)).map { it.copy(price = when (Kind.Fresh) { Kind.Fresh -> 3; Kind.Stale -> 1 }) }\n"
    val ok = pipeline.run(allowed.replace("breads.forEach", "breads().forEach"))
    check("allowed code passes verification + CheckClassAdapter (${ok.timings})", ok.success && ok.diagnostics.isEmpty()) {
        ok.diagnostics.joinToString("\n")
    }
    ok.entry?.let { entry ->
        Console.clear()
        val rendered = runCatching { render(entry, budgetMillis = 1_000) }
        check("allowed code renders in a slice", rendered.isSuccess) { rendered.exceptionOrNull()!!.stackTraceToString() }
        check("println captured in Console", "rendering 1 Fresh" in Console.text()) { "console: '${Console.text()}'" }
    }

    // Instrumentation: an infinite loop in composition is aborted by its tick.
    val loop = pipeline.run(screen("    var i = 0\n    while (true) { i++ }"))
    loop.entry?.let { entry ->
        val start = System.nanoTime()
        val error = runCatching { render(entry, budgetMillis = 200) }.exceptionOrNull()
        val ms = (System.nanoTime() - start) / 1_000_000
        val abort = generateSequence(error) { it.cause }.firstOrNull { it is PlayerAbort } as PlayerAbort?
        check("while(true) aborted by Guard after $ms ms (budget 200)", abort?.kind == PlayerAbort.Kind.TIME && ms < 1_000) {
            error?.stackTraceToString() ?: "no exception"
        }
    } ?: check("while(true) compiles", false) { loop.diagnostics.joinToString("\n") }

    loaderBlocksWhatTheGuardMissed()
}

private fun render(entry: LevelEntry, budgetMillis: Long) {
    Guard.timeBudgetMillis = budgetMillis
    try {
        // The scene composes in its constructor, so it must be created inside the slice too.
        Guard.slice {
            val scene = ImageComposeScene(320, 480, Density(1f)) { entry.content() }
            try {
                scene.render()
            } finally {
                scene.close()
            }
        }
    } finally {
        Guard.timeBudgetMillis = 1_000
    }
}

/** Defense in depth: with bytecode verification skipped, the class loader alone refuses java.io.File. */
private fun loaderBlocksWhatTheGuardMissed() {
    val source = "class Sneaky : Runnable { override fun run() { java.io.File(\"x\").exists() } }"
    val out = PlayerCompiler().compile(listOf(SourceFile("Player.kt", source)))
    val loader = PlayerClassLoader(out.classes, LevelEntry::class.java.classLoader)
    val error = runCatching {
        (loader.loadClass("Sneaky").getDeclaredConstructor().newInstance() as Runnable).run()
    }.exceptionOrNull()
    check("class loader refuses java.io.File without bytecode check", error is NoClassDefFoundError && "Sandbox" in (error.cause?.message ?: "")) {
        error?.stackTraceToString() ?: "no error: File was linked"
    }
}
