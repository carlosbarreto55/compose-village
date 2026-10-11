package village.app.spike

import village.compiler.PlayerClasspath
import village.compiler.PlayerPipeline
import java.io.File
import kotlin.system.exitProcess

// The headless spike harness: ./gradlew :app:spike [-Pargs="--only latency,memory --runs 30 --cycles 150"]
// Writes docs/spike/results.md and CSVs next to it (docs/spike/report.md is the written report); exits 1 if any check fails.

internal class Options(
    /** Sections to run, or null for all. */
    val only: Set<String>?,
    /** Warm compiles per latency variant. */
    val runs: Int,
    /** Reload cycles in the memory test. */
    val cycles: Int,
    val outDir: File,
) {
    companion object {
        fun parse(args: Array<String>): Options {
            val map = args.toList().chunked(2).associate { it[0].removePrefix("--") to it.getOrElse(1) { "" } }
            val unknown = map.keys - setOf("only", "runs", "cycles", "out")
            require(unknown.isEmpty()) { "unknown option(s) $unknown; known: --only <a,b>, --runs <n>, --cycles <n>, --out <dir>" }
            val only = map["only"]?.split(",")?.map { it.trim() }?.toSet()
            only?.let { require(SECTIONS.containsAll(it)) { "unknown section(s) ${it - SECTIONS}; known: $SECTIONS" } }
            return Options(
                only = only,
                runs = map["runs"]?.toInt() ?: 30,
                cycles = map["cycles"]?.toInt() ?: 150,
                // A partial run must not overwrite the full report.
                outDir = File(map["out"] ?: if (only == null) "docs/spike" else "app/build/spike"),
            )
        }
    }
}

private val SECTIONS = listOf("latency", "outofprocess", "diagnostics", "sandbox", "crashes", "runaways", "heatmap", "window", "memory")

fun main(args: Array<String>) {
    val options = Options.parse(args)
    val report = Report(options.outDir)
    fun runs(section: String) = options.only == null || section in options.only

    // Latency first: it only spawns child JVMs. Out-of-process next: its in-process half needs a JVM
    // that has not compiled yet. Memory last: it starts from a JVM the other checks warmed up.
    if (runs("latency")) latency(report, options)
    if (runs("outofprocess")) outOfProcess(report, options)
    val pipeline = PlayerPipeline(checkInstrumented = true)
    if (runs("diagnostics")) diagnostics(report, pipeline)
    if (runs("sandbox")) sandbox(report, pipeline)
    if (runs("crashes")) crashes(report, pipeline)
    if (runs("runaways")) runaways(report, pipeline)
    if (runs("heatmap")) heatmap(report, pipeline)
    if (runs("window")) window(report)
    if (runs("memory")) memory(report, options)

    val runtime = Runtime.getRuntime()
    val file = report.write(listOf(
        listOf("Git commit", gitCommit()),
        listOf("JVM", "${System.getProperty("java.vm.vendor")} ${System.getProperty("java.vm.name")} ${System.getProperty("java.runtime.version")}"),
        listOf("OS", "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"),
        listOf("CPUs / max heap", "${runtime.availableProcessors()} / ${runtime.maxMemory() / (1024 * 1024)} MB"),
        listOf("Player compile classpath", "${PlayerClasspath.fromHost().size} jars (trimmed variant: ${PlayerClasspath.trimmed().size})"),
        listOf("Compose compiler plugin", PlayerClasspath.composePluginJar().name),
        listOf("Version matrix", "[`VERSIONS.md`](VERSIONS.md)"),
        listOf("Sections run", options.only?.joinToString() ?: "all"),
    ))
    println("\nwrote ${file.path}")
    if (report.failed) {
        println("some checks FAILED")
        exitProcess(1)
    }
    exitProcess(0)
}

private fun gitCommit(): String = runCatching {
    val p = ProcessBuilder("git", "describe", "--always", "--dirty").redirectErrorStream(true).start()
    p.inputStream.use { it.bufferedReader().readText().trim() }.also { p.waitFor() }
}.getOrDefault("unknown")
