package village.levels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The `main-street` spec's scenarios for levels 3–5, run through the real pipeline and customer. */
class MainStreetTest {
    private val repository = LevelRepository()

    private fun verdict(level: Level, source: String) = assertIs<Verdict.Evaluated>(Attempts.verdict(level, source), source)
    private fun Verdict.Evaluated.failed() = results.filterNot { it.passed }.map { it.id }
    private fun Verdict.Evaluated.detail(id: String) = results.single { it.id == id }.detail

    // ---- 3 · Restaurant Menu ----

    private val marco by lazy { repository.load("002-restaurant-menu") }

    /** Marco's starter code with [body] as `PlayerScreen`'s body. */
    private fun menu(body: String) = marco.starterCode.substringBefore("// Tip:") + """
        @Composable
        fun PlayerScreen() {
            $body
        }
    """.trimIndent()

    @Test fun marcosStarterShowsOnlyTheFirstDish() {
        val verdict = verdict(marco, marco.starterCode)
        assertTrue(results(verdict, "name"))
        assertEquals("I can't find \"Lasagna\" on the screen.", verdict.detail("dishes"))
        assertTrue(!verdict.passed)
    }

    @Test fun everyDishWithALoop() {
        val verdict = verdict(marco, menu("""
            Column {
                Text("Marco's Kitchen")
                menu.forEach { dish -> Row { Text(dish.name); Text(" " + dish.price) } }
            }
        """))
        assertTrue(verdict.passed, verdict.toString())
    }

    @Test fun pricesUnderTheDishes() {
        val verdict = verdict(marco, menu("""
            Column {
                Text("Marco's Kitchen")
                menu.forEach { dish -> Text(dish.name); Text(dish.price) }
            }
        """))
        assertEquals(listOf("prices"), verdict.failed())
        assertEquals(
            "\"€12\" and \"Spaghetti\" are stacked on top of each other; \"€12\" should be to the right of \"Spaghetti\".",
            verdict.detail("prices"),
        )
    }

    // ---- 4 · Contest Scoreboard ----

    private val lucia by lazy { repository.load("003-contest-scoreboard") }

    private fun board(state: String, plusGreen: String) = """
        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.*

        @Composable
        fun PlayerScreen() {
            $state
            Column {
                Row { Text("Blue: ${'$'}blue"); Button(onClick = { blue++ }) { Text("+1 Blue") } }
                Row { Text("Green: ${'$'}green"); Button(onClick = { $plusGreen }) { Text("+1 Green") } }
                Button(onClick = { blue = 0; green = 0 }) { Text("Reset") }
            }
        }
    """.trimIndent()

    @Test fun plainVariablesNeverChangeTheBoard() {
        val verdict = verdict(lucia, lucia.starterCode)
        assertTrue(results(verdict, "board"))
        assertEquals("A customer pressed \"+1 Blue\", but the screen didn't change.", verdict.detail("blue-point"))
    }

    @Test fun rememberedScores() {
        val verdict = verdict(lucia, board("var blue by remember { mutableStateOf(0) }\nvar green by remember { mutableStateOf(0) }", "green++"))
        assertTrue(verdict.passed, verdict.toString())
    }

    @Test fun oneSharedScore() {
        val shared = "var blue by remember { mutableStateOf(0) }\nvar green by remember { mutableStateOf(0) }"
        val verdict = verdict(lucia, board(shared, "blue++"))
        assertEquals(listOf("teams"), verdict.failed())
        assertTrue(verdict.detail("teams")!!.startsWith("After a customer pressed \"+1 Green\":"), verdict.detail("teams"))
    }

    private fun results(verdict: Verdict.Evaluated, id: String) = verdict.results.single { it.id == id }.passed
}
