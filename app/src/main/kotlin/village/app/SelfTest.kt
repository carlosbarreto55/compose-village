package village.app

import androidx.compose.ui.graphics.toPixelMap
import village.compiler.PlayerCompiler
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * `--selftest`: compiles the sample, renders it, clicks its button and checks the screen updates,
 * all headlessly and once. Returns the process exit code: 0 on success.
 */
fun selfTest(): Int {
    println("Compose Village self-test")
    val javaHome = File(System.getProperty("java.home"))
    val compiler = PlayerCompiler()
    println("  java.home = $javaHome")
    println("  compiler JDK home = ${compiler.jdkHome}")
    if (compiler.jdkHome.canonicalFile != javaHome.canonicalFile || !File(compiler.jdkHome, "lib/modules").isFile) {
        println("FAIL: the compiler does not read JDK classes from the runtime the game runs on")
        return 7
    }
    val result = PlayerPipeline(compiler).run(Samples.BAKERY)
    if (!result.success) {
        result.diagnostics.forEach { println("  $it") }
        println("FAIL: the sample did not compile")
        return 1
    }
    println("  compiled: ${result.timings}")
    PreviewEngine().use { engine ->
        val state = try {
            engine.load(result.entry!!, Samples.BAKERY.lines().size, result.loader).get(30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            println("FAIL: the preview did not load: $e")
            return 2
        }
        state.error?.let { println("FAIL: the sample crashed: ${it.message} (line ${it.line})"); return 3 }
        val frame = state.frame ?: run { println("FAIL: no frame was rendered"); return 4 }
        val pixels = frame.toPixelMap()
        val colors = (0 until pixels.width step 4).flatMap { x -> (0 until pixels.height step 4).map { y -> pixels[x, y] } }.toSet().size
        if (colors < 3) { println("FAIL: the frame is blank"); return 5 }
        println("  rendered ${frame.width}x${frame.height} in ${"%.1f".format(state.renderMillis)} ms ($colors colors)")

        val clicked = engine.click("Sell").get(10, TimeUnit.SECONDS)
        val after = engine.renderNow().get(10, TimeUnit.SECONDS)
        if (!clicked || (after.recompositions["Counter"] ?: 0) < 2) {
            println("FAIL: clicking Sell did not update the screen (clicked=$clicked, recompositions=${after.recompositions})")
            return 6
        }
        println("  clicked Sell: recompositions ${after.recompositions}")
    }
    println("OK")
    return 0
}
