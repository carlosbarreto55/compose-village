package village.app.spike

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import village.api.Guard
import village.compiler.PipelineResult
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import village.preview.PreviewState
import village.preview.RuntimeError
import java.util.concurrent.TimeUnit

/** Helpers shared by the checks that run player code in a [PreviewEngine]. */
internal class Previews(private val pipeline: PlayerPipeline) {
    fun compile(source: String): PipelineResult = pipeline.run(source).also {
        require(it.success) { "test source does not compile:\n" + it.diagnostics.joinToString("\n") }
    }

    fun PreviewEngine.loadSource(source: String): PreviewState {
        val r = compile(source)
        return load(r.entry!!, source.lines().size, r.loader).get(30, TimeUnit.SECONDS)
    }

    fun waitFor(engine: PreviewEngine, millis: Long, condition: (PreviewState) -> Boolean): PreviewState {
        val end = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < end && !condition(engine.state.value)) Thread.sleep(10)
        return engine.state.value
    }
}

internal fun describe(s: PreviewState) = "frames=${s.frameCount} error=${s.error} counts=${s.recompositions}"

/** Distinct colors on a 4-pixel grid; a blank canvas has one or two. */
internal fun distinctColors(frame: ImageBitmap?): Int {
    val p = frame?.toPixelMap() ?: return 0
    return (0 until p.width step 4).flatMap { x -> (0 until p.height step 4).map { y -> p[x, y] } }.toSet().size
}

/** Task 6.4 plus the rendering half of the "rendered output" criterion. */
internal fun crashes(report: Report, pipeline: PlayerPipeline) = report.section("Rendering and crash isolation", Criterion.RENDERED, Criterion.SURVIVES_CRASHES) {
    with(Previews(pipeline)) {
        PreviewEngine().use { engine ->
            val first = engine.loadSource(counterApp)
            val colors = distinctColors(first.frame)
            check("counter app renders a non-blank ${engine.width}x${engine.height} frame", first.error == null && colors > 2, Criterion.RENDERED) {
                "first render ${"%.1f".format(first.renderMillis)} ms, $colors colors; ${describe(first)}"
            }
            engine.click("Sold 0").get(5, TimeUnit.SECONDS)
            val clicked = engine.renderNow().get(5, TimeUnit.SECONDS)
            check("a synthetic click re-renders the frame", clicked.frameCount > first.frameCount && clicked.error == null, Criterion.RENDERED) {
                "frames ${first.frameCount} -> ${clicked.frameCount}, render ${"%.1f".format(clicked.renderMillis)} ms"
            }

            val rows = mutableListOf<List<Any?>>()
            fun crash(name: String, expectedLine: Int, trigger: () -> PreviewState) {
                val s = trigger()
                rows += listOf(name, s.error?.type, s.error?.line, s.error?.message)
                check("$name -> runtime error at line $expectedLine", s.error?.kind == RuntimeError.Kind.EXCEPTION && s.error?.line == expectedLine,
                    Criterion.SURVIVES_CRASHES, Criterion.ERROR_LINES) { describe(s) }
            }
            crash("NullPointerException in composition", 7) { engine.loadSource(crashInComposition) }
            crash("exception in onClick (synthetic click)", 8) {
                engine.loadSource(crashInClick)
                engine.click("Bake").get(5, TimeUnit.SECONDS)
                waitFor(engine, 2_000) { it.error != null }
            }
            crash("exception in LaunchedEffect after delay", 10) {
                engine.loadSource(crashInEffect)
                waitFor(engine, 2_000) { it.error != null }
            }
            val recovered = engine.loadSource(counterApp)
            check("recompile after a crash renders again", recovered.error == null && recovered.frame != null, Criterion.SURVIVES_CRASHES) { describe(recovered) }
            table(listOf("Crash", "Type", "Line", "Message shown"), rows)
        }
    }
}

/**
 * Task 6.5. Each runaway runs with the default 1 s budget; the abort must arrive within 1.5x the
 * budget, and a good attempt must render afterwards. A host heartbeat thread measures how long the
 * rest of the process stalls meanwhile (it must not freeze like a blocked UI thread would).
 */
