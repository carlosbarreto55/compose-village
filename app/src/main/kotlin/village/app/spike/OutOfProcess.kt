package village.app.spike

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import village.app.Samples
import village.app.server.CompileClient
import village.app.server.HelperFailure
import village.app.server.Reply
import village.compiler.Diagnostic
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import village.preview.RuntimeError
import village.preview.SemanticsSnapshot
import java.io.File
import java.util.concurrent.TimeUnit

/** One timed request. For the helper, [requestMs] is the round trip and [decodeMs] the host's PNG decoding. */
private class Timed(
    val mode: String, val kind: String, val run: Int, val ok: Boolean, val requestMs: Double, val decodeMs: Double = 0.0,
    val serverMs: Double? = null, val renderMs: Double? = null, val encodeMs: Double? = null, val pngBytes: Int? = null,
) {
    /** Until the frame can be shown on the host. */
    val totalMs get() = requestMs + decodeMs

    /** Process boundary cost: everything but the helper's own work, plus decoding. */
    val overheadMs get() = serverMs?.let { requestMs - it + decodeMs }
}

private class Recovery(val kind: String, val detectMs: Double, val exitCode: Int?, val recoverMs: Double, val recovered: Boolean)

/**
 * Tasks 7.2 and 7.3: the same workload in this JVM and in a [CompileClient]-driven helper JVM,
 * then helper faults and how long the game needs to get a working preview back.
 *
 * Runs right after the latency section, so this JVM has not compiled or rendered anything yet:
 * its first compile is cold and its RSS growth is what the pipeline adds to the game.
 */
