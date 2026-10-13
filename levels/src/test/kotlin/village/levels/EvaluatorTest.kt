package village.levels

import village.preview.DpBounds
import village.preview.SemanticsSnapshot
import village.preview.SnapshotNode
import village.preview.Viewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Each test builds a snapshot by hand: no scene, no compiler. */
class EvaluatorTest {
    private fun text(t: String, top: Float, left: Float = 0f, width: Float = 100f, height: Float = 20f, role: String? = null) =
        SnapshotNode(texts = listOf(t), role = role, bounds = DpBounds(left, top, left + width, top + height))

    private fun snapshot(vararg nodes: SnapshotNode) =
        SemanticsSnapshot(Viewport(360f, 640f), listOf(SnapshotNode(bounds = DpBounds(0f, 0f, 360f, 640f), children = nodes.toList())))

    private fun passes(c: Criterion, s: SemanticsSnapshot) = Evaluator.evaluate(listOf(c), s).passed

    private fun exists(selector: Selector, count: Int? = null) = Criterion.Exists("c", "m", selector, count)
    private fun above(a: String, b: String) = Criterion.Above("order", "m", Selector(text = a), Selector(text = b))
    private fun leftOf(a: String, b: String) = Criterion.LeftOf("order", "m", Selector(text = a), Selector(text = b))

    private val title = text("Rosa's Bakery", top = 0f)
    private val subtitle = text("Fresh bread daily", top = 20f)

    @Test fun extraWhitespaceMatches() = assertTrue(passes(exists(Selector(text = "Rosa's Bakery")), snapshot(text("  Rosa's   Bakery ", 0f))))

    @Test fun whitespaceInTheExpectedTextIsNormalizedToo() = assertTrue(passes(exists(Selector(text = " Rosa's\nBakery")), snapshot(title)))

    @Test fun caseSensitiveByDefault() = assertFalse(passes(exists(Selector(text = "Rosa's Bakery")), snapshot(text("rosa's bakery", 0f))))

    @Test fun ignoreCase() = assertTrue(passes(exists(Selector(text = "Rosa's Bakery", ignoreCase = true)), snapshot(text("ROSA'S BAKERY", 0f))))

    @Test fun contains() {
        val welcome = snapshot(text("Welcome to Rosa's Bakery!", 0f))
        assertTrue(passes(exists(Selector(text = "Rosa's Bakery", contains = true)), welcome))
        assertFalse(passes(exists(Selector(text = "Rosa's Bakery")), welcome))
    }

    @Test fun mergedTextsMatchOneByOneOrJoined() {
        val merged = snapshot(SnapshotNode(texts = listOf("Rosa's", "Bakery"), bounds = DpBounds(0f, 0f, 100f, 20f)))
        assertTrue(passes(exists(Selector(text = "Rosa's")), merged))
        assertTrue(passes(exists(Selector(text = "Rosa's Bakery")), merged))
        assertFalse(passes(exists(Selector(text = "Bakery Rosa's")), merged))
    }

    @Test fun roleAndTextMustBothMatch() {
        val order = Selector(role = "Button", text = "Order")
        assertTrue(passes(exists(order), snapshot(text("Order", 0f, role = "Button"))))
        assertFalse(passes(exists(order), snapshot(text("Order", 0f))))
        assertFalse(passes(exists(order), snapshot(text("Pay", 0f, role = "Button"))))
    }

    @Test fun contentDescription() {
        val logo = SnapshotNode(contentDescriptions = listOf("Bakery  logo"), role = "Image", bounds = DpBounds(0f, 0f, 48f, 48f))
        assertTrue(passes(exists(Selector(contentDescription = "Bakery logo", role = "Image")), snapshot(logo)))
        assertFalse(passes(exists(Selector(text = "Bakery logo")), snapshot(logo)))
    }

    @Test fun zeroSizeNodesAreAbsent() {
        val hidden = snapshot(text("Rosa's Bakery", 0f, width = 0f, height = 0f))
        assertFalse(passes(exists(Selector(text = "Rosa's Bakery")), hidden))
        assertTrue(passes(Criterion.Absent("c", "m", Selector(text = "Rosa's Bakery")), hidden))
    }

    @Test fun nodesOutsideTheViewportAreAbsent() {
        assertFalse(passes(exists(Selector(text = "Rosa's Bakery")), snapshot(text("Rosa's Bakery", top = 640f))))
        assertFalse(passes(exists(Selector(text = "Rosa's Bakery")), snapshot(text("Rosa's Bakery", top = 0f, left = -100f))))
        // partly visible still counts
        assertTrue(passes(exists(Selector(text = "Rosa's Bakery")), snapshot(text("Rosa's Bakery", top = 630f))))
    }

