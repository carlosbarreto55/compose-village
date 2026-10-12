package village.levels

import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Density of rendered targets: sharp on Retina displays, scaled down for the thumbnail. */
private const val TARGET_DENSITY = 2f

/**
 * Build step (add-level-screen design D1): renders every indexed level's reference solution through
 * the real pipeline and preview engine, at the level's viewport, to `<out>/<id>/target.png`.
 * Fails the build if a solution is missing, does not compile or crashes.
 *
 * Usage: RenderTargets <levels dir> <solutions dir> <output dir>
 */
fun main(args: Array<String>) {
    val (levelsDir, solutionsDir, outDir) = args.map(::File)
    outDir.deleteRecursively() // no stale targets of removed levels
    val levels = LevelRepository(LevelFiles.directory(levelsDir)).all()
    val pipeline = PlayerPipeline()
    val problems = mutableListOf<String>()
    PreviewEngine(density = TARGET_DENSITY).use { engine ->
        for (level in levels) {
            val file = File(solutionsDir, "${level.id}/Solution.kt")
            if (!file.isFile) { problems += "${level.id}: no reference solution at $file"; continue }
            val source = file.readText()
            val result = pipeline.run(source, level.entryFunction)
            val entry = result.entry
            if (entry == null) {
                problems += "${level.id}: Solution.kt does not compile:\n" + result.diagnostics.joinToString("\n") { "    $it" }
                continue
            }
            val state = engine.load(entry, source.lines().size, result.loader, level.viewport).get(30, TimeUnit.SECONDS)
            val frame = state.frame
            if (state.error != null || frame == null) { problems += "${level.id}: Solution.kt crashed: ${state.error?.message}"; continue }
            val png = Image.makeFromBitmap(frame.asSkiaBitmap()).use { it.encodeToData(EncodedImageFormat.PNG)!!.bytes }
            File(outDir, "${level.id}/${LevelRepository.TARGET_FILE}").apply { parentFile.mkdirs() }.writeBytes(png)
            println("rendered ${level.id}: ${frame.width}x${frame.height}")
        }
    }
    if (problems.isNotEmpty()) {
        System.err.println("Could not render level targets:\n" + problems.joinToString("\n"))
        exitProcess(1)
    }
    exitProcess(0) // the engine's player thread is a daemon, but the compiler may leave others
}