internal fun outOfProcess(report: Report, options: Options) = report.section("Out-of-process comparison") {
    val runs = options.runs
    val log = File("app/build/spike/compile-server.log").apply { parentFile.mkdirs(); delete() }
    val samples = mutableListOf<Timed>()
    val badSource = counterApp.replace("Text(\"Open for", "Txet(\"Open for") // unresolved reference at 26:9
    fun warmSource(i: Int) = counterApp.replace("Text(\"Bakery\"", "Text(\"Bakery $i\"")

    // ---- in-process ----
    val hostBefore = rssMb()
    val pipeline = PlayerPipeline()
    var localDiagnostics: List<Diagnostic>
    var localCrash: RuntimeError?
    var localSnapshot: SemanticsSnapshot?
    PreviewEngine().use { engine ->
        fun compile(kind: String, run: Int, source: String) {
            val start = System.nanoTime()
            val r = pipeline.run(source)
            val state = r.entry?.let { engine.load(it, source.lines().size, r.loader).get(30, TimeUnit.SECONDS) }
            val ms = millisSince(start)
            samples += Timed("in-process", kind, run, state?.frame != null && state.error == null, ms, renderMs = state?.renderMillis)
        }
        compile("cold", 0, Samples.BAKERY)
        compile("first", 0, counterApp)
        localSnapshot = engine.snapshot().get(10, TimeUnit.SECONDS)
        repeat(runs) { compile("warm", it + 1, warmSource(it + 1)) }
        repeat(runs) { i ->
            val before = engine.state.value.frameCount
            val start = System.nanoTime()
            val clicked = engine.click("Sold $i").get(5, TimeUnit.SECONDS)
            val state = engine.renderNow().get(5, TimeUnit.SECONDS)
            samples += Timed("in-process", "frame", i + 1, clicked && state.frameCount > before && state.error == null, millisSince(start),
                renderMs = state.renderMillis)
        }
        localDiagnostics = pipeline.run(badSource).diagnostics
        val crash = pipeline.run(crashInComposition)
        localCrash = engine.load(crash.entry!!, crashInComposition.lines().size, crash.loader).get(30, TimeUnit.SECONDS).error
    }
    val hostAfter = rssMb()

    // ---- out-of-process ----
    val remoteDiagnostics: List<Diagnostic>
    val remoteCrash: RuntimeError?
    var remoteSnapshot: SemanticsSnapshot? = null
    val helperRss = mutableMapOf<String, Double?>()
    val spawnMillis: Double
    val sameJavaHome: Boolean
    CompileClient.spawn(log).use { helper ->
        spawnMillis = helper.spawnMillis
        sameJavaHome = helper.ready.javaHome == System.getProperty("java.home")
        check("helper JVM starts and reports ready", helper.alive) { "pid ${helper.ready.pid}, ${spawnMillis.fmt()} to ready, java.home ${helper.ready.javaHome}" }
        helperRss["idle"] = rssMb(helper.pid)
        var previous: ByteArray? = null
        fun request(kind: String, run: Int, send: () -> Reply.Result, isNew: (Reply.Result) -> Boolean) {
            val start = System.nanoTime()
            val reply = send()
            val requestMs = millisSince(start)
            val decodeStart = System.nanoTime()
            reply.png?.let { png -> Image.makeFromEncoded(png).use { it.toComposeImageBitmap() } }
            val decodeMs = millisSince(decodeStart)
            samples += Timed("out-of-process", kind, run, isNew(reply), requestMs, decodeMs, reply.serverMillis, reply.renderMillis, reply.encodeMillis,
                reply.png?.size)
            previous = reply.png
        }
        val rendered = { r: Reply.Result -> r.ok && r.png != null && r.error == null }
        request("cold", 0, { helper.compile(Samples.BAKERY) }, rendered)
        helperRss["after warm-up compile"] = rssMb(helper.pid)
        request("first", 0, { helper.compile(counterApp).also { remoteSnapshot = it.snapshot } }, rendered)
        repeat(runs) { i -> request("warm", i + 1, { helper.compile(warmSource(i + 1)) }, rendered) }
        repeat(runs) { i ->
            val last = previous
            request("frame", i + 1, { helper.click("Sold $i") }) { rendered(it) && !it.png.contentEquals(last) }
        }
        helperRss["after ${runs} compiles + $runs frames"] = rssMb(helper.pid)
        remoteDiagnostics = helper.compile(badSource).diagnostics
        remoteCrash = helper.compile(crashInComposition).error
    }
    val hostAfterRemote = rssMb()

    // ---- helper faults ----
    fun recovery(kind: String, timeoutMillis: Long = 3_000): Recovery {
        val failure = CompileClient.spawn(log).use { helper ->
            helper.compile(Samples.BAKERY) // a warmed-up helper, as in the game
            val start = System.nanoTime()
            helper.fault(kind)
            val failure = try {
                helper.compile(counterApp, timeoutMillis = timeoutMillis)
                null
            } catch (e: HelperFailure) {
                e
            }
            val detectMs = millisSince(start)
            helper.kill()
            failure?.let { detectMs to it.exitCode }
        }
        val start = System.nanoTime()
        val recovered = CompileClient.spawn(log).use { helper -> helper.compile(counterApp).let { it.ok && it.png != null } }
        return Recovery(kind, failure?.first ?: Double.NaN, failure?.second, millisSince(start), failure != null && recovered)
    }
    val recoveries = listOf(recovery("exit"), recovery("oom"), recovery("wedge")).associateBy { it.kind }

    // ---- checks ----
    for (mode in listOf("in-process", "out-of-process")) {
        for (kind in listOf("warm", "frame")) {
            val of = samples.filter { it.mode == mode && it.kind == kind }
            val what = if (kind == "warm") "$runs warm compiles rendered a first frame" else "$runs clicks each produced a new frame"
            check("$mode: $what", of.size == runs && of.all { it.ok }) { "${of.count { it.ok }}/${of.size} ok" }
        }
    }
    check("helper uses the game's Java runtime", sameJavaHome)
    check("diagnostics cross the process boundary unchanged (26:9)", remoteDiagnostics == localDiagnostics && remoteDiagnostics.firstOrNull()?.line == 26) {
        "in-process $localDiagnostics, helper $remoteDiagnostics"
    }
    check("semantics snapshots cross the process boundary unchanged", remoteSnapshot != null && remoteSnapshot == localSnapshot) {
        "${localSnapshot?.nodes()?.count()} nodes in-process, ${remoteSnapshot?.nodes()?.count()} from the helper"
    }
    check("runtime errors cross the process boundary unchanged (line 7)", remoteCrash == localCrash && remoteCrash?.line == 7) {
        "in-process $localCrash, helper $remoteCrash"
    }
    for (r in recoveries.values) {
        check("helper fault '${r.kind}': the game survives and a new helper renders", r.recovered) {
            "detected after ${r.detectMs.fmt()}, exit code ${r.exitCode ?: "none (killed)"}, recovered in ${r.recoverMs.fmt()}"
        }
    }

    // ---- report ----
    report.csv("outofprocess.csv", listOf("mode", "kind", "run", "ok", "request_ms", "decode_ms", "server_ms", "render_ms", "encode_ms", "png_bytes"),
        samples.map { listOf(it.mode, it.kind, it.run, it.ok, it.requestMs.f(), it.decodeMs.f(), it.serverMs?.f(), it.renderMs?.f(), it.encodeMs?.f(), it.pngBytes) })

    fun of(mode: String, kind: String) = samples.filter { it.mode == mode && it.kind == kind }
    fun stats(values: List<Double>) = "${values.percentile(50.0).fmt()} / ${values.percentile(95.0).fmt()}"
    fun single(mode: String, kind: String) = of(mode, kind).singleOrNull()?.totalMs?.fmt()
    val localFrames = of("in-process", "frame")
    val remoteFrames = of("out-of-process", "frame")
    val remoteWarm = of("out-of-process", "warm")
    fun rss(value: Double?) = value?.let { "%.0f MB".format(it) } ?: "n/a"
    fun growth(from: Double?, to: Double?) = if (from != null && to != null) "%+.0f MB".format(to - from) else "n/a"
    fun recoveryCell(r: Recovery, how: String) =
        "$how after ${r.detectMs.fmt()}; new helper spawned and rendered in ${r.recoverMs.fmt()} (cold compile)"

    text("""
        The same workload runs in this JVM (*in-process*, the game's own JVM) and in a helper JVM started from the
        game's classpath (`CompileServer`, out-of-process). The helper runs the identical `PlayerPipeline` + `PreviewEngine`
        and returns diagnostics and a PNG frame over a length-prefixed stdin/stdout protocol. Times are measured on the
        host until the frame can be shown, so the helper's numbers include the round trip and decoding the PNG into an
        `ImageBitmap`. Workload: the ~30-line counter app; ${runs} warm compiles, then ${runs} synthetic clicks.
        In-process ran first, when this JVM had not compiled or rendered anything yet. Raw samples: `outofprocess.csv`.

        Helper faults are simulated with spike-only requests, because the sandbox and the guard block their real causes:
        `exit` calls `System.exit(0)` (a sandbox escape), `oom` allocates until the heap is exhausted (the helper runs with
        `-XX:+ExitOnOutOfMemoryError`), `wedge` spins forever on the helper's request thread (a hung compile or render),
        which the game detects with a 3 s request timeout and then kills the helper. *Recovered* is the time to spawn a
        new helper and get the next attempt's first frame, which is a cold compile again.
    """)
    table(listOf("", "In-process", "Out-of-process"), listOf(
        listOf("Start-up before the first compile", "none (runs in the game's JVM)", "${spawnMillis.fmt()} (spawn to ready)"),
        listOf("Cold compile → first frame", single("in-process", "cold"), single("out-of-process", "cold")),
        listOf("First compile after warm-up → first frame", single("in-process", "first"), single("out-of-process", "first")),
        listOf("Warm compile → first frame, median / p95", stats(of("in-process", "warm").map { it.totalMs }), stats(remoteWarm.map { it.totalMs })),
        listOf("↳ process boundary share (protocol + PNG encode/decode), median", "none", remoteWarm.mapNotNull { it.overheadMs }.percentile(50.0).fmt()),
        listOf("Click → new frame on the host, median / p95", stats(localFrames.map { it.totalMs }), stats(remoteFrames.map { it.totalMs })),
        listOf("↳ render / PNG encode / PNG decode, median", "${localFrames.mapNotNull { it.renderMs }.percentile(50.0).fmt()} / none / none",
            listOf(remoteFrames.mapNotNull { it.renderMs }, remoteFrames.mapNotNull { it.encodeMs }, remoteFrames.map { it.decodeMs })
                .joinToString(" / ") { it.percentile(50.0).fmt() } +
                " (PNG ${"%.0f".format(remoteFrames.mapNotNull { it.pngBytes?.toDouble() }.percentile(50.0) / 1024)} KB)"),
        listOf("Memory", "game RSS ${rss(hostBefore)} → ${rss(hostAfter)} (${growth(hostBefore, hostAfter)}) with the pipeline in it",
            "second JVM: helper RSS ${helperRss.entries.joinToString { "${rss(it.value)} ${it.key}" }}; game RSS ${growth(hostAfter, hostAfterRemote)} meanwhile"),
        listOf("`System.exit` in player code", "blocked by the sandbox (see Sandbox); if it escaped, the game exits",
            recoveries["exit"]?.let { recoveryCell(it, "helper exited (code ${it.exitCode}), detected") }),
        listOf("Heap exhaustion", "per-slice allocation budget stops instrumented loops (see Runaway code); a big allocation in library code can still exhaust the shared heap",
            recoveries["oom"]?.let { recoveryCell(it, "helper exited on OOM (code ${it.exitCode}), detected") }),
        listOf("Wedged thread (no ticks)", "the watchdog abandons the player thread and restarts the preview; the thread keeps burning CPU until the game exits (see Runaway code)",
            recoveries["wedge"]?.let { recoveryCell(it, "request timed out, helper killed") }),
    ))
    text("""
        RSS is sampled with `ps` and is approximate. Both JVMs run with `-Xmx1g`, and most of their RSS is heap that G1
        grew towards that limit while compiling and does not give back, so the comparison is: one JVM of that size
        (in-process) versus the game plus a second JVM of about the same size (out-of-process).
        Helper stderr: `app/build/spike/compile-server.log`.
    """)
}

private fun millisSince(startNanos: Long) = (System.nanoTime() - startNanos) / 1e6

/** Nearest-rank percentile; NaN for an empty list. */
private fun List<Double>.percentile(p: Double): Double =
    if (isEmpty()) Double.NaN else sorted()[(Math.ceil(size * p / 100).toInt() - 1).coerceIn(0, size - 1)]

private fun Double.fmt() = if (isNaN()) "n/a" else if (this >= 100) "%.0f ms".format(this) else "%.1f ms".format(this)

private fun Double.f() = "%.2f".format(this)