    @Test fun exactCount() {
        val buttons = snapshot(*Array(3) { text("B$it", top = it * 30f, role = "Button") })
        assertFalse(passes(exists(Selector(role = "Button"), count = 2), buttons))
        assertTrue(passes(exists(Selector(role = "Button"), count = 3), buttons))
        assertTrue(passes(exists(Selector(role = "Button")), buttons))
    }

    @Test fun absent() {
        assertTrue(passes(Criterion.Absent("c", "m", Selector(text = "Closed")), snapshot(title)))
        assertFalse(passes(Criterion.Absent("c", "m", Selector(text = "Rosa's Bakery")), snapshot(title)))
    }

    @Test fun columnIsAboveNotLeftOf() {
        val column = snapshot(title, subtitle)
        assertTrue(passes(above("Rosa's Bakery", "Fresh bread daily"), column))
        assertFalse(passes(above("Fresh bread daily", "Rosa's Bakery"), column))
        assertFalse(passes(leftOf("Rosa's Bakery", "Fresh bread daily"), column))
    }

    @Test fun rowIsLeftOfNotAbove() {
        val row = snapshot(text("Rosa's Bakery", 0f, left = 0f), text("Fresh bread daily", 0f, left = 100f))
        assertFalse(passes(above("Rosa's Bakery", "Fresh bread daily"), row))
        assertTrue(passes(leftOf("Rosa's Bakery", "Fresh bread daily"), row))
    }

    @Test fun overlapWithinTolerance() {
        val slight = snapshot(title, text("Fresh bread daily", top = 19.5f))
        assertTrue(passes(above("Rosa's Bakery", "Fresh bread daily"), slight))
        assertFalse(passes(above("Rosa's Bakery", "Fresh bread daily").copy(toleranceDp = 0f), slight))
        assertFalse(passes(above("Rosa's Bakery", "Fresh bread daily"), snapshot(title, text("Fresh bread daily", top = 18f))))
    }

    @Test fun orderNeedsTwoDifferentNodes() {
        // a node is never above itself, even when both selectors match it
        val one = snapshot(text("Rosa's Bakery", 0f))
        assertFalse(passes(Criterion.Above("o", "m", Selector(text = "Rosa's Bakery"), Selector(contains = true, text = "Bakery")), one))
    }

    @Test fun orderFailsWhenASideIsMissing() = assertFalse(passes(above("Rosa's Bakery", "Fresh bread daily"), snapshot(title)))

    @Test fun anyPairIsEnough() {
        val withExtra = snapshot(text("Rosa's Bakery", top = 100f), subtitle, text("Rosa's Bakery", top = 0f, left = 200f))
        assertTrue(passes(above("Rosa's Bakery", "Fresh bread daily"), withExtra))
    }

    @Test fun partialProgressListsEveryCriterionInOrder() {
        val criteria = listOf(
            Criterion.Exists("title", "d", Selector(text = "Rosa's Bakery"), message = "Rosa doesn't see her shop's name yet."),
            Criterion.Exists("subtitle", "d", Selector(text = "Fresh bread daily"), message = "The sign is missing \"Fresh bread daily\"."),
            Criterion.Above("order", "d", Selector(text = "Rosa's Bakery"), Selector(text = "Fresh bread daily"), message = "Rosa wants her name on top."),
        )
        val verdict = Evaluator.evaluate(criteria, snapshot(title))
        assertFalse(verdict.passed)
        assertEquals(
            listOf(
                CriterionResult("title", "d", true, null, null),
                CriterionResult("subtitle", "d", false, "I can't find \"Fresh bread daily\" on the screen.", "The sign is missing \"Fresh bread daily\"."),
                CriterionResult("order", "d", false, "I can't find \"Fresh bread daily\" on the screen.", "Rosa wants her name on top."),
            ),
            verdict.results,
        )
        val solved = Evaluator.evaluate(criteria, snapshot(title, subtitle))
        assertTrue(solved.passed)
        assertTrue(solved.results.all { it.detail == null && it.message == null })
    }

    // ---- generated failure details ----

    private fun detail(c: Criterion, s: SemanticsSnapshot) = Evaluator.evaluate(listOf(c), s).results.single().detail

    @Test fun missingDetail() =
        assertEquals("I can't find \"Rosa's Bakery\" on the screen.", detail(exists(Selector(text = "Rosa's Bakery")), snapshot(subtitle)))

