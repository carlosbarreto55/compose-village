package village.app

import village.levels.Verdict
import kotlin.math.min

/** Typing speed of every villager line (README "Talking villagers"). */
const val CHARS_PER_SECOND = 40

/** While a line types, the mouth swaps between open and closed this often. */
const val MOUTH_FLAP_MILLIS = 120L

/** The game's clock for dialogue, in milliseconds; any monotonic clock works since only differences matter. */
fun dialogueNow(): Long = System.nanoTime() / 1_000_000

/**
 * One line typing out (talking-villagers D1): what is shown depends only on the time since [startedAt],
 * so speed never depends on frame rate. [finished] shows the whole line at once (a click, or reduced motion).
 */
data class Typewriter(val line: String, val startedAt: Long, val finished: Boolean = false) {
    /** Characters shown at [now]. */
    fun shown(now: Long): Int =
        if (finished) line.length else min(line.length.toLong(), (now - startedAt).coerceAtLeast(0) * CHARS_PER_SECOND / 1000).toInt()

    fun typing(now: Long): Boolean = shown(now) < line.length

    /** Whether the mouth is open at [now]: it flaps while typing and closes as soon as the line is out. */
    fun mouthOpen(now: Long): Boolean = typing(now) && ((now - startedAt) / MOUTH_FLAP_MILLIS) % 2 == 0L

    /** The whole line, now. */
    fun finish(): Typewriter = copy(finished = true)
}

/** The page being said in a multi-line dialogue, and its typewriter. */
data class DialogueState(val lines: List<String>, val page: Int, val typewriter: Typewriter, val reducedMotion: Boolean) {
    init {
        require(lines.isNotEmpty()) { "a dialogue needs at least one line" }
    }

    val isLast: Boolean get() = page == lines.lastIndex

    /** "2 of 4". */
    val pageLabel: String get() = "${page + 1} of ${lines.size}"

    /** On the last page with its line fully shown: only the dialogue's closing button is left. */
    fun atEnd(now: Long): Boolean = isLast && !typewriter.typing(now)

    /** A click, Space or Enter: finish the line, or start the next one; nothing once [atEnd]. */
    fun advance(now: Long): DialogueState = when {
        typewriter.typing(now) -> copy(typewriter = typewriter.finish())
        isLast -> this
        else -> copy(page = page + 1, typewriter = Typewriter(lines[page + 1], now, finished = reducedMotion))
    }

    companion object {
        fun start(lines: List<String>, now: Long, reducedMotion: Boolean) =
            DialogueState(lines, 0, Typewriter(lines.first(), now, finished = reducedMotion), reducedMotion)
    }
}

/** A villager's face (spec: Villager busts). */
enum class Expression { NEUTRAL, PUZZLED, DELIGHTED }

/** The villager card's face for the latest verdict: puzzled by failed requirements, delighted by a pass. */
fun expressionFor(verdict: Verdict?): Expression = when {
    verdict !is Verdict.Evaluated -> Expression.NEUTRAL
    verdict.passed -> Expression.DELIGHTED
    else -> Expression.PUZZLED
}

/** The big dialogue the controller wants open (talking-villagers D2): the level's intro or its celebration. */
data class BigDialogue(val kind: Kind, val lines: List<String>) {
    enum class Kind { INTRO, CELEBRATION }
}
