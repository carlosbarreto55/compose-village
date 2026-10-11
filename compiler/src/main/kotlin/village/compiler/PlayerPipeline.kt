package village.compiler

import village.api.LevelEntry

class PipelineResult(
    /** The attempt's entry, or null when it failed to compile, verify or load. */
    val entry: LevelEntry?,
    /** Problems in terms of the player's file, sorted by position. */
    val diagnostics: List<Diagnostic>,
    val timings: Timings,
    /** The attempt's class loader, kept so callers can track whether it is collected (task 4.8). */
    val loader: ClassLoader?,
) {
    val success get() = entry != null
}

/**
 * One attempt, end to end: compile the player's file with the generated entry wrapper, map the
 * diagnostics to the player's file, verify and instrument the bytecode, and load it in a fresh
 * class loader.
 *
 * Not thread-safe, like [PlayerCompiler].
 */
class PlayerPipeline(
    private val compiler: PlayerCompiler = PlayerCompiler(),
    private val parentLoader: ClassLoader = LevelEntry::class.java.classLoader,
) {
    /**
     * @param source the player's `Player.kt`.
     * @param entryFunction the composable the level shows, e.g. `BakeryScreen`.
     */
    fun run(source: String, entryFunction: String = "PlayerScreen"): PipelineResult {
        val timings = Timings()
        val wrapper = EntryWrapper(source, entryFunction)

        val output = compiler.compile(listOf(SourceFile(PLAYER_FILE, source), SourceFile(EntryWrapper.FILE_NAME, wrapper.source)))
        timings.compileMs = output.millis
        val diagnostics = wrapper.mapDiagnostics(output.diagnostics, output.classes)
        if (!output.success || diagnostics.any { it.severity == Severity.ERROR }) {
            return PipelineResult(null, diagnostics, timings, null)
        }

        var start = System.nanoTime()
        val classes = output.classes // TODO(tasks 3.2, 3.3): sandbox verification + instrumentation.
        timings.guardMs = millisSince(start)

        start = System.nanoTime()
        val loader = PlayerClassLoader(classes, parentLoader)
        val entry = try {
            Class.forName(EntryWrapper.CLASS_NAME, true, loader).getField("INSTANCE").get(null) as LevelEntry
        } catch (t: Throwable) {
            timings.loadMs = millisSince(start)
            val error = Diagnostic(Severity.ERROR, 0, 0, "Could not load your code: $t")
            return PipelineResult(null, diagnostics + error, timings, null)
        }
        timings.loadMs = millisSince(start)
        return PipelineResult(entry, diagnostics, timings, loader)
    }

    private fun millisSince(startNanos: Long) = (System.nanoTime() - startNanos) / 1_000_000

    companion object {
        /** The name the player's file is compiled under; runtime stack frames refer to it. */
        const val PLAYER_FILE = "Player.kt"
    }
}