    @Test fun hiddenDetail() = assertEquals(
        "\"Rosa's Bakery\" is there, but it has no size or is off the screen.",
        detail(exists(Selector(text = "Rosa's Bakery")), snapshot(text("Rosa's Bakery", top = 700f))),
    )

    @Test fun wrongCountDetail() {
        val buttons = snapshot(*Array(3) { text("B$it", top = it * 30f, role = "Button") })
        assertEquals("I see 3 matches for a button, but there should be 2.", detail(exists(Selector(role = "Button"), count = 2), buttons))
    }

    @Test fun presentDetail() = assertEquals(
        "\"Closed\" should not be on the screen.",
        detail(Criterion.Absent("c", "d", Selector(text = "Closed")), snapshot(text("Closed", 0f))),
    )

    private val below = Criterion.Below("order", "d", Selector(text = "Fresh bread daily"), Selector(text = "Rosa's Bakery"))

    @Test fun belowIsAboveSwapped() {
        assertTrue(passes(below, snapshot(title, subtitle)))
        assertFalse(passes(below, snapshot(text("Fresh bread daily", 0f), text("Rosa's Bakery", 20f))))
    }

    @Test fun reversedDetail() = assertEquals(
        "\"Fresh bread daily\" is above \"Rosa's Bakery\".",
        detail(below, snapshot(text("Fresh bread daily", 0f), text("Rosa's Bakery", 20f))),
    )

    @Test fun sideBySideDetail() = assertEquals(
        "\"Fresh bread daily\" and \"Rosa's Bakery\" are side by side; \"Fresh bread daily\" should be under \"Rosa's Bakery\".",
        detail(below, snapshot(text("Rosa's Bakery", 0f, left = 0f), text("Fresh bread daily", 0f, left = 100f))),
    )

    @Test fun overlappingDetail() = assertEquals(
        "\"Fresh bread daily\" and \"Rosa's Bakery\" overlap; \"Fresh bread daily\" should be under \"Rosa's Bakery\".",
        detail(below, snapshot(text("Rosa's Bakery", 0f), text("Fresh bread daily", 5f, left = 10f))),
    )

    @Test fun orderDetailForAMissingSide() =
        assertEquals("I can't find \"Fresh bread daily\" on the screen.", detail(below, snapshot(title)))

    private val orderRightOfName = Criterion.RightOf("button", "d", Selector(role = "Button", text = "Order"), Selector(text = "Tom's Flowers"))

    @Test fun rightOf() {
        val row = snapshot(text("Tom's Flowers", 0f, left = 0f), text("Order", 0f, left = 120f, role = "Button"))
        assertTrue(passes(orderRightOfName, row))
        val reversed = snapshot(text("Order", 0f, left = 0f, role = "Button"), text("Tom's Flowers", 0f, left = 120f))
        assertEquals("A button \"Order\" is to the left of \"Tom's Flowers\".", detail(orderRightOfName, reversed))
        val stacked = snapshot(text("Tom's Flowers", 0f), text("Order", 40f, role = "Button"))
        assertEquals(
            "A button \"Order\" and \"Tom's Flowers\" are stacked on top of each other; a button \"Order\" should be to the right of \"Tom's Flowers\".",
            detail(orderRightOfName, stacked),
        )
    }

    @Test fun buttonPhrase() =
        assertEquals("I can't find a button \"Order\" on the screen.", detail(exists(Selector(role = "Button", text = "Order")), snapshot(title)))

    @Test fun phrases() {
        assertEquals("\"Rosa's Bakery\"", Selector(text = "  Rosa's  Bakery").phrase())
        assertEquals("a text containing \"Bakery\"", Selector(text = "Bakery", contains = true).phrase())
        assertEquals("a button", Selector(role = "Button").phrase())
        assertEquals("a button containing \"Order\"", Selector(role = "Button", text = "Order", contains = true).phrase())
        assertEquals("an image described as \"logo\"", Selector(role = "Image", contentDescription = "logo").phrase())
        assertEquals("something described as \"logo\"", Selector(contentDescription = "logo").phrase())
        assertEquals("a radio button", Selector(role = "RadioButton").phrase())
    }

    @Test fun messageOnlyWhenFailed() {
        val c = Criterion.Exists("title", "The text \"Rosa's Bakery\" is on screen", Selector(text = "Rosa's Bakery"), message = "Where's my name?")
        val failed = Evaluator.evaluate(listOf(c), snapshot(subtitle)).results.single()
        assertEquals(CriterionResult("title", c.description, false, "I can't find \"Rosa's Bakery\" on the screen.", "Where's my name?"), failed)
        assertEquals(CriterionResult("title", c.description, true, null, null), Evaluator.evaluate(listOf(c), snapshot(title)).results.single())
    }

