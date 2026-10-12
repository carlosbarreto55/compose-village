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

/** [message] is the criterion's player-facing message when it failed, null when it passed. */
data class CriterionResult(val id: String, val passed: Boolean, val message: String?)

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
        val visible = snapshot.nodes().filter { it.isVisibleIn(snapshot) }.toList()
        return Verdict.Evaluated(criteria.map { c ->
            val passed = check(c, visible)
            CriterionResult(c.id, passed, if (passed) null else c.message)
        })
    }

    private fun check(c: Criterion, visible: List<SnapshotNode>): Boolean = when (c) {
        is Criterion.Exists -> visible.count { c.node.matches(it) }.let { n -> if (c.count == null) n >= 1 else n == c.count }
        is Criterion.Absent -> visible.none { c.node.matches(it) }
        is Criterion.Above -> somePair(visible, c.a, c.b) { a, b -> a.bounds.bottom <= b.bounds.top + c.toleranceDp }
        is Criterion.LeftOf -> somePair(visible, c.a, c.b) { a, b -> a.bounds.right <= b.bounds.left + c.toleranceDp }
    }

    /** Some node matching [a] and some *other* node matching [b] satisfy [ordered]. */
    private inline fun somePair(visible: List<SnapshotNode>, a: Selector, b: Selector, ordered: (SnapshotNode, SnapshotNode) -> Boolean): Boolean {
        val bs = visible.filter { b.matches(it) }
        return visible.any { x -> a.matches(x) && bs.any { y -> x !== y && ordered(x, y) } }
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
