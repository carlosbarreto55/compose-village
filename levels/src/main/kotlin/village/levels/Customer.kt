package village.levels

import village.api.Console
import village.preview.PreviewEngine
import java.util.concurrent.TimeUnit

/**
 * Performs a level's customer steps on the running attempt, after its first frame, and records what
 * happened for the evaluator (customer-bot design D4). The game and the self-checking level tests both
 * use it, so levels are tested exactly as they are played.
 */
object Customer {
    /** Where a performed step's output is in the attempt's console ([length] chars from [consoleOffset]), so the Console can label it. */
    data class Mark(val action: Action, val consoleOffset: Int, val length: Int)

    /** The attempt record, and a [Mark] per step performed. */
    data class Visit(val attempt: Attempt, val marks: List<Mark>)

    /**
     * Runs [actions] in order on [engine]'s loaded attempt. Blocks, so call it off the UI thread. Returns
     * null when there is no scene: nothing loaded, or the attempt crashed, before or during a step.
     *
     * Each step acts on the first visible node matching its selector in the screen the previous step
     * left. A step whose target isn't found is not performed, and the customer stops there.
     */
    fun visit(engine: PreviewEngine, actions: List<Action>, timeoutSeconds: Long = 10): Visit? {
        var screen = engine.snapshot().get(timeoutSeconds, TimeUnit.SECONDS) ?: return null
        // Everything printed so far was printed while the screen first appeared.
        val first = Moment(screen, Console.text())
        val steps = mutableListOf<Step>()
        val marks = mutableListOf<Mark>()
        for (action in actions) {
            if (steps.any { it.outcome !is Outcome.Done }) {
                steps += Step(action, Outcome.NotReached)
                continue
            }
            val target = Evaluator.target(screen, action.node)
            val node = target.node
            if (node == null) {
                steps += Step(action, Outcome.NotFound(target.hidden))
                continue
            }
            val result = when (action) {
                is Action.Press -> engine.press(node.id)
                is Action.EnterText -> engine.enterText(node.id, action.text)
            }.get(timeoutSeconds, TimeUnit.SECONDS)
            val after = result.snapshot ?: return null // crashed in the handler, or while rendering after it
            if (!result.found) {
                // Typing into something that isn't a text field: the customer can't find where to type.
                steps += Step(action, Outcome.NotFound(hidden = false))
                continue
            }
            steps += Step(action, Outcome.Done(Moment(after, result.printed)))
            marks += Mark(action, result.consoleStart, result.printed.length)
            screen = after
        }
        return Visit(Attempt(first, steps), marks)
    }
}
