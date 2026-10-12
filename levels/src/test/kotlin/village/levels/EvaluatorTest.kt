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
                CriterionResult("title", true, null),
                CriterionResult("subtitle", false, "The sign is missing \"Fresh bread daily\"."),
                CriterionResult("order", false, "Rosa wants her name on top."),
            ),
            verdict.results,
        )
        assertTrue(Evaluator.evaluate(criteria, snapshot(title, subtitle)).passed)
    }
}
