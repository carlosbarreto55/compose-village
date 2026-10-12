package village.levels

import village.preview.SemanticsSnapshot
import village.preview.SnapshotNode
import kotlin.math.max
import kotlin.math.min

/** The outcome of checking one attempt against a level. */
sealed interface Verdict {
    /** The attempt ran and every criterion was checked, in the level's order. */
    data class Evaluated(val results: List<CriterionResult>) : Verdict {
        val passed get() = results.all { it.passed }
    }

    /** The attempt never produced a UI, so no criterion was checked. */
    data class NotRun(val reason: Reason) : Verdict {
        enum class Reason { DID_NOT_COMPILE, CRASHED }
    }
}

/**
 * One checklist line. When the criterion failed, [detail] explains why (generated, see [Failure])
 * and [message] is the criterion's optional villager line; both are null when it passed.
 */
data class CriterionResult(val id: String, val description: String, val passed: Boolean, val detail: String?, val message: String?)

/** Why a criterion failed (design D2). [detail] phrases it for the player using the selectors, never code. */
sealed interface Failure {
    val detail: String

    /** No node matches. */
    data class Missing(val node: Selector) : Failure {
        override val detail get() = "I can't find ${node.phrase()} on the screen."
    }

    /** A node matches, but it has no size or is outside the viewport. */
    data class Hidden(val node: Selector) : Failure {
        override val detail get() = "${node.phrase().capitalized()} is there, but it has no size or is off the screen."
    }

    data class WrongCount(val node: Selector, val seen: Int, val expected: Int) : Failure {
        override val detail get() = "I see $seen matches for ${node.phrase()}, but there should be $expected."
    }

    /** An `absent` node is on screen. */
    data class Present(val node: Selector) : Failure {
        override val detail get() = "${node.phrase().capitalized()} should not be on the screen."
    }

    /** The two nodes are in the opposite order. [a] and [b] are the author's, so the sentence reads like the description. */
    data class Reversed(val a: Selector, val b: Selector, val relation: Relation) : Failure {
        override val detail get() = "${a.phrase().capitalized()} is ${relation.opposite.words} ${b.phrase()}."
    }

    /** Both nodes are on screen, but neither order holds: side by side for a vertical relation, stacked for a horizontal one, or overlapping. */
    data class NotInLine(val a: Selector, val b: Selector, val relation: Relation, val overlapping: Boolean) : Failure {
        override val detail get(): String {
            val how = when {
                overlapping -> "overlap"
                relation.vertical -> "are side by side"
                else -> "are stacked on top of each other"
            }
            return "${a.phrase().capitalized()} and ${b.phrase()} $how; ${a.phrase()} should be ${relation.words} ${b.phrase()}."
        }
    }
}

/** Where an order criterion wants [Criterion.Order.a] relative to [Criterion.Order.b]. */
enum class Relation(val words: String, val vertical: Boolean) {
    ABOVE("above", true), BELOW("under", true), LEFT_OF("to the left of", false), RIGHT_OF("to the right of", false);

    val opposite get() = when (this) {
        ABOVE -> BELOW
        BELOW -> ABOVE
        LEFT_OF -> RIGHT_OF
        RIGHT_OF -> LEFT_OF
    }
}

/**
 * Checks acceptance criteria against a semantics snapshot (design D2/D5). A pure function of its
 * inputs: it never touches the scene, the player's classes or the player thread.
 */
object Evaluator {
    /**
     * The verdict for an attempt at [level] (design D6): [compiled] is false when the attempt failed
     * to compile or load, and [snapshot] is null when it has no scene because it crashed.
     */
    fun verdict(level: Level, compiled: Boolean, snapshot: SemanticsSnapshot?): Verdict = when {
        !compiled -> Verdict.NotRun(Verdict.NotRun.Reason.DID_NOT_COMPILE)
        snapshot == null -> Verdict.NotRun(Verdict.NotRun.Reason.CRASHED)
        else -> evaluate(level, snapshot)
    }

    fun evaluate(level: Level, snapshot: SemanticsSnapshot): Verdict.Evaluated = evaluate(level.criteria, snapshot)

    fun evaluate(criteria: List<Criterion>, snapshot: SemanticsSnapshot): Verdict.Evaluated {
        val nodes = Nodes(snapshot.nodes().toList(), snapshot.nodes().filter { it.isVisibleIn(snapshot) }.toList())
        return Verdict.Evaluated(criteria.map { c ->
            val failure = nodes.check(c)
            CriterionResult(c.id, c.description, failure == null, failure?.detail, if (failure == null) null else c.message)
        })
    }

    /** Every node of the snapshot, and the visible ones that criteria consider. */
    private class Nodes(val all: List<SnapshotNode>, val visible: List<SnapshotNode>) {
        fun matching(s: Selector) = visible.filter { s.matches(it) }

        /** Missing or Hidden if nothing visible matches [s], else null. */
        fun absence(s: Selector): Failure? = when {
            visible.any { s.matches(it) } -> null
            all.any { s.matches(it) } -> Failure.Hidden(s)
            else -> Failure.Missing(s)
        }

