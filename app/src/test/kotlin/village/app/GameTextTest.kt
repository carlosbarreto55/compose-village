package village.app

import village.app.ui.ConsoleLine
import village.app.ui.consoleTranscript
import village.levels.Action
import village.levels.CriterionResult
import village.levels.Customer
import village.levels.LevelRepository
import village.levels.Selector
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

class ConsoleTranscriptTest {
    private val order = Action.Press("order", Selector(role = "Button", text = "Order"))

    private fun lines(console: String, vararg marks: Customer.Mark) = consoleTranscript(console, marks.toList()).map { it.kind to it.text }

    @Test fun customerLineBeforeTheStepsOutput() = assertEquals(
        listOf(ConsoleLine.Kind.OUTPUT to "composing", ConsoleLine.Kind.CUSTOMER to "customer › pressed [Order]", ConsoleLine.Kind.OUTPUT to "New order!"),
        lines("composing\nNew order!\n", Customer.Mark(order, 10, 11)),
    )

    @Test fun nothingPrinted() = assertEquals(
        listOf(ConsoleLine.Kind.CUSTOMER to "customer › pressed [Order]", ConsoleLine.Kind.NOTHING to "(nothing printed)"),
        lines("", Customer.Mark(order, 0, 0)),
    )

    @Test fun thePlayersOwnClicksAfterTheCustomerHaveNoCustomerLine() = assertEquals(
        listOf(ConsoleLine.Kind.CUSTOMER to "customer › pressed [Order]", ConsoleLine.Kind.NOTHING to "(nothing printed)", ConsoleLine.Kind.OUTPUT to "clicked"),
        lines("clicked\n", Customer.Mark(order, 0, 0)),
    )

    @Test fun typedLineAndCaption() {
        val name = Action.EnterText("name", Selector(text = "Your name"), "Rosa")
        assertEquals("customer › typed \"Rosa\" into [Your name]", GameText.customerLine(name))
        assertEquals("A customer typed into Your name", GameText.customerCaption(name))
        assertEquals("A customer pressed Order", GameText.customerCaption(order))
        assertEquals("customer › pressed [Button]", GameText.customerLine(Action.Press("go", Selector(role = "Button"))))
    }
}
