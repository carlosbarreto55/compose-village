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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import village.levels.Verdict
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

        // The levels, played through the window like a player would (add-level-screen).
        fun texts(): List<String> = scene.semanticsOwners.flatMap { owner ->
            fun walk(n: SemanticsNode): List<String> =
                n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + n.children.flatMap(::walk)
            walk(owner.rootSemanticsNode)
        }
        fun onScreen(text: String) = texts().any { text in it }
        fun clickText(text: String) {
            val node = scene.semanticsOwners.asSequence().flatMap { owner ->
                fun walk(n: SemanticsNode): Sequence<SemanticsNode> = sequenceOf(n) + n.children.asSequence().flatMap(::walk)
                walk(owner.rootSemanticsNode)
            }.first { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
            val center = node.boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press, center, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, center, buttons = PointerButtons(), button = PointerButton.Primary)
            settle { true }
        }
        fun details() = (controller.verdict as? Verdict.Evaluated)?.results?.map { it.detail }

        val hello = controller.levels.single { it.id == "000-hello-main-street" }
        controller.open(hello)
        settle { true }
        check("level: the target and its ghost show before the first Run", controller.target != null &&
            controller.engine.state.value.frame == null && onScreen("Rosa's sketch") && onScreen("Target")) { texts().joinToString(" | ") }
        check("level: Hello Main Street hides the Console and Recompositions panels", !onScreen("Console") && !onScreen("Recompositions")) {
            texts().joinToString(" | ")
        }
        check("level: the checklist lists both requirements before the first Run",
            hello.criteria.all { onScreen(it.description) } && onScreen("Rosa's checklist"))
        shot("5-level-before-run")

        runCode(hello.starterCode)
        check("level: the starter shows both requirements unmet with \"can't find\" details",
            details() == listOf("I can't find \"Rosa's Bakery\" on the screen.", "I can't find \"Fresh bread daily\" on the screen.")) { "${controller.verdict}" }
        shot("6-level-starter")

        runCode(hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Fresh bread daily\")\n        Text(\"Rosa's Bakery\")"))
        check("level: the wrong order fails only the order, saying which text is on top",
            details() == listOf(null, "\"Fresh bread daily\" is above \"Rosa's Bakery\".")) { "${controller.verdict}" }
        clickText("Target")
        check("level: the Target switch shows the target in the preview", controller.showTarget && onScreen("Your screen")) {
            "showTarget=${controller.showTarget}"
        }
        shot("7-level-target-view")

        runCode(hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Rosa's Bakery\")\n        Text(\"Fresh bread daily\")"))
        check("level: Run switches the preview back to the player's screen",
            !controller.showTarget && controller.engine.state.value.frame != null) { "showTarget=${controller.showTarget}" }
        check("level: the solution completes the level", (controller.verdict as? Verdict.Evaluated)?.passed == true &&
            onScreen("Level complete!") && onScreen("Continue")) { "${controller.verdict}" }
        check("level: the preview renders at the level's 400 x 300 dp viewport",
            controller.engine.state.value.frame?.let { it.width to it.height } == (400 to 300)) { "${controller.engine.state.value.frame?.let { "${it.width}x${it.height}" }}" }
        shot("8-level-complete")

        clickText("Continue")
        check("level: Continue opens Open Sign with the Console unlocked and highlighted",
            controller.level?.id == "001-open-sign" && onScreen("Console") && !onScreen("Recompositions") &&
                onScreen("New: Console") && controller.highlighted.isNotEmpty()) { "${controller.level}, ${texts().joinToString(" | ")}" }
        shot("9-open-sign-unlock")
        clickText("Got it")
        check("level: \"Got it\" dismisses the unlock explanation", !onScreen("New: Console") && onScreen("Console"))

        val openSign = controller.levels.single { it.id == "001-open-sign" }
        runCode(openSign.starterCode.replace("Text(\"Tom's Flowers\")", "Text(\"Tom's Flowers\")\n        Button(onClick = { println(\"New order!\") }) { Text(\"Order\") }"))
        check("level: solving the last level says more is coming instead of Continue",
            onScreen("Level complete!") && onScreen("More of Main Street is coming soon.") && !onScreen("Continue")) { texts().joinToString(" | ") }
        shot("10-open-sign-complete")
        text("Screenshots of each state: `app/build/spike/window/*.png` (not committed).")
    } finally {
        scene.close()
        controller.close()
    }
}
