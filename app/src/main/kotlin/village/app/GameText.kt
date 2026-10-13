package village.app

import village.levels.Level
import village.levels.Verdict

/** What a villager says after a Run, and whether it's a happy line (the bubble's outline turns green). */
data class Reaction(val text: String, val happy: Boolean = false)

/** The game's own lines, the ones that are not level data (design D7). */
object GameText {
    const val DID_NOT_COMPILE = "I can't see anything yet. Something in the code needs fixing."
    const val CRASHED = "Oh! The sign fell down while I was looking at it."
    const val SEE_CHECKLIST = "Almost! Have a look at my checklist."
    const val THANKS = "Thank you! It's perfect."

    /** "This is Tom's sketch. Press Run to build yours on top of it." */
    fun ghostCaption(name: String) = "This is $name's sketch. Press Run to build yours on top of it."
    fun sketchTab(name: String) = "$name's sketch"
    fun checklistTitle(name: String) = "$name's checklist"
    fun shopOpen(shop: String) = "$shop is open!"
    fun wishesCameTrue(count: Int, name: String) = "All $count of $name's wishes came true."
    const val COMING_SOON = "More of Main Street is coming soon."
    fun finishedIn(tries: Int) = "You finished this in $tries ${if (tries == 1) "try" else "tries"}."
    const val PLAY_AGAIN = "Play again"
    const val START_OVER = "Start over"
    const val STREET = "Main Street"
    const val CONSOLE_EMPTY = "Anything your code prints shows up here after Run."
}

/** The villager's reaction bubble for the latest [verdict], or null before the first Run (design D7). */
fun reactionFor(level: Level, verdict: Verdict?): Reaction? {
    return when (verdict) {
        null -> null
        is Verdict.NotRun -> when (verdict.reason) {
            Verdict.NotRun.Reason.DID_NOT_COMPILE -> Reaction(GameText.DID_NOT_COMPILE)
            Verdict.NotRun.Reason.CRASHED -> Reaction(GameText.CRASHED)
        }
        is Verdict.Evaluated ->
            if (verdict.passed) Reaction(level.thanks ?: GameText.THANKS, happy = true)
            else Reaction(verdict.results.firstOrNull { !it.passed && it.message != null }?.message ?: GameText.SEE_CHECKLIST)
    }
}

/** The completion card's Lines: the player's code without blank, `import` and `package` lines. */
fun countCodeLines(code: String): Int = code.lineSequence().map { it.trim() }
    .count { it.isNotEmpty() && !it.startsWith("import ") && !it.startsWith("package ") }
