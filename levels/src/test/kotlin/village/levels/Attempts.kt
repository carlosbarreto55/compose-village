package village.levels

import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import java.util.concurrent.TimeUnit

/**
 * Runs player code at a level the way the game does: compile, load at the level's viewport,
 * snapshot, judge. One pipeline and one engine for the whole test JVM, so the compiler warms up once.
 */
object Attempts {
    private val pipeline by lazy { PlayerPipeline() }
    private val engine by lazy { PreviewEngine() }

    @Synchronized
    fun verdict(level: Level, source: String): Verdict {
        val result = pipeline.run(source, level.entryFunction)
        val entry = result.entry ?: return Evaluator.verdict(level, compiled = false, snapshot = null)
        engine.load(entry, source.lines().size, result.loader, level.viewport).get(30, TimeUnit.SECONDS)
        return Evaluator.verdict(level, compiled = true, snapshot = engine.snapshot().get(10, TimeUnit.SECONDS))
    }

    /** Diagnostics of a failed compile, to explain test failures. */
    fun diagnostics(level: Level, source: String) = pipeline.run(source, level.entryFunction).diagnostics.joinToString("\n")
}
