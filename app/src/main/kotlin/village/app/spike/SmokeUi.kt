package village.app.spike

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import village.app.Samples
import village.app.SpikeController
import village.app.ui.SpikeScreen
import java.io.File

// Throwaway check for section 5: renders the spike window offscreen to PNGs under build/smoke/ for a visual review.

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun ui() {
    val out = File("app/build/smoke").apply { mkdirs() }
    val controller = SpikeController(CoroutineScope(Dispatchers.Unconfined), density = 1f)
    val scene = ImageComposeScene(1280, 860, Density(1f)) {
        MaterialTheme(colorScheme = lightColorScheme()) { SpikeScreen(controller) }
    }
    fun settle(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end && !condition()) { Snapshot.sendApplyNotifications(); scene.render(); Thread.sleep(30) }
        repeat(5) { Snapshot.sendApplyNotifications(); scene.render(); Thread.sleep(30) }
    }
    fun shot(name: String) {
        val file = File(out, "$name.png")
        file.writeBytes(scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
        println("    wrote ${file.path}")
    }

    fun runCode(source: String) {
        controller.code = androidx.compose.ui.text.input.TextFieldValue(source)
        controller.run()
        settle { !controller.running }
    }

    controller.warmUp()
    settle { !controller.warmingUp }
    controller.run()
    settle { !controller.running && controller.engine.state.value.frame != null }
    controller.engine.click("Sell").get()
    controller.engine.click("Sell").get()
    controller.engine.renderNow().get()
    settle { true }
    check("spike window shows a successful run", controller.diagnostics.isEmpty() && controller.engine.state.value.frame != null) { "${controller.diagnostics}" }
    shot("1-success")

    runCode(Samples.BAKERY.replace("Text(\"Loaves sold: \$count\")", "Text(loaves)"))
    check("compile error listed at 16:14 with a red line", controller.diagnostics.singleOrNull()?.let { it.line == 16 && it.column == 14 } == true) { "${controller.diagnostics}" }
    shot("2-compile-error")
    // Click the diagnostic row (bottom of the left column) like a mouse would.
    val row = androidx.compose.ui.geometry.Offset(120f, 837f)
    scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, row,
        buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
    scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, row,
        buttons = androidx.compose.ui.input.pointer.PointerButtons(), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
    settle { true }
    val caret = controller.code.selection.start
    val expected = village.app.ui.offsetOf(controller.code.text, 16, 14)
    check("clicking the diagnostic moves the caret to 16:14", caret == expected) { "caret at $caret, expected $expected" }

    runCode(Samples.BAKERY.replace("Spacer(Modifier.width(12.dp))", "java.io.File(\"bread.txt\").delete()"))
    check("sandbox violation listed at line 17", controller.diagnostics.any { it.line == 17 && it.source == village.compiler.Diagnostic.Source.SANDBOX }) { "${controller.diagnostics}" }
    shot("3-sandbox")

    runCode(Samples.BAKERY.replace("Button(onClick = onSell)", "Button(onClick = { error(\"out of flour\") })"))
    controller.engine.click("Sell").get()
    settle { controller.engine.state.value.error != null }
    check("runtime error points at line 18", controller.engine.state.value.error?.line == 18) { "${controller.engine.state.value}" }
    shot("4-runtime-error")

    scene.close()
    controller.close()
}
