package village.app.spike

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import village.app.Samples
import village.app.SpikeController
import village.app.ui.SpikeScreen
import village.app.ui.offsetOf
import village.compiler.Diagnostic
import java.io.File

/**
 * The spike window itself, rendered offscreen: the player's UI shows in its preview pane, and
 * compile errors, sandbox violations and runtime errors show at the right lines. Screenshots go
 * to `app/build/spike/window/` for a visual review.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun window(report: Report) = report.section("Spike window", Criterion.RENDERED) {
    val out = File("app/build/spike/window").apply { mkdirs() }
    val controller = SpikeController(CoroutineScope(Dispatchers.Unconfined), density = 1f)
    val scene = ImageComposeScene(1280, 860, Density(1f)) {
        MaterialTheme(colorScheme = lightColorScheme()) { SpikeScreen(controller) }
    }
    fun settle(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end && !condition()) { Snapshot.sendApplyNotifications(); scene.render(); Thread.sleep(30) }
        repeat(5) { Snapshot.sendApplyNotifications(); scene.render(); Thread.sleep(30) }
    }
    fun shot(name: String) = File(out, "$name.png").writeBytes(scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
    fun runCode(source: String) {
        controller.code = TextFieldValue(source)
        controller.run()
        settle { !controller.running }
    }

    try {
        controller.warmUp()
        settle { !controller.warmingUp }
        controller.run()
        settle { !controller.running && controller.engine.state.value.frame != null }
        controller.engine.click("Sell").get()
        controller.engine.click("Sell").get()
        controller.engine.renderNow().get()
        settle { true }
        check("the sample runs and shows in the preview pane", controller.diagnostics.isEmpty() && distinctColors(controller.engine.state.value.frame) > 2,
            Criterion.RENDERED) { "warm-up ${controller.warmUpMillis} ms, run ${controller.timings}; ${controller.diagnostics}" }
        shot("1-success")

        runCode(Samples.BAKERY.replace("Text(\"Loaves sold: \$count\")", "Text(loaves)"))
        check("a compile error is listed at 16:14", controller.diagnostics.singleOrNull()?.let { it.line == 16 && it.column == 14 } == true,
            Criterion.ERROR_LINES) { "${controller.diagnostics}" }
        shot("2-compile-error")
        // Click the diagnostic row (bottom of the left column) like a mouse would.
        val row = Offset(120f, 837f)
        scene.sendPointerEvent(PointerEventType.Press, row, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, row, buttons = PointerButtons(), button = PointerButton.Primary)
        settle { true }
        val caret = controller.code.selection.start
        val expected = offsetOf(controller.code.text, 16, 14)
        check("clicking the diagnostic moves the caret to 16:14", caret == expected) { "caret at $caret, expected $expected" }

        runCode(Samples.BAKERY.replace("Spacer(Modifier.width(12.dp))", "java.io.File(\"bread.txt\").delete()"))
        check("a sandbox violation is listed at line 17", controller.diagnostics.any { it.line == 17 && it.source == Diagnostic.Source.SANDBOX },
            Criterion.ERROR_LINES) { "${controller.diagnostics}" }
        shot("3-sandbox")

        runCode(Samples.BAKERY.replace("Button(onClick = onSell)", "Button(onClick = { error(\"out of flour\") })"))
        controller.engine.click("Sell").get()
        settle { controller.engine.state.value.error != null }
        check("a runtime error points at line 18", controller.engine.state.value.error?.line == 18, Criterion.ERROR_LINES) {
            "${controller.engine.state.value.error}"
        }
        shot("4-runtime-error")
        text("Screenshots of each state: `app/build/spike/window/*.png` (not committed).")
    } finally {
        scene.close()
        controller.close()
    }
}
