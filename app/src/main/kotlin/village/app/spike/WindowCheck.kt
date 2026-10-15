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
import village.app.BigDialogue
import village.app.GameScreens
import village.app.ProgressStore
import village.app.Samples
import village.app.Screen
import village.app.SpikeController
import village.app.reactionFor
import village.app.ui.offsetOf
import village.app.ui.villagers.BIG_DIALOGUE_TAG
import village.app.ui.theme.VillageTheme
import village.compiler.Diagnostic
import androidx.compose.ui.semantics.SemanticsActions
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
    fun newScene(width: Int, of: SpikeController = controller, reducedMotion: Boolean = true) = ImageComposeScene(width, 860, Density(1f)) {
        VillageTheme(reducedMotion = reducedMotion) { GameScreens(of) }
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
    fun onScreen(text: String, on: ImageComposeScene = scene) = texts(on).any { text in it }
    fun node(text: String, on: ImageComposeScene = scene) =
        nodes(on).first { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
    fun clickText(text: String) {
        val center = node(text).boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press, center, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, center, buttons = PointerButtons(), button = PointerButton.Primary)
        settle { true }
    }
    /** The line the big dialogue shows: the last node with one of its lines, since the bubble may say the same. */
    fun dialogueNode(on: ImageComposeScene = scene) = controller.bigDialogue?.lines?.let { lines ->
        nodes(on).lastOrNull { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text in lines } == true }
    }
    /** Clicks the dialogue box, like a player reading on (talking-villagers D10). */
    fun advanceDialogue(on: ImageComposeScene = scene) {
        val center = dialogueNode(on)!!.boundsInRoot.center
        on.sendPointerEvent(PointerEventType.Press, center, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        on.sendPointerEvent(PointerEventType.Release, center, buttons = PointerButtons(), button = PointerButton.Primary)
        settle { true }
    }
    /** Whether the big dialogue is on screen, fading out included. */
    fun dialogueShown() = nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == BIG_DIALOGUE_TAG }
    /** Presses [button] of the big dialogue (a closing button or Skip intro), then waits for it to fade out. */
    fun closeDialogue(button: String) {
        clickText(button)
        settle { !dialogueShown() }
    }
    /** Reads every line of the open dialogue, then presses its closing button [closing]. */
    fun readThrough(closing: String) {
        repeat(controller.bigDialogue!!.lines.size - 1) { advanceDialogue() }
        closeDialogue(closing)
    }
    /** Presses the button showing [text] through its click action, like a keyboard user; it works below the fold too. */
    fun activate(text: String) {
        node(text).config[SemanticsActions.OnClick].action!!.invoke()
        settle { true }
    }

    /**
     * At 900 dp, the narrowest window, each of [level]'s intro lines fits the dialogue box's three
     * lines. Call it with [level]'s intro open on its first line.
     */
    fun introFitsAt900(level: village.levels.Level, screenshot: String) {
        val narrowDialogue = newScene(900)
        try {
            fun render() = repeat(5) { Snapshot.sendApplyNotifications(); frame(narrowDialogue); Thread.sleep(30) }
            // The line's own text node: the merged tree only has the whole clickable box.
            fun lineHeight() = narrowDialogue.semanticsOwners.asSequence().flatMap { owner ->
                fun walk(n: SemanticsNode): Sequence<SemanticsNode> = sequenceOf(n) + n.children.asSequence().flatMap(::walk)
                walk(owner.unmergedRootSemanticsNode)
            }.last { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text in level.intro } == true }.size.height
            render()
            val heights = level.intro.indices.map { page ->
                if (page > 0) { advanceDialogue(narrowDialogue); render() }
                lineHeight()
            }
            check("intro: every one of ${level.villager.name}'s lines fits three lines of the dialogue box at 900 dp", heights.all { it <= 3 * 36 + 1 }) { "$heights" }
            shot(screenshot, narrowDialogue)
        } finally {
            narrowDialogue.close()
        }
    }

    try {
        castSheet(File(out, "0-cast.png"))
        controller.warmUp()
        settle { !controller.warmingUp }

        // The home street (home-screen): the game opens on it, in its first-launch form.
        check("home: the game opens on the street in its first-launch form", controller.screen == Screen.HOME &&
            onScreen("Welcome to Compose Village") && onScreen("Start: 1 · Hello Main Street") && onScreen("How a level works") &&
            onScreen("Start here") && onScreen("Not open yet") && !onScreen("Coming soon") && onScreen("5 · Ferry Times") &&
            !onScreen("shops open")) { texts().joinToString(" | ") }
        shot("0-home-first-launch")
        val narrowHome = newScene(900)
        try {
            shot("0-home-narrow", narrowHome)
            val shops = listOf("Rosa's Bakery", "Tom's Flowers", "Marco's Kitchen", "Dona Lúcia's School", "Iara's Ferry", "The Sandbox")
            val boxes = shops.map { shop -> node(shop, narrowHome).let { Rect(it.positionInRoot, it.size.toSize()) } }
            check("home: at 900 dp every building is on screen, wrapping instead of scrolling sideways",
                boxes.all { it.left >= 0f && it.right <= 900f }) { "$boxes" }
        } finally {
            narrowHome.close()
        }
        activate("Open the Sandbox")
        check("home: Open the Sandbox opens the sandbox", controller.screen == Screen.PLAY && controller.level == null &&
            onScreen("Run")) { texts().joinToString(" | ") }

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
        fun openSign() = controller.levels.single { it.id == "001-open-sign" }
        clickText("Sandbox") // the sandbox is open; this only checks the footer entry is there and harmless
        check("level: the street shows one shop per level, none open yet, and the sandbox breadcrumb",
            onScreen("0 of 5 shops open") && texts().count { it == "Sandbox" } == 2) { texts().joinToString(" | ") }
        clickText("Main Street")
        check("home: the breadcrumb's Main Street goes home", controller.screen == Screen.HOME && onScreen("Welcome to Compose Village")) {
            texts().joinToString(" | ")
        }
        activate("Start: 1 · Hello Main Street")
        check("home: Start opens the first level", controller.screen == Screen.PLAY && controller.level == hello) { "${controller.level}" }

        // Talking villagers (talking-villagers): Rosa's intro plays on the first opening.
        val rosaIntro = hello.intro
        check("intro: the first opening dims the screen and Rosa says her first line, whole under reduced motion",
            controller.bigDialogue?.kind == BigDialogue.Kind.INTRO && onScreen(rosaIntro[0]) && onScreen("1 of 3") && onScreen("Skip intro")) {
            "${controller.bigDialogue} / ${texts().takeLast(8).joinToString(" | ")}"
        }
        shot("5a-intro")
        val runsBefore = controller.runCount
        controller.run()
        settle { true }
        check("intro: Run does nothing while the intro is open", controller.runCount == runsBefore && !controller.running && controller.verdict == null) {
            "runCount ${controller.runCount}"
        }
        advanceDialogue()
        check("intro: clicking the box moves to the next line", onScreen(rosaIntro[1]) && onScreen("2 of 3")) { texts().joinToString(" | ") }
        closeDialogue("Skip intro")
        check("intro: Skip closes it and saves it as seen", controller.bigDialogue == null && controller.progress[hello.id].introSeen &&
            !onScreen("Skip intro")) { "${controller.bigDialogue} / ${texts().takeLast(8).joinToString(" | ")}" }
        clickText("Talk to Rosa")
        check("intro: Talk to Rosa plays it again from the first line", controller.bigDialogue?.kind == BigDialogue.Kind.INTRO &&
            onScreen(rosaIntro[0]) && onScreen("1 of 3")) { texts().joinToString(" | ") }
        readThrough("Let's build it!")
        check("intro: Let's build it! closes it on the level", controller.bigDialogue == null && onScreen("Rosa's checklist")) { texts().joinToString(" | ") }
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
        check("celebration: the first completion opens Rosa's celebration before the completion card",
            controller.bigDialogue?.kind == BigDialogue.Kind.CELEBRATION && onScreen(hello.celebration[0]) && onScreen("1 of 2") &&
                !onScreen("Rosa's Bakery is open!") && onScreen("0 of 5 shops open") && controller.completed == setOf(hello.id)) { texts().joinToString(" | ") }
        shot("8-celebration")
        readThrough("See Rosa's Bakery")
        check("level: the solution completes the level", (controller.verdict as? Verdict.Evaluated)?.passed == true &&
            onScreen("Rosa's Bakery is open!") && onScreen("All 2 of Rosa's wishes came true.") && onScreen("Next: 2 · Open Sign") &&
                onScreen("1 of 5 shops open") && onScreen("2 of 2") && onScreen("That's my sign!")) { texts().joinToString(" | ") }
        check("level: the preview renders at the level's 400 x 300 dp viewport",
            controller.engine.state.value.frame?.let { it.width to it.height } == (400 to 300)) { "${controller.engine.state.value.frame?.let { "${it.width}x${it.height}" }}" }
        shot("8b-level-complete")

        clickText("Main Street")
        check("home: after the first level, its shop is lit and the street welcomes the player back",
            onScreen("Welcome back") && onScreen("1 of 5 shops open") && onScreen("Rosa's Bakery is open. Tom is still waiting for a sign.") &&
                onScreen("Up next") && onScreen("Continue: Open Sign") && onScreen("0 of 4 wishes") && onScreen("Starter code is ready") &&
                onScreen("Open shops") && onScreen("3 tries") && onScreen("Visit")) { texts().joinToString(" | ") }
        shot("8a-home-welcome-back")
        clickText("Rosa's Bakery")
        check("home: clicking the open level's shop goes back to it as it was",
            controller.screen == Screen.PLAY && controller.level == hello && onScreen("Rosa's Bakery is open!")) { texts().joinToString(" | ") }

        clickText("Next: 2 · Open Sign")
        check("intro: Next opens Open Sign with Tom's four-line intro", controller.bigDialogue?.kind == BigDialogue.Kind.INTRO &&
            onScreen("Oh, hi! You must be the new sign maker.") && onScreen("1 of 4")) { texts().joinToString(" | ") }
        shot("9a-open-sign-intro")
        introFitsAt900(openSign(), "9b-intro-narrow")
        closeDialogue("Skip intro")
        check("level: Next opens Open Sign with the Console unlocked, and the opening click doesn't dismiss the tip",
            controller.level?.id == "001-open-sign" && onScreen("CONSOLE") && !onScreen("RECOMPOSITIONS") &&
                onScreen("New: Console") && controller.highlighted.isNotEmpty()) { "${controller.level}, ${texts().joinToString(" | ")}" }
        shot("9-open-sign-unlock")
        clickText("Got it")
        check("level: \"Got it\" dismisses the unlock explanation", !onScreen("New: Console") && onScreen("CONSOLE"))

        val tomEdit = controller.code.text + "\n// the bell comes next\n"
        controller.code = TextFieldValue(tomEdit)
        clickText("Main Street")
        check("home: an edited level shows as saved on the pick-up card", onScreen("Your code is saved") && onScreen("Continue: Open Sign")) {
            texts().joinToString(" | ")
        }
        activate("Continue: Open Sign")
        check("home: Continue brings the edited code back", controller.screen == Screen.PLAY && controller.code.text == tomEdit) { controller.code.text }

        val openSign = openSign()
        // The customer presses Order (customer-bot): with an empty onClick, Tom's bell stays quiet.
        runCode(openSign.starterCode.replace("Text(\"Tom's Flowers\")", "Text(\"Tom's Flowers\")\n        Button(onClick = { }) { Text(\"Order\") }"))
        settle { onScreen("(nothing printed)") }
        val bell = (controller.verdict as? Verdict.Evaluated)?.results?.singleOrNull { it.id == "bell" }
        check("customer: an empty onClick fails only the bell, with the customer in the Console and on the preview",
            bell?.passed == false && bell.detail == "A customer pressed \"Order\", but nothing was printed." &&
                (controller.verdict as Verdict.Evaluated).results.count { it.passed } == 3 &&
                onScreen("customer › pressed [Order]") && onScreen("(nothing printed)") && onScreen("A customer pressed Order") &&
                reactionFor(openSign, controller.verdict)?.text == "The sign looks lovely! But a customer pressed Order and my bell stayed quiet…") {
            "$bell / ${texts().joinToString(" | ")}"
        }
        shot("9c-silent-bell")
        // The player presses their own screen in the preview (where the button is drawn).
        val yourScreen = nodes().first { n -> n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Your screen") == true }.boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press, yourScreen, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, yourScreen, buttons = PointerButtons(), button = PointerButton.Primary)
        settle { controller.customerCaption == null }
        check("customer: clicking the preview hands it back to the player", controller.customerCaption == null && !onScreen("A customer pressed Order")) {
            texts().joinToString(" | ")
        }

        runCode(openSign.starterCode.replace("Text(\"Tom's Flowers\")", "Text(\"Tom's Flowers\")\n        Button(onClick = { println(\"New order!\") }) { Text(\"Order\") }"))
        check("celebration: Tom celebrates his first completion", controller.bigDialogue?.kind == BigDialogue.Kind.CELEBRATION &&
            onScreen(openSign.celebration[0])) { texts().joinToString(" | ") }
        readThrough("See Tom's Flowers")
        check("level: solving Open Sign offers Marco's level next",
            onScreen("Tom's Flowers is open!") && onScreen("Next: 3 · Restaurant Menu") && onScreen("2 of 5 shops open")) { texts().joinToString(" | ") }
        check("customer: the bell rings in the Console", onScreen("customer › pressed [Order]\nNew order!")) { texts().joinToString(" | ") }
        shot("10-open-sign-complete")
        clickText("Keep tinkering")
        settle { true }
        check("level: Keep tinkering hides the completion card and keeps the level open and its shop lit",
            !onScreen("Tom's Flowers is open!") && controller.level == openSign && onScreen("2 of 5 shops open")) { texts().joinToString(" | ") }

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

        // The rest of Main Street (main-street-levels): Marco, Dona Lúcia and Iara, each harder than the last.
        fun level(id: String) = controller.levels.single { it.id == id }
        fun solution(id: String) = File("levels/src/test/resources/levels/$id/Solution.kt").readText()
        fun result(id: String) = (controller.verdict as? Verdict.Evaluated)?.results?.single { it.id == id }
        fun previewSize() = controller.engine.state.value.frame?.let { it.width to it.height }
        fun viewportOf(l: village.levels.Level) = l.viewport.widthDp.toInt() to l.viewport.heightDp.toInt()

        val marco = level("002-restaurant-menu")
        controller.open(marco)
        settle { controller.bigDialogue != null }
        check("intro: Marco introduces himself and his menu", controller.bigDialogue?.kind == BigDialogue.Kind.INTRO &&
            onScreen(marco.intro[0]) && onScreen("1 of 4")) { texts().joinToString(" | ") }
        closeDialogue("Skip intro")
        check("level: Restaurant Menu lists no panels and shows its concept chips",
            !onScreen("CONSOLE") && !onScreen("RECOMPOSITIONS") && onScreen("New: forEach")) { texts().joinToString(" | ") }
        runCode(marco.starterCode)
        check("menu: the starter shows only the first dish", result("name")?.passed == true &&
            result("dishes")?.detail == "I can't find \"Lasagna\" on the screen.") { "${controller.verdict}" }
        shot("15-menu-starter")
        runCode(solution(marco.id))
        readThrough("See Marco's Kitchen")
        check("menu: the solution opens Marco's Kitchen and offers Dona Lúcia next, at the level's viewport",
            onScreen("Marco's Kitchen is open!") && onScreen("Next: 4 · Contest Scoreboard") && onScreen("3 of 5 shops open") &&
                previewSize() == viewportOf(marco)) { "${previewSize()} / ${texts().joinToString(" | ")}" }
        shot("15b-menu-complete")

        val lucia = level("003-contest-scoreboard")
        clickText("Next: 4 · Contest Scoreboard")
        check("intro: Dona Lúcia introduces the scoreboard", controller.bigDialogue?.kind == BigDialogue.Kind.INTRO &&
            onScreen(lucia.intro[0]) && onScreen("1 of 4")) { texts().joinToString(" | ") }
        closeDialogue("Skip intro")
        check("level: the scoreboard unlocks Recompositions, and the Console comes without a tip",
            controller.level == lucia && onScreen("RECOMPOSITIONS") && onScreen("CONSOLE") && onScreen("New: Recompositions") &&
                !onScreen("New: Console")) { texts().joinToString(" | ") }
        shot("16-scoreboard-unlock")
        clickText("Got it")
        runCode(lucia.starterCode)
        settle { onScreen("customer › pressed [Reset]") || onScreen("customer › pressed [+1 Blue]") }
        check("scoreboard: plain variables leave the board unchanged, and Dona Lúcia says so",
            result("board")?.passed == true &&
                result("blue-point")?.detail == "A customer pressed \"+1 Blue\", but the screen didn't change." &&
                reactionFor(lucia, controller.verdict)?.text == "The children cheered, but the board didn't count it…" &&
                onScreen("customer › pressed [+1 Blue]")) { "${controller.verdict} / ${texts().joinToString(" | ")}" }
        shot("16b-scoreboard-unchanged")
        runCode(solution(lucia.id))
        check("scoreboard: remembered scores pass, and PlayerScreen recomposes after each press",
            controller.bigDialogue?.kind == BigDialogue.Kind.CELEBRATION &&
                (controller.engine.state.value.recompositions["PlayerScreen"] ?: 0) >= 4) {
            "${controller.engine.state.value.recompositions}"
        }
        readThrough("See Dona Lúcia's School")
        check("scoreboard: Dona Lúcia's School opens at the level's viewport, with Iara next",
            onScreen("Dona Lúcia's School is open!") && onScreen("Next: 5 · Ferry Times") && onScreen("4 of 5 shops open") &&
                previewSize() == viewportOf(lucia)) { "${previewSize()} / ${texts().joinToString(" | ")}" }
        shot("16c-scoreboard-complete")

        val iara = level("004-ferry-times")
        clickText("Next: 5 · Ferry Times")
        check("intro: Iara introduces the timetable", controller.bigDialogue?.kind == BigDialogue.Kind.INTRO &&
            onScreen(iara.intro[0]) && onScreen("1 of 4")) { texts().joinToString(" | ") }
        introFitsAt900(iara, "17a-ferry-intro-narrow")
        closeDialogue("Skip intro")
        check("level: Ferry Times shows both panels without unlock tips", onScreen("CONSOLE") && onScreen("RECOMPOSITIONS") &&
            !onScreen("New: Recompositions") && !onScreen("New: Console")) { texts().joinToString(" | ") }
        runCode(solution(iara.id).replace("contains(query, ignoreCase = true)", "contains(query)"))
        settle { onScreen("customer › typed \"Moon Bay\" into [Where to?]") }
        check("ferry: a case-sensitive filter fails only the coral search, with the customer's typing in the Console",
            (controller.verdict as? Verdict.Evaluated)?.results?.filterNot { it.passed }?.map { it.id } == listOf("coral") &&
                onScreen("customer › typed \"coral\" into [Where to?]")) { "${controller.verdict} / ${texts().joinToString(" | ")}" }
        shot("17b-ferry-case-sensitive")
        runCode(solution(iara.id))
        readThrough("See Iara's Ferry")
        check("ferry: the last level completes Main Street", onScreen("Iara's Ferry is open!") &&
            onScreen("Main Street is complete! More of the village is coming soon.") && !onScreen("Next:") &&
            onScreen("5 of 5 shops open") && previewSize() == viewportOf(iara)) { "${previewSize()} / ${texts().joinToString(" | ")}" }
        shot("17c-main-street-complete")

        // Saved progress (village-progress): coming back to a finished level, playing again, drafts, a relaunch.
        val helloSolution = hello.starterCode.replace("Text(\"Your sign goes here\")", "Text(\"Rosa's Bakery\")\n        Text(\"Fresh bread daily\")")
        clickText("Main Street")
        check("home: with every shop open, the street says so and lists all five open shops",
            onScreen("Every shop is open") && onScreen("5 of 5 shops open") && texts().count { it == "Visit" } == 5) { texts().joinToString(" | ") }
        shot("12a-home-all-open")
        activate("Visit")
        settle { !controller.running }
        check("progress: reopening a finished level replays its code and shows the welcome back card",
            controller.code.text == helloSolution && (controller.verdict as? Verdict.Evaluated)?.passed == true && controller.revisiting &&
                controller.runsThisVisit == 0 && onScreen("Rosa's Bakery is open!") && onScreen("You finished this in 3 tries.") &&
                onScreen("Play again") && onScreen("Next: 2 · Open Sign") && onScreen("5 of 5 shops open")) { texts().joinToString(" | ") }
        shot("12-welcome-back")
        clickText("Play again")
        check("progress: Play again restores the starter code and the sketch, and the shop stays lit",
            controller.code.text == hello.starterCode && controller.verdict == null && controller.overlaySketch &&
                !onScreen("Start over") && onScreen("5 of 5 shops open")) { texts().joinToString(" | ") }
        check("intro: a finished level reopens without its intro", controller.bigDialogue == null) { "${controller.bigDialogue}" }
        runCode(helloSolution)
        check("celebration: passing again after Play again shows the completion card without the celebration",
            controller.bigDialogue == null && onScreen("Rosa's Bakery is open!")) { texts().joinToString(" | ") }
        val edited = hello.starterCode.replace("Your sign goes here", "Rosa's")
        controller.code = TextFieldValue(edited)
        settle { true }
        check("progress: edited code offers Start over", onScreen("Start over")) { texts().joinToString(" | ") }
        controller.open(null)
        controller.open(hello)
        settle { !controller.running }
        check("progress: unfinished code comes back after switching levels", controller.code.text == edited) { controller.code.text }

        // With motion on, Talk to Rosa types her first line: a frame 300 ms in, for a visual check.
        val motion = newScene(1280, reducedMotion = false)
        try {
            controller.talk()
            repeat(10) { Snapshot.sendApplyNotifications(); frame(motion); Thread.sleep(30) }
            File(out, "14-typing.png").writeBytes(frame(motion).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        } finally {
            motion.close()
            controller.closeDialogue()
            settle { !dialogueShown() }
        }

        val relaunched = SpikeController(CoroutineScope(Dispatchers.Unconfined), density = 1f, ProgressStore(progressFile))
        val relaunchedScene = newScene(1280, relaunched)
        try {
            check("progress: a relaunch reads the street and the saved tries from the file",
                relaunched.completed == controller.levels.map { it.id }.toSet() && relaunched.progress["000-hello-main-street"].tries == 3) {
                "${relaunched.progress}"
            }
            frame(relaunchedScene)
            check("home: a relaunch opens on the street's welcome back form", relaunched.screen == Screen.HOME &&
                onScreen("Welcome back", relaunchedScene) && onScreen("5 of 5 shops open", relaunchedScene)) { texts(relaunchedScene).joinToString(" | ") }
        } finally {
            relaunchedScene.close()
            relaunched.close()
        }
        text("Screenshots of each state: `app/build/spike/window/*.png` (not committed).")
    } finally {
        scene.close()
        controller.close()
    }
}
