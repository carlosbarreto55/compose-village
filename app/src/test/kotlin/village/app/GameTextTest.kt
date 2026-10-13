package village.app

import village.levels.CriterionResult
import village.levels.LevelRepository
import village.levels.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameTextTest {
    private val openSign = LevelRepository().load("001-open-sign")

    private fun result(id: String, passed: Boolean, message: String? = null) = CriterionResult(id, "d", passed, if (passed) null else "detail", message)

    @Test fun noReactionBeforeTheFirstRun() = assertNull(reactionFor(openSign, null))

    @Test fun firstFailedMessageIsSpoken() {
        val verdict = Verdict.Evaluated(listOf(result("name", true), result("button", false), result("placement", false, "second"), result("x", false, "third")))
        assertEquals(Reaction("second"), reactionFor(openSign, verdict))
    }

    @Test fun failuresWithoutMessagesPointToTheChecklist() {
        val verdict = Verdict.Evaluated(listOf(result("name", false)))
        assertEquals(Reaction(GameText.SEE_CHECKLIST), reactionFor(openSign, verdict))
    }

    @Test fun completeUsesTheLevelsThanks() {
        val verdict = Verdict.Evaluated(listOf(result("name", true)))
        assertEquals(Reaction(openSign.thanks!!, happy = true), reactionFor(openSign, verdict))
    }

    @Test fun notRunLines() {
        assertEquals(GameText.DID_NOT_COMPILE, reactionFor(openSign, Verdict.NotRun(Verdict.NotRun.Reason.DID_NOT_COMPILE))?.text)
        assertEquals(GameText.CRASHED, reactionFor(openSign, Verdict.NotRun(Verdict.NotRun.Reason.CRASHED))?.text)
    }

    @Test fun linesSkipBlankImportAndPackageLines() {
        val code = """
            package shop
            import androidx.compose.material3.*

            @Composable
            fun PlayerScreen() {
                Text("Tom's Flowers")
            }
        """.trimIndent()
        assertEquals(4, countCodeLines(code))
    }
}
