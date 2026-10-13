package village.app.spike

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import village.app.ProgressStore
import village.app.Samples
import village.app.SpikeController
import village.app.ui.SpikeScreen
import village.app.ui.offsetOf
import village.app.ui.theme.VillageTheme
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
    // A fresh progress file per check, so the street starts dark (village-progress).
    val progressFile = File(out, "progress.json").apply { delete() }
    val controller = SpikeController(CoroutineScope(Dispatchers.Unconfined), density = 1f, ProgressStore(progressFile))
    fun newScene(width: Int) = ImageComposeScene(width, 860, Density(1f)) {
        VillageTheme(reducedMotion = true) { SpikeScreen(controller) }
    }
    val scene = newScene(1280)
    // Frame times advance with the wall clock, so animations (fades, the checklist stagger) play out.
    val start = System.nanoTime()
    fun frame(on: ImageComposeScene = scene) = on.render(System.nanoTime() - start)
    fun settle(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end && !condition()) { Snapshot.sendApplyNotifications(); frame(); Thread.sleep(30) }
        repeat(5) { Snapshot.sendApplyNotifications(); frame(); Thread.sleep(30) }
    }
    /** Lets every running animation finish (the completion card starts 600 ms after the glow), then saves a screenshot. */
    fun shot(name: String, on: ImageComposeScene = scene) {
        val end = System.currentTimeMillis() + 1_300
        while (System.currentTimeMillis() < end) { Snapshot.sendApplyNotifications(); frame(on); Thread.sleep(30) }
        File(out, "$name.png").writeBytes(frame(on).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
    fun runCode(source: String) {
        controller.code = TextFieldValue(source)
        controller.run()
        settle { !controller.running }
    }

    fun nodes(on: ImageComposeScene = scene): Sequence<SemanticsNode> = on.semanticsOwners.asSequence().flatMap { owner ->
        fun walk(n: SemanticsNode): Sequence<SemanticsNode> = sequenceOf(n) + n.children.asSequence().flatMap(::walk)
        walk(owner.rootSemanticsNode)
    }
    fun texts(on: ImageComposeScene = scene): List<String> = nodes(on).flatMap { n -> n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }.toList()
    fun onScreen(text: String) = texts().any { text in it }
    fun node(text: String, on: ImageComposeScene = scene) =
        nodes(on).first { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
    fun clickText(text: String) {
        val center = node(text).boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press, center, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, center, buttons = PointerButtons(), button = PointerButton.Primary)
        settle { true }
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
        check("the editor's status line counts the problem and the header says it didn't build",
            onScreen("1 problem") && onScreen("Didn't build")) { texts().joinToString(" | ") }
        // Click the problem in the editor card like a mouse would.
        clickText("16:14")
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

        // The levels, played through the window like a player would (add-level-screen, cozy-level-screen).
        fun details() = (controller.verdict as? Verdict.Evaluated)?.results?.map { it.detail }

        val hello = controller.levels.single { it.id == "000-hello-main-street" }
        clickText("Sandbox") // the sandbox is open; this only checks the footer entry is there and harmless
        check("level: the street shows one shop per level, none open yet, and the sandbox breadcrumb",
            onScreen("0 of 2 shops open") && texts().count { it == "Sandbox" } == 2) { texts().joinToString(" | ") }
        controller.open(hello)
        settle { true }
        check("level: the sketch shows as an overlay ghost before the first Run, with the switch", controller.target != null &&
            controller.engine.state.value.frame == null && controller.overlaySketch &&
            onScreen("This is Rosa's sketch. Press Run to build yours on top of it.") && onScreen("Rosa's sketch") && onScreen("Your screen")) {
            texts().joinToString(" | ")
        }
        check("level: the header shows the breadcrumb and the villager card shows Rosa, her profession and concept chips",
            onScreen("1 · Hello Main Street") && onScreen("Main Street") && onScreen("Rosa") && onScreen("Baker") &&
                onScreen("New: Column") && onScreen("Compiler ready")) { texts().joinToString(" | ") }
        check("level: Hello Main Street hides the Console and Recompositions panels", !onScreen("CONSOLE") && !onScreen("RECOMPOSITIONS")) {
            texts().joinToString(" | ")
        }
        check("level: the checklist lists both requirements before the first Run",
            hello.criteria.all { onScreen(it.description) } && onScreen("Rosa's checklist") && onScreen("0 of 2"))
        shot("5-level-before-run")

        runCode(hello.starterCode)
        check("level: the starter shows both requirements unmet with \"can't find\" details",
            details() == listOf("I can't find \"Rosa's Bakery\" on the screen.", "I can't find \"Fresh bread daily\" on the screen.")) { "${controller.verdict}" }
        check("level: the first frame turns the overlay off; the header says it built and offers Run again",
            !controller.overlaySketch && onScreen("Built in") && onScreen("Run again") && onScreen("Compiles fine")) { texts().joinToString(" | ") }
        check("level: Rosa reacts with the first failed requirement's message", onScreen("I don't see my shop's name yet.") &&
            !onScreen("New: Column")) { texts().joinToString(" | ") }
        shot("6-level-starter")

        runCode(hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Fresh bread daily\")\n        Text(\"Rosa's Bakery\")"))
        check("level: the wrong order fails only the order, saying which text is on top",
            details() == listOf(null, "\"Fresh bread daily\" is above \"Rosa's Bakery\".")) { "${controller.verdict}" }
        clickText("Rosa's sketch")
        check("level: the sketch switch shows the target in the preview", controller.showTarget && onScreen("Your screen")) {
            "showTarget=${controller.showTarget}"
        }
        shot("7-level-target-view")

        runCode(hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Rosa's Bakery\")\n        Text(\"Fresh bread daily\")"))
        check("level: Run switches the preview back to the player's screen",
            !controller.showTarget && controller.engine.state.value.frame != null) { "showTarget=${controller.showTarget}" }
        check("level: the solution completes the level", (controller.verdict as? Verdict.Evaluated)?.passed == true &&
            onScreen("Rosa's Bakery is open!") && onScreen("All 2 of Rosa's wishes came true.") && onScreen("Next: 2 · Open Sign") &&
                onScreen("1 of 2 shops open") && onScreen("2 of 2") && onScreen("That's my sign!")) { texts().joinToString(" | ") }
        check("level: the preview renders at the level's 400 x 300 dp viewport",
            controller.engine.state.value.frame?.let { it.width to it.height } == (400 to 300)) { "${controller.engine.state.value.frame?.let { "${it.width}x${it.height}" }}" }
        shot("8-level-complete")

        clickText("Next: 2 · Open Sign")
        check("level: Next opens Open Sign with the Console unlocked, and the opening click doesn't dismiss the tip",
            controller.level?.id == "001-open-sign" && onScreen("CONSOLE") && !onScreen("RECOMPOSITIONS") &&
                onScreen("New: Console") && controller.highlighted.isNotEmpty()) { "${controller.level}, ${texts().joinToString(" | ")}" }
        shot("9-open-sign-unlock")
        clickText("Got it")
        check("level: \"Got it\" dismisses the unlock explanation", !onScreen("New: Console") && onScreen("CONSOLE"))

        val openSign = controller.levels.single { it.id == "001-open-sign" }
        runCode(openSign.starterCode.replace("Text(\"Tom's Flowers\")", "Text(\"Tom's Flowers\")\n        Button(onClick = { println(\"New order!\") }) { Text(\"Order\") }"))
        check("level: solving the last level says more is coming instead of Continue",
            onScreen("Tom's Flowers is open!") && onScreen("More of Main Street is coming soon.") && !onScreen("Next:") &&
                onScreen("2 of 2 shops open")) { texts().joinToString(" | ") }
        shot("10-open-sign-complete")
        clickText("Keep tinkering")
        settle { true }
        check("level: Keep tinkering hides the completion card and keeps the level open and its shop lit",
            !onScreen("Tom's Flowers is open!") && controller.level == openSign && onScreen("2 of 2 shops open")) { texts().joinToString(" | ") }

        // A narrow window: the right column wraps under the editor (design D5).
        val narrow = newScene(1000)
        try {
            shot("11-narrow", narrow)
            // Unclipped positions: in the narrow layout the checklist is below the fold.
            fun SemanticsNode.box() = Rect(positionInRoot, size.toSize())
            val editor = node("PlayerScreen.kt", narrow).box()
            val checklist = node("Tom's checklist", narrow).box()
            val wideChecklist = node("Tom's checklist").box()
            check("layout: at 1000 dp the checklist is under the editor; at 1280 dp it is beside it",
                checklist.top > editor.bottom && wideChecklist.left > node("PlayerScreen.kt").box().right) { "narrow $checklist vs editor $editor; wide $wideChecklist" }
        } finally {
            narrow.close()
        }

        // Saved progress (village-progress): coming back to a finished level, playing again, drafts, a relaunch.
        val helloSolution = hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Rosa's Bakery\")\n        Text(\"Fresh bread daily\")")
        controller.open(hello)
        settle { !controller.running }
        check("progress: reopening a finished level replays its code and shows the welcome back card",
            controller.code.text == helloSolution && (controller.verdict as? Verdict.Evaluated)?.passed == true && controller.revisiting &&
                controller.runsThisVisit == 0 && onScreen("Rosa's Bakery is open!") && onScreen("You finished this in 3 tries.") &&
                onScreen("Play again") && onScreen("Next: 2 · Open Sign") && onScreen("2 of 2 shops open")) { texts().joinToString(" | ") }
        shot("12-welcome-back")
        clickText("Play again")
        check("progress: Play again restores the starter code and the sketch, and the shop stays lit",
            controller.code.text == hello.starterCode && controller.verdict == null && controller.overlaySketch &&
                !onScreen("Start over") && onScreen("2 of 2 shops open")) { texts().joinToString(" | ") }
        val edited = hello.starterCode.replace("Your sign goes here", "Rosa's")
        controller.code = TextFieldValue(edited)
        settle { true }
        check("progress: edited code offers Start over", onScreen("Start over")) { texts().joinToString(" | ") }
        controller.open(null)
        controller.open(hello)
        settle { !controller.running }
        check("progress: unfinished code comes back after switching levels", controller.code.text == edited) { controller.code.text }

        val relaunched = SpikeController(CoroutineScope(Dispatchers.Unconfined), density = 1f, ProgressStore(progressFile))
        try {
            check("progress: a relaunch reads the street and the saved tries from the file",
                relaunched.completed == setOf("000-hello-main-street", "001-open-sign") && relaunched.progress["000-hello-main-street"].tries == 3) {
                "${relaunched.progress}"
            }
        } finally {
            relaunched.close()
        }
        text("Screenshots of each state: `app/build/spike/window/*.png` (not committed).")
    } finally {
        scene.close()
        controller.close()
    }
}
