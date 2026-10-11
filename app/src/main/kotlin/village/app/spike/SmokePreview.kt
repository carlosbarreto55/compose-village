package village.app.spike

import androidx.compose.ui.graphics.toPixelMap
import village.api.Guard
import village.compiler.PipelineResult
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import village.preview.PreviewState
import village.preview.RuntimeError
import java.util.concurrent.TimeUnit

// Throwaway checks for section 4 (preview runtime) until the harness (6.4, 6.5, 6.7, 6.8) asserts them properly.

internal val heatmapApp = """
    import androidx.compose.foundation.layout.Column
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*

    @Composable
    fun Header() {
        Text("Bakery")
    }

    @Composable
    fun Counter(count: Int, onClick: () -> Unit) {
        Button(onClick = onClick) { Text("Sold ${'$'}count") }
    }

    @Composable
    fun PlayerScreen() {
        var count by remember { mutableStateOf(0) }
        println("composing PlayerScreen")
        Column {
            Header()
            Counter(count) { count++ }
        }
    }
""".trimIndent()

private val crashInComposition = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        val name: String? = null
        Text(name!!)
    }
""".trimIndent()

private val crashInClick = """
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        Button(onClick = {
            error("the oven is on fire")
        }) { Text("Bake") }
    }
""".trimIndent()

private val crashInEffect = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import kotlinx.coroutines.delay

    @Composable
    fun PlayerScreen() {
        LaunchedEffect(Unit) {
            delay(50)
            throw IllegalArgumentException("no flour")
        }
        Text("Waiting")
    }
""".trimIndent()

private val loopInFirstComposition = """
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        var i = 0
        while (true) { i++ }
    }
""".trimIndent()

/** ~10 s inside java.math (uninstrumented library code), where no tick ever runs. */
private val wedgedInLibrary = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        val digits = java.math.BigInteger.valueOf(7).pow(10_000_000).toString().length
        Text("7^10000000 has ${'$'}digits digits")
    }
""".trimIndent()

internal fun preview() {
    val pipeline = PlayerPipeline()
    fun compile(source: String): PipelineResult = pipeline.run(source).also {
        check(it.success) { "test source does not compile:\n" + it.diagnostics.joinToString("\n") }
    }
    fun PreviewEngine.loadSource(source: String): PreviewState {
        val r = compile(source)
        return load(r.entry!!, source.lines().size, r.loader).get(10, TimeUnit.SECONDS)
    }
    fun waitFor(engine: PreviewEngine, millis: Long, condition: (PreviewState) -> Boolean): PreviewState {
        val end = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < end && !condition(engine.state.value)) Thread.sleep(20)
        return engine.state.value
    }
    fun describe(s: PreviewState) = "frames=${s.frameCount} error=${s.error} counts=${s.recompositions}"

    PreviewEngine().use { engine ->
        // 4.2 / 4.3: first frame is published; the frame is not a blank canvas.
        val first = engine.loadSource(heatmapApp)
        val pixels = first.frame?.toPixelMap()
        val distinct = pixels?.let { p -> (0 until p.width step 4).flatMap { x -> (0 until p.height step 4).map { y -> p[x, y] } }.toSet().size } ?: 0
        check("counter app renders a non-blank frame (${first.renderMillis.format()} ms, $distinct colors)", first.error == null && distinct > 2) { describe(first) }
        check("println captured in preview console", "composing PlayerScreen" in first.console) { "console: '${first.console}'" }

        // 4.6 / 4.7: synthetic clicks via semantics; per-composable recomposition counts.
        repeat(5) { i ->
            val clicked = engine.click("Sold $i").get(5, TimeUnit.SECONDS)
            engine.renderNow().get(5, TimeUnit.SECONDS)
            if (!clicked) check("click 'Sold $i' found its node", false) { describe(engine.state.value) }
        }
        val afterClicks = waitFor(engine, 2_000) { it.frameCount > first.frameCount && "Sold 5" in it.console + it.recompositions }
        val counts = engine.renderNow().get(5, TimeUnit.SECONDS).recompositions
        check("5 clicks recompose Counter, not Header: $counts", (counts["Counter"] ?: 0) >= 6 && counts["Header"] == 1) { describe(afterClicks) }
        println("    trace info sample: ${engine.state.value.traceInfoSample}")

        // 4.4: crashes in composition, in a click handler and in an effect; recovery.
        val composition = engine.loadSource(crashInComposition)
        check("crash in composition -> line 7 (${composition.error?.type})", composition.error?.line == 7 && composition.frame == null) { describe(composition) }

        engine.loadSource(crashInClick)
        engine.click("Bake").get(5, TimeUnit.SECONDS)
        val click = waitFor(engine, 2_000) { it.error != null }
        check("crash in onClick -> line 8 (${click.error?.message})", click.error?.line == 8) { describe(click) }

        engine.loadSource(crashInEffect)
        val effect = waitFor(engine, 2_000) { it.error != null }
        check("crash in LaunchedEffect after delay -> line 10 (${effect.error?.message})", effect.error?.line == 10) { describe(effect) }

        val recovered = engine.loadSource(heatmapApp)
        check("recompile after crash renders again", recovered.error == null && recovered.frame != null) { describe(recovered) }

        // Scene creation runs inside a slice: an infinite loop in the first composition is stopped.
        Guard.timeBudgetMillis = 300
        val start = System.nanoTime()
        val loop = engine.loadSource(loopInFirstComposition)
        val ms = (System.nanoTime() - start) / 1_000_000
        Guard.timeBudgetMillis = 1_000
        check("infinite loop in first composition stopped after $ms ms -> line 6", loop.error?.kind == RuntimeError.Kind.INFINITE_LOOP && loop.error?.line == 6) { describe(loop) }

        // 4.8: discarded attempts' class loaders are collected.
        repeat(30) { engine.loadSource(heatmapApp) }
        val refs = engine.loaderRefs.dropLast(1)
        repeat(5) { System.gc(); Thread.sleep(50) }
        val collected = refs.count { it.get() == null }
        val alive = refs.indices.filter { refs[it].get() != null }
        check("reload releases class loaders: $collected/${refs.size} collected", collected * 100 >= refs.size * 95) {
            "still reachable: loads #$alive of ${refs.size} (0-based, in load order)"
        }
    }

    // 4.5: a slice wedged in uninstrumented library code is abandoned and the engine restarts.
    PreviewEngine(hardLimitMillis = 1_500).use { engine ->
        val r = compile(wedgedInLibrary)
        val start = System.nanoTime()
        engine.load(r.entry!!, wedgedInLibrary.lines().size, r.loader)
        val restarted = waitFor(engine, 6_000) { it.error?.kind == RuntimeError.Kind.RESTARTED }
        val ms = (System.nanoTime() - start) / 1_000_000
        check("wedged slice restarted by watchdog after $ms ms", engine.restarts == 1 && restarted.error?.kind == RuntimeError.Kind.RESTARTED) { describe(restarted) }
        val after = engine.loadSource(heatmapApp)
        check("engine works after restart", after.error == null && after.frame != null) { describe(after) }
    }
}

private fun Double.format() = "%.1f".format(this)
