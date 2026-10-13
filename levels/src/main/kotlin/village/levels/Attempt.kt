package village.levels

import village.preview.SemanticsSnapshot

/**
 * Everything the evaluator judges (customer-bot design D3): the first frame, then each of the level's
 * customer steps, in order. Plain data, so tests build it by hand.
 */
data class Attempt(val first: Moment, val steps: List<Step> = emptyList()) {
    companion object {
        /** An attempt with no customer: only its first frame, printing nothing. */
        fun still(snapshot: SemanticsSnapshot) = Attempt(Moment(snapshot, ""))
    }
}

/** The screen at one point of an attempt, and what the attempt printed on the way there. */
data class Moment(val snapshot: SemanticsSnapshot, val printed: String)

/** One customer step, and what came of it. */
data class Step(val action: Action, val outcome: Outcome)

sealed interface Outcome {
    /** The step was performed; [moment] is the screen after it and what it printed. */
    data class Done(val moment: Moment) : Outcome

    /** Not performed: no visible node matched its target. [hidden] when one matched but has no size or is off screen. */
    data class NotFound(val hidden: Boolean) : Outcome

    /** Not performed, because an earlier step's target wasn't found. */
    data object NotReached : Outcome
}