        /** Null when [c] passes, otherwise why it failed. */
        fun check(c: Criterion): Failure? = when (c) {
            is Criterion.Exists -> absence(c.node) ?: matching(c.node).size.let { n ->
                if (c.count == null || n == c.count) null else Failure.WrongCount(c.node, n, c.count)
            }
            is Criterion.Absent -> if (matching(c.node).isEmpty()) null else Failure.Present(c.node)
            is Criterion.Order -> checkOrder(c)
        }

        private fun checkOrder(c: Criterion.Order): Failure? {
            absence(c.a)?.let { return it }
            absence(c.b)?.let { return it }
            val relation = when (c) {
                is Criterion.Above -> Relation.ABOVE
                is Criterion.Below -> Relation.BELOW
                is Criterion.LeftOf -> Relation.LEFT_OF
                is Criterion.RightOf -> Relation.RIGHT_OF
            }
            val (aNodes, bNodes) = matching(c.a) to matching(c.b)
            fun holds(r: Relation) = aNodes.any { x -> bNodes.any { y -> x !== y && x.isPlaced(r, y, c.toleranceDp) } }
            return when {
                holds(relation) -> null
                holds(relation.opposite) -> Failure.Reversed(c.a, c.b, relation)
                else -> {
                    // Neither order: describe the first pair, as side by side / stacked, or overlapping.
                    val x = aNodes.first()
                    val y = bNodes.firstOrNull { it !== x } ?: return Failure.Missing(c.b) // a and b only match the same node
                    val other = if (relation.vertical) Relation.LEFT_OF else Relation.ABOVE
                    val apart = x.isPlaced(other, y, c.toleranceDp) || x.isPlaced(other.opposite, y, c.toleranceDp)
                    Failure.NotInLine(c.a, c.b, relation, overlapping = !apart)
                }
            }
        }
    }

    /** Whether this node is placed [r] relative to [other], within [toleranceDp]. */
    private fun SnapshotNode.isPlaced(r: Relation, other: SnapshotNode, toleranceDp: Float) = when (r) {
        Relation.ABOVE -> bounds.bottom <= other.bounds.top + toleranceDp
        Relation.BELOW -> other.bounds.bottom <= bounds.top + toleranceDp
        Relation.LEFT_OF -> bounds.right <= other.bounds.left + toleranceDp
        Relation.RIGHT_OF -> other.bounds.right <= bounds.left + toleranceDp
    }

    /** Non-zero size, and overlapping the viewport with non-zero area: hidden and off-screen nodes don't count. */
    private fun SnapshotNode.isVisibleIn(snapshot: SemanticsSnapshot): Boolean {
        val b = bounds
        if (b.width <= 0 || b.height <= 0) return false
        val overlapWidth = min(b.right, snapshot.viewport.widthDp) - max(b.left, 0f)
        val overlapHeight = min(b.bottom, snapshot.viewport.heightDp) - max(b.top, 0f)
        return overlapWidth > 0 && overlapHeight > 0
    }
}

/** Whether [node] matches every field this selector sets. */
fun Selector.matches(node: SnapshotNode): Boolean =
    (text == null || matchesText(text, node.texts)) &&
        (role == null || role == node.role) &&
        (contentDescription == null || matchesText(contentDescription, node.contentDescriptions))

/** Matches any single value, or all values joined by spaces (a merged node holds one value per merged text). */
private fun Selector.matchesText(expected: String, values: List<String>): Boolean {
    val want = normalize(expected)
    val candidates = if (values.size > 1) values + values.joinToString(" ") else values
    return candidates.any { value ->
        val have = normalize(value)
        if (contains) have.contains(want, ignoreCase) else have.equals(want, ignoreCase)
    }
}

private val WHITESPACE = Regex("\\s+")

/** Trims and collapses every run of whitespace into one space. */
internal fun normalize(text: String) = text.trim().replace(WHITESPACE, " ")

private val ROLE_WORDS = mapOf(
    "Button" to "button", "Checkbox" to "checkbox", "Switch" to "switch", "RadioButton" to "radio button", "Tab" to "tab",
    "Image" to "image", "DropdownList" to "dropdown list", "ValuePicker" to "value picker", "Carousel" to "carousel",
)

/** How the player would name what this selector matches: `"Rosa's Bakery"`, `a button "Order"`, `an image described as "logo"`. */
internal fun Selector.phrase(): String {
    val quoted = text?.let { "\"${normalize(it)}\"" }
    val described = contentDescription?.let { "described as \"${normalize(it)}\"" }
    val noun = role?.let { ROLE_WORDS[it] ?: it }
    val parts = when {
        noun != null -> listOfNotNull(article(noun) + " " + noun, quoted?.let { if (contains) "containing $it" else it }, described)
        quoted != null -> listOfNotNull(if (contains) "a text containing $quoted" else quoted, described)
        else -> listOf("something", described!!)
    }
    return parts.joinToString(" ")
}

private fun article(noun: String) = if (noun.first() in "aeiou") "an" else "a"

private fun String.capitalized() = replaceFirstChar { it.uppercase() }
