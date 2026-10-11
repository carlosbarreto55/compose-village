package village.app.spike

import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import java.lang.management.ManagementFactory
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private class Sample(
    val cycle: Int, val compileMs: Long, val heapMb: Double, val metaspaceMb: Double, val classSpaceMb: Double?, val loadedClasses: Int,
    val unloadedClasses: Long, val openFds: Long?, val rssMb: Double?, val loaderCollected: Boolean,
) {
    fun line() = "MEMORY $cycle $compileMs $heapMb $metaspaceMb ${classSpaceMb ?: -1.0} $loadedClasses $unloadedClasses ${openFds ?: -1} ${rssMb ?: -1.0} $loaderCollected"

    fun csv() = listOf(cycle, compileMs, "%.2f".format(heapMb), "%.2f".format(metaspaceMb), classSpaceMb?.let { "%.2f".format(it) }, loadedClasses,
        unloadedClasses, openFds, rssMb?.let { "%.1f".format(it) }, loaderCollected)

    companion object {
        val CSV_HEADER = listOf("cycle", "compile_ms", "heap_after_gc_mb", "metaspace_mb", "class_space_mb", "loaded_classes", "unloaded_classes", "open_fds", "rss_mb", "loader_collected")

        fun parse(line: String): Sample {
            val p = line.split(" ")
            return Sample(p[1].toInt(), p[2].toLong(), p[3].toDouble(), p[4].toDouble(), p[5].toDouble().takeIf { it >= 0 }, p[6].toInt(),
                p[7].toLong(), p[8].toLong().takeIf { it >= 0 }, p[9].toDouble().takeIf { it >= 0 }, p[10].toBoolean())
        }
    }
}

/**
 * Task 6.7: [Options.cycles] compile -> guard -> load -> render -> close cycles, each run in a fresh
 * JVM so earlier sections (e.g. a thread the watchdog abandoned) cannot disturb the baseline.
 * Samples are taken after a full GC at the end of each cycle, when no attempt is live.
 *
 * Total Metaspace is not flat with the default tiered JIT: the host's own code (compiler, Compose)
 * keeps gaining profiling metadata (MethodData) as more of it gets hot, in the app loader's
 * non-class Metaspace, while class space and the loaded-class count stay flat. So Metaspace is
 * judged in a control run with C1 only (`-XX:TieredStopAtLevel=1`, no profiling metadata), and
 * the default run judges everything else.
 *
 * Growth limits, cycle 20 -> last cycle, are what leaking 10 % of the attempts in that range would
 * add, from the per-attempt costs: about 10 classes and 100 KB of Metaspace per loader, and a
 * ~0.9 MB frame per scene. A real per-attempt leak exceeds them; GC noise does not.
 */
internal fun memory(report: Report, options: Options) = report.section("Memory over ${options.cycles} reloads", Criterion.NO_LEAKS) {
    class Run(val label: String, val samples: List<Sample>, val collected: Int) {
        val baseline = samples.first { it.cycle == minOf(20, samples.size) }
        val last = samples.last()
    }
    fun run(label: String, vararg jvmArgs: String): Run {
        println("  running $label in a fresh JVM ...")
        val lines = runChildJvm("village.app.spike.MemoryCheckKt", options.cycles.toString(), jvmArgs = jvmArgs.toList())
        val samples = lines.filter { it.startsWith("MEMORY ") }.map(Sample::parse)
        require(samples.size == options.cycles) { "$label: ${samples.size}/${options.cycles} cycles completed:\n${lines.takeLast(20).joinToString("\n")}" }
        val collected = lines.first { it.startsWith("COLLECTED ") }.substringAfter(" ").toInt()
        return Run(label, samples, collected)
    }

    val main = run("default JIT")
    val control = run("C1-only control", "-XX:TieredStopAtLevel=1")
    report.csv("memory.csv", Sample.CSV_HEADER, main.samples.map { it.csv() })
    report.csv("memory-c1.csv", Sample.CSV_HEADER, control.samples.map { it.csv() })

    val leaked = (main.last.cycle - main.baseline.cycle) * 0.1 // attempts a 10 % leak would keep alive
    class Metric(val name: String, val run: Run, val at: (Sample) -> Double?, val limit: Double?, val unit: String)
    val metrics = listOf(
        Metric("Heap used after GC", main, { it.heapMb }, leaked * 0.9, "MB"),
        Metric("Loaded classes", main, { it.loadedClasses.toDouble() }, leaked * 10, ""),
        Metric("Open file descriptors", main, { it.openFds?.toDouble() }, 10.0, ""),
        Metric("Metaspace", control, { it.metaspaceMb }, leaked * 0.1, "MB"),
        Metric("Metaspace", main, { it.metaspaceMb }, null, "MB"),
        Metric("Class space (part of Metaspace)", main, { it.classSpaceMb }, null, "MB"),
        Metric("Process RSS", main, { it.rssMb }, null, "MB"),
        Metric("Process RSS", control, { it.rssMb }, null, "MB"),
    )
    text("""
        Each cycle compiles the ~30-line counter app (a different literal per cycle, so every attempt has its own
        classes), verifies and loads it, renders it and clicks it twice, then closes the scene. Samples are taken after
        `System.gc()` at the end of each cycle. Limits are what leaking 10 % of the attempts after cycle ${main.baseline.cycle} would
        add (per attempt: ~10 classes, ~100 KB Metaspace, a ~0.9 MB frame).

        Metaspace is judged in a C1-only control run: with the default tiered JIT it keeps growing slowly (decelerating)
        because the host's own hot code accumulates profiling metadata in the app class loader's non-class Metaspace,
        while class space and the class count stay flat. A leaked attempt would add classes and class space instead.
        RSS is recorded only: it is noisy (heap the JVM does not return to the OS, JIT code cache), and in the control
        run, which renders the same scenes, it stays flat, so frames are not leaking native memory.
        Per-cycle series: `memory.csv` (default JIT) and `memory-c1.csv` (control).
    """)
    table(listOf("Metric", "Run", "Cycle ${main.baseline.cycle}", "Cycle ${main.last.cycle}", "Growth", "Limit"), metrics.map { m ->
        val a = m.at(m.run.baseline)
        val b = m.at(m.run.last)
        listOf(m.name, m.run.label, a?.fmt(m.unit), b?.fmt(m.unit), if (a != null && b != null) (b - a).fmt(m.unit, signed = true) else null,
            m.limit?.fmt(m.unit) ?: "recorded only")
    })

    for (m in metrics) {
        val limit = m.limit ?: continue
        val a = m.at(m.run.baseline) ?: continue
        val b = m.at(m.run.last) ?: continue
        check("${m.name} (${m.run.label}): cycle ${m.run.baseline.cycle} -> ${m.run.last.cycle} grows by less than ${limit.fmt(m.unit)}", b - a < limit,
            Criterion.NO_LEAKS) { "${a.fmt(m.unit)} -> ${b.fmt(m.unit)}" }
    }
    for (r in listOf(main, control)) {
        check("at least 95 % of discarded class loaders were collected (${r.label})", r.collected * 100 >= r.samples.size * 95, Criterion.NO_LEAKS) {
            "${r.collected}/${r.samples.size} collected; ${r.samples.count { it.loaderCollected }} already at the end of their own cycle"
        }
    }
    report.evidence(Criterion.NO_LEAKS, "${main.samples.size} cycles; heap ${(main.last.heapMb - main.baseline.heapMb).fmt("MB", signed = true)}, " +
        "classes ${(main.last.loadedClasses - main.baseline.loadedClasses).toDouble().fmt("", signed = true)}, " +
        "Metaspace ${(control.last.metaspaceMb - control.baseline.metaspaceMb).fmt("MB", signed = true)} (C1 control), " +
        "loaders collected ${main.collected}/${main.samples.size}")
}