internal fun runaways(report: Report, pipeline: PlayerPipeline) = report.section("Runaway code", Criterion.SURVIVES_CRASHES) {
    val budget = Guard.timeBudgetMillis
    val rows = mutableListOf<List<Any?>>()
    with(Previews(pipeline)) {
        PreviewEngine().use { engine ->
            fun runaway(name: String, source: String, kind: RuntimeError.Kind, line: Int?, limitMillis: Long = budget * 3 / 2, untilError: Boolean = false) {
                val r = compile(source)
                val heartbeat = Heartbeat()
                val start = System.nanoTime()
                val state = engine.load(r.entry!!, source.lines().size, r.loader).get(30, TimeUnit.SECONDS)
                    .let { if (untilError) waitFor(engine, limitMillis * 4) { it.error != null } else it }
                val ms = (System.nanoTime() - start) / 1_000_000
                val stall = heartbeat.stop()
                val after = engine.loadSource(counterApp)
                rows += listOf(name, state.error?.kind, state.error?.line, ms, stall, state.error?.message)
                check("$name stopped as ${kind.name} within $limitMillis ms${line?.let { " at line $it" } ?: ""}",
                    state.error?.kind == kind && ms <= limitMillis && (line == null || state.error?.line == line), Criterion.SURVIVES_CRASHES) {
                    "$ms ms; ${describe(state)}"
                }
                check("after $name: host heartbeat never stalled > 500 ms, next attempt renders", stall < 500 && after.error == null && after.frame != null,
                    Criterion.SURVIVES_CRASHES) { "max stall $stall ms; next attempt: ${describe(after)}" }
            }

            runaway("while (true) {} in composition", loopInComposition, RuntimeError.Kind.INFINITE_LOOP, line = 6)
            runaway("nested loop swallowing the abort with catch (Throwable)", loopSwallowingAbort, RuntimeError.Kind.INFINITE_LOOP, line = null)
            runaway("while (true) {} in a LaunchedEffect", loopInEffect, RuntimeError.Kind.INFINITE_LOOP, line = 8, untilError = true)
            runaway("unbounded list growth", unboundedList, RuntimeError.Kind.TOO_MUCH_MEMORY, line = null)
            runaway("unbounded recursion", unboundedRecursion, RuntimeError.Kind.STACK_OVERFLOW, line = 4)
        }

        // Code wedged in uninstrumented library code never ticks: the hard watchdog restarts the engine.
        PreviewEngine(hardLimitMillis = 1_500).use { engine ->
            val r = compile(wedgedInLibrary)
            val heartbeat = Heartbeat()
            val start = System.nanoTime()
            engine.load(r.entry!!, wedgedInLibrary.lines().size, r.loader)
            val restarted = waitFor(engine, 6_000) { it.error?.kind == RuntimeError.Kind.RESTARTED }
            val ms = (System.nanoTime() - start) / 1_000_000
            val stall = heartbeat.stop()
            rows += listOf("wedged in java.math (hard limit 1.5 s)", restarted.error?.kind, null, ms, stall, restarted.error?.message)
            check("wedged slice restarted by the watchdog", engine.restarts == 1 && restarted.error?.kind == RuntimeError.Kind.RESTARTED,
                Criterion.SURVIVES_CRASHES) { "$ms ms; ${describe(restarted)}" }
            val after = engine.loadSource(counterApp)
            check("engine works after the restart", after.error == null && after.frame != null, Criterion.SURVIVES_CRASHES) { describe(after) }
        }
    }
    text("Time budget per slice: $budget ms; allocation budget: ${Guard.allocBudgetBytes / (1024 * 1024)} MB. *Stopped after* is measured from `load` to the reported error.")
    table(listOf("Runaway", "Reported as", "Line", "Stopped after (ms)", "Max host stall (ms)", "Message shown"), rows)
}

/** Task 6.8: per-function recomposition counts for a counter app after 5 synthetic clicks. */
internal fun heatmap(report: Report, pipeline: PlayerPipeline) = report.section("Recomposition heatmap probe", Criterion.RENDERED) {
    with(Previews(pipeline)) {
        PreviewEngine().use { engine ->
            val first = engine.loadSource(heatmapApp)
            check("println is captured in the preview console", "composing PlayerScreen" in first.console) { "console: '${first.console}'" }
            repeat(5) { i ->
                val clicked = engine.click("Sold $i").get(5, TimeUnit.SECONDS)
                engine.renderNow().get(5, TimeUnit.SECONDS)
                if (!clicked) check("click 'Sold $i' found its node", false) { describe(engine.state.value) }
            }
            val state = engine.renderNow().get(5, TimeUnit.SECONDS)
            val counts = state.recompositions
            check("5 clicks recompose Counter on every click, Header once", (counts["Counter"] ?: 0) >= 6 && counts["Header"] == 1, Criterion.RENDERED) {
                "$counts"
            }
            val rows = counts.entries.sortedByDescending { it.value }.map { listOf(it.key, it.value) }
            report.csv("heatmap.csv", listOf("function", "count"), rows)
            text("""
                Counts from `Composer.setTracer`, filtered to the player thread and `Player.kt`, after the first composition
                and 5 clicks on the counter. Trace info string format (design open question), first one seen:
                `${state.traceInfoSample}`
            """)
            table(listOf("Function", "Executions"), rows)
        }
    }
}

/** Measures the longest gap of a thread that wakes every 10 ms, i.e. how long the host process stalled. */
internal class Heartbeat {
    @Volatile private var running = true
    @Volatile private var maxGapNanos = 0L
    private val thread = Thread({
        var last = System.nanoTime()
        while (running) {
            Thread.sleep(10)
            val now = System.nanoTime()
            maxGapNanos = maxOf(maxGapNanos, now - last - 10_000_000)
            last = now
        }
    }, "host-heartbeat").apply { isDaemon = true; start() }

    /** Stops and returns the longest stall beyond the 10 ms period, in milliseconds. */
    fun stop(): Long {
        running = false
        thread.join()
        return maxGapNanos / 1_000_000
    }
}
