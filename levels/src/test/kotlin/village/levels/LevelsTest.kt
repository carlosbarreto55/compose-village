package village.levels

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every bundled level must pass these checks (design D9, `level-definition` "Self-checking levels"):
 * it loads, its reference solution passes every criterion, its starter compiles and fails at least
 * one, and the index lists exactly the level folders.
 */
class LevelsTest {
    private val repository = LevelRepository()

    // Gradle runs tests from the module directory.
    private val levelsDir = File("src/main/resources/levels")
    private val solutionsDir = File("src/test/resources/levels")

    @Test fun indexListsExactlyTheLevelFolders() {
        val folders = levelsDir.listFiles { f -> f.isDirectory }!!.map { it.name }.toSet()
        val ids = repository.ids()
        assertEquals(emptySet(), folders - ids.toSet(), "level folders missing from index.json")
        assertEquals(emptySet(), ids.toSet() - folders, "index.json lists ids with no folder")
    }

    @Test fun everySolutionBelongsToALevel() {
        val solutions = solutionsDir.listFiles { f -> f.isDirectory }.orEmpty().map { it.name }.toSet()
        assertEquals(emptySet(), solutions - repository.ids().toSet(), "solution folders with no level")
    }

    @Test fun everyLevelLoads() {
        val problems = repository.ids().mapNotNull { id -> runCatching { repository.load(id) }.exceptionOrNull()?.message }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test fun bundledVillagersTalk() {
        val hello = repository.load("000-hello-main-street")
        val openSign = repository.load("001-open-sign")
        assertEquals("rosa" to 3, hello.villager.id to hello.intro.size)
        assertEquals("tom" to 4, openSign.villager.id to openSign.intro.size)
        assertTrue(openSign.intro.first().startsWith("Oh, hi! You must be the new sign maker."))
        assertEquals(1, hello.celebration.size)
        assertEquals(1, openSign.celebration.size)
    }

    @Test fun everyLevelHasATargetAtTwiceItsViewport() = forEachLevel { level ->
        val png = level.targetPng ?: return@forEachLevel "${level.id}: no target.png (rendered by :levels:renderTargets)"
        val image = ImageIO.read(png.inputStream()) ?: return@forEachLevel "${level.id}: target.png is not an image"
        val expected = (level.viewport.widthDp * 2).toInt() to (level.viewport.heightDp * 2).toInt()
        if (image.width to image.height != expected) "${level.id}: target is ${image.width}x${image.height}, expected ${expected.first}x${expected.second}" else null
    }

    @Test fun solutionsPassEveryCriterion() = forEachLevel { level ->
        val solution = assertNotNull(File(solutionsDir, "${level.id}/Solution.kt").takeIf { it.isFile }, "${level.id} has no Solution.kt").readText()
        when (val verdict = Attempts.verdict(level, solution)) {
            is Verdict.NotRun -> "${level.id}: Solution.kt ${verdict.describe()}\n${Attempts.diagnostics(level, solution)}"
            is Verdict.Evaluated -> verdict.results.filterNot { it.passed }.map { it.id }.takeIf { it.isNotEmpty() }
                ?.let { "${level.id}: Solution.kt fails criteria $it" }
        }
    }

    @Test fun startersCompileAndDoNotSolveTheLevel() = forEachLevel { level ->
        when (val verdict = Attempts.verdict(level, level.starterCode)) {
            is Verdict.NotRun -> "${level.id}: the starter ${verdict.describe()}\n${Attempts.diagnostics(level, level.starterCode)}"
            is Verdict.Evaluated -> if (verdict.passed) "${level.id}: the starter already passes every criterion" else null
        }
    }

    /** Runs [check] on every level; it returns a problem or null. Fails listing every problem. */
    private fun forEachLevel(check: (Level) -> String?) {
        val problems = repository.all().mapNotNull(check)
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    private fun Verdict.NotRun.describe() = when (reason) {
        Verdict.NotRun.Reason.DID_NOT_COMPILE -> "does not compile"
        Verdict.NotRun.Reason.CRASHED -> "crashes"
    }
}

/** Spec scenarios that need real Compose components, run through the real pipeline. */
class LevelScenariosTest {
    private val level = LevelRepository().load("000-hello-main-street")

    private fun screen(body: String) = """
        import androidx.compose.foundation.background
        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp

        @Composable
        fun PlayerScreen() {
        $body
        }
    """.trimIndent()

    private fun evaluated(body: String) = assertIs<Verdict.Evaluated>(Attempts.verdict(level, screen(body)))

    @Test fun styledSolutionPasses() {
        val verdict = evaluated("""
            Column(Modifier.background(Color(0xFFFFF3E0)).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Rosa's Bakery", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8D6E63))
                Text("  Fresh bread   daily ")
            }
        """)
        assertTrue(verdict.passed, verdict.toString())
    }

    @Test fun wrongOrderFailsOnlyTheOrder() {
        val verdict = evaluated("""Column { Text("Fresh bread daily"); Text("Rosa's Bakery") }""")
        assertEquals(listOf("order"), verdict.results.filterNot { it.passed }.map { it.id })
        val order = verdict.results.single { it.id == "order" }
        assertEquals("\"Fresh bread daily\" is above \"Rosa's Bakery\".", order.detail)
        assertEquals("I'd like my name on top, please!", order.message)
    }

    @Test fun starterFailsBothWithCantFindDetails() {
        val verdict = assertIs<Verdict.Evaluated>(Attempts.verdict(level, level.starterCode))
        assertEquals(
            listOf("I can't find \"Rosa's Bakery\" on the screen.", "I can't find \"Fresh bread daily\" on the screen."),
            verdict.results.map { it.detail },
        )
    }

    @Test fun sideBySideIsNotAbove() {
        val verdict = evaluated("""Row { Text("Rosa's Bakery"); Text("Fresh bread daily") }""")
        assertEquals(listOf("order"), verdict.results.filterNot { it.passed }.map { it.id })
        assertEquals(
            "\"Fresh bread daily\" and \"Rosa's Bakery\" are side by side; \"Fresh bread daily\" should be under \"Rosa's Bakery\".",
            verdict.results.single { it.id == "order" }.detail,
        )
    }

    @Test fun buttonIsMatchedByRoleAndText() {
        val order = listOf(Criterion.Exists("order", "m", Selector(role = "Button", text = "Order")))
        fun passes(body: String) = Attempts.verdict(Level(level.id, "", level.villager, "", "", entryFunction = "PlayerScreen", starterCode = "", viewport = level.viewport, criteria = order), screen(body))
            .let { assertIs<Verdict.Evaluated>(it).passed }
        assertTrue(passes("""Button(onClick = {}) { Row { Text("Order") } }"""))
        assertTrue(!passes("""Text("Order")"""))
    }

    @Test fun openSignButtonOnTheLeftIsReversed() {
        val openSign = LevelRepository().load("001-open-sign")
        val verdict = assertIs<Verdict.Evaluated>(Attempts.verdict(openSign, screen("""Row { Button(onClick = {}) { Text("Order") }; Text("Tom's Flowers") }""")))
        assertEquals(listOf("placement"), verdict.results.filterNot { it.passed }.map { it.id })
        assertEquals("A button \"Order\" is to the left of \"Tom's Flowers\".", verdict.results.single { !it.passed }.detail)
    }

    @Test fun compileErrorIsNotRun() {
        val verdict = Attempts.verdict(level, screen("""Text(42)"""))
        assertEquals(Verdict.NotRun(Verdict.NotRun.Reason.DID_NOT_COMPILE), verdict)
    }

    @Test fun crashIsNotRun() {
        val verdict = Attempts.verdict(level, screen("""Text("Rosa's Bakery"); error("oops")"""))
        assertEquals(Verdict.NotRun(Verdict.NotRun.Reason.CRASHED), verdict)
    }
}
