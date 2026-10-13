package village.app

import village.levels.Action
import village.levels.Level
import village.levels.Selector
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

    // The customer (customer-bot design D6).
    const val CONSOLE_EMPTY_CUSTOMER = "A customer will press your buttons here after Run."
    const val NOTHING_PRINTED = "(nothing printed)"
    /** `customer › pressed [Order]`, `customer › typed "Rosa" into [Your name]`. */
    fun customerLine(action: Action) = when (action) {
        is Action.Press -> "customer › pressed [${action.node.name()}]"
        is Action.EnterText -> "customer › typed \"${action.text}\" into [${action.node.name()}]"
    }
    /** The preview's caption after the customer's last step: "A customer pressed Order". */
    fun customerCaption(action: Action) = when (action) {
        is Action.Press -> "A customer pressed ${action.node.name()}"
        is Action.EnterText -> "A customer typed into ${action.node.name()}"
    }
    /** A step's target as the Console names it: its text, or its role when it has none. */
    private fun Selector.name() = text?.trim() ?: role ?: contentDescription?.trim() ?: "something"

    // The home screen (home-screen spec).
    const val HOME_TAGLINE = "A cozy game for learning Jetpack Compose"
    const val WELCOME = "Welcome to Compose Village"
    const val WELCOME_INTRO = "The shops on Main Street need signs. Write real Kotlin and Compose code to build each one, and watch the street light up."
    const val WELCOME_BACK = "Welcome back"
    const val EVERY_SHOP_OPEN = "Every shop on Main Street is open."
    fun stillWaiting(name: String) = "$name is still waiting for a sign."
    fun openAndWaiting(shop: String, name: String) = "$shop is open. ${stillWaiting(name)}"
    fun shopsOpen(open: Int, total: Int) = "$open of $total shops open"
    const val STATUS_OPEN = "Open"
    const val STATUS_START_HERE = "Start here"
    const val STATUS_UP_NEXT = "Up next"
    const val STATUS_NOT_YET = "Not open yet"
    const val STATUS_COMING_SOON = "Coming soon"
    const val SANDBOX = "The Sandbox"
    const val SANDBOX_PLACE = "End of the street"
    const val SANDBOX_STATUS = "Free play"
    const val SANDBOX_BLURB = "A free editor with every panel. Nothing to pass, just try things."
    const val FIRST_VILLAGER = "Your first villager"
    const val STARTER_READY_FIRST = "Starter code is ready for you. No setup needed."
    const val PICK_UP = "Pick up where you left off"
    const val CODE_SAVED = "Your code is saved"
    const val STARTER_READY = "Starter code is ready"
    fun wishes(met: Int, total: Int) = "$met of $total wishes"
    const val OR_PICK_ANY = "Or pick any shop on the street."
    const val ALL_OPEN_TITLE = "Every shop is open"
    const val ALL_OPEN_BODY = "You built every sign on Main Street. More shops are coming soon."
    const val OPEN_SHOPS = "Open shops"
    fun shopStats(lines: Int?, tries: Int) = listOfNotNull(lines?.let { "$it ${if (it == 1) "line" else "lines"}" }, "$tries ${if (tries == 1) "try" else "tries"}").joinToString(" · ")
    const val VISIT = "Visit"
    const val OPEN = "Open"
    const val HOW_A_LEVEL_WORKS = "How a level works"
    val HOW_STEPS = listOf(
        "Read the wish" to "Each villager asks for a sign and brings a checklist of what it needs.",
        "Write the code" to "Real Kotlin and Compose, starting from code that already compiles.",
        "Run it" to "When every wish comes true, the shop opens and its sign lights up.",
    )
    const val JUST_EXPERIMENT = "Just want to experiment?"
    const val OPEN_THE_SANDBOX = "Open the Sandbox"
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