    // ---- criteria at a moment, and prints (customer-bot); records built by hand ----

    private val order = Action.Press("order", Selector(role = "Button", text = "Order"))
    private val name = Action.EnterText("name", Selector(text = "Your name"), "Rosa")
    private val bell = Criterion.Prints("bell", "d", "New order!", after = "order")
    private val sign = snapshot(text("Tom's Flowers", 0f), text("Order", 0f, left = 120f, role = "Button"))

    private fun pressed(printed: String, screen: SemanticsSnapshot = sign) = Attempt(Moment(sign, ""), listOf(Step(order, Outcome.Done(Moment(screen, printed)))))
    private fun result(c: Criterion, a: Attempt) = Evaluator.evaluate(listOf(c), a).results.single()

    @Test fun printsAfterAPress() = assertTrue(result(bell, pressed("New order!\n")).passed)

    @Test fun extraOutputStillPrints() = assertTrue(result(bell, pressed("Ding!\n  New   order! \n")).passed)

    @Test fun printedDuringTheFirstFrameIsNotThePress() {
        val attempt = Attempt(Moment(sign, "New order!\n"), listOf(Step(order, Outcome.Done(Moment(sign, "")))))
        assertEquals("A customer pressed \"Order\", but nothing was printed.", result(bell, attempt).detail)
        assertTrue(result(bell.copy(after = null), attempt).passed)
    }

    @Test fun somethingElsePrinted() = assertEquals(
        "A customer pressed \"Order\" and \"New oder!\" was printed, but I wanted \"New order!\".",
        result(bell, pressed("New oder!\n")).detail,
    )

    @Test fun printsOptions() {
        assertTrue(result(bell.copy(ignoreCase = true), pressed("NEW ORDER!")).passed)
        assertTrue(result(bell.copy(contains = true), pressed("Ding! New order! Ding!")).passed)
        assertFalse(result(bell, pressed("new order!")).passed)
    }

    @Test fun nothingPrintedWhenTheScreenAppeared() = assertEquals(
        "Nothing printed \"Hello\" when your screen appeared.",
        result(Criterion.Prints("hello", "d", "Hello"), Attempt.still(sign)).detail,
    )

    @Test fun existsAfterAStep() {
        val one = Criterion.Exists("one", "d", Selector(text = "Count: 1"), after = "order")
        val attempt = Attempt(Moment(snapshot(text("Count: 0", 0f)), ""), listOf(Step(order, Outcome.Done(Moment(snapshot(text("Count: 1", 0f)), "")))))
        assertTrue(result(one, attempt).passed)
        assertFalse(result(one.copy(after = null), attempt).passed)
    }

    @Test fun stepTargetMissingOrHidden() {
        val missing = Attempt(Moment(sign, ""), listOf(Step(order, Outcome.NotFound(hidden = false))))
        assertEquals("The customer couldn't find a button \"Order\" to press.", result(bell, missing).detail)
        val hidden = Attempt(Moment(sign, ""), listOf(Step(order, Outcome.NotFound(hidden = true))))
        assertEquals("The customer found a button \"Order\", but it has no size or is off the screen.", result(bell, hidden).detail)
    }

    @Test fun notReachedExplainsWhatStoppedTheCustomer() {
        val attempt = Attempt(Moment(sign, ""), listOf(Step(name, Outcome.NotFound(hidden = false)), Step(order, Outcome.NotReached)))
        assertEquals("The customer couldn't find \"Your name\" to type into.", result(bell, attempt).detail)
    }

    @Test fun typedDetail() {
        val greeted = Criterion.Prints("hi", "d", "Hi, Rosa", after = "name")
        val attempt = Attempt(Moment(sign, ""), listOf(Step(name, Outcome.Done(Moment(sign, "")))))
        assertEquals("A customer typed \"Rosa\" into \"Your name\", but nothing was printed.", result(greeted, attempt).detail)
    }

    @Test fun pressedNodeWithoutTextIsNamedByItsPhrase() {
        val press = Action.Press("go", Selector(role = "Button"))
        val attempt = Attempt(Moment(sign, ""), listOf(Step(press, Outcome.Done(Moment(sign, "")))))
        assertEquals("A customer pressed a button, but nothing was printed.", result(Criterion.Prints("p", "d", "Go", after = "go"), attempt).detail)
    }

    @Test fun messageShownWhenACustomerCriterionFails() {
        val failed = result(bell.copy(message = "My bell stayed quiet."), pressed(""))
        assertEquals("My bell stayed quiet.", failed.message)
    }
}