/** Child-JVM entry point: `<cycles>`. Prints one `MEMORY` line per cycle, then `COLLECTED <n>`. */
fun main(args: Array<String>) {
    val cycles = args[0].toInt()
    val memory = ManagementFactory.getMemoryMXBean()
    val pools = ManagementFactory.getMemoryPoolMXBeans()
    val metaspace = pools.first { it.name == "Metaspace" }
    val classSpace = pools.firstOrNull { it.name == "Compressed Class Space" }
    val classLoading = ManagementFactory.getClassLoadingMXBean()
    val os = ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.UnixOperatingSystemMXBean
    val pipeline = PlayerPipeline()
    val loaders = mutableListOf<WeakReference<ClassLoader>>()

    /** One attempt, in its own frame so nothing of it stays reachable from a local while we sample. */
    fun attempt(engine: PreviewEngine, cycle: Int): Long {
        val source = counterApp.replace("Text(\"Bakery\"", "Text(\"Bakery $cycle\"")
        val r = pipeline.run(source)
        require(r.success) { "cycle $cycle did not compile: ${r.diagnostics}" }
        loaders += WeakReference(r.loader!!)
        val loaded = engine.load(r.entry!!, source.lines().size, r.loader).get(30, TimeUnit.SECONDS)
        require(loaded.error == null) { "cycle $cycle crashed: ${loaded.error}" }
        // A few more frames, as a player would produce by clicking.
        repeat(2) { engine.click("Sold").get(5, TimeUnit.SECONDS); engine.renderNow().get(5, TimeUnit.SECONDS) }
        engine.unload().get(5, TimeUnit.SECONDS)
        return r.timings.compileMs
    }

    PreviewEngine().use { engine ->
        for (cycle in 1..cycles) {
            val compileMs = attempt(engine, cycle)
            repeat(3) { System.gc(); Thread.sleep(20) }
            println(Sample(
                cycle, compileMs, memory.heapMemoryUsage.used.mb(), metaspace.usage.used.mb(), classSpace?.usage?.used?.mb(),
                classLoading.loadedClassCount, classLoading.unloadedClassCount, os?.openFileDescriptorCount, rssMb(), loaders.last().get() == null,
            ).line())
        }
    }
    repeat(3) { System.gc(); Thread.sleep(50) }
    println("COLLECTED ${loaders.count { it.get() == null }}")
    exitProcess(0)
}

private fun Long.mb() = this / (1024.0 * 1024.0)

private fun Double.fmt(unit: String, signed: Boolean = false): String {
    val number = if (unit.isEmpty()) "%${if (signed) "+" else ""}.0f".format(this) else "%${if (signed) "+" else ""}.1f".format(this)
    return if (unit.isEmpty()) number else "$number $unit"
}

/** Resident set size of this process via `ps` (macOS/Linux), or null where unavailable. */
private fun rssMb(): Double? = runCatching {
    val p = ProcessBuilder("ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).redirectErrorStream(true).start()
    p.outputStream.close()
    val kb = p.inputStream.use { it.bufferedReader().readText() }.trim().toLong()
    p.waitFor()
    kb / 1024.0
}.getOrNull()
