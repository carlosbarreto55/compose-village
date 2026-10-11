package village.app.spike

import village.app.Samples
import village.compiler.PlayerClasspath
import village.compiler.PlayerCompiler
import village.compiler.PlayerPipeline
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Task 6.2. Each variant runs in a fresh JVM, so its first compile is a true cold compile and the
 * keepalive property cannot leak between variants. In each JVM: one cold compile of the sample
 * (the game's start-up warm-up), the player's first compile after it, then [Options.runs] warm
 * compiles of a ~30-line composable.
 */
internal fun latency(report: Report, options: Options) = report.section("Compile latency", Criterion.WARM_COMPILE) {
    data class Variant(val keepalive: Boolean, val classpath: String) {
        val label get() = "keepalive ${if (keepalive) "on" else "off"}, $classpath classpath"
    }
    data class Sample(val kind: String, val run: Int, val compileMs: Long, val guardMs: Long, val loadMs: Long, val environment: String) {
        val totalMs get() = compileMs + guardMs + loadMs
    }

    val variants = listOf(Variant(true, "full"), Variant(true, "trimmed"), Variant(false, "full"), Variant(false, "trimmed"))
    val results = variants.associateWith { v ->
        println("  running ${v.label} in a fresh JVM ...")
        val lines = runChildJvm("village.app.spike.LatencyKt", v.keepalive.toString(), v.classpath, options.runs.toString())
        val jars = lines.firstOrNull { it.startsWith("CLASSPATH ") }?.substringAfter(" ")?.toInt()
        val samples = lines.filter { it.startsWith("LATENCY ") }.map { line ->
            val p = line.split(" ")
            Sample(p[1], p[2].toInt(), p[3].toLong(), p[4].toLong(), p[5].toLong(), p[6])
        }
        val warm = samples.count { it.kind == "warm" }
        check("${v.label}: ${options.runs} warm compiles succeeded", warm == options.runs) {
            if (warm == options.runs) "$jars jars, ${samples.size} compiles" else lines.takeLast(20).joinToString("\n")
        }
        jars to samples
    }

    report.csv("latency.csv", listOf("variant", "kind", "run", "compile_ms", "guard_ms", "load_ms", "compiler_environment"), results.flatMap { (v, r) ->
        r.second.map { listOf(v.label, it.kind, it.run, it.compileMs, it.guardMs, it.loadMs, it.environment) }
    })

    text("""
        Workload: the ~30-line counter composable (`counterApp` in `Sources.kt`), recompiled with a different string
        literal each run. *Cold* is the first compile in a fresh JVM (the start-up warm-up compile of the sample),
        *first* is the player's first compile right after it, *warm* are the following ${options.runs} runs.
        Compile is the embedded `K2JVMCompiler` call; pipeline adds bytecode verification + instrumentation and class loading.
        *Environment* is what the compiler's application environment (`KotlinCoreEnvironment.ourApplicationEnvironment`)
        looked like after each warm compile, confirming that the keepalive switch took effect.
    """)
    table(
        listOf("Variant", "Jars", "Cold", "First after warm-up", "Warm median", "Warm p95", "Warm min / max", "Pipeline warm median", "Environment"),
        results.map { (v, r) ->
            val (jars, samples) = r
            val warm = samples.filter { it.kind == "warm" }
            val compile = warm.map { it.compileMs }
            listOf(
                v.label, jars, samples.firstOrNull { it.kind == "cold" }?.compileMs?.ms(), samples.firstOrNull { it.kind == "first" }?.compileMs?.ms(),
                compile.median()?.ms(), compile.p95()?.ms(), "${compile.minOrNull()?.ms()} / ${compile.maxOrNull()?.ms()}",
                warm.map { it.totalMs }.median()?.ms(),
                warm.map { it.environment }.distinct().let { envs ->
                    when {
                        envs == listOf("none") -> "disposed after each compile"
                        envs.size == 1 -> "one instance, reused"
                        else -> "${envs.size} instances"
                    }
                },
            )
        },
    )

    val default = results[variants.first()]!!.second.filter { it.kind == "warm" }
    val median = default.map { it.compileMs }.median()
    val p95 = default.map { it.compileMs }.p95()
    report.evidence(Criterion.WARM_COMPILE, "median ${median?.ms()}, p95 ${p95?.ms()} over ${default.size} warm runs, keepalive on, full classpath")
    check("warm compile median < 2000 ms (keepalive on, full classpath)", median != null && median < 2_000, Criterion.WARM_COMPILE) {
        "median ${median?.ms()}, p95 ${p95?.ms()}"
    }
}

/** Child-JVM entry point for one latency variant: `<keepalive> <full|trimmed> <runs>`. Prints one line per compile. */
fun main(args: Array<String>) {
    val keepalive = args[0].toBoolean()
    val classpath = if (args[1] == "trimmed") PlayerClasspath.trimmed() else PlayerClasspath.fromHost()
    val runs = args[2].toInt()
    val pipeline = PlayerPipeline(PlayerCompiler(classpath = classpath, keepEnvironmentAlive = keepalive))
    println("CLASSPATH ${classpath.size}")

    fun measure(kind: String, run: Int, source: String) {
        val r = pipeline.run(source)
        if (!r.success) {
            println("FAILED $kind $run: ${r.diagnostics}")
            exitProcess(1)
        }
        val t = r.timings
        println("LATENCY $kind $run ${t.compileMs} ${t.guardMs} ${t.loadMs} ${compilerEnvironment()}")
    }
    measure("cold", 0, Samples.BAKERY)
    measure("first", 0, counterApp)
    repeat(runs) { i -> measure("warm", i + 1, counterApp.replace("Text(\"Bakery\"", "Text(\"Bakery ${i + 1}\"")) }
    exitProcess(0)
}

/** Identity of the compiler's kept-alive application environment, or `none` when it was disposed. */
private fun compilerEnvironment(): String {
    val field = Class.forName("org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment").getDeclaredField("ourApplicationEnvironment")
    field.isAccessible = true
    return field.get(null)?.let { Integer.toHexString(System.identityHashCode(it)) } ?: "none"
}

/** Runs [mainClass] in a new JVM on this JVM's classpath and returns its output lines. */
internal fun runChildJvm(mainClass: String, vararg args: String, jvmArgs: List<String> = emptyList(), timeoutMinutes: Long = 15): List<String> {
    val java = ProcessHandle.current().info().command().orElse(File(System.getProperty("java.home"), "bin/java").path)
    val process = ProcessBuilder(
        listOf(java, "-Xmx1g", "-Djava.awt.headless=true", "-XX:+UseG1GC") + jvmArgs + listOf("-cp", System.getProperty("java.class.path"), mainClass) + args,
    ).redirectErrorStream(true).start()
    val lines = mutableListOf<String>()
    val reader = Thread { process.inputStream.bufferedReader().forEachLine { synchronized(lines) { lines += it } } }.apply { start() }
    if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
        process.destroyForcibly()
        error("$mainClass ${args.joinToString(" ")} timed out after $timeoutMinutes min")
    }
    reader.join()
    return synchronized(lines) { lines.toList() }
}

internal fun List<Long>.median(): Long? = if (isEmpty()) null else sorted()[size / 2]

/** Nearest-rank 95th percentile. */
internal fun List<Long>.p95(): Long? = if (isEmpty()) null else sorted()[(Math.ceil(size * 0.95).toInt() - 1).coerceIn(0, size - 1)]

internal fun Long.ms() = "$this ms"
